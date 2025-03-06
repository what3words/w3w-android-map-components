package com.what3words.components.compose.maps.extensions

import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import java.util.Collections

fun MutableMap<String, MutableList<W3WMarker>>.addMarker(
    listName: String,
    marker: W3WMarker,
): W3WResult<W3WMarker> {
    // Get or create the current list of markers (using MutableList for in-place updates)
    val currentList = this.getOrPut(listName) { Collections.synchronizedList(mutableListOf()) }

    // Check if a marker with the same ID already exists
    val index = currentList.indexOfFirst { it.square.id == marker.square.id }

    return if (index != -1) {
        // If a marker already exists, throw an exception
        W3WResult.Failure(W3WError("Marker with coordinates ${marker.center} already exists in the list '$listName'."))
    } else {
        // Marker doesn't exist, add it to the list
        currentList.add(marker)
        W3WResult.Success(marker)
    }
}

/**
 * Convert Map maker to list marker with unique ID
 *
 * @return list of W3WMarker
 */
fun MutableMap<String, MutableList<W3WMarker>>.toMarkers(): List<W3WMarker> {
    return this.values.flatten()
}