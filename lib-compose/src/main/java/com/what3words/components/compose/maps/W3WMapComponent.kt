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
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.providers.googlemap.W3WGoogleMap
import com.what3words.components.compose.maps.providers.mapbox.W3WMapBox
import com.what3words.components.compose.maps.state.W3WCameraState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.geometry.W3WCoordinates


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    mapManager: W3WMapManager,
    content: (@Composable () -> Unit)? = null,
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
                mapProvider = mapManager.mapProvider,
                content = content,
                state = state,
                onMapTypeClicked = {
                    mapManager.setMapType(it)
                },
                onMapClicked = {

                },
                onCameraUpdated = {
                    mapManager.updateCameraState(it)
                }
            )
        }

        else -> {
            onError?.invoke(W3WError(message = "Map component needs permission"))
        }
    }
}


@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    state: W3WMapState,
    mapProvider: MapProvider,
    content: (@Composable () -> Unit)? = null,
    onMapTypeClicked: ((W3WMapType) -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit)? = null,
    onCameraUpdated: (W3WCameraState<*>) -> Unit
) {
    W3WMapContent(
        modifier = modifier,
        layoutConfig = layoutConfig,
        mapConfig = mapConfig,
        mapProvider = mapProvider,
        content = content,
        state = state,
        onMapClicked = {
            onMapClicked?.invoke(it)
        },
        onMapTypeClicked = {
            onMapTypeClicked?.invoke(it)
        },
        onCameraUpdated = {
            onCameraUpdated.invoke(it)
        }
    )
}

@Composable
internal fun W3WMapContent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    state: W3WMapState,
    mapProvider: MapProvider,
    content: (@Composable () -> Unit)? = null,
    onMapTypeClicked: ((W3WMapType) -> Unit),
    onMapClicked: (W3WCoordinates) -> Unit,
    onCameraUpdated: (W3WCameraState<*>) -> Unit
) {
    Box(modifier = modifier) {
        W3WMapView(
            modifier = modifier,
            layoutConfig = layoutConfig,
            mapConfig = mapConfig,
            mapProvider = mapProvider,
            state = state,
            onMapClicked = onMapClicked,
            content = content,
            onCameraUpdated = {
                onCameraUpdated.invoke(it)
            }
        )

        W3WMapButtons(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(layoutConfig.contentPadding),
            onMyLocationClicked = {
                //TODO
            },
            onMapTypeClicked = onMapTypeClicked,
        )
    }
}

@Composable
internal fun W3WMapView(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    mapProvider: MapProvider,
    state: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: (W3WCameraState<*>) -> Unit
) {
    when (mapProvider) {
        MapProvider.GOOGLE_MAP -> {
            W3WGoogleMap(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                state = state,
                onMapClicked = onMapClicked,
                content = content,
                onCameraUpdated = {
                    onCameraUpdated.invoke(it)
                }
            )
        }

        MapProvider.MAPBOX -> {
            W3WMapBox(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                state = state,
                onMapClicked = onMapClicked,
                content = content
            )
        }
    }
}





