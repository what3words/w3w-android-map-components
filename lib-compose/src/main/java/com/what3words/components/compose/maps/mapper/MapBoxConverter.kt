package com.what3words.components.compose.maps.mapper

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.Style
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.core.types.geometry.W3WCoordinates

fun W3WMapState.MapType.toMapBoxMapType(isDarkMode: Boolean): String {
    return when (this) {
        W3WMapState.MapType.NORMAL -> if (isDarkMode) Style.DARK else Style.MAPBOX_STREETS
        W3WMapState.MapType.SATELLITE -> Style.SATELLITE_STREETS
        W3WMapState.MapType.HYBRID -> Style.SATELLITE_STREETS
        W3WMapState.MapType.TERRAIN -> Style.OUTDOORS
    }
}

fun W3WMapState.CameraPosition.toMapBoxCameraOptions(): CameraOptions {
    return CameraOptions.Builder().pitch(this.tilt.toDouble()).bearing(this.bearing.toDouble())
        .center(Point.fromLngLat(this.coordinates.lng, this.coordinates.lat))
        .zoom(this.zoom.toDouble()).build()
}

fun CameraState.toW3WCameraPosition(isMoving: Boolean): W3WMapState.CameraPosition {
    return W3WMapState.CameraPosition(
        zoom = this.zoom.toFloat(),
        bearing = this.bearing.toFloat(),
        coordinates = this.center.let { W3WCoordinates(it.latitude(), it.longitude()) },
        tilt = this.pitch.toFloat(),
        isMoving = isMoving,
    )
}