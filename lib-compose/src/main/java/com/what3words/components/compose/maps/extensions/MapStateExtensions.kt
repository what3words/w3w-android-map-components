package com.what3words.components.compose.maps.extensions

import com.what3words.components.compose.maps.models.MarkerType
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WSquare
import com.what3words.components.compose.maps.state.LIST_DEFAULT_ID

internal fun MutableMap<String, MutableList<W3WMarker>>.addListMarker(
    listName: String = LIST_DEFAULT_ID,
    markers: List<W3WMarker>,
) {
    if (markers.isEmpty()) return

    val currentList = this.getOrPut(listName) { mutableListOf() }

    markers.forEach { marker ->
        // Check if the marker already exists (based on its `id`)
        val index = currentList.indexOfFirst { it.id == marker.id }

        if (index != -1) {
            // If the marker exists, update it with the new one
            currentList[index] = marker
        } else {
            // If the marker does not exist, add it to the list
            currentList.add(marker)
        }
    }

    this[listName] = currentList
}

internal fun MutableMap<String, MutableList<W3WMarker>>.addMarker(
    listName: String = LIST_DEFAULT_ID,
    marker: W3WMarker,
) {
    // Get or create the current list of markers (using MutableList for in-place updates)
    val currentList = this[listName] ?: mutableListOf()

    // Create a new list by either updating or adding the marker
    if (marker in currentList) {
        // Replace the existing marker if it exists (by id)
        val index = currentList.indexOfFirst { it.id == marker.id }
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
    return this.values.flatten().map { item ->
        item.copy(
            type = getMarkerType(
                item,
                this
            )
        )
    }
}

internal fun getMarkerType(
    marker: W3WMarker,
    listMarkers: Map<String, List<W3WMarker>>
): MarkerType {
    return when (listMarkers.values.flatten().count { it.id == marker.id }) {
        0 -> MarkerType.NOT_IN_LIST
        1 -> MarkerType.IN_SINGLE_LIST
        else -> MarkerType.IN_MULTIPLE_LIST
    }
}

internal fun W3WSquare.contains(coordinates: W3WLatLng): Boolean {
    return if (coordinates.lat >= this.southwest.lat && coordinates.lat <= this.northeast.lat && coordinates.lng >= this.southwest.lng && coordinates.lng <= this.northeast.lng) return true
    else false
}