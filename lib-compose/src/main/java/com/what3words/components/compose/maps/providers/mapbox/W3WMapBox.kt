package com.what3words.components.compose.maps.providers.mapbox

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.mapbox.common.toValue
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.extension.compose.style.rememberStyleState
import com.mapbox.maps.extension.compose.style.standard.StandardStyleConfigurationState
import com.mapbox.maps.extension.compose.style.styleImportsConfig
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.toCameraOptions
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.mapper.toMapBoxMapType
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WMapboxCameraState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val MAPBOX_MIN_ZOOM_LEVEL = 3.0
private const val MAPBOX_MAX_ZOOM_PITCH = 60.0
private const val MAPBOX_DEFAULT_CAMERA_PADDING = 10.0

// This is used to calculate the padding for the camera when the user's padding is too large.
// It ensures that at least 1/3 of the map width is available for the content.
private const val FALLBACK_PADDING_RATIO = 3

/**
 * A composable function that displays a What3Words (W3W) map using the Mapbox Maps SDK for Android.
 *
 * ## Dual-pane / split-screen gesture fix
 *
 * On automotive dual-pane displays (e.g. Hyundai/Kia infotainment systems), the Mapbox SDK's
 * internal gesture handler uses [MotionEvent.getRawX]/[MotionEvent.getRawY] (display-absolute
 * coordinates) for focal point calculations. When the app runs in a secondary pane, `getRawX/Y`
 * includes the pane's display offset (e.g. ~860–2880px depending on the vehicle configuration)
 * while `getX/Y` correctly returns view-relative coordinates. The [MapView] may also report
 * `getLocationOnScreen() = (0, 0)` even when positioned on the right pane, preventing the SDK
 * from compensating for the offset internally.
 *
 * This causes drag and pinch-to-zoom gestures to malfunction (tap still works because it uses a
 * different code path). Fast flings may partially work due to velocity-based detection.
 *
 * **Workaround**: An [android.view.View.OnTouchListener] intercepts all touch events and creates
 * corrected [MotionEvent] copies (via [createCorrectedMotionEvent]) where `getRawX() == getX()`
 * and `getRawY() == getY()`, then dispatches the corrected event to [MapView.onTouchEvent].
 * This is safe for single-pane displays where raw already equals local coordinates.
 *
 * @param modifier Modifier for styling and layout of the map view.
 * @param layoutConfig Configuration for the map's layout, such as padding and content alignment.
 * @param mapConfig Configuration for the map's appearance, such as map type and zoom controls.
 * @param mapColor Configuration for the map's colors for markers, grid lines, etc.
 * @param state The [W3WMapState] object that holds the state of the map.
 * @param content Optional composable content to be displayed on the map, such as markers or overlays.
 * @param onMarkerClicked Callback invoked when the user clicks on a marker.
 * @param onMapClicked Callback invoked when the user clicks on the map.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 * @param onMapProjectionUpdated Callback invoked when the map projection is updated.
 * @param onMapLoaded Callback invoked when the Mapbox map has finished loading and is ready for use.
 * @param onGridDrawn Callback invoked when the grid overlay is rendered at a zoom level
 *   where grid lines are enabled.
 *
 */
