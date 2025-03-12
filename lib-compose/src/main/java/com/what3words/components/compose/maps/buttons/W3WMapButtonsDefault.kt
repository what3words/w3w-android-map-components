package com.what3words.components.compose.maps.buttons

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault.defaultLocationButtonConfig
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault.defaultMapSwitchButtonLayoutConfig
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault.defaultRecallButtonLayoutConfig
import com.what3words.design.library.ui.theme.w3wColorScheme

/**
 * Default values and configurations for the map buttons components.
 */
object W3WMapButtonsDefault {
    /**
     * Configuration for map buttons layout.
     * @property buttonSpacing Spacing between buttons.
     * @property recallButtonLayoutConfig Layout configuration for the recall button.
     * @property locationButtonLayoutConfig Layout configuration for the location button.
     * @property mapSwitchButtonLayoutConfig Layout configuration for the map switch button.
     */
    @Immutable
    data class ButtonLayoutConfig(
        val buttonSpacing: Dp,
        val recallButtonLayoutConfig: RecallButtonLayoutConfig,
        val locationButtonLayoutConfig: LocationButtonLayoutConfig,
        val mapSwitchButtonLayoutConfig: MapSwitchButtonLayoutConfig
    )

    /**
     * Layout configuration for the recall button.
     * @property buttonSize Size of the recall button.
     * @property imageSize Size of the image inside recall button.
     * @property buttonPadding Padding for the recall button.
     * @property imagePadding Padding for the image inside recall button.
     * @property elevation Elevation (shadow) of the recall button.
     * @property disabledIconOpacity Opacity of the icon when disabled.
     */
    @Immutable
    data class RecallButtonLayoutConfig(
        val buttonSize: Dp,
        val imageSize: Dp,
        val buttonPadding: PaddingValues,
        val imagePadding: PaddingValues,
        val elevation: Dp,
        val disabledIconOpacity: Float,
    )

    /**
     * Layout configuration for the location button.
     * @property buttonVisibleAnimation Animation when button becomes visible.
     * @property accuracyEnterAnimation Animation when accuracy indicator appears.
     * @property accuracyExitAnimation Animation when accuracy indicator disappears.
     * @property locationButtonSize Size of the location button.
     * @property locationIconSize Size of the location icon.
     * @property accuracyIndicatorSize Size of the accuracy indicator.
     * @property accuracyTextStyle Text style for accuracy text.
     * @property disabledIconOpacity Opacity of the icon when disabled.
     * @property padding Padding around the map switch button, should be equal to or greater than elevation.
     * @property elevation Elevation (shadow) of the location button.
     */
    @Immutable
    data class LocationButtonLayoutConfig(
        val buttonVisibleAnimation: EnterTransition,
        val accuracyEnterAnimation: EnterTransition,
        val accuracyExitAnimation: ExitTransition,
        val locationButtonSize: Dp,
        val locationIconSize: Dp,
        val accuracyIndicatorSize: Dp,
        val accuracyTextStyle: TextStyle,
        val disabledIconOpacity: Float,
        val padding: PaddingValues,
        val elevation: Dp
    )

    /**
     * Layout configuration for the map switch button.
     * @property buttonSize Size of the map switch button.
     * @property padding Padding around the map switch button, should be equal to or greater than elevation.
     * @property elevation Elevation (shadow) of the map switch button.
     * @property disabledIconOpacity Opacity of the icon when disabled.
     */
    @Immutable
    data class MapSwitchButtonLayoutConfig(
        val buttonSize: Dp,
        val padding: PaddingValues,
        val elevation: Dp,
        val disabledIconOpacity: Float,
    )

