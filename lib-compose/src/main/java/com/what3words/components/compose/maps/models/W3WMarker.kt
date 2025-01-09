package com.what3words.components.compose.maps.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

@Immutable
data class W3WMarker(
    val words: String,
    val square: W3WRectangle,
    val color: MarkerColor,
    val center: W3WCoordinates,
    val title: String? = null,
    val snippet: String? = null
)

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