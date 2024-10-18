package com.what3words.components.compose.maps.mapper

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.core.types.geometry.W3WCoordinates

internal fun W3WCoordinates.toLatLng(): LatLng {
    return LatLng(this.lat,this.lng)
}

internal fun W3WMapType.toMapType(): MapType {
    return when(this) {
        W3WMapType.NORMAL -> MapType.NORMAL
        W3WMapType.SATELLITE -> MapType.SATELLITE
        W3WMapType.HYBRID -> MapType.HYBRID
        W3WMapType.TERRAIN -> MapType.TERRAIN
    }
}