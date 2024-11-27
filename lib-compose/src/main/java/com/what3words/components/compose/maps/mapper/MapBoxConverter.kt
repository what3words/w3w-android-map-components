package com.what3words.components.compose.maps.mapper

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.what3words.components.compose.maps.models.W3WCameraPosition
import com.what3words.components.compose.maps.models.W3WMapType

fun W3WMapType.toMapBoxMapType(isDarkMode: Boolean): String {
    return when (this) {
        W3WMapType.NORMAL -> if (isDarkMode) Style.DARK else Style.MAPBOX_STREETS
        W3WMapType.SATELLITE -> Style.SATELLITE_STREETS
        W3WMapType.HYBRID -> Style.SATELLITE_STREETS
        W3WMapType.TERRAIN -> Style.OUTDOORS
    }
}

fun W3WCameraPosition.toMapBoxCameraOptions(): CameraOptions {
    return CameraOptions.Builder()
        .pitch(this.tilt.toDouble())
        .bearing(this.bearing.toDouble())
        .center(Point.fromLngLat(this.latLng.lng, this.latLng.lat))
        .zoom(this.zoom.toDouble())
        .build()
}