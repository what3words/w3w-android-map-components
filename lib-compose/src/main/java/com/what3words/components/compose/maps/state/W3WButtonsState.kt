package com.what3words.components.compose.maps.state

import androidx.compose.runtime.Immutable

@Immutable
import androidx.compose.ui.graphics.Color

data class W3WButtonsState(

    // My Location button
    val accuracyDistance: Float = 0.0F,
    val isLocationActive: Boolean = false,

    // Recall button
    val arrowColor: Color = Color.White,
    val backgroundColor: Color = Color.Red,
    val rotationDegree: Float = 0F,
)