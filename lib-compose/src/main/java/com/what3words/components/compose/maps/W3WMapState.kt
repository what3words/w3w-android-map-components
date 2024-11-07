package com.what3words.components.compose.maps

import androidx.compose.ui.graphics.Color
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language

const val LIST_DEFAULT_ID = "LIST_DEFAULT_ID"
const val DEFAULT_BOUNDS_SCALE = 6f

data class W3WMapState(
    // Map Config
    val language: W3WRFC5646Language = W3WRFC5646Language.EN_GB,
    val mapType: MapType = MapType.NORMAL,
    val isDarkMode: Boolean = false,
    val isMapGestureEnable: Boolean = false,
    val isMyLocationEnabled: Boolean = false,

    // Button control
    val isMyLocationButtonEnabled: Boolean = false,

    // Grid view
    val isGridEnabled: Boolean = true,
    val gridColor: Color? = null,
    val zoomSwitchLevel: Float? = null,

    // Camera control
    val cameraPosition: CameraPosition? = null,

    // Square selected
    val selectedAddress: W3WAddress? = null,

    // List marker
    val listMakers: Map<String, List<Marker>> = emptyMap(),

    // Grid
    val gridLines: GridLines? = null,
    val gridScale: Float = DEFAULT_BOUNDS_SCALE
) {
    data class CameraPosition(
        val zoom: Float,
        val coordinates: W3WCoordinates,
        val bearing: Float = 0f,
        val isAnimated: Boolean = false,
        val visibleBounds: List<W3WCoordinates>? = null
    )

    data class GridLines(
        val verticalLines: List<W3WCoordinates>,
        val horizontalLines: List<W3WCoordinates>,
        val geoJSON: String? = null
    )

    data class Marker(
        val address: W3WAddress,
        val color: Color,
        val title: String? = null,
        val snippet: String? = null
    )

    enum class ZoomOption {
        NONE,
        CENTER,
        CENTER_AND_ZOOM
    }

    enum class MapType {
        NORMAL,
        HYBRID,
        TERRAIN,
        SATELLITE
    }
}

fun W3WMapState.addOrUpdateMarker(
    listId: String? = null,
    marker: W3WMapState.Marker // Assuming W3WMapMarker holds marker data
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

fun W3WMapState.getListIdsByMarker(marker: W3WMapState.Marker): List<String> {
    return listMakers.entries.filter { (_, markers) -> marker in markers }.map { it.key }
}

fun W3WMapState.getMarkersByListId(listId: String): List<W3WMapState.Marker> {
    return listMakers[listId] ?: emptyList()
}

fun W3WMapState.removeMarkersByList(listId: String): W3WMapState {
    return copy(listMakers = listMakers - listId)
}

fun W3WMapState.getAllMarkers(): List<W3WMapState.Marker> {
    return listMakers.values.flatten()
}

fun W3WMapState.removeAllMarkers(): W3WMapState {
    return copy(listMakers = emptyMap())
}

fun W3WMapState.updateGridLines(
    gridLines: W3WMapState.GridLines?
): W3WMapState {
    return copy(gridLines = gridLines)
}