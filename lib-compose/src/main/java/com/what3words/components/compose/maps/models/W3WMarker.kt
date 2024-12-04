package com.what3words.components.compose.maps.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class W3WMarker(
    val words: String,
    val latLng: W3WLatLng,
    val square: W3WSquare,
    val color: W3WMarkerColor,
    val title: String? = null,
    val snippet: String? = null
) {
    val id: Long by lazy { generateUniqueId() }

    private fun generateUniqueId(): Long {
        if (this.latLng.lat < -90 || this.latLng.lat > 90) {
            throw IllegalArgumentException("Invalid latitude value: must be between -90 and 90")
        }
        if (this.latLng.lng < -180 || this.latLng.lng > 180) {
            throw IllegalArgumentException("Invalid longitude value: must be between -180 and 180")
        }
        val latBits = (this.latLng.lat * 1e6).toLong() shl 32
        val lngBits = (this.latLng.lng * 1e6).toLong() and 0xffffffff
        return (latBits or lngBits)
    }
}

@Immutable
data class W3WMarkerColor(
    val background: Color,
    val slash: Color
)