package com.what3words.components.compose.maps.models

import androidx.compose.ui.graphics.Color

data class W3WMarker(
    val words: String,
    val latLng: W3WLatLng,
    val square: W3WSquare? = null,
    val color: W3WMarkerColor? = null,
    val title: String? = null,
    val snippet: String? = null
)

data class W3WMarkerColor(
    val background: Color,
    val slash: Color
)