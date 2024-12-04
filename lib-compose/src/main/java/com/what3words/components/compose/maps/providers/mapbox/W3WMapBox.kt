package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.toCameraOptions
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.mapper.toMapBoxMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WMapboxCameraState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val MAPBOX_MIN_ZOOM_LEVEL = 3.0

/**
 * A composable function that displays a What3Words (W3W) map using the Mapbox Maps SDK for Android.
 *
 * @param modifier Modifier for styling and layout of the map view.
 * @param layoutConfig Configuration for the map's layout, such as padding and content alignment.
 * @param mapConfig Configuration for the map's appearance, such as map type and zoom controls.
 * @param state The [W3WMapState] object that holds the state of the map.
 * @param content Optional composable content to be displayed on the map, such as markers or overlays.
 * @param onMapClicked Callback invoked when the user clicks on the map.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 *
 */
@Composable
fun W3WMapBox(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    state: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMarkerClicked: (W3WMarker) -> Unit,
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    onMapProjectionUpdated: (W3WMapProjection) -> Unit,
) {
    var mapView: MapView? by remember {
        mutableStateOf(null)
    }

    val mapViewportState = (state.cameraState as W3WMapboxCameraState).cameraState

    var lastProcessedCameraState by remember { mutableStateOf(mapViewportState.cameraState) }

    LaunchedEffect(mapViewportState) {
        snapshotFlow { mapViewportState.cameraState }
            .filterNotNull()
            .onEach { currentCameraState ->
                mapView?.mapboxMap?.let { mapboxMap ->
                    updateGridBound(
                        mapboxMap,
                        mapConfig.gridLineConfig,
                        onGridBoundUpdate = { newBound ->
                            lastProcessedCameraState = currentCameraState
                            val newCameraState = W3WMapboxCameraState(mapViewportState)
                            newCameraState.gridBound = newBound
                            onCameraUpdated(newCameraState)
                        }
                    )
//                    if (mapConfig.buttonConfig.isRecallButtonEnabled) {
//                        onMapProjectionUpdated(W3WMapBoxMapProjection(mapboxMap))
//                    }
                }
            }.launchIn(this)
    }


    val mapState = rememberMapState()

    val mapStyle = remember(state.mapType, state.isDarkMode) {
        state.mapType.toMapBoxMapType(state.isDarkMode)
    }

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

    MapboxMap(
        modifier = modifier,
        mapState = mapState,
        mapViewportState = mapViewportState,
        onMapClickListener = {
            onMapClicked(W3WCoordinates(it.latitude(), it.longitude()))
            true
        },
        style = {
            GenericStyle(
                style = mapStyle,
            )
        }
    ) {
        MapEffect(Unit) {
            val cameraBounds = CameraBoundsOptions.Builder()
                // Zoom out to continent level only, prevent zooming to the Earth. Zoom levels detail: https://docs.mapbox.com/help/glossary/zoom-level/
                .minZoom(MAPBOX_MIN_ZOOM_LEVEL)
                .build()

            mapView = it.also {
                it.location.updateSettings {
                    enabled = state.isMyLocationEnabled
                    locationPuck = createDefault2DPuck(withBearing = false)
                }
                it.mapboxMap.setBounds(cameraBounds)
            }
            mapView?.mapboxMap?.let { map ->
                onMapProjectionUpdated(W3WMapBoxMapProjection(map))
            }
        }

        W3WMapBoxDrawer(state, mapConfig, onMarkerClicked)
        content?.invoke()
    }
}

private fun updateGridBound(
    mapboxMap: MapboxMap,
    gridLinesConfig: W3WMapDefaults.GridLinesConfig,
    onGridBoundUpdate: (W3WRectangle) -> Unit,
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

    val box = W3WRectangle(
        W3WCoordinates(
            finalSWLat,
            finalSWLng
        ),
        W3WCoordinates(finalNELat, finalNELng)
    )

    onGridBoundUpdate.invoke(box)
}