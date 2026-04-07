package com.what3words.components.compose.maps.providers.googlemap

import android.graphics.Point
import android.view.View
import android.widget.RelativeLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.doOnLayout
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.mapper.toGoogleMapType
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WGoogleCameraState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

/**
 * A composable function that displays a What3Words (W3W) map using the Google Maps Platform.
 *
 * @param modifier Modifier for styling and layout of the map view.
 * @param layoutConfig Configuration for the map's layout, such as padding and content alignment.
 * @param mapConfig Configuration for the map's appearance, such as map type and zoom controls.
 * @param mapColor Configuration for the map's color scheme.
 * @param state The [W3WMapState] object that holds the state of the map.
 * @param content Optional composable content to be displayed on the map, such as markers or overlays.
 * @param onMarkerClicked Callback invoked when a marker on the map is clicked.
 * @param onMapClicked Callback invoked when the user clicks on the map.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 * @param onMapProjectionUpdated Callback invoked when the map projection is updated.
 * @param onMapLoaded Callback invoked when the Google Map has finished loading and
 *   [com.google.android.gms.maps.CameraUpdateFactory] is safe to use.
 *
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun W3WGoogleMap(
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    mapColor: W3WMapDefaults.MapColor,
    state: W3WMapState,
    onMarkerClicked: (W3WMarker) -> Unit,
    onMapClicked: (W3WCoordinates) -> Unit,
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    modifier: Modifier = Modifier,
    onMapProjectionUpdated: ((W3WMapProjection) -> Unit)? = null,
    onMapLoaded: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    // Update the map properties based on map type, isMyLocationEnabled, and dark mode
    val mapProperties = remember(state.mapType, state.isMyLocationEnabled, state.isDarkMode) {
        MapProperties(
            isBuildingEnabled = mapConfig.isBuildingEnable,
            isIndoorEnabled = false,
            mapType = state.mapType.toGoogleMapType(),
            isMyLocationEnabled = state.isMyLocationEnabled,
            mapStyleOptions = if (state.isDarkMode) mapConfig.darkModeCustomJsonStyle?.let {
                MapStyleOptions(
                    it
                )
            } else null
        )
    }

    val cameraPositionState = (state.cameraState as W3WGoogleCameraState).cameraState

    var mapProjection: W3WGoogleMapProjection? by remember {
        mutableStateOf(null)
    }

    val movableContent = remember {
        movableContentOf { content?.let { it() } }
    }

    LaunchedEffect(cameraPositionState) {
        /// A hybrid approach that combines immediate updates for significant changes with debounced updates for fine-tuning
        snapshotFlow { cameraPositionState.position to cameraPositionState.projection }
            .conflate()
            .onEach { (_, projection) ->
                projection?.let {
                    if (mapConfig.buttonConfig.isRecallFeatureEnabled || onMapProjectionUpdated != null) {
                        mapProjection?.projection = projection
                        mapProjection?.let {
                            onMapProjectionUpdated?.invoke(it)
                        }
                    }
                    updateCameraBound(
                        projection,
                        mapConfig.gridLineConfig
                    ) { gridBound, visibleBound ->
                        state.cameraState.gridBound = gridBound
                        state.cameraState.visibleBound = visibleBound
                        onCameraUpdated(state.cameraState)
                    }
                }
            }.launchIn(this)
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        state.cameraState.isCameraMoving = cameraPositionState.isMoving
        onCameraUpdated(state.cameraState)
    }

    // Reposition Google Map compass to align with app's design
    val view = LocalView.current
    LaunchedEffect(mapConfig.isGoogleCompassAlignedRight) {
        if (mapConfig.isGoogleCompassAlignedRight) {
            val compass = view.findViewWithTag<View>("GoogleMapCompass")

            compass?.doOnLayout {
                val params = compass.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
                params.addRule(RelativeLayout.ALIGN_PARENT_END)
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP)

                compass.layoutParams = params
                compass.requestLayout()
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentPadding = layoutConfig.contentPadding,
        mapColorScheme = if (state.isDarkMode) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
        onMapLoaded = { onMapLoaded?.invoke() },
        uiSettings = MapUiSettings(
            compassEnabled = mapConfig.isCompassButtonEnabled,
            indoorLevelPickerEnabled = false,
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            scrollGesturesEnabled = state.isMapGestureEnable,
            tiltGesturesEnabled = state.isMapGestureEnable,
            zoomGesturesEnabled = state.isMapGestureEnable,
            rotationGesturesEnabled = state.isMapGestureEnable,
            scrollGesturesEnabledDuringRotateOrZoom = state.isMapGestureEnable,
            mapToolbarEnabled = mapConfig.isScaleBarEnabled
        ),
        properties = mapProperties,
        onMapClick = {
            onMapClicked(W3WCoordinates(it.latitude, it.longitude))
        })
    {
        MapEffect(Unit) { map ->
            mapProjection = W3WGoogleMapProjection(map.projection)
        }
        W3WGoogleMapDrawer(
            state = state,
            mapConfig = mapConfig,
            mapColor = mapColor,
            onMarkerClicked = onMarkerClicked
        )

        movableContent()
    }
}

/**
 * Updates the camera bounds for the map and notifies listeners about the changes.
 *
 * This function calculates both the grid bounds (scaled according to gridLinesConfig) and
 * the actual visible bounds of the map. The results are passed to the provided callback.
 *
 * @param projection The map's current projection that provides the visible region
 * @param gridLinesConfig Configuration for the grid lines, including scale factor
 * @param onCameraBoundUpdate Callback that receives the calculated grid bounds and visible bounds
 */
