package com.what3words.components.compose.maps.models

import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.core.types.domain.W3WAddress

internal data class W3WAddressWithStyle(
    val address: W3WAddress,
    val markerColor: W3WMarkerColor
)