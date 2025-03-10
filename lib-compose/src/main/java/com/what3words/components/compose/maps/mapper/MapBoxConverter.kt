package com.what3words.components.compose.maps.mapper

import com.mapbox.maps.Style
import com.what3words.components.compose.maps.models.W3WMapType

/**
 * Converts a [W3WMapType] to its corresponding Mapbox map style string.
 *
 * @receiver The [W3WMapType] to convert
 * @return The Mapbox style string representing the map type
 *
 * @see Style For available Mapbox map styles
 */
fun W3WMapType.toMapBoxMapType(): String {
    return when (this) {
        W3WMapType.NORMAL -> Style.STANDARD
        W3WMapType.SATELLITE -> Style.SATELLITE_STREETS
        W3WMapType.HYBRID -> Style.SATELLITE_STREETS
        W3WMapType.TERRAIN -> Style.OUTDOORS
    }
}