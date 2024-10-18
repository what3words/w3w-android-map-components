package com.what3words.components.compose.maps.providers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.what3words.components.compose.maps.models.W3WMapState

interface W3WMapProvider {
    @Composable
    fun Map(
        modifier: Modifier,
        contentPadding: PaddingValues,
        state: W3WMapState,
        onMapUpdate: (() -> Unit),
        onMapMove: (() -> Unit)
    )
}
