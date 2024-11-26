package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.models.DarkModeStyle

enum class MapProvider {
    GOOGLE_MAP,
    MAPBOX
}

object W3WMapDefaults {
    data class MapConfig(
        val darkModeCustomJsonStyle: String = DarkModeStyle.darkMode,
        // Grid view
        val gridLineConfig: GridLinesConfig,
        // Buttons
        val buttonConfig: ButtonConfig
    )

    data class GridLinesConfig(
        val isGridEnabled: Boolean = true,
        val gridColor: Color? = null,
        val zoomSwitchLevel: Float = 19f
    )

    data class LayoutConfig(
        val contentPadding: PaddingValues
    )

    data class ButtonsLayoutConfig(
        val contentPadding: PaddingValues
    )

    data class ButtonConfig(
        val isMapSwitchButtonEnabled: Boolean,
        val isMyLocationButtonEnabled: Boolean,
        val isRecallButtonEnabled: Boolean,
    )

    @Composable
    fun defaultLayoutConfig(
        contentPadding: PaddingValues = PaddingValues(bottom = 24.dp, end = 8.dp)
    ): LayoutConfig {
        return LayoutConfig(
            contentPadding = contentPadding
        )
    }

    fun defaultMapConfig(
        darkModeCustomJsonStyle: String = DarkModeStyle.darkMode,
        gridLineConfig: GridLinesConfig = defaultGridLinesConfig(),
        buttonConfig: ButtonConfig = defaultButtonConfig()
    ): MapConfig {
        return MapConfig(
            darkModeCustomJsonStyle = darkModeCustomJsonStyle,
            gridLineConfig = gridLineConfig,
            buttonConfig = buttonConfig
        )
    }

    fun defaultGridLinesConfig(
        isGridEnabled: Boolean = true,
        gridColor: Color? = null,
        zoomSwitchLevel: Float = 19f
    ): GridLinesConfig {
        return GridLinesConfig(
            isGridEnabled = isGridEnabled,
            gridColor = gridColor,
            zoomSwitchLevel = zoomSwitchLevel
        )
    }

    fun defaultButtonConfig(
        isMapSwitchButtonEnabled: Boolean = true,
        isMyLocationButtonEnable: Boolean = true,
        isRecallButtonEnable: Boolean = false
    ): ButtonConfig {
        return ButtonConfig(
            isMapSwitchButtonEnabled = isMapSwitchButtonEnabled,
            isMyLocationButtonEnabled = isMyLocationButtonEnable,
            isRecallButtonEnabled = isRecallButtonEnable
        )
    }

}