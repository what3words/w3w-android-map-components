package com.what3words.components.compose.maps.providers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates

interface W3WMapProvider {
    val defaultZoomLevel: Float

    @Composable
    fun What3WordsMap(
        modifier: Modifier,
        contentPadding: PaddingValues,
        state: W3WMapState,
        onMapClicked: ((W3WCoordinates) -> Unit),
        onMapUpdate: (() -> Unit),
        onMapMove: (() -> Unit)
    )

    @Composable
    fun W3WMapDrawer(state: W3WMapState)

    @Composable
    fun DrawGridLines(zoomLevel: Float)

    @Composable
    fun DrawSelectedAddress(zoomLevel: Float, address: W3WAddress)

    @Composable
    fun DrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMapState.Marker>>)

}