fun updateCameraBound(
    projection: Projection,
    gridLinesConfig: W3WMapDefaults.GridLinesConfig,
    onCameraBoundUpdate: (gridBound: W3WRectangle, visibleBound: W3WRectangle) -> Unit
) {
    val latLngBounds = try {
        projection.visibleRegion.latLngBounds
    } catch (_: IllegalStateException) {
        // Map layout isn't ready. Skip this frame safely.
        return
    }

    val lastScaledBounds = scaleBounds(latLngBounds, projection, gridLinesConfig)
    val gridBound = W3WRectangle(
        W3WCoordinates(
            lastScaledBounds.southwest.latitude,
            lastScaledBounds.southwest.longitude
        ),
        W3WCoordinates(
            lastScaledBounds.northeast.latitude,
            lastScaledBounds.northeast.longitude
        )
    )

    val visibleBound = W3WRectangle(
        W3WCoordinates(
            latLngBounds.southwest.latitude,
            latLngBounds.southwest.longitude
        ),
        W3WCoordinates(
            latLngBounds.northeast.latitude,
            latLngBounds.northeast.longitude
        )
    )

    onCameraBoundUpdate.invoke(gridBound, visibleBound)
}

/**
 * Scales the given map bounds by the specified grid scale factor.
 *
 * This function transforms the map bounds by scaling them from the center point.
 * It converts the bounds to screen coordinates, applies the scale factor, and then
 * converts back to geographical coordinates.
 *
 * @param bounds The original [LatLngBounds] to scale
 * @param projection The map [Projection] used for coordinate transformations
 * @param gridLinesConfig Configuration containing the grid scale factor
 * @return A new [LatLngBounds] that represents the scaled bounds
 */
private fun scaleBounds(
    bounds: LatLngBounds,
    projection: Projection,
    gridLinesConfig: W3WMapDefaults.GridLinesConfig
): LatLngBounds {
    val scale = gridLinesConfig.gridScale
    try {
        val center = bounds.center
        val centerPoint: Point = projection.toScreenLocation(center)
        val screenPositionNortheast: Point =
            projection.toScreenLocation(bounds.northeast)
        screenPositionNortheast.x =
            ((scale * (screenPositionNortheast.x - centerPoint.x) + centerPoint.x).roundToInt())
        screenPositionNortheast.y =
            ((scale * (screenPositionNortheast.y - centerPoint.y) + centerPoint.y).roundToInt())
        val scaledNortheast = projection.fromScreenLocation(screenPositionNortheast)
        val screenPositionSouthwest: Point =
            projection.toScreenLocation(bounds.southwest)
        screenPositionSouthwest.x =
            ((scale * (screenPositionSouthwest.x - centerPoint.x) + centerPoint.x).roundToInt())
        screenPositionSouthwest.y =
            ((scale * (screenPositionSouthwest.y - centerPoint.y) + centerPoint.y).roundToInt())
        val scaledSouthwest = projection.fromScreenLocation(screenPositionSouthwest)
        return LatLngBounds(scaledSouthwest, scaledNortheast)
    } catch (e: Exception) {
        //fallback to original bounds if something goes wrong with scaling
        e.printStackTrace()
        return bounds
    }
}