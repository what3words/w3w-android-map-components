package com.what3words.components.compose.maps.mapper

import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WSquare
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

fun W3WRectangle.toW3WSquare(): W3WSquare {
    return W3WSquare(this.southwest.toW3WLatLong(), this.northeast.toW3WLatLong())
}

fun W3WCoordinates.toW3WLatLong(): W3WLatLng {
    return W3WLatLng(this.lat, this.lng)
}