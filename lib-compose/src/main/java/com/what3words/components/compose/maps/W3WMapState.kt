package com.what3words.components.compose.maps

import com.google.android.gms.maps.model.LatLng

data class W3WMapState(
    val cameraPosition: W3WMapCameraPosition? = null,
    val selectedMarker: W3WMapMarker? = null,
    val markers: List<W3WMapMarker> = emptyList()
)

data class W3WMapMarker(
    val position: LatLng,
    val title: String? = null,
    val snippet: String? = null
)

data class W3WMapCameraPosition(
    val zoom: Float,
    val target: LatLng
)
