package com.what3words.components.compose.maps.models

import com.what3words.core.types.geometry.W3WCoordinates

/**
 * Data class representing the grid lines displayed on a What3Words (W3W) map.
 *
 * This class holds the coordinates for the vertical and horizontal lines that
 * form the grid overlay on the map, visually representing the W3W squares.
 *
 * @property verticalLines A list of [W3WCoordinates] representing the vertical grid lines. Defaults to an empty list.
 * @property horizontalLines A list of [W3WCoordinates] representing the horizontal grid lines. Defaults to an empty list.
 */
data class W3WGridLines(
    val verticalLines: List<W3WCoordinates> = emptyList(),
    val horizontalLines: List<W3WCoordinates> = emptyList()
)