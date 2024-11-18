package com.what3words.components.compose.maps.mapper

import com.mapbox.maps.Style
import com.what3words.components.compose.maps.models.MapType

fun MapType.toMapBoxMapType(isDarkMode: Boolean): String {
    return when (this) {
        MapType.NORMAL -> if (isDarkMode) Style.DARK else Style.MAPBOX_STREETS
        MapType.SATELLITE -> Style.SATELLITE_STREETS
        MapType.HYBRID -> Style.SATELLITE_STREETS
        MapType.TERRAIN -> Style.OUTDOORS
    }
}