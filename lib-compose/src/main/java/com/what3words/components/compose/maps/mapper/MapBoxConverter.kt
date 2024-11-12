package com.what3words.components.compose.maps.mapper

import com.mapbox.maps.Style
import com.what3words.components.compose.maps.W3WMapState

fun W3WMapState.MapType.toMapBoxMapType(isDarkMode: Boolean): String {
    return when (this) {
        W3WMapState.MapType.NORMAL -> if (isDarkMode) Style.DARK else Style.MAPBOX_STREETS
        W3WMapState.MapType.SATELLITE -> Style.SATELLITE_STREETS
        W3WMapState.MapType.HYBRID -> Style.SATELLITE_STREETS
        W3WMapState.MapType.TERRAIN -> Style.OUTDOORS
    }
}