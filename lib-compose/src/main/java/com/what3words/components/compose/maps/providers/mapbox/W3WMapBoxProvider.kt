package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.maps.extension.compose.MapboxMap
import com.what3words.components.compose.maps.models.W3WMapState
import com.what3words.components.compose.maps.providers.W3WMapProvider

class W3WMapBoxProvider : W3WMapProvider {
    companion object {
        const val DEFAULT_ZOOM_SWITCH_LEVEL = 19f
    }


    @Composable
    override fun Map(
        modifier: Modifier,
        contentPadding: PaddingValues,
        state: W3WMapState,
        onMapUpdate: () -> Unit,
        onMapMove: () -> Unit,
    ) {

        //TODO:
        // cameraPositionState: animate camera
        // uiSetting: turn off some buttons control
        // mapProperties: switch mapType

        MapboxMap(
            modifier = modifier,
        ) {
            W3WMapBoxDrawer(state)
        }
    }
}