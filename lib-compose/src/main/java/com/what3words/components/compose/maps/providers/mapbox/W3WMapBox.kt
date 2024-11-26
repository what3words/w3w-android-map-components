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
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.GenericStyle
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.mapper.toMapBoxMapType
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WMapboxCameraState
import com.what3words.core.types.geometry.W3WCoordinates

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
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: (W3WCameraState<*>) -> Unit
) {
    var mapView: MapView? by remember {
        mutableStateOf(null)
    }

    val mapViewportState = (state.cameraState as W3WMapboxCameraState).cameraState

    LaunchedEffect(mapViewportState) {
        onCameraUpdated(W3WMapboxCameraState(mapViewportState))
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