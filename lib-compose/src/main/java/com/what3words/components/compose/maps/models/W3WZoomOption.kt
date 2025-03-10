package com.what3words.components.compose.maps.models

import androidx.compose.runtime.Immutable
import com.what3words.components.compose.maps.models.W3WZoomOption.CENTER
import com.what3words.components.compose.maps.models.W3WZoomOption.CENTER_AND_ZOOM
import com.what3words.components.compose.maps.models.W3WZoomOption.NONE

/**
 * Defines zoom behavior options when updating the map camera.
 *
 * @property NONE No zoom or centering will be applied when updating the map.
 * @property CENTER The map will center on the target location but maintain the current zoom level.
 * @property CENTER_AND_ZOOM The map will both center on the target location and adjust to the specified zoom level.
 */
@Immutable
enum class W3WZoomOption {
    NONE,
    CENTER,
    CENTER_AND_ZOOM
}
