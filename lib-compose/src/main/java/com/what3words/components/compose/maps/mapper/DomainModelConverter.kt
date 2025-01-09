package com.what3words.components.compose.maps.mapper

import com.what3words.components.compose.maps.W3WMapDefaults.MARKER_COLOR_DEFAULT
import com.what3words.components.compose.maps.models.MarkerColor
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.core.types.domain.W3WAddress


fun W3WAddress.toW3WMarker(makerColor: MarkerColor = MARKER_COLOR_DEFAULT): W3WMarker {
    return W3WMarker(
        words = this.words,
        square = this.square!!,
        color = makerColor,
        center = this.center!!
    )
}