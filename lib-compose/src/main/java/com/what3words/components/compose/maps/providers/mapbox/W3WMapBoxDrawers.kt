package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.runtime.Composable
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.core.types.domain.W3WAddress


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
fun W3WMapBoxDrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMarker>>) {
    //TODO: Draw select for zoom in: filled square

    //TODO: Draw select for zoom out: circle
}