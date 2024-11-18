package com.what3words.components.compose.maps.mapper

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import com.what3words.core.types.geometry.W3WCoordinates

fun W3WCoordinates.toGoogleLatLng(): LatLng {
    return LatLng(this.lat, this.lng)
}

fun LatLng.toW3WCoordinates(): W3WCoordinates {
    return W3WCoordinates(this.latitude, this.longitude)
}

fun com.what3words.components.compose.maps.models.MapType.toGoogleMapType(): MapType {
    return when (this) {
        com.what3words.components.compose.maps.models.MapType.NORMAL -> MapType.NORMAL
        com.what3words.components.compose.maps.models.MapType.SATELLITE -> MapType.SATELLITE
        com.what3words.components.compose.maps.models.MapType.HYBRID -> MapType.HYBRID
        com.what3words.components.compose.maps.models.MapType.TERRAIN -> MapType.TERRAIN
    }
}