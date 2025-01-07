package com.what3words.components.compose.maps.mapper

import com.what3words.components.compose.maps.W3WMapDefaults.MARKER_COLOR_DEFAULT
import com.what3words.components.compose.maps.models.MarkerColor
import com.what3words.components.compose.maps.models.Square
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

fun W3WCoordinates.toW3WLatLong(): W3WLatLng {
    return W3WLatLng(this.lat, this.lng)
}

fun W3WAddress.toW3WMarker(makerColor: MarkerColor = MARKER_COLOR_DEFAULT): W3WMarker {
    return W3WMarker(
        words = this.words,
        square = Square(
            southwest = this.square!!.southwest.toW3WLatLong(),
            northeast = this.square!!.northeast.toW3WLatLong(),
            center = this.center!!.toW3WLatLong(),
        ),
        color = makerColor
    )
}

fun W3WLatLng.toW3WCoordinates(): W3WCoordinates {
    return W3WCoordinates(this.lat, this.lng)
}

fun Square.toW3WRectangle(): W3WRectangle {
    return W3WRectangle(
        southwest = this.southwest.toW3WCoordinates(),
        northeast = this.northeast.toW3WCoordinates()
    )
}