package com.what3words.components.compose.maps.state

import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language

const val LIST_DEFAULT_ID = "LIST_DEFAULT_ID"

data class W3WMapState(
    // Map Config
    val language: W3WRFC5646Language = W3WRFC5646Language.EN_GB,

    val mapType: W3WMapType = W3WMapType.NORMAL,

    val isDarkMode: Boolean = false,

    val isMapGestureEnable: Boolean = true,

    val isMyLocationEnabled: Boolean = true,

    // Square selected
    val selectedAddress: W3WAddress? = null,

    // List marker
    val listMakers: Map<String, List<W3WMarker>> = emptyMap(),

    val cameraState: W3WCameraState<*>? = null,

    // The pair of vertical and horizontal grid polylines
    internal val gridPolyline: Pair<List<W3WCoordinates>, List<W3WCoordinates>> = Pair(
        emptyList(),
        emptyList()
    ),
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