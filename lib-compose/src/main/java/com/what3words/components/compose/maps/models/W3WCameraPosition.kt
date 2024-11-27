package com.what3words.components.compose.maps.models

data class W3WCameraPosition(
    val latLng: W3WLatLng,
    val zoom: Float,
    val bearing: Float,
    val tilt: Float
)