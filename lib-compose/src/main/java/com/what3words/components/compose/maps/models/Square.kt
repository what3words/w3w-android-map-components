package com.what3words.components.compose.maps.models

import androidx.compose.runtime.Immutable
import com.what3words.core.types.geometry.W3WCoordinates

@Immutable
data class Square(
    val southwest: W3WLatLng,
    val northeast: W3WLatLng,
    val center: W3WLatLng? = null,
) {
    val id: Long by lazy { generateUniqueId() }

    private fun generateUniqueId(): Long {
        checkNotNull(center) { "Center can not be null" }

        if (this.center.lat < -90 || this.center.lat > 90) {
            throw IllegalArgumentException("Invalid latitude value: must be between -90 and 90")
        }
        if (this.center.lng < -180 || this.center.lng > 180) {
            throw IllegalArgumentException("Invalid longitude value: must be between -180 and 180")
        }
        val latBits = (this.center.lat * 1e6).toLong() shl 32
        val lngBits = (this.center.lng * 1e6).toLong() and 0xffffffff
        return (latBits or lngBits)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is W3WMarker) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return (id xor (id ushr 32)).toInt()
    }

    fun isEqual(coordinates: W3WCoordinates?): Boolean {
        if (this.center == null) return false
        if (coordinates == null) return false

        return this.center.lat == coordinates.lat && this.center.lng == coordinates.lng
    }
}