package com.what3words.components.compose.maps.buttons

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object W3WMapButtonsDefault {

    data class LocationButtonConfig(
        val enterAnimation: EnterTransition,
        val exitAnimation: ExitTransition,
        val locationButtonSize: Dp,
        val locationIconSize: Dp,
        val accuracyIndicatorSize: Dp,
        val accuracyTextStyle: TextStyle,
    )

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
            accuracyIndicatorSize = accuracyIndicatorSize,
            locationIconSize = locationIconSize,
            accuracyTextStyle = accuracyTextStyle
        )
    }
}