package com.what3words.components.compose.maps.models

import androidx.compose.runtime.Immutable
import com.what3words.components.compose.maps.models.W3WMapType.HYBRID
import com.what3words.components.compose.maps.models.W3WMapType.NORMAL
import com.what3words.components.compose.maps.models.W3WMapType.SATELLITE
import com.what3words.components.compose.maps.models.W3WMapType.TERRAIN

/**
 * W3WMapType represents the available map types for What3Words maps.
 *
 * The available map types are:
 * - [NORMAL]: Standard map view with roads, labels, and slight terrain information.
 * - [HYBRID]: Satellite imagery with roads and label overlays.
 * - [TERRAIN]: Map with detailed terrain information and roads.
 * - [SATELLITE]: Satellite imagery with no overlays.
 */
@Immutable
enum class W3WMapType {
    NORMAL,
    HYBRID,
    TERRAIN,
    SATELLITE
}