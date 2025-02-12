package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.models.DarkModeStyle
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.design.library.ui.theme.colors_blue_20
import com.what3words.design.library.ui.theme.colors_blue_99
import com.what3words.design.library.ui.theme.colors_grey_100
import com.what3words.design.library.ui.theme.colors_grey_44
import com.what3words.design.library.ui.theme.colors_red_50
import com.what3words.design.library.ui.theme.colors_red_90
import com.what3words.design.library.ui.theme.colors_red_99

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
    val LOCATION_DEFAULT = W3WCoordinates(51.521251, -0.203586)
    val MARKER_COLOR_DEFAULT = W3WMarkerColor(background = colors_red_50, slash = colors_red_99)
    const val MIN_SUPPORT_GRID_ZOOM_LEVEL_GOOGLE = 19f
    const val MIN_SUPPORT_GRID_ZOOM_LEVEL_MAP_BOX = 18.5f

    /**
     * Data class representing the configuration for the map.
     *
     * @property darkModeCustomJsonStyle The custom JSON style for dark mode.
     * @property gridLineConfig The configuration for grid lines on the map.
     */
    @Immutable
    data class MapConfig(
        // Map config
        val darkModeCustomJsonStyle: String = DarkModeStyle.darkMode,
        val isBuildingEnable: Boolean,

        // Grid view
        val gridLineConfig: GridLinesConfig,

        // Buttons
        val buttonConfig: ButtonConfig
    )

    /**
     * Data class representing the configuration for grid lines on the map.
     *
     * @property isGridEnabled Whether the grid is enabled or not.
     * @property gridLineWidth The width of the grid lines.
     * @property zoomSwitchLevel The zoom level at which the grid appearance changes.
     * @property gridScale The scale factor for the grid. Determines how much larger the grid is
     * compared to the visible camera bounds. A value of 1.0 matches the visible area, while larger
     * values (e.g., 2.0) make the grid cover an area twice the size of the visible bounds.
     */
    @Immutable
    data class GridLinesConfig(
        val isGridEnabled: Boolean,
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
        val isMapSwitchButtonAvailable: Boolean,
        val isMyLocationButtonAvailable: Boolean,
        val isRecallButtonAvailable: Boolean,
    )

    @Immutable
    data class MapColors(
        val normalMapColor: MapColor,
        val darkMapColor: MapColor,
        val satelliteMapColor: MapColor
    )

    @Immutable
    data class MapColor(
        val gridLineColor: Color,
        val markerColors: MarkerColors
    )

    @Immutable
    data class MarkerColors(
        val selectedColor: Color,
        val selectedZoomOutColor: W3WMarkerColor,
        val defaultMarkerColor: W3WMarkerColor
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
        isBuildingEnable: Boolean = false,
        gridLineConfig: GridLinesConfig = defaultGridLinesConfig(),
        buttonConfig: ButtonConfig = defaultButtonConfig(),
    ): MapConfig {
        return MapConfig(
            darkModeCustomJsonStyle = darkModeCustomJsonStyle,
            isBuildingEnable = isBuildingEnable,
            gridLineConfig = gridLineConfig,
            buttonConfig = buttonConfig,
        )
    }

    fun defaultGridLinesConfig(
        isGridEnabled: Boolean = true,
        zoomSwitchLevel: Float = 19f,
        gridLineWidth: Dp = 2.dp,
        gridScale: Float = 6f
    ): GridLinesConfig {
        return GridLinesConfig(
            isGridEnabled = isGridEnabled,
            zoomSwitchLevel = zoomSwitchLevel,
            gridLineWidth = gridLineWidth,
            gridScale = gridScale
        )
    }

    fun defaultButtonConfig(
        isMapSwitchButtonAvailable: Boolean = true,
        isMyLocationButtonAvailable: Boolean = true,
        isRecallButtonAvailable: Boolean = false
    ): ButtonConfig {
        return ButtonConfig(
            isMapSwitchButtonAvailable = isMapSwitchButtonAvailable,
            isMyLocationButtonAvailable = isMyLocationButtonAvailable,
            isRecallButtonAvailable = isRecallButtonAvailable
        )
    }

    fun defaultMapColors(
        normalMapColor: MapColor = defaultNormalMapColor(),
        darkMapColor: MapColor = defaultDarkMapColor(),
        satelliteMapColor: MapColor = defaultSatelliteMapColor()
    ): MapColors {
        return MapColors(
            normalMapColor = normalMapColor,
            satelliteMapColor = satelliteMapColor,
            darkMapColor = darkMapColor
        )
    }

    fun defaultNormalMapColor(
        gridLineColor: Color = colors_grey_44.copy(alpha = 0.16f),
        markerColors: MarkerColors = defaultMarkerColor(
            selectedZoomOutColor = W3WMarkerColor(background = colors_blue_20, slash = colors_blue_99),
            defaultMarkerColor = MARKER_COLOR_DEFAULT,
            selectedColor = colors_blue_20
        )
    ): MapColor {
        return MapColor(
            gridLineColor = gridLineColor,
            markerColors = markerColors
        )
    }

    fun defaultSatelliteMapColor(
        gridLineColor: Color = colors_grey_100.copy(alpha = 0.24f),
        markerColors: MarkerColors = defaultMarkerColor(
            selectedZoomOutColor = W3WMarkerColor(background = colors_blue_99, slash = colors_blue_20),
            defaultMarkerColor = MARKER_COLOR_DEFAULT,
            selectedColor = colors_blue_99
        )
    ): MapColor {
        return MapColor(
            gridLineColor = gridLineColor,
            markerColors = markerColors
        )
    }

    fun defaultDarkMapColor(
        gridLineColor: Color = colors_grey_100.copy(alpha = 0.16f),
        markerColors: MarkerColors = defaultMarkerColor(
            selectedZoomOutColor = W3WMarkerColor(background = colors_blue_99, slash = colors_blue_20),
            defaultMarkerColor = MARKER_COLOR_DEFAULT,
            selectedColor = colors_blue_99
        )
    ): MapColor {
        return MapColor(
            gridLineColor = gridLineColor,
            markerColors = markerColors
        )
    }

    fun defaultMarkerColor(
        selectedZoomOutColor: W3WMarkerColor,
        defaultMarkerColor: W3WMarkerColor,
        selectedColor: Color,
    ): MarkerColors {
        return MarkerColors(
            selectedZoomOutColor = selectedZoomOutColor,
            defaultMarkerColor = defaultMarkerColor,
            selectedColor = selectedColor
        )
    }
}