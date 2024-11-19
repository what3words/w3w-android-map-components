package com.what3words.components.compose.maps.models

import com.what3words.core.types.geometry.W3WCoordinates

data class W3WGridLines(
    val verticalLines: List<W3WCoordinates> = emptyList(),
    val horizontalLines: List<W3WCoordinates> = emptyList()
)