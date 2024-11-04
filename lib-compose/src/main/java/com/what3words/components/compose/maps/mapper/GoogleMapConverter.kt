package com.what3words.components.compose.maps.mapper

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.core.types.geometry.W3WCoordinates

internal fun W3WCoordinates.toLatLng(): LatLng {
    return LatLng(this.lat,this.lng)
}

internal fun W3WMapState.MapType.toMapType(): MapType {
    return when(this) {
        W3WMapState.MapType.NORMAL -> MapType.NORMAL
        W3WMapState.MapType.SATELLITE -> MapType.SATELLITE
        W3WMapState.MapType.HYBRID -> MapType.HYBRID
        W3WMapState.MapType.TERRAIN -> MapType.TERRAIN
    }
}