package com.what3words.components.compose.maps.providers.googlemap

import android.graphics.Point
import android.util.Log
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
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.mapper.toGoogleMapType
import com.what3words.components.compose.maps.state.W3WCameraState
import com.what3words.components.compose.maps.state.W3WGoogleCameraState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun W3WGoogleMap(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    state: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMapClicked: (W3WCoordinates) -> Unit,
    onCameraUpdated: (W3WCameraState<*>) -> Unit
) {
    val mapProperties = remember(state.mapType, state.isMyLocationEnabled, state.isDarkMode) {
        MapProperties(
            mapType = state.mapType.toGoogleMapType(),
            isMyLocationEnabled = state.isMyLocationEnabled,
            mapStyleOptions = if (state.isDarkMode) MapStyleOptions(mapConfig.darkModeCustomJsonStyle) else null
        )
    }

    val uiSettings = remember(state.isMyLocationButtonEnabled, state.isMapGestureEnable) {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            scrollGesturesEnabled = state.isMapGestureEnable,
            tiltGesturesEnabled = state.isMapGestureEnable,
            zoomGesturesEnabled = state.isMapGestureEnable,
            rotationGesturesEnabled = state.isMapGestureEnable,
            scrollGesturesEnabledDuringRotateOrZoom = state.isMapGestureEnable
        )
    }

    val cameraPositionState = (state.cameraState as W3WGoogleCameraState).cameraState

    var lastProcessedPosition by remember { mutableStateOf(cameraPositionState.position) }

    LaunchedEffect(cameraPositionState) {
        /// A hybrid approach that combines immediate updates for significant changes with debounced updates for fine-tuning
        snapshotFlow { cameraPositionState.position to cameraPositionState.projection }
            .conflate()
            .onEach { (position, projection) ->
                val significantChange = isSignificantChange(position, lastProcessedPosition)
                if (significantChange) {
                    delay(300)
                } else {
                    delay(500)
                }
                projection?.let {
                    updateGridBound(projection) { newBound ->
                        lastProcessedPosition = position
                        val newCameraState = W3WGoogleCameraState(cameraPositionState)
                        newCameraState.gridBound = newBound
                        onCameraUpdated(newCameraState)
                    }
                }
            }.launchIn(this)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentPadding = layoutConfig.contentPadding,
        uiSettings = uiSettings,
        properties = mapProperties,
        onMapClick = {
            onMapClicked.invoke(W3WCoordinates(it.latitude, it.longitude))
        },
    ) {
        W3WGoogleMapDrawer(state = state, mapConfig)
        content?.invoke()
    }
}

private fun isSignificantChange(
    newPosition: com.google.android.gms.maps.model.CameraPosition,
    lastPosition: com.google.android.gms.maps.model.CameraPosition
): Boolean {
    val latDiff = abs(newPosition.target.latitude - lastPosition.target.latitude)
    val lngDiff = abs(newPosition.target.longitude - lastPosition.target.longitude)
    val zoomDiff = abs(newPosition.zoom - lastPosition.zoom)

    return latDiff > 0.01 || lngDiff > 0.01 || zoomDiff > 0.5
}

private suspend fun updateGridBound(
    projection: Projection,
    onGridBoundUpdate: (W3WRectangle) -> Unit
) {
    withContext(Dispatchers.IO) {
        val lastScaledBounds = scaleBounds(projection.visibleRegion.latLngBounds, projection)
        val box = W3WRectangle(
            W3WCoordinates(
                lastScaledBounds.southwest.latitude,
                lastScaledBounds.southwest.longitude
            ),
            W3WCoordinates(
                lastScaledBounds.northeast.latitude,
                lastScaledBounds.northeast.longitude
            )
        )

        withContext(Dispatchers.Main) {
            onGridBoundUpdate.invoke(box)
        }
    }
}

private fun scaleBounds(
    bounds: LatLngBounds,
    projection: Projection,
    scale: Float = 6f
): LatLngBounds {
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