package com.what3words.components.compose.maps.mapper

import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.core.types.geometry.W3WCoordinates

fun W3WMapType.toMapBoxMapType(isDarkMode: Boolean): String {
    return when (this) {
        W3WMapType.NORMAL -> if (isDarkMode) Style.DARK else Style.MAPBOX_STREETS
        W3WMapType.SATELLITE -> Style.SATELLITE_STREETS
        W3WMapType.HYBRID -> Style.SATELLITE_STREETS
        W3WMapType.TERRAIN -> Style.OUTDOORS
    }
}

fun W3WCoordinates.toMapBoxPoint(): Point {
    return Point.fromLngLat(this.lng, this.lat)
}

fun Point.toW3WCoordinates(): W3WCoordinates {
    return W3WCoordinates(this.longitude(), this.latitude())
}