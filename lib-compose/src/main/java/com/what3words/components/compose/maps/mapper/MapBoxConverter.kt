package com.what3words.components.compose.maps.mapper

import com.mapbox.maps.Style
import com.what3words.components.compose.maps.models.W3WMapType

fun W3WMapType.toMapBoxMapType(isDarkMode: Boolean): String {
    return when (this) {
        W3WMapType.NORMAL -> if (isDarkMode) Style.DARK else Style.MAPBOX_STREETS
        W3WMapType.SATELLITE -> Style.SATELLITE_STREETS
        W3WMapType.HYBRID -> Style.SATELLITE_STREETS
        W3WMapType.TERRAIN -> Style.OUTDOORS
    }
}