package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.models.DarkModeStyle

object W3WMapDefaults {
    data class MapConfig(
        val isGridEnabled: Boolean = true,

        // Grid view
        val darkModeCustomJsonStyle: String = DarkModeStyle.darkMode,
        val gridColor: Color? = null,
        val zoomSwitchLevel: Float? = null
    )

    data class LayoutConfig(
        val contentPadding: PaddingValues
    )

    data class ButtonsLayoutConfig(
        val contentPadding: PaddingValues
    )

    @Composable
    fun defaultLayoutConfig(
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ): LayoutConfig {
        return LayoutConfig(
            contentPadding = contentPadding
        )
    }

    fun defaultMapConfig(
        isGridEnabled: Boolean = true,
        darkModeCustomJsonStyle: String = DarkModeStyle.darkMode,
        gridColor: Color? = null,
        zoomSwitchLevel: Float? = null
    ): MapConfig {
        return MapConfig(
            isGridEnabled = isGridEnabled,
            darkModeCustomJsonStyle = darkModeCustomJsonStyle,
            gridColor = gridColor,
            zoomSwitchLevel = zoomSwitchLevel
        )
    }
}