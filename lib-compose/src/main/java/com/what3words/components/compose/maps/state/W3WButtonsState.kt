package com.what3words.components.compose.maps.state

import androidx.compose.runtime.Immutable

@Immutable
data class W3WButtonsState(

    // My Location button
    val accuracyDistance: Float = 0.0F,
    val isLocationActive: Boolean = false,

    // Recall button
)