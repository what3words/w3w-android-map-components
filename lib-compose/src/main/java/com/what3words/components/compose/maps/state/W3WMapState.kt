package com.what3words.components.compose.maps.state

import androidx.compose.runtime.Immutable
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

const val LIST_DEFAULT_ID = "LIST_DEFAULT_ID"

/**
 * Data class representing the state of the What3Words (W3W) map.
 *
 * This class holds various properties that define the configuration and current
 * status of the map, such as language, map type, dark mode, gestures,
 * selected address, markers, camera state, and grid lines.
 * @property mapType The type of map displayed. Defaults to `W3WMapType.NORMAL`.
 * @property isDarkMode Whether dark mode is enabled for the map. Defaults to `false`.
 * @property isMapGestureEnable Whether map gestures are enabled. Defaults to `true`.
 * @property isMyLocationEnabled Whether the "My Location" feature is enabled. Defaults to `true`.
 * @property selectedAddress The currently selected W3W address. Defaults to `null`.
 * @property listMarkers A map of marker lists, keyed by their identifier. Defaults to an empty map.
 * @property cameraState The current state of the map's camera. Defaults to `null`.
 * @property gridLines [W3WGridLines] data class handling draw grid line on map
 */
@Immutable
data class W3WMapState(

    val mapType: W3WMapType = W3WMapType.NORMAL,

    val isDarkMode: Boolean = false,

    val isMapGestureEnable: Boolean = true,

    val isMyLocationEnabled: Boolean = true,

    val selectedAddress: W3WMarker? = null,

    val listMarkers: ImmutableMap<String, ImmutableList<W3WMarker>> = persistentMapOf(),

    // Control camera position of map
    internal val cameraState: W3WCameraState<*>? = null,

    // data class handling draw grid lines on map
    internal val gridLines: W3WGridLines = W3WGridLines(),
)

internal fun W3WMapState.addListMarker(
    listName: String,
    listColor: W3WMarkerColor,
    markers: ImmutableList<W3WMarker>
): W3WMapState {
    // Create a new list of markers with updated colors
    val updatedMarkers = markers.map { it.copy(color = listColor) }

    // Add or update the list of markers for the given listName
    val updatedSavedListMarkers = listMarkers + (listName to updatedMarkers.toImmutableList())

    // Return a new state with the updated map
    return copy(listMarkers = updatedSavedListMarkers.toImmutableMap())
}


internal fun W3WMapState.addMarker(
    listName: String? = null,  // Optional list identifier
    marker: W3WMarker,       // Marker to add or update
): W3WMapState {
    // Determine the listName: use provided listName or a default
    val key = listName ?: LIST_DEFAULT_ID

    // Get or create the current list of markers (using MutableList for in-place updates)
    val currentList = listMarkers[key] ?: persistentListOf()

    // Create a new list by either updating or adding the marker
    val updatedList = if (marker in currentList) {
        // Replace the existing marker
        currentList.map { if (it.id == marker.id) marker else it }
    } else {
        // Marker doesn't exist, add it to the list
        currentList + marker
    }

    // Create a new map with the updated list for the key, ensuring it's an ImmutableMap
    val updatedMap = listMarkers + (key to updatedList.toImmutableList())

    // Return a new map with the updated list for the key
    return copy(listMarkers = updatedMap.toImmutableMap())
}


internal fun isExistInOtherList(
    listName: String,  // The current listName
    marker: W3WMarker,  // The marker to check
    listMarkers: ImmutableMap<String, ImmutableList<W3WMarker>>
): Boolean {
    // Check if the marker exists in any other list besides the one with listName
    return listMarkers.filter { (key, listMarker) ->
        // Skip the current listName
        key != listName && listMarker.any { it.id == marker.id }
    }.isNotEmpty()
}

internal fun isMarkerInSavedList(
    listMarkers: ImmutableMap<String, ImmutableList<W3WMarker>>,
    marker: W3WMarker
): MarkerStatus {

    // Count how many lists contain the marker
    var foundMarker: W3WMarker? = null

    // Iterate over the saved lists
    for (list in listMarkers.values) {
        // Check if the marker exists in the current list
        list.firstOrNull { it.id == marker.id }?.let {
            if (foundMarker != null) {
                // Marker found in multiple lists, immediately return Multiple
                return MarkerStatus.InMultipleList
            }

            foundMarker = it
        }
    }

    // If we found the marker in exactly one list, return Single; otherwise, return None
    return foundMarker?.let { MarkerStatus.InSingleList(it) } ?: MarkerStatus.NotSaved
}

@Immutable
sealed class MarkerStatus {
    object NotSaved : MarkerStatus() // Marker doesn't exist in any list
    data class InSingleList(val marker: W3WMarker) :
        MarkerStatus() // Marker exists in exactly one list

    object InMultipleList : MarkerStatus() // Marker exists in multiple lists
}