package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.components.compose.maps.mapper.toMapBoxMapType
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates

@Composable
fun W3WMapBox(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    state: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: ((W3WMapState.CameraPosition) -> Unit)
) {
//    val mapProperties = remember(state.mapType, state.isMyLocationEnabled, state.isDarkMode) {
//        MapProperties(
//            mapType = state.mapType.toMapType(),
//            isMyLocationEnabled = state.isMyLocationEnabled,
//            mapStyleOptions = if (state.isDarkMode) MapStyleOptions(mapConfig.darkModeCustomJsonStyle) else null
//        )
//    }

//    mapType = state.mapType.toGoogleMapType(),
//    isMyLocationEnabled = state.isMyLocationEnabled,
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(2.0)
            center(Point.fromLngLat(-98.0, 39.5))
            pitch(0.0)
            bearing(0.0)
        }
    }

    val mapState = rememberMapState()

    val mapStyle = remember(state.mapType, state.isDarkMode) {
        state.mapType.toMapBoxMapType(state.isDarkMode)
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
        style = {
            GenericStyle(
                style = mapStyle,
            )
        }
    ) {
        MapEffect(Unit) { mapView ->
            mapView.location.updateSettings {
                locationPuck = createDefault2DPuck(withBearing = true)
                enabled = true
                pulsingEnabled = true
            }
            mapViewportState.transitionToFollowPuckState()
        }

        W3WMapBoxDrawer(state, mapConfig)
        content?.invoke()
    }
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawGridLines(zoomLevel: Float) {
    //TODO: Draw visible grid lines based on zoomLevel
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawSelectedAddress(zoomLevel: Float, address: W3WAddress) {
    //TODO: Draw select for zoom in: grid, square

    //TODO: Draw select for zoom out: pin (maker)
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMapState.Marker>>) {
    //TODO: Draw select for zoom in: filled square

    //TODO: Draw select for zoom out: circle
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawer(state: W3WMapState, mapConfig: W3WMapDefaults.MapConfig) {
    //Draw the grid lines by zoom in state
    W3WMapBoxDrawGridLines(mapConfig.zoomSwitchLevel)

    //Draw the markers
    W3WMapBoxDrawMarkers(mapConfig.zoomSwitchLevel, state.listMakers)

    //Draw the selected address
    state.selectedAddress?.let { W3WMapBoxDrawSelectedAddress(mapConfig.zoomSwitchLevel, it) }
}