@Composable
fun W3WMapBox(
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    mapColor: W3WMapDefaults.MapColor,
    state: W3WMapState,
    onMarkerClicked: (W3WMarker) -> Unit,
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    modifier: Modifier = Modifier,
    onMapProjectionUpdated: ((W3WMapProjection) -> Unit)? = null,
    onMapLoaded: (() -> Unit)? = null,
    onGridDrawn: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    var mapView: MapView? by remember {
        mutableStateOf(null)
    }

    val mapViewportState = (state.cameraState as W3WMapboxCameraState).cameraState

    LaunchedEffect(mapViewportState.cameraState) {
        snapshotFlow { mapViewportState.cameraState }
            .filterNotNull()
            .onEach { currentCameraState ->
                mapView?.mapboxMap?.let { mapboxMap ->
                    updateGridBound(
                        mapboxMap,
                        mapConfig.gridLineConfig,
                        onCameraBoundUpdate = { gridBound, visibleBound ->
                            state.cameraState.gridBound = gridBound
                            state.cameraState.visibleBound = visibleBound
                            onCameraUpdated(state.cameraState)
                        }
                    )
                }
            }.launchIn(this)
    }


    val mapState = rememberMapState()

    LaunchedEffect(state.isMyLocationEnabled) {
        mapView?.let {
            it.location.updateSettings {
                enabled = state.isMyLocationEnabled
            }
        }
    }

    LaunchedEffect(state.isMapGestureEnable) {
        state.isMapGestureEnable.let {
            mapState.gesturesSettings = GesturesSettings {
                pitchEnabled = it
                rotateEnabled = it
                scrollEnabled = it
                quickZoomEnabled = it
                pinchScrollEnabled = it
                doubleTapToZoomInEnabled = it
                doubleTouchToZoomOutEnabled = it
            }
        }
    }

    LaunchedEffect(state.cameraState.cameraForCoordinates) {
        state.cameraState.cameraForCoordinates?.let { points ->
            mapView?.let { mapView ->
                mapView.mapboxMap.also { map ->
                    // Set the camera based on coordinates with individual padding for each side
                    val mapWidth = map.getSize().width.toDouble()
                    val targetPadding = state.cameraState.cameraPadding.toDouble()
                    // If the target padding is too large (taking up more than the full width),
                    // fallback to using a portion of the width as padding to ensure content visibility.
                    val padding = if (targetPadding * 2 >= mapWidth) {
                        Log.w(
                            "W3WMapBox",
                            "The padding provided is too large for the map width. Falling back to using 1/$FALLBACK_PADDING_RATIO of the map width as padding."
                        )
                        mapWidth / FALLBACK_PADDING_RATIO
                    } else {
                        targetPadding
                    }

                    map.cameraForCoordinates(
                        points,
                        CameraOptions.Builder().build(),
                        EdgeInsets(
                            0.0,
                            padding,
                            0.0,
                            padding
                        ),
                        null,
                        null
                    ) { options ->
                        // Once the camera options are set, clear the coordinates state
                        state.cameraState.cameraForCoordinates = null

                        // Apply the new camera options to the map
                        map.setCamera(options)
                    }
                }
            }
        }
    }

    val movableContent = remember {
        movableContentOf { content?.let { it() } }
    }

    MapboxMap(
        modifier = modifier,
        mapState = mapState,
        mapViewportState = mapViewportState,
        logo = {
            Logo(
                modifier = Modifier.padding(layoutConfig.contentPadding)
            )
        },
        scaleBar = {
            if (mapConfig.isScaleBarEnabled) {
                ScaleBar(
                    modifier = Modifier.padding(layoutConfig.contentPadding)
                )
            }
        },
        compass = {
            if (mapConfig.isCompassButtonEnabled) {
                Compass(
                    modifier = Modifier.padding(layoutConfig.contentPadding)
                )
            }
        },
        attribution = {
            Attribution(
                modifier = Modifier.padding(layoutConfig.contentPadding)
            )
        },
        onMapClickListener = {
            onMapClicked(W3WCoordinates(it.latitude(), it.longitude()))
            true
        },
        style = {
            GenericStyle(
                style = state.mapType.toMapBoxMapType(),
                styleState = rememberStyleState {
                    styleImportsConfig = styleImportsConfig {
                        importConfig(importId = "basemap") {
                            with(StandardStyleConfigurationState().apply {
                                show3dObjects = BooleanValue(mapConfig.isBuildingEnable)
                            }) {
                                config(
                                    "show3dObjects",
                                    show3dObjects.value
                                )
                                config(
                                    "lightPreset",
                                    (if (state.isDarkMode) "night" else "day").toValue()
                                )
                            }
                        }
                    }
                }
            )
        }
    ) {
        MapEffect(Unit) {
            val cameraBounds = CameraBoundsOptions.Builder()
                // Zoom out to continent level only, prevent zooming to the Earth. Zoom levels detail: https://docs.mapbox.com/help/glossary/zoom-level/
                .minZoom(MAPBOX_MIN_ZOOM_LEVEL)
                .maxPitch(MAPBOX_MAX_ZOOM_PITCH)
                .build()

            mapView = it.also {
                it.location.updateSettings {
                    enabled = state.isMyLocationEnabled
                    locationPuck = createDefault2DPuck(withBearing = false)
                }
                it.mapboxMap.setBounds(cameraBounds)
            }

            // Fix for dual-pane/split-screen gesture handling.
            // See class KDoc for full explanation.
            @Suppress("ClickableViewAccessibility")
            it.setOnTouchListener { view, event ->
                val correctedEvent = createCorrectedMotionEvent(event)
                view.onTouchEvent(correctedEvent)
                correctedEvent.recycle()
                true
            }

            it.mapboxMap.subscribeMapIdle {
                if (state.cameraState.isCameraMoving) {
                    state.cameraState.isCameraMoving = false
                    onCameraUpdated(state.cameraState)
                }
            }

            it.mapboxMap.subscribeCameraChanged {
                if (!state.cameraState.isCameraMoving) {
                    state.cameraState.isCameraMoving = true
                    onCameraUpdated(state.cameraState)
                }
            }

            if (mapConfig.buttonConfig.isRecallFeatureEnabled || onMapProjectionUpdated != null) {
                mapView?.mapboxMap?.let { map ->
                    onMapProjectionUpdated?.invoke(W3WMapBoxMapProjection(map))
                }
            }

            onMapLoaded?.invoke()
        }

        W3WMapBoxDrawer(
            state = state,
            mapConfig = mapConfig,
            mapColor = mapColor,
            onMarkerClicked = onMarkerClicked,
            onGridDrawn = onGridDrawn,
        )

        movableContent()
    }
}

