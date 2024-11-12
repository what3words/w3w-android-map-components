package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.maps.extension.compose.MapboxMap
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.components.compose.maps.providers.W3WMapProvider
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates

class W3WMapBoxProvider : W3WMapProvider {
    override val defaultZoomLevel: Float
        get() = 19f
    override val minZoomLevel: Float
        get() = 17f
    override val maxZoomLevel: Float
        get() = 20f

    @Composable
    override fun What3WordsMap(
        modifier: Modifier,
        layoutConfig: W3WMapDefaults.LayoutConfig,
        mapConfig: W3WMapDefaults.MapConfig,
        state: W3WMapState,
        onMapClicked: ((W3WCoordinates) -> Unit),
        onCameraUpdated: ((W3WMapState.CameraPosition) -> Unit)
    ) {

        //TODO:
        // cameraPositionState: animate camera
        // uiSetting: turn off some buttons control
        // mapProperties: switch mapType

        MapboxMap(
            modifier = modifier,
        ) {
            W3WMapDrawer(state, mapConfig)
        }
    }

    @Composable
    override fun DrawGridLines(zoomLevel: Float) {
        //TODO: Draw visible grid lines based on zoomLevel
    }

    @Composable
    override fun DrawSelectedAddress(zoomLevel: Float, address: W3WAddress) {
        //TODO: Draw select for zoom in: grid, square

        //TODO: Draw select for zoom out: pin (maker)
    }

    @Composable
    override fun DrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMapState.Marker>>) {
        //TODO: Draw select for zoom in: filled square

        //TODO: Draw select for zoom out: circle
    }

    @Composable
    override fun W3WMapDrawer(state: W3WMapState, mapConfig: W3WMapDefaults.MapConfig) {
        val zoomLevel = mapConfig.zoomSwitchLevel ?: defaultZoomLevel

        //Draw the grid lines by zoom in state
        DrawGridLines(zoomLevel)

        //Draw the markers
        DrawMarkers(zoomLevel, state.listMakers)

        //Draw the selected address
        state.selectedAddress?.let { DrawSelectedAddress(zoomLevel, it) }
    }
}