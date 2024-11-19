package com.what3words.components.compose.maps.mapper

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import com.what3words.components.compose.maps.models.W3WCameraPosition
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.core.types.geometry.W3WCoordinates

fun W3WCoordinates.toGoogleLatLng(): LatLng {
    return LatLng(this.lat, this.lng)
}

fun LatLng.toW3WCoordinates(): W3WCoordinates {
    return W3WCoordinates(this.latitude, this.longitude)
}

fun W3WMapType.toGoogleMapType(): MapType {
    return when (this) {
        W3WMapType.NORMAL -> MapType.NORMAL
        W3WMapType.SATELLITE -> MapType.SATELLITE
        W3WMapType.HYBRID -> MapType.HYBRID
        W3WMapType.TERRAIN -> MapType.TERRAIN
    }
}

fun W3WCameraPosition.toGoogleCameraPosition(): CameraPosition {
    return CameraPosition(
        this.coordinates.toGoogleLatLng(),
        this.zoom,
        this.tilt,
        this.bearing
    )
}