/**
 * Updates the grid bound based on the current camera position in the MapBox map.
 *
 * @param mapboxMap The MapboxMap instance to get coordinates from.
 * @param gridLinesConfig Configuration for the grid lines, including scale.
 * @param onCameraBoundUpdate Callback that receives the calculated grid bound and visible bound rectangles.
 *                           The grid bound is scaled according to the configuration, while the visible bound
 *                           represents the actual visible area on the map.
 */
fun updateGridBound(
    mapboxMap: MapboxMap,
    gridLinesConfig: W3WMapDefaults.GridLinesConfig,
    onCameraBoundUpdate: (gridBound: W3WRectangle, visibleBound: W3WRectangle) -> Unit,
) {
    val scale = gridLinesConfig.gridScale
    val bounds = mapboxMap
        .coordinateBoundsForCamera(mapboxMap.cameraState.toCameraOptions())
    val center = bounds.center()
    val finalNELat =
        ((scale * (bounds.northeast.latitude() - center.latitude()) + center.latitude()))
    val finalNELng =
        ((scale * (bounds.northeast.longitude() - center.longitude()) + center.longitude()))
    val finalSWLat =
        ((scale * (bounds.southwest.latitude() - center.latitude()) + center.latitude()))
    val finalSWLng =
        ((scale * (bounds.southwest.longitude() - center.longitude()) + center.longitude()))

    val gridBound = W3WRectangle(
        southwest = W3WCoordinates(
            finalSWLat,
            finalSWLng
        ),
        northeast = W3WCoordinates(
            finalNELat,
            finalNELng
        )
    )

    val visibleBound = W3WRectangle(
        southwest = W3WCoordinates(
            bounds.southwest.latitude(),
            bounds.southwest.longitude()
        ),
        northeast = W3WCoordinates(
            bounds.northeast.latitude(),
            bounds.northeast.longitude()
        )
    )

    onCameraBoundUpdate.invoke(gridBound, visibleBound)
}

/**
 * Creates a corrected [MotionEvent] where raw coordinates (getRawX/getRawY) match
 * the view-local coordinates (getX/getY).
 *
 * This fixes an issue on automotive dual-pane displays where getRawX/getRawY returns
 * display-absolute coordinates (offset by the pane position, e.g. ~2880px) while
 * getX/getY correctly returns view-relative coordinates. Mapbox's gesture handler
 * uses getRawX/getRawY for focal point calculations, which breaks gesture handling
 * when the app window has a non-zero display offset.
 *
 * By creating a new MotionEvent from PointerCoords (which contain view-local positions),
 * both raw and cooked coordinates will be identical and view-relative.
 *
 * @param event The original MotionEvent with potentially mismatched raw/local coordinates
 * @return A new MotionEvent where getRawX() == getX() and getRawY() == getY()
 */
private fun createCorrectedMotionEvent(event: MotionEvent): MotionEvent {
    val pointerCount = event.pointerCount
    val pointerProperties = Array(pointerCount) { i ->
        MotionEvent.PointerProperties().also { props ->
            event.getPointerProperties(i, props)
        }
    }
    val pointerCoords = Array(pointerCount) { i ->
        MotionEvent.PointerCoords().also { coords ->
            event.getPointerCoords(i, coords)
        }
    }
    return MotionEvent.obtain(
        event.downTime,
        event.eventTime,
        event.action,
        pointerCount,
        pointerProperties,
        pointerCoords,
        event.metaState,
        event.buttonState,
        event.xPrecision,
        event.yPrecision,
        event.deviceId,
        event.edgeFlags,
        event.source,
        event.flags
    )
}