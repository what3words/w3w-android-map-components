package com.what3words.components.compose.maps.providers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.maps.extension.compose.MapboxMap
import com.what3words.components.compose.maps.W3WMapProvider
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.core.types.geometry.W3WCoordinates


class MapboxProvider : W3WMapProvider {
    @Composable
    override fun Map(
        modifier: Modifier,
        state: W3WMapState,
        onMapClicked: ((W3WCoordinates) -> Unit)?,
    ) {
        MapboxMap(
            modifier = modifier,
            onMapClickListener = { point ->
                onMapClicked?.invoke(W3WCoordinates(point.latitude(), point.longitude()))
                true
            }
        ) {
            // Example

        }
    }
}