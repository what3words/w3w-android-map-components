package com.what3words.components.compose.maps.buttons

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.what3words.design.library.ui.theme.w3wColorScheme

object W3WMapButtonsDefault {
    @Immutable
    data class ButtonColors(
        val locationButtonColor: LocationButtonColor
    )

    @Immutable
    data class LocationButtonConfig(
        val enterAnimation: EnterTransition,
        val exitAnimation: ExitTransition,
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
    fun defaultButtonColors(
        locationButtonColor: LocationButtonColor = defaultLocationButtonColor()
    ): ButtonColors {
        return ButtonColors(
            locationButtonColor = locationButtonColor
        )
    }

    @Composable
    fun defaultLocationButtonColor(
        locationBackgroundColor: Color = MaterialTheme.colorScheme.surface,
        locationIconColorInactive: Color = Color(0xFFAAABAE),
        locationIconColorActive: Color = Color(0xFF14B5FF),
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
        enterAnimation: EnterTransition = expandHorizontally(
            animationSpec = tween(durationMillis = 1000),
            expandFrom = Alignment.Start
        ),
        exitAnimation: ExitTransition = shrinkHorizontally(
            animationSpec = tween(durationMillis = 1000),
            shrinkTowards = Alignment.Start
        ),
        locationButtonSize: Dp = 50.dp,
        locationIconSize: Dp = 30.dp,
        accuracyIndicatorSize: Dp = 20.dp,
        accuracyTextStyle: TextStyle = MaterialTheme.typography.labelSmall
    ): LocationButtonConfig {
        return LocationButtonConfig(
            enterAnimation = enterAnimation,
            exitAnimation = exitAnimation,
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