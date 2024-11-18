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

fun com.what3words.components.compose.maps.models.W3WMapType.toGoogleMapType(): MapType {
    return when (this) {
        com.what3words.components.compose.maps.models.W3WMapType.NORMAL -> MapType.NORMAL
        com.what3words.components.compose.maps.models.W3WMapType.SATELLITE -> MapType.SATELLITE
        com.what3words.components.compose.maps.models.W3WMapType.HYBRID -> MapType.HYBRID
        com.what3words.components.compose.maps.models.W3WMapType.TERRAIN -> MapType.TERRAIN
    }
}