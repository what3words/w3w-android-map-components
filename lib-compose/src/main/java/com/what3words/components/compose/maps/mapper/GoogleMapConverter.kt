package com.what3words.components.compose.maps.mapper

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapType
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.core.types.geometry.W3WCoordinates

fun W3WCoordinates.toGoogleLatLng(): LatLng {
    return LatLng(this.lat, this.lng)
}

fun LatLng.toW3WCoordinates(): W3WCoordinates {
    return W3WCoordinates(this.latitude, this.longitude)
}

fun W3WMapState.CameraPosition.toGoogleCameraPosition(): CameraPosition {
    return CameraPosition(
        this.coordinates.toGoogleLatLng(),
        this.zoom,
        this.tilt,
        this.bearing
    )
}

fun CameraPositionState.toW3WMapStateCameraPosition(): W3WMapState.CameraPosition {
    return W3WMapState.CameraPosition(
        coordinates = this.position.target.toW3WCoordinates(),
        zoom = this.position.zoom,
        bearing = this.position.bearing,
        isMoving = this.isMoving,
        tilt = this.position.tilt,
        isAnimated = false,
    )
}

fun W3WMapState.MapType.toGoogleMapType(): MapType {
    return when (this) {
        W3WMapState.MapType.NORMAL -> MapType.NORMAL
        W3WMapState.MapType.SATELLITE -> MapType.SATELLITE
        W3WMapState.MapType.HYBRID -> MapType.HYBRID
        W3WMapState.MapType.TERRAIN -> MapType.TERRAIN
    }
}