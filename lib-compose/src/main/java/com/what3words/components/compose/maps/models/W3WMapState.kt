package com.what3words.components.compose.maps.models

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.language.W3WRFC5646Language

const val LIST_DEFAULT_ID = "LIST_DEFAULT_ID"

data class W3WMapState(
    val language: W3WRFC5646Language = W3WRFC5646Language.EN_GB,
    val isGridEnabled: Boolean = true,
    val gridColor: Color? = null,
    val selectedAddress: W3WAddress? = null,
    val listMakers: Map<String, List<W3WMapMarker>> = emptyMap(),
    val mapType: W3WMapType = W3WMapType.NORMAL,
    val zoom: Float? = null,
    val isDarkMode: Boolean = false,
)

data class W3WMapMarker(
    val address: W3WAddress,
    val color: Color,
    val title: String? = null,
    val snippet: String? = null
)

fun W3WMapState.addMarker(listId: String?=null, marker: W3WMapMarker): W3WMapState {
    val key = listId?:LIST_DEFAULT_ID

    return copy(
        listMakers = listMakers + (key to (listMakers[key] ?: emptyList()) + marker)
    )
}