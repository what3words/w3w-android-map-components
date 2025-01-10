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
 * @param state The [W3WMapState] object that holds the state of the map.
 * @param content Optional composable content to be displayed on the map, such as markers or overlays.
 * @param onMapClicked Callback invoked when the user clicks on the map.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 *
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun W3WGoogleMap(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
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
            mapStyleOptions = if (state.isDarkMode) MapStyleOptions(mapConfig.darkModeCustomJsonStyle) else null
        )
    }

    // Update the MapUiSettings based on isMapGestureEnable
    val uiSettings = remember(state.isMapGestureEnable) {
        MapUiSettings(
            indoorLevelPickerEnabled = false,
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            scrollGesturesEnabled = state.isMapGestureEnable,
            tiltGesturesEnabled = state.isMapGestureEnable,
            zoomGesturesEnabled = state.isMapGestureEnable,
            rotationGesturesEnabled = state.isMapGestureEnable,
            scrollGesturesEnabledDuringRotateOrZoom = state.isMapGestureEnable,
            mapToolbarEnabled = false
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
                    if (mapConfig.buttonConfig.isRecallButtonUsed) {
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

//    Rebugger(
//        trackMap = mapOf(
//            "layoutConfig" to layoutConfig,
//            "mapConfig" to mapConfig,
//            "mapProperties" to mapProperties,
//            "uiSettings" to uiSettings,
//            "state" to state,
//            "onMarkerClicked" to onMarkerClicked,
//            "onMapClicked" to onMapClicked,
//            "onMapProjectionUpdated" to onMapProjectionUpdated,
//        )
//    )

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentPadding = layoutConfig.contentPadding,
        uiSettings = uiSettings,
        properties = mapProperties,
        onMapClick = {
            onMapClicked(W3WCoordinates(it.latitude, it.longitude))
        })
    {
        MapEffect(Unit) { map ->
            mapProjection = W3WGoogleMapProjection(map.projection)
        }
        W3WGoogleMapDrawer(state = state, mapConfig, onMarkerClicked)
        content?.invoke()
    }
}

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