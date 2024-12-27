package com.what3words.components.compose.maps.mapper

import com.what3words.components.compose.maps.W3WMapDefaults.LOCATION_DEFAULT
import com.what3words.components.compose.maps.W3WMapDefaults.MARKER_COLOR_DEFAULT
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.models.W3WSquare
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

fun W3WRectangle.toW3WSquare(): W3WSquare {
    return W3WSquare(this.southwest.toW3WLatLong(), this.northeast.toW3WLatLong())
}

fun W3WCoordinates.toW3WLatLong(): W3WLatLng {
    return W3WLatLng(this.lat, this.lng)
}

fun W3WAddress.toW3WMarker(makerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT): W3WMarker {
    return W3WMarker(
        words = this.words,
        square = this.square!!.toW3WSquare(),
        latLng = this.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
        color = makerColor
    )
}

fun W3WLatLng.toW3WCoordinates(): W3WCoordinates {
    return W3WCoordinates(this.lat, this.lng)
}

fun W3WSquare.toW3WRectangle(): W3WRectangle {
    return W3WRectangle(
        southwest = this.southwest.toW3WCoordinates(),
        northeast = this.northeast.toW3WCoordinates()
    )

}