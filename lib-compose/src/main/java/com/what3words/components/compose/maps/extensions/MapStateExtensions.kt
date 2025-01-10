package com.what3words.components.compose.maps.extensions

import com.what3words.components.compose.maps.models.W3WMarker

internal fun MutableMap<String, MutableList<W3WMarker>>.addMarker(
    listName: String,
    marker: W3WMarker,
) {
    // Get or create the current list of markers (using MutableList for in-place updates)
    val currentList = this[listName] ?: mutableListOf()

    // Create a new list by either updating or adding the marker
    if (marker in currentList) {
        // Replace the existing marker if it exists (by id)
        val index = currentList.indexOfFirst { it.square.id == marker.square.id }
        if (index != -1) {
            currentList[index] = marker
        }
    } else {
        // Marker doesn't exist, add it to the list
        currentList.add(marker)
    }

    // Update the map with the modified list
    this[listName] = currentList
}

/**
 * Convert Map maker to list marker with unique ID
 *
 * @return list of W3WMarker
 */
internal fun MutableMap<String, MutableList<W3WMarker>>.toMarkers(): List<W3WMarker> {
    return this.values.flatten()
}