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
import com.what3words.components.compose.maps.providers.googlemap.W3WGoogleMap
import com.what3words.components.compose.maps.providers.mapbox.W3WMapBox
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.geometry.W3WCoordinates


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    mapProvider: MapProvider,
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
                mapProvider = mapProvider,
                content = content,
                state = state,
                onMapTypeClicked = {
                    mapManager.setMapType(it)
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


@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    state: W3WMapState,
    mapProvider: MapProvider,
    content: (@Composable () -> Unit)? = null,
    onMapTypeClicked: ((W3WMapState.MapType) -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit)? = null,
    onCameraUpdated: ((W3WMapState.CameraPosition) -> Unit)? = null
) {
    W3WMapContent(
        modifier = modifier,
        layoutConfig = layoutConfig,
        mapConfig = mapConfig,
        mapProvider = mapProvider,
        content = content,
        state = state,
        onCameraUpdated = {
            onCameraUpdated?.invoke(it)
        },
        onMapClicked = {
            onMapClicked?.invoke(it)
        },
        onMapTypeClicked = {
            onMapTypeClicked?.invoke(it)
        },
    )
}

@Composable
fun W3WMapContent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    state: W3WMapState,
    mapProvider: MapProvider,
    content: (@Composable () -> Unit)? = null,
    onMapTypeClicked: ((W3WMapState.MapType) -> Unit),
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: ((W3WMapState.CameraPosition) -> Unit)
) {
    Box(modifier = modifier) {
        W3WMapView(
            modifier = modifier,
            layoutConfig = layoutConfig,
            mapConfig = mapConfig,
            mapProvider = mapProvider,
            state = state,
            onMapClicked = onMapClicked,
            onCameraUpdated = onCameraUpdated,
            content = content
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
fun W3WMapView(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    mapProvider: MapProvider,
    state: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: ((W3WMapState.CameraPosition) -> Unit)
) {
    when (mapProvider) {
        MapProvider.GOOGLE_MAP -> {
            W3WGoogleMap(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                state = state,
                onMapClicked = onMapClicked,
                onCameraUpdated = onCameraUpdated,
                content = content
            )
        }

        MapProvider.MAPBOX -> {
            W3WMapBox(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                state = state,
                onMapClicked = onMapClicked,
                onCameraUpdated = onCameraUpdated,
                content = content
            )
        }
    }
}





