package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.what3words.components.compose.maps.buttons.W3WMapButtons
import com.what3words.components.compose.maps.providers.W3WMapProvider
import com.what3words.core.types.geometry.W3WCoordinates


/**
 * This is a closed library that allow for the creation of what3words maps and control through [W3WMapManager]
 *
 * A composable function that displays a what3words map and handles interactions
 * with [W3WMapManager] and [W3WMapProvider].
 *
 * This component collects the map state from the [W3WMapManager] and passes it
 * to another [W3WMapComponent] for rendering. It also provides callbacks to
 * the [W3WMapManager] for map updates and movements.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param layoutConfig The layout configuration for the map.
 * @param mapManager The [W3WMapManager] instance used to manage the map state.
 */
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapManager: W3WMapManager,
) {

    val state by mapManager.state.collectAsState()

    W3WMapComponent(
        modifier = modifier,
        layoutConfig = layoutConfig,
        state = state,
        mapProvider = mapManager.mapProvider,
        onMapUpdate = {
            mapManager.updateMap()
        },
        onMapMove = {
            mapManager.updateMove()
        },
        onMapClicked = {
            mapManager.onMapClicked(it)
        }
    )
}


/**
 * This is an open library that allow for the creation of what3words maps and control through [W3WMapState]
 *
 * A composable function that displays a what3words map with UI elements.
 *
 * This component renders the map using the provided [mapProvider] and displays
 * UI elements such as buttons using [W3WMapButtons]. It also handles map
 * updates and movements through the [onMapUpdate] and [onMapMove] callbacks.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param layoutConfig The layout configuration for the map.
 * @param state The current state of the what3words map.
 * @param mapProvider The provider used to render the map.
 * @param onMapUpdate A callback function invoked when the map is updated.
 * @param onMapMove A callback function invoked when the map is moved.*/
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    state: W3WMapState,
    mapProvider: W3WMapProvider,
    onMapClicked: ((W3WCoordinates) -> Unit),
    onMapUpdate: (() -> Unit),
    onMapMove: (() -> Unit)
) {
    Box(modifier = modifier) {
        mapProvider.What3WordsMap(
            modifier = modifier,
            contentPadding = layoutConfig.contentPadding,
            state = state,
            onMapUpdate = {
                onMapUpdate.invoke()
            },
            onMapMove = {
                onMapMove.invoke()
            },
            onMapClicked = {
                onMapClicked.invoke(it)
            }
        )

        W3WMapButtons(
            modifier = Modifier.align(Alignment.BottomEnd).padding(layoutConfig.contentPadding)
        )
    }
}


