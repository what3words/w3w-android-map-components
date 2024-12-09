package com.what3words.components.compose.maps.mapper

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMapType

fun W3WLatLng.toGoogleLatLng(): LatLng {
    return LatLng(this.lat, this.lng)
}

fun LatLng.toW3WLatLng(): W3WLatLng {
    return W3WLatLng(this.latitude, this.longitude)
}

fun W3WMapType.toGoogleMapType(): MapType {
    return when (this) {
        W3WMapType.NORMAL -> MapType.NORMAL
        W3WMapType.SATELLITE -> MapType.SATELLITE
        W3WMapType.HYBRID -> MapType.HYBRID
        W3WMapType.TERRAIN -> MapType.TERRAIN
    }
}