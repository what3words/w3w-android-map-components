package com.what3words.components.compose.maps.models

import androidx.compose.runtime.Immutable
import com.what3words.core.types.geometry.W3WCoordinates
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Data class representing the grid lines displayed on a What3Words (W3W) map.
 *
 * This class holds the coordinates for the vertical and horizontal lines that
 * form the grid overlay on the map, visually representing the W3W squares.
 *
 * @property verticalLines A list of [W3WCoordinates] representing the vertical grid lines. Defaults to an empty list.
 * @property horizontalLines A list of [W3WCoordinates] representing the horizontal grid lines. Defaults to an empty list.
 */
@Immutable
data class W3WGridLines(
    val verticalLines: ImmutableList<W3WCoordinates> = persistentListOf(),
    val horizontalLines: ImmutableList<W3WCoordinates> = persistentListOf()
)