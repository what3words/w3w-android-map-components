package com.what3words.components.compose.maps.state

import androidx.compose.runtime.Immutable
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.core.types.domain.W3WAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Data class representing the state of the what3words map.
 *
 * This class holds various properties that define the configuration and current
 * status of the map, such as language, map type, dark mode, gestures,
 * selected address, markers, camera state, and grid lines.
 * @property mapType The type of map displayed.
 * @property isDarkMode Whether dark mode is enabled for the raw map.
 * @property isMapGestureEnable Whether map gestures are enabled.
 * @property isMyLocationEnabled Whether the "My Location" feature is enabled.
 * @property selectedAddress The currently selected what3words address.
 * @property markers A list of markers on the map.
 * @property cameraState The current state of the map's camera.
 * @property gridLines [W3WGridLines] The grid lines displayed on the map.
 */
@Immutable
data class W3WMapState(

    val mapType: W3WMapType = W3WMapType.NORMAL,

    val isDarkMode: Boolean = false,

    val isMapGestureEnable: Boolean = true,

    val isMyLocationEnabled: Boolean = true,

    val selectedAddress: W3WAddress? = null,

    val markers: ImmutableList<W3WMarker> = persistentListOf(),

    internal val cameraState: W3WCameraState<*>? = null,

    internal val gridLines: W3WGridLines = W3WGridLines(),
)
