package com.what3words.components.compose.maps.models

import com.what3words.core.types.geometry.W3WCoordinates

data class W3WCameraPosition(
    val coordinates: W3WCoordinates,
    val zoom: Float,
    val bearing: Float,
    val tilt: Float
)