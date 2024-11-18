package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.components.compose.maps.mapper.toMapBoxCameraOptions
import com.what3words.components.compose.maps.mapper.toMapBoxMapType
import com.what3words.components.compose.maps.mapper.toW3WCameraPosition
import com.what3words.components.compose.maps.models.Marker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import kotlinx.coroutines.launch

@Composable
fun W3WMapBox(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    state: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit),
) {
    var mapView: MapView? by remember {
        mutableStateOf(null)
    }

    var isCameraMoving: Boolean by remember {
        mutableStateOf(false)
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            state.cameraPosition?.toMapBoxCameraOptions()
        }
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
        style = {
            GenericStyle(
                style = mapStyle,
            )
        }
    ) {
        MapEffect(Unit) {
            mapView = it
            it.location.updateSettings {
                enabled = state.isMyLocationEnabled
                locationPuck = createDefault2DPuck(withBearing = false)
            }
        }

        W3WMapBoxDrawer(state, mapConfig)
        content?.invoke()
    }
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawGridLines(gridLineConfig: W3WMapDefaults.GridLineConfig) {
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
fun W3WMapBoxDrawMarkers(zoomLevel: Float, listMakers: Map<String, List<Marker>>) {
    //TODO: Draw select for zoom in: filled square

    //TODO: Draw select for zoom out: circle
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawer(state: W3WMapState, mapConfig: W3WMapDefaults.MapConfig) {
    //Draw the grid lines by zoom in state
    W3WMapBoxDrawGridLines(mapConfig.gridLineConfig)

    //Draw the markers
    W3WMapBoxDrawMarkers(mapConfig.gridLineConfig.zoomSwitchLevel, state.listMakers)

    //Draw the selected address
    state.selectedAddress?.let {
        W3WMapBoxDrawSelectedAddress(
            mapConfig.gridLineConfig.zoomSwitchLevel,
            it
        )
    }
}