package com.what3words.components.compose.maps.mapper

import com.what3words.components.compose.maps.W3WMapDefaults.MARKER_COLOR_DEFAULT
import com.what3words.components.compose.maps.extensions.computeHorizontalLines
import com.what3words.components.compose.maps.extensions.computeVerticalLines
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WGridSection
import kotlinx.collections.immutable.toImmutableList


fun W3WAddress.toW3WMarker(makerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT): W3WMarker {
    return W3WMarker(
        words = this.words,
        square = this.square!!,
        color = makerColor,
        center = this.center!!
    )
}

fun W3WGridSection.toW3WGridLines(): W3WGridLines {
    return W3WGridLines(
        verticalLines = this.lines.computeVerticalLines().toImmutableList(),
        horizontalLines = this.lines.computeHorizontalLines().toImmutableList()
    )
}