package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.models.DarkModeStyle
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMarkerColor

enum class MapProvider {
    GOOGLE_MAP,
    MAPBOX
}

/**
 * Object containing default configurations and utility functions for What3Words map components.
 *
 * This object provides default configurations for various aspects of the What3Words map,
 * including map configuration, grid lines, layout, and buttons.
 */
object W3WMapDefaults {
    val LOCATION_DEFAULT = W3WLatLng(51.521251, -0.203586)
    val MARKER_COLOR_DEFAULT = W3WMarkerColor(background = Color.Red, slash = Color.White)
    val MUlTI_MARKERS_COLOR_DEFAULT = W3WMarkerColor(background = Color.Blue, slash = Color.White)

    /**
     * Data class representing the configuration for the map.
     *
     * @property darkModeCustomJsonStyle The custom JSON style for dark mode.
     * @property gridLineConfig The configuration for grid lines on the map.
     */
    @Immutable
    data class MapConfig(
        val darkModeCustomJsonStyle: String = DarkModeStyle.darkMode,
        // Grid view
        val gridLineConfig: GridLinesConfig,
        // Buttons
        val buttonConfig: ButtonConfig
    )

    /**
     * Data class representing the configuration for grid lines on the map.
     *
     * @property isGridEnabled Whether the grid is enabled or not.
     * @property gridColor The color of the grid lines.
     * @property gridLineWidth The width of the grid lines.
     * @property zoomSwitchLevel The zoom level at which the grid appearance changes.
     * @property gridScale The scale factor for the grid. Determines how much larger the grid is
     * compared to the visible camera bounds. A value of 1.0 matches the visible area, while larger
     * values (e.g., 2.0) make the grid cover an area twice the size of the visible bounds.
     */
    @Immutable
    data class GridLinesConfig(
        val isGridEnabled: Boolean,
        val gridColor: Color,
        val gridLineWidth: Dp,
        val zoomSwitchLevel: Float,
        val gridScale: Float
    )

    /**
     * Data class representing the layout configuration for the map.
     *
     * @property contentPadding The padding values for the map content.
     */
    @Immutable
    data class LayoutConfig(
        val contentPadding: PaddingValues
    )

    /**
     * Data class representing the layout configuration for map buttons.
     *
     * @property contentPadding The padding values for the button layout.
     */
    @Immutable
    data class ButtonsLayoutConfig(
        val contentPadding: PaddingValues
    )

    @Immutable
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
        gridColor: Color = Color.LightGray,
        zoomSwitchLevel: Float = 19f,
        gridLineWidth: Dp = 1.dp,
        gridScale: Float = 4f
    ): GridLinesConfig {
        return GridLinesConfig(
            isGridEnabled = isGridEnabled,
            gridColor = gridColor,
            zoomSwitchLevel = zoomSwitchLevel,
            gridLineWidth = gridLineWidth,
            gridScale = gridScale
        )
    }

    fun defaultButtonConfig(
        isMapSwitchButtonEnabled: Boolean = true,
        isMyLocationButtonEnabled: Boolean = true,
        isRecallButtonEnabled: Boolean = false
    ): ButtonConfig {
        return ButtonConfig(
            isMapSwitchButtonEnabled = isMapSwitchButtonEnabled,
            isMyLocationButtonEnabled = isMyLocationButtonEnabled,
            isRecallButtonEnabled = isRecallButtonEnabled
        )
    }

}