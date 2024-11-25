package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
        val gridLineConfig: GridLinesConfig
    )

    data class GridLinesConfig(
        val isGridEnabled: Boolean = true,
        val gridColor: Color = Color.LightGray,
        val zoomSwitchLevel: Float = 19f,
        val gridLineWidth: Dp = 1.dp
    )

    data class LayoutConfig(
        val contentPadding: PaddingValues
    )

    data class ButtonsLayoutConfig(
        val contentPadding: PaddingValues
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
        gridLineConfig: GridLinesConfig = defaultGridLinesConfig()
    ): MapConfig {
        return MapConfig(
            darkModeCustomJsonStyle = darkModeCustomJsonStyle,
            gridLineConfig = gridLineConfig
        )
    }

    private fun defaultGridLinesConfig(
        isGridEnabled: Boolean = true,
        gridColor: Color = Color.LightGray,
        zoomSwitchLevel: Float = 19f
    ): GridLinesConfig {
        return GridLinesConfig(
            isGridEnabled = isGridEnabled,
            gridColor = gridColor,
            zoomSwitchLevel = zoomSwitchLevel
        )
    }

}