package com.what3words.components.compose.maps.models

import androidx.compose.ui.graphics.Color
import com.what3words.core.types.domain.W3WAddress

data class Marker(
    val address: W3WAddress,
    val color: Color,
    val title: String? = null,
    val snippet: String? = null
)