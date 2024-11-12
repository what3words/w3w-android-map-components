package com.what3words.components.compose.maps

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.what3words.components.compose.maps.buttons.W3WMapButtons
import com.what3words.components.compose.maps.providers.W3WMapProvider
import com.what3words.core.types.common.W3WError
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
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    mapProvider: W3WMapProvider,
    mapManager: W3WMapManager,
    onError: ((W3WError) -> Unit)? = null,
) {

    val state by mapManager.state.collectAsState()

    val permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    when {
        permissionState.allPermissionsGranted -> {
            W3WMapContent(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                state = state,
                mapProvider = mapProvider,
                onMapTypeClicked = {
                    mapManager.setMyLocationButton(!state.isMyLocationButtonEnabled)
                    mapManager.setDarkMode(!state.isDarkMode)
                },
                onMapClicked = {

                },
                onCameraUpdated = {
                    mapManager.onCameraUpdated(it)
                },
            )
        }

        else -> {
            onError?.invoke(W3WError(message = "Map component needs permission"))
        }
    }
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
 */
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    state: W3WMapState,
    mapProvider: W3WMapProvider,
    onMapTypeClicked: (() -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit)? = null,
    onCameraUpdated: ((W3WMapState.CameraPosition) -> Unit)? = null
) {
    W3WMapContent(
        modifier = modifier,
        layoutConfig = layoutConfig,
        mapConfig = mapConfig,
        state = state,
        mapProvider = mapProvider,
        onCameraUpdated = {
            onCameraUpdated?.invoke(it)
        },
        onMapClicked = {
            onMapClicked?.invoke(it)
        },
        onMapTypeClicked = {
            onMapTypeClicked?.invoke()
        }
    )
}

@Composable
fun W3WMapContent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    state: W3WMapState,
    mapProvider: W3WMapProvider,
    onMapTypeClicked: (() -> Unit),
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: ((W3WMapState.CameraPosition) -> Unit)
) {
    Box(modifier = modifier) {
        mapProvider.What3WordsMap(
            modifier = modifier,
            layoutConfig = layoutConfig,
            mapConfig = mapConfig,
            state = state,
            onCameraUpdated = onCameraUpdated,
            onMapClicked = onMapClicked
        )

        W3WMapButtons(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(layoutConfig.contentPadding),
            onMapTypeClicked = onMapTypeClicked
        )
    }
}



