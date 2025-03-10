package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.MapProvider.GOOGLE_MAP
import com.what3words.components.compose.maps.MapProvider.MAPBOX
import com.what3words.components.compose.maps.W3WMapDefaults.defaultMarkerColor
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.design.library.ui.theme.colors_blue_20
import com.what3words.design.library.ui.theme.colors_blue_99
import com.what3words.design.library.ui.theme.colors_grey_100
import com.what3words.design.library.ui.theme.colors_grey_44
import com.what3words.design.library.ui.theme.colors_red_50
import com.what3words.design.library.ui.theme.colors_red_99

/**
 * Enum class representing the map provider options available.
 *
 * @property GOOGLE_MAP Google Maps as the provider
 * @property MAPBOX Mapbox as the provider
 */
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
    const val DEFAULT_MAP_ZOOM_SWITCH_LEVEL = 19f

    /**
     * Data class representing the configuration for the map.
     *
     * @property darkModeCustomJsonStyle The custom JSON style for dark mode
     * @property isBuildingEnable Whether 3D buildings are enabled on the map
     * @property gridLineConfig The configuration for grid lines on the map
     * @property buttonConfig The configuration for map control buttons
     */
    @Immutable
    data class MapConfig(
        val darkModeCustomJsonStyle: String?,
        val isBuildingEnable: Boolean,
        val gridLineConfig: GridLinesConfig,
        val buttonConfig: ButtonConfig
    )

    /**
     * Data class representing the configuration for grid lines on the map.
     *
     * @property isGridEnabled Whether the grid is enabled or not
     * @property gridLineWidth The width of the grid lines
     * @property zoomSwitchLevel The zoom level at which the grid appearance changes
     * @property gridScale The scale factor for the grid. Determines how much larger the grid is
     * compared to the visible camera bounds. A value of 1.0 matches the visible area, while larger
     * values (e.g., 2.0) make the grid cover an area twice the size of the visible bounds
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
     * @property contentPadding The padding values for the map content
     */
    @Immutable
    data class LayoutConfig(
        val contentPadding: PaddingValues
    )

    /**
     * Data class representing the layout configuration for map buttons.
     *
     * @property contentPadding The padding values for the button layout
     */
    @Immutable
    data class ButtonsLayoutConfig(
        val contentPadding: PaddingValues
    )

    /**
     * Data class representing the configuration for map buttons.
     *
     * @property isMapSwitchButtonAvailable Whether the map type switch button is available
     * @property isMyLocationButtonAvailable Whether the my location button is available
     * @property isRecallButtonAvailable Whether the recall button is available
     */
    @Immutable
    data class ButtonConfig(
        val isMapSwitchButtonAvailable: Boolean,
        val isMyLocationButtonAvailable: Boolean,
        val isRecallButtonAvailable: Boolean,
    )

    /**
     * Data class representing the color configurations for different map types.
     *
     * @property normalMapColor Colors for the standard map style
     * @property darkMapColor Colors for the dark mode map style
     * @property satelliteMapColor Colors for the satellite map style
     */
    @Immutable
    data class MapColors(
        val normalMapColor: MapColor,
        val darkMapColor: MapColor,
        val satelliteMapColor: MapColor
    )

    /**
     * Data class representing the color configuration for a specific map type.
     *
     * @property gridLineColor The color for grid lines
     * @property markerColors The colors for map markers
     */
    @Immutable
    data class MapColor(
        val gridLineColor: Color,
        val markerColors: MarkerColors
    )

    /**
     * Data class representing color configurations for different marker states.
     *
     * @property selectedColor The color for the selected marker
     * @property selectedZoomOutColor The color scheme for the selected marker when zoomed out
     * @property defaultMarkerColor The default color scheme for markers
     */
    @Immutable
    data class MarkerColors(
        val selectedColor: Color,
        val selectedZoomOutColor: W3WMarkerColor,
        val defaultMarkerColor: W3WMarkerColor
    )

    /**
     * Creates a default layout configuration with customizable content padding.
     *
     * @param contentPadding The padding values for the map content
     * @return A LayoutConfig instance with the specified parameters
     */
    @Composable
    fun defaultLayoutConfig(
        contentPadding: PaddingValues = PaddingValues(bottom = 24.dp, end = 8.dp)
    ): LayoutConfig {
        return LayoutConfig(
            contentPadding = contentPadding
        )
    }

    /**
     * Creates a default map configuration with customizable parameters.
     *
     * @param darkModeCustomJsonStyle The custom JSON style for dark mode
     * @param isBuildingEnable Whether 3D buildings are enabled on the map
     * @param gridLineConfig The configuration for grid lines on the map
     * @param buttonConfig The configuration for map control buttons
     * @return A MapConfig instance with the specified parameters
     */
    fun defaultMapConfig(
        darkModeCustomJsonStyle: String? = null,
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

    /**
     * Creates a default grid lines configuration with customizable parameters.
     *
     * @param isGridEnabled Whether the grid is enabled or not
     * @param zoomSwitchLevel The zoom level at which the grid appearance changes
     * @param gridLineWidth The width of the grid lines
     * @param gridScale The scale factor for the grid
     * @return A GridLinesConfig instance with the specified parameters
     */
    fun defaultGridLinesConfig(
        isGridEnabled: Boolean = true,
        zoomSwitchLevel: Float = DEFAULT_MAP_ZOOM_SWITCH_LEVEL,
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

    /**
     * Creates a default button configuration with customizable parameters.
     *
     * @param isMapSwitchButtonAvailable Whether the map type switch button is available
     * @param isMyLocationButtonAvailable Whether the my location button is available
     * @param isRecallButtonAvailable Whether the recall button is available
     * @return A ButtonConfig instance with the specified parameters
     */
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

    /**
     * Creates a default map colors configuration with customizable parameters.
     *
     * @param normalMapColor Colors for the standard map style
     * @param darkMapColor Colors for the dark mode map style
     * @param satelliteMapColor Colors for the satellite map style
     * @return A MapColors instance with the specified parameters
     */
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

    /**
     * Creates a default color configuration for the normal map style.
     *
     * @param gridLineColor The color for grid lines
     * @param markerColors The colors for map markers
     * @return A MapColor instance for the normal map style
     */
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

    /**
     * Creates a default color configuration for the satellite map style.
     *
     * @param gridLineColor The color for grid lines
     * @param markerColors The colors for map markers
     * @return A MapColor instance for the satellite map style
     */
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

    /**
     * Creates a default color configuration for the dark map style.
     *
     * @param gridLineColor The color for grid lines
     * @param markerColors The colors for map markers
     * @return A MapColor instance for the dark map style
     */
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

    /**
     * Creates a default marker color configuration with customizable parameters.
     *
     * @param selectedZoomOutColor The color scheme for the selected marker when zoomed out
     * @param defaultMarkerColor The default color scheme for markers
     * @param selectedColor The color for the selected marker
     * @return A MarkerColors instance with the specified parameters
     */
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