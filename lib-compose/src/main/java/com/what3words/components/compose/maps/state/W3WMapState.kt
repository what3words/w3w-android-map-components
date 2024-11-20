package com.what3words.components.compose.maps.state

import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.language.W3WRFC5646Language

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
data class W3WMapState(
    val language: W3WRFC5646Language = W3WRFC5646Language.EN_GB,

    val mapType: W3WMapType = W3WMapType.NORMAL,

    val isDarkMode: Boolean = false,

    val isMapGestureEnable: Boolean = true,

    val isMyLocationEnabled: Boolean = true,

    val selectedAddress: W3WAddress? = null,

    val listMakers: Map<String, List<W3WMarker>> = emptyMap(),

    // Control camera position of map
    val cameraState: W3WCameraState<*>? = null,

    // data class handling draw grid lines on map
    internal val gridLines: W3WGridLines = W3WGridLines(),
)

fun W3WMapState.addOrUpdateMarker(
    listId: String? = null,
    marker: W3WMarker
): W3WMapState {
    val key = listId ?: LIST_DEFAULT_ID
    val currentList = listMakers[key] ?: emptyList()
    val existingMarkerIndex = currentList.indexOfFirst { it.address == marker.address }

    val updatedList = if (existingMarkerIndex != -1) {
        // Update existing marker
        currentList.toMutableList().also { it[existingMarkerIndex] = marker }
    } else {
        // Add new marker
        currentList + marker
    }

    return copy(listMakers = listMakers + (key to updatedList))
}

fun W3WMapState.getListIdsByMarker(marker: W3WMarker): List<String> {
    return listMakers.entries.filter { (_, markers) -> marker in markers }.map { it.key }
}

fun W3WMapState.getMarkersByListId(listId: String): List<W3WMarker> {
    return listMakers[listId] ?: emptyList()
}

fun W3WMapState.removeMarkersByList(listId: String): W3WMapState {
    return copy(listMakers = listMakers - listId)
}

fun W3WMapState.getAllMarkers(): List<W3WMarker> {
    return listMakers.values.flatten()
}

fun W3WMapState.removeAllMarkers(): W3WMapState {
    return copy(listMakers = emptyMap())
}