    /**
     * Color configuration for the location button.
     * @property locationBackgroundColor Background color of the location button.
     * @property locationIconColorInactive Color of the location icon when inactive.
     * @property locationIconColorActive Color of the location icon when active.
     * @property warningLowBackgroundColor Background color for low warning state.
     * @property warningLowIconColor Icon color for low warning state.
     * @property warningHighBackgroundColor Background color for high warning state.
     * @property warningHighIconColor Icon color for high warning state.
     * @property accuracyBackgroundColor Background color for accuracy indicator.
     * @property accuracyTextColor Text color for accuracy indicator.
     */
    @Immutable
    data class LocationButtonColor(
        val locationBackgroundColor: Color,
        val locationIconColorInactive: Color,
        val locationIconColorActive: Color,
        val warningLowBackgroundColor: Color,
        val warningLowIconColor: Color,
        val warningHighBackgroundColor: Color,
        val warningHighIconColor: Color,
        val accuracyBackgroundColor: Color,
        val accuracyTextColor: Color,
    )

    /**
     * Color configuration for the recall button.
     * @property recallArrowColor Color of the arrow in the recall button.
     * @property recallBackgroundColor Background color of the recall button.
     */
    @Immutable
    data class RecallButtonColor(
        val recallArrowColor: Color,
        val recallBackgroundColor: Color
    )

    /**
     * String resources for buttons.
     * @property accuracyMessage Format string for accuracy message.
     */
    @Immutable
    data class ResourceString(
        val accuracyMessage: String
    )

    /**
     * Content descriptions for accessibility.
     * @property locationButtonDescription Content description for location button.
     * @property warningIconDescription Content description for warning icon.
     * @property recallButtonDescription Content description for recall button.
     * @property mapSwitchButtonDescription Content description for map switch button.
     */
    @Immutable
    data class ContentDescription(
        val locationButtonDescription: String,
        val warningIconDescription: String,
        val recallButtonDescription: String,
        val mapSwitchButtonDescription: String,
    )

    /**
     * Creates default button layout configuration.
     * @param recallButtonLayoutConfig Layout configuration for the recall button. Default is obtained from [defaultRecallButtonLayoutConfig].
     * @param locationButtonLayoutConfig Layout configuration for the location button. Default is obtained from [defaultLocationButtonConfig].
     * @param mapSwitchButtonLayoutConfig Layout configuration for the map switch button. Default is obtained from [defaultMapSwitchButtonLayoutConfig].
     * @return [ButtonLayoutConfig] object with the specified configurations.
     */
    @Composable
    fun defaultButtonLayoutConfig(
        buttonSpacing: Dp = 12.dp,
        recallButtonLayoutConfig: RecallButtonLayoutConfig = defaultRecallButtonLayoutConfig(),
        locationButtonLayoutConfig: LocationButtonLayoutConfig = defaultLocationButtonConfig(),
        mapSwitchButtonLayoutConfig: MapSwitchButtonLayoutConfig = defaultMapSwitchButtonLayoutConfig()
    ): ButtonLayoutConfig {
        return ButtonLayoutConfig(
            buttonSpacing = buttonSpacing,
            recallButtonLayoutConfig = recallButtonLayoutConfig,
            locationButtonLayoutConfig = locationButtonLayoutConfig,
            mapSwitchButtonLayoutConfig = mapSwitchButtonLayoutConfig
        )
    }

    /**
     * Creates default map switch button layout configuration.
     * @param buttonSize Size of the map switch button. Default is 50.dp.
     * @param padding Padding around the map switch button. Default is 4.dp.
     * @param elevation Elevation (shadow) of the map switch button. Default is 3.dp.
     * @param disabledIconOpacity Opacity of the icon when disabled. Default is 0.38f.
     * @return [MapSwitchButtonLayoutConfig] object with the specified measurements.
     */
    @Composable
    fun defaultMapSwitchButtonLayoutConfig(
        buttonSize: Dp = 50.dp,
        padding: PaddingValues = PaddingValues(4.dp),
        elevation: Dp = 3.dp,
        disabledIconOpacity: Float = 0.38f,
    ): MapSwitchButtonLayoutConfig {
        return MapSwitchButtonLayoutConfig(
            buttonSize = buttonSize,
            padding = padding,
            elevation = elevation,
            disabledIconOpacity = disabledIconOpacity
        )
    }

    /**
     * Creates default recall button layout configuration.
     * @param buttonSize Size of the recall button. Default is 50.dp.
     * @param imageSize Size of the image inside the recall button. Default is 30.dp.
     * @param buttonPadding Padding for the recall button. Default is 4.dp on all sides.
     * @param imagePadding Padding for the image inside the recall button. Default is 1.25.dp on all sides.
     * @param elevation Elevation (shadow) of the recall button. Default is 3.dp.
     * @param disabledIconOpacity Opacity of the icon when disabled. Default is 0.38f.
     * @return [RecallButtonLayoutConfig] object with the specified measurements.
     */
    @Composable
    fun defaultRecallButtonLayoutConfig(
        buttonSize: Dp = 50.dp,
        imageSize: Dp = 30.dp,
        buttonPadding: PaddingValues = PaddingValues(4.dp),
        imagePadding: PaddingValues = PaddingValues(1.25.dp),
        elevation: Dp = 3.dp,
        disabledIconOpacity: Float = 0.38f,
    ): RecallButtonLayoutConfig {
        return RecallButtonLayoutConfig(
            buttonSize = buttonSize,
            imageSize = imageSize,
            buttonPadding = buttonPadding,
            imagePadding = imagePadding,
            elevation = elevation,
            disabledIconOpacity = disabledIconOpacity
        )
    }

    /**
     * Creates default color configuration for the recall button.
     * @param recallArrowColor Color of the arrow in the recall button. Default is the slash color from default marker colors.
     * @param recallBackgroundColor Background color of the recall button. Default is the background color from default marker colors.
     * @return [RecallButtonColor] object with the specified colors.
     */
    @Composable
    fun defaultRecallButtonColor(
        recallArrowColor: Color = W3WMapDefaults.MARKER_COLOR_DEFAULT.slash,
        recallBackgroundColor: Color = W3WMapDefaults.MARKER_COLOR_DEFAULT.background
    ): RecallButtonColor {
        return RecallButtonColor(
            recallArrowColor = recallArrowColor,
            recallBackgroundColor = recallBackgroundColor
        )
    }

    /**
     * Creates default color configuration for the location button.
     * @param locationBackgroundColor Background color of the location button. Default is the surface color.
     * @param locationIconColorInactive Color of the location icon when inactive. Default is the outline color.
     * @param locationIconColorActive Color of the location icon when active. Default is the brand sky blue color.
     * @param warningLowBackgroundColor Background color for low warning state. Default is the warning color.
     * @param warningLowIconColor Icon color for low warning state. Default is the color that appears on warning background.
     * @param warningHighBackgroundColor Background color for high warning state. Default is the error color.
     * @param warningHighIconColor Icon color for high warning state. Default is the color that appears on error background.
     * @param accuracyBackgroundColor Background color for accuracy indicator. Default is semi-transparent white.
     * @param accuracyTextColor Text color for accuracy indicator. Default is the color for text on secondary container.
     * @return [LocationButtonColor] object with the specified colors.
     */
    @Composable
    fun defaultLocationButtonColor(
        locationBackgroundColor: Color = MaterialTheme.colorScheme.surface,
        locationIconColorInactive: Color = MaterialTheme.colorScheme.outline,
        locationIconColorActive: Color = MaterialTheme.w3wColorScheme.brandCustomSkyBlue,
        warningLowBackgroundColor: Color = MaterialTheme.w3wColorScheme.warning,
        warningLowIconColor: Color = MaterialTheme.w3wColorScheme.onWarning,
        warningHighBackgroundColor: Color = MaterialTheme.colorScheme.error,
        warningHighIconColor: Color = MaterialTheme.colorScheme.onError,
        accuracyBackgroundColor: Color = Color.White.copy(alpha = 0.16f),
        accuracyTextColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    ): LocationButtonColor {
        return LocationButtonColor(
            locationBackgroundColor = locationBackgroundColor,
            locationIconColorInactive = locationIconColorInactive,
            locationIconColorActive = locationIconColorActive,
            warningLowBackgroundColor = warningLowBackgroundColor,
            warningLowIconColor = warningLowIconColor,
            warningHighBackgroundColor = warningHighBackgroundColor,
            warningHighIconColor = warningHighIconColor,
            accuracyBackgroundColor = accuracyBackgroundColor,
            accuracyTextColor = accuracyTextColor,
        )
    }

    /**
     * Creates default layout configuration for the location button.
     * @param buttonVisibleAnimation Animation when button becomes visible. Default is a fade in with slide up animation.
     * @param accuracyEnterAnimation Animation when accuracy indicator appears. Default is horizontal expansion animation.
     * @param accuracyExitAnimation Animation when accuracy indicator disappears. Default is horizontal shrinking animation.
     * @param locationButtonSize Size of the location button. Default is 50.dp.
     * @param locationIconSize Size of the location icon. Default is 30.dp.
     * @param accuracyIndicatorSize Size of the accuracy indicator. Default is 20.dp.
     * @param accuracyTextStyle Text style for accuracy text. Default is the small label typography style.
     * @param disabledIconOpacity Opacity of the icon when disabled. Default is 0.38f.
     * @param padding Padding around the location button, should be equal to or greater than elevation. Default is 4.dp.
     * @param elevation Elevation (shadow) of the location button. Default is 3.dp.
     * @return [LocationButtonLayoutConfig] object with the specified configurations.
     */
    @Composable
    fun defaultLocationButtonConfig(
        buttonVisibleAnimation: EnterTransition = fadeIn(
            animationSpec = tween(durationMillis = 800, delayMillis = 400)
        ) + slideIn(
            initialOffset = { IntOffset(0, 20) }, // Move up 20px
            animationSpec = tween(durationMillis = 800, delayMillis = 400)
        ),
        accuracyEnterAnimation: EnterTransition = expandHorizontally(
            animationSpec = tween(durationMillis = 1000),
            expandFrom = Alignment.Start
        ),
        accuracyExitAnimation: ExitTransition = shrinkHorizontally(
            animationSpec = tween(durationMillis = 1000),
            shrinkTowards = Alignment.Start
        ),
        locationButtonSize: Dp = 50.dp,
        locationIconSize: Dp = 30.dp,
        accuracyIndicatorSize: Dp = 20.dp,
        accuracyTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
        disabledIconOpacity: Float = 0.38f,
        padding: PaddingValues = PaddingValues(4.dp),
        elevation: Dp = 3.dp
    ): LocationButtonLayoutConfig {
        return LocationButtonLayoutConfig(
            buttonVisibleAnimation = buttonVisibleAnimation,
            accuracyEnterAnimation = accuracyEnterAnimation,
            accuracyExitAnimation = accuracyExitAnimation,
            locationButtonSize = locationButtonSize,
            locationIconSize = locationIconSize,
            accuracyIndicatorSize = accuracyIndicatorSize,
            accuracyTextStyle = accuracyTextStyle,
            disabledIconOpacity = disabledIconOpacity,
            padding = padding,
            elevation = elevation
        )
    }

    /**
     * Creates default resource strings for buttons.
     * @param accuracyMessage Format string for accuracy message. Default is "GPS Accuracy (%d%s)".
     * @return [ResourceString] object with the specified strings.
     */
    fun defaultResourceString(
        accuracyMessage: String = "GPS Accuracy (%d%s)"
    ): ResourceString {
        return ResourceString(
            accuracyMessage = accuracyMessage
        )
    }

    /**
     * Creates default content descriptions for accessibility.
     * @param locationButtonDescription Content description for location button. Default is empty.
     * @param warningIconDescription Content description for warning icon. Default is empty.
     * @param recallButtonDescription Content description for recall button. Default is empty.
     * @param mapSwitchButtonDescription Content description for map switch button. Default is empty.
     * @return [ContentDescription] object with the specified descriptions.
     */
    fun defaultContentDescription(
        locationButtonDescription: String = "",
        warningIconDescription: String = "",
        recallButtonDescription: String = "",
        mapSwitchButtonDescription: String = "",
    ): ContentDescription {
        return ContentDescription(
            locationButtonDescription = locationButtonDescription,
            warningIconDescription = warningIconDescription,
            recallButtonDescription = recallButtonDescription,
            mapSwitchButtonDescription = mapSwitchButtonDescription,
        )
    }
}