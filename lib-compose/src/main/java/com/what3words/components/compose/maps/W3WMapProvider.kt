package com.what3words.components.compose.maps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.what3words.core.types.geometry.W3WCoordinates

interface W3WMapProvider {
    @Composable
    fun Map(
        modifier: Modifier,
        state: W3WMapState,
        onMapClicked: ((W3WCoordinates) -> Unit)?
    )
}