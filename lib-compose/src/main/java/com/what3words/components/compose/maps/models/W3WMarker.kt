package com.what3words.components.compose.maps.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Immutable
data class W3WMarker(
    val words: String,
    val square: Square,
    val color: MarkerColor,
    val title: String? = null,
    val snippet: String? = null
) {
    val id: Long by lazy { generateUniqueId() }

    private fun generateUniqueId(): Long {
        checkNotNull(this.square.center) { "Center can not be null" }

        if (this.square.center.lat < -90 || this.square.center.lat > 90) {
            throw IllegalArgumentException("Invalid latitude value: must be between -90 and 90")
        }
        if (this.square.center.lng < -180 || this.square.center.lng > 180) {
            throw IllegalArgumentException("Invalid longitude value: must be between -180 and 180")
        }
        val latBits = (this.square.center.lat * 1e6).toLong() shl 32
        val lngBits = (this.square.center.lng * 1e6).toLong() and 0xffffffff
        return (latBits or lngBits)
    }
}

@Immutable
data class MarkerColor(
    val background: Color,
    val slash: Color
) {
    val id: Long
        get() {
            val backgroundLong = background.toArgb().toLong() and 0xFFFFFFFFL
            val slashLong = slash.toArgb().toLong() and 0xFFFFFFFFL
            return (backgroundLong shl 32) or slashLong
        }
}