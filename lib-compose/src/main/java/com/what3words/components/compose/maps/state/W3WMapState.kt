package com.what3words.components.compose.maps.state

import androidx.compose.runtime.Immutable
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.core.types.language.W3WRFC5646Language
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
 * @property language The language used for displaying W3W addresses. Defaults to `W3WRFC5646Language.EN_GB`.
 * @property mapType The type of map displayed. Defaults to `W3WMapType.NORMAL`.
 * @property isDarkMode Whether dark mode is enabled for the map. Defaults to `false`.
 * @property isMapGestureEnable Whether map gestures are enabled. Defaults to `true`.
 * @property isMyLocationEnabled Whether the "My Location" feature is enabled. Defaults to `true`.
 * @property selectedAddress The currently selected W3W address. Defaults to `null`.
 * @property listMakers A map of marker lists, keyed by their identifier. Defaults to an empty map.
 * @property cameraState The current state of the map's camera. Defaults to `null`.
 * @property gridLines [W3WGridLines] data class handling draw grid line on map
 */
@Immutable
data class W3WMapState(
    val language: W3WRFC5646Language = W3WRFC5646Language.EN_GB,

    val mapType: W3WMapType = W3WMapType.NORMAL,

    val isDarkMode: Boolean = false,

    val isMapGestureEnable: Boolean = true,

    val isMyLocationEnabled: Boolean = true,

    val selectedAddress: W3WMarker? = null,

    val listMakers: ImmutableMap<String, W3WListMarker> = persistentMapOf(),

    // Control camera position of map
    internal val cameraState: W3WCameraState<*>? = null,

    // data class handling draw grid lines on map
    internal val gridLines: W3WGridLines = W3WGridLines(),
)

@Immutable
data class W3WListMarker(
    val listColor: W3WMarkerColor? = null,
    val markers: ImmutableList<W3WMarker> = persistentListOf()
)

fun W3WMapState.addOrUpdateMarker(
    listId: String? = null,  // Optional list identifier
    marker: W3WMarker,       // Marker to add or update
    listColor: W3WMarkerColor? = null
): W3WMapState {
    // Determine the listId: use provided listId or a default
    val key = listId ?: LIST_DEFAULT_ID

    // Get or create the current list of markers (using MutableList for in-place updates)
    val currentList = listMakers[key]?.markers?.toMutableList() ?: mutableListOf()

    // Check if the marker already exists in the list by its address
    val existingMarkerIndex = currentList.indexOfFirst { it == marker }

    if (existingMarkerIndex != -1) {
        // Marker exists, update it in-place
        currentList[existingMarkerIndex] = marker
    } else {
        // Marker doesn't exist, add it
        currentList.add(marker)
    }

    // Create or update the W3WListMarker in place
    val updatedList = W3WListMarker(markers = currentList.toImmutableList())

    // Use a mutable map to update the list in-place
    return copy(listMakers = listMakers.toMutableMap().apply {
        this[key] = updatedList
    }.toImmutableMap())
}

fun W3WMapState.addOrUpdateMarker(
    listId: String? = null,  // Optional list identifier
    markers: List<W3WMarker>, // Markers to add or update
    listColor: W3WMarkerColor? = null  // Optional list color
): W3WMapState {
    // Determine the listId: use provided listId or a default one
    val key = listId ?: LIST_DEFAULT_ID

    // Get the current list of markers or create a new empty list if not found
    val currentList = listMakers[key]?.markers?.toMutableList() ?: mutableListOf()

    // Iterate over the list of new markers to add or update
    markers.forEach { marker ->
        // Check if the marker already exists in the list by its address
        val existingMarkerIndex = currentList.indexOfFirst { it == marker }

        if (existingMarkerIndex != -1) {
            // If the marker exists, update it in place (preserving the rest of the list)
            currentList[existingMarkerIndex] = marker
        } else {
            // If the marker doesn't exist, add it to the list
            currentList.add(marker)
        }
    }

    // Create a new W3WListMarker with the updated list of markers
    val updatedListMarker = W3WListMarker(
        listColor = listColor ?: listMakers[key]?.listColor,  // Use the provided listColor, or keep the existing one
        markers = currentList.toImmutableList()
    )

    // Return a new W3WMapState with the updated list of markers
    return copy(listMakers = listMakers.toMutableMap().apply {
        this[key] = updatedListMarker // Update the list in the map
    }.toImmutableMap())
}

fun isExistInOtherList(
    listId: String,  // The current listId
    marker: W3WMarker,  // The marker to check
    listMakers: Map<String, W3WListMarker>
): Boolean {
    // Check if the marker exists in any other list besides the one with listId
    return listMakers.filter { (key, listMarker) ->
        // Skip the current listId
        key != listId && listMarker.markers.any { it == marker }
    }.isNotEmpty()
}

fun W3WMapState.setSelectedAddress(
    marker: W3WMarker
): W3WMapState {
//    val listExist = getListIdsByMarker(marker)
//    return when(listExist.size) {
//        0 -> copy(selectedAddress = marker)
//        1 -> copy(selectedAddress = getMarkersByListId(listExist.))
//        else -> {
//            copy(selectedAddress = marker, listMakers = listMakers - listExist)
//        }
//    }

    // case 1: not exist return marker
    // case 2: exists in multiple list

    return copy(selectedAddress = marker)
}


//fun W3WMapState.removeMarkersByList(listId: String): W3WMapState {
//    return copy(listMakers = listMakers - listId)
//}
//
//fun W3WMapState.removeAllMarkers(): W3WMapState {
//    return copy(listMakers = emptyMap())
//}