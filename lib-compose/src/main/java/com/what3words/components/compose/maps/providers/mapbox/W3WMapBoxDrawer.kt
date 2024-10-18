package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.runtime.Composable
import com.what3words.components.compose.maps.models.W3WMapMarker
import com.what3words.components.compose.maps.models.W3WMapState
import com.what3words.core.types.domain.W3WAddress


@Composable
private fun DrawGridLines(zoomLevel: Float) {
    //TODO: Draw visible grid lines based on zoomLevel
}

@Composable
private fun DrawSelectedAddress(zoomLevel: Float, address: W3WAddress) {
    //TODO: Draw select for zoom in: grid, square

    //TODO: Draw select for zoom out: pin (maker)

}

@Composable
private fun DrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMapMarker>>) {
    //TODO: Draw select for zoom in: filled square

    //TODO: Draw select for zoom out: circle
}


@Composable
fun W3WMapBoxDrawer(state: W3WMapState) {
    val zoomLevel = state.zoom ?: W3WMapBoxProvider.DEFAULT_ZOOM_SWITCH_LEVEL

    //Draw the grid lines by zoom in state
    DrawGridLines(zoomLevel)

    //Draw the markers
    DrawMarkers(zoomLevel, state.listMakers)

    //Draw the selected address
    state.selectedAddress?.let { DrawSelectedAddress(zoomLevel, it) }
}