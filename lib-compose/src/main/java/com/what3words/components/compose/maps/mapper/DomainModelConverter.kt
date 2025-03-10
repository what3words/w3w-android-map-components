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

/**
 * Converts a [W3WAddress] object into a [W3WMarker] object.
 * 
 * @param makerColor The color of the marker. Defaults to [MARKER_COLOR_DEFAULT].
 * @return A [W3WMarker] object created from the [W3WAddress].
 */
fun W3WAddress.toW3WMarker(makerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT): W3WMarker {
    return W3WMarker(
        words = this.words,
        square = this.square!!,
        color = makerColor,
        center = this.center!!
    )
}

/**
 * Converts a [W3WGridSection] object into a [W3WGridLines] object.
 * 
 * @return A [W3WGridLines] object containing the vertical and horizontal grid lines.
 */
fun W3WGridSection.toW3WGridLines(): W3WGridLines {
    return W3WGridLines(
        verticalLines = this.lines.computeVerticalLines().toImmutableList(),
        horizontalLines = this.lines.computeHorizontalLines().toImmutableList()
    )
}