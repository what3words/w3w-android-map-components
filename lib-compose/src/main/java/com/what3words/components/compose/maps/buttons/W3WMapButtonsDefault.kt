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
import com.what3words.design.library.ui.theme.w3wColorScheme

object W3WMapButtonsDefault {
    @Immutable
    data class ButtonLayoutConfig(
        val recallButtonLayoutConfig: RecallButtonLayoutConfig,
        val locationButtonLayoutConfig: LocationButtonLayoutConfig
    )

    @Immutable
    data class RecallButtonLayoutConfig(
        val buttonSize: Dp,
        val imageSize: Dp,
        val buttonPadding: PaddingValues,
        val imagePadding: PaddingValues
    )

    @Immutable
    data class LocationButtonLayoutConfig(
        val buttonVisibleAnimation: EnterTransition,
        val accuracyEnterAnimation: EnterTransition,
        val accuracyExitAnimation: ExitTransition,
        val locationButtonSize: Dp,
        val locationIconSize: Dp,
        val accuracyIndicatorSize: Dp,
        val accuracyTextStyle: TextStyle,
    )

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

    @Immutable
    data class RecallButtonColor(
        val recallArrowColor: Color,
        val recallBackgroundColor: Color
    )

    @Immutable
    data class ResourceString(
        val accuracyMessage: String
    )

    @Immutable
    data class ContentDescription(
        val locationButtonDescription: String,
        val warningIconDescription: String,
        val recallButtonDescription: String,
        val mapSwitchButtonDescription: String,
    )

    @Composable
    fun defaultButtonLayoutConfig(
        recallButtonLayoutConfig: RecallButtonLayoutConfig = defaultRecallButtonLayoutConfig(),
        locationButtonLayoutConfig: LocationButtonLayoutConfig = defaultLocationButtonConfig()
    ): ButtonLayoutConfig {
        return ButtonLayoutConfig(
            recallButtonLayoutConfig = recallButtonLayoutConfig,
            locationButtonLayoutConfig = locationButtonLayoutConfig
        )
    }

    @Composable
    fun defaultRecallButtonLayoutConfig(
        buttonSize: Dp = 50.dp,
        imageSize: Dp = 30.dp,
        buttonPadding: PaddingValues = PaddingValues(4.dp),
        imagePadding: PaddingValues = PaddingValues(1.25.dp)
    ): RecallButtonLayoutConfig {
        return RecallButtonLayoutConfig(
            buttonSize = buttonSize,
            imageSize = imageSize,
            buttonPadding = buttonPadding,
            imagePadding = imagePadding
        )
    }

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

    @Composable
    fun defaultLocationButtonConfig(
        buttonVisibleAnimation: EnterTransition = fadeIn(animationSpec = tween(800)) + slideIn(
            initialOffset = { IntOffset(0, 20) }, // Move up 20px
            animationSpec = tween(800)
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
        accuracyTextStyle: TextStyle = MaterialTheme.typography.labelSmall
    ): LocationButtonLayoutConfig {
        return LocationButtonLayoutConfig(
            buttonVisibleAnimation = buttonVisibleAnimation,
            accuracyEnterAnimation = accuracyEnterAnimation,
            accuracyExitAnimation = accuracyExitAnimation,
            locationButtonSize = locationButtonSize,
            locationIconSize = locationIconSize,
            accuracyIndicatorSize = accuracyIndicatorSize,
            accuracyTextStyle = accuracyTextStyle,
        )
    }

    fun defaultResourceString(
        accuracyMessage: String = "GPS Accuracy (%d%s)"
    ): ResourceString {
        return ResourceString(
            accuracyMessage = accuracyMessage
        )
    }

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