package com.what3words.components.compose.maps.providers.googlemap

import android.graphics.Point
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
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
 *
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun W3WGoogleMap(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    mapColor: W3WMapDefaults.MapColor,
    state: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMarkerClicked: (W3WMarker) -> Unit,
    onMapClicked: (W3WCoordinates) -> Unit,
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    onMapProjectionUpdated: (W3WMapProjection) -> Unit
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

    var lastProcessedPosition by remember { mutableStateOf(cameraPositionState.position) }

    var mapProjection: W3WGoogleMapProjection? by remember {
        mutableStateOf(null)
    }

    LaunchedEffect(cameraPositionState) {
        /// A hybrid approach that combines immediate updates for significant changes with debounced updates for fine-tuning
        snapshotFlow { cameraPositionState.position to cameraPositionState.projection }
            .conflate()
            .onEach { (position, projection) ->
                projection?.let {
                    if (mapConfig.buttonConfig.isRecallButtonAvailable) {
                        mapProjection?.projection = projection
                        mapProjection?.let(onMapProjectionUpdated)
                    }
                    updateCameraBound(
                        projection,
                        mapConfig.gridLineConfig
                    ) { gridBound, visibleBound ->
                        lastProcessedPosition = position
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

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentPadding = layoutConfig.contentPadding,
        mapColorScheme = if (state.isDarkMode) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
        uiSettings = MapUiSettings(
            indoorLevelPickerEnabled = false,
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            scrollGesturesEnabled = state.isMapGestureEnable,
            tiltGesturesEnabled = state.isMapGestureEnable,
            zoomGesturesEnabled = state.isMapGestureEnable,
            rotationGesturesEnabled = state.isMapGestureEnable,
            scrollGesturesEnabledDuringRotateOrZoom = state.isMapGestureEnable,
            mapToolbarEnabled = false
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
        content?.invoke()
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
private suspend fun updateCameraBound(
    projection: Projection,
    gridLinesConfig: W3WMapDefaults.GridLinesConfig,
    onCameraBoundUpdate: (gridBound: W3WRectangle, visibleBound: W3WRectangle) -> Unit
) {
    withContext(Dispatchers.IO) {
        val lastScaledBounds =
            scaleBounds(projection.visibleRegion.latLngBounds, projection, gridLinesConfig)
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
                projection.visibleRegion.latLngBounds.southwest.latitude,
                projection.visibleRegion.latLngBounds.southwest.longitude
            ),
            W3WCoordinates(
                projection.visibleRegion.latLngBounds.northeast.latitude,
                projection.visibleRegion.latLngBounds.northeast.longitude
            )
        )

        withContext(Dispatchers.Main) {
            onCameraBoundUpdate.invoke(gridBound, visibleBound)
        }
    }
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