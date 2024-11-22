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
import com.what3words.components.compose.maps.models.W3WLocationSource
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.providers.googlemap.W3WGoogleMap
import com.what3words.components.compose.maps.providers.mapbox.W3WMapBox
import com.what3words.components.compose.maps.state.W3WButtonsState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.geometry.W3WCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/**
 * A composable function that displays a What3Words (W3W) map.
 *
 * This component provides a convenient way to integrate a W3W map into your Jetpack Compose UI.
 * It handles map configuration, location services, user interactions, and error handling.
 *
 * @param modifier Modifier for styling and layout.
 * @param layoutConfig [W3WMapDefaults.LayoutConfig] Configuration for the map's layout.
 * @param mapConfig [W3WMapDefaults.MapConfig] Configuration for the map's appearance such as custom dark mode, grid line config.
 * @param mapManager The [W3WMapManager] instance that manages the map's mapState and interactions.
 * @param locationSource An optional [W3WLocationSource] used to fetch the user's location.
 * @param content Optional composable content to be displayed on the map.
 * @param onError Callback invoked when an error occurs.
 */
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    mapManager: W3WMapManager,
    locationSource: W3WLocationSource? = null,
    content: (@Composable () -> Unit)? = null,
    onError: ((W3WError) -> Unit)? = null,
) {

    val mapState by mapManager.mapState.collectAsState()
    val buttonState by mapManager.buttonState.collectAsState()

    W3WMapContent(
        modifier = modifier,
        layoutConfig = layoutConfig,
        mapConfig = mapConfig,
        mapProvider = mapManager.mapProvider,
        content = content,
        mapState = mapState,
        buttonState = buttonState,
        onMapTypeClicked = {
            mapManager.setMapType(it)
            mapManager.orientCamera()
        },
        onMapClicked = {
            mapManager.selectAtCoordinates(it)
        },
        onCameraUpdated = {
            mapManager.updateCameraState(it)
        },
        onMyLocationClicked = {
            fetchCurrentLocation(
                locationSource = locationSource,
                mapManager = mapManager,
                onError = onError
            )
        },
        onError = onError
    )
}

/*** A composable function that displays a What3Words (W3W) map.
 *
 * This component provides a convenient way to integrate a W3W map into your Jetpack Compose UI.
 * It handles map configuration, location services, user interactions, and error handling.
 *
 * @param modifier Modifier for styling and layout.
 * @param layoutConfig [W3WMapDefaults.LayoutConfig] Configuration for the map's layout.
 * @param mapConfig [W3WMapDefaults.MapConfig] Configuration for the map's appearance.
 * @param mapState The [W3WMapState] object that holds the mapState of the map.
 * @param buttonState The [W3WButtonsState] object that holds the buttonState of the map.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param content Optional composable content to be displayed on the map.
 * @param onMapTypeClicked Callback invoked when the map type is clicked.
 * @param onMapClicked Callback invoked when the map is clicked.
 * @param onMyLocationClicked Callback invoked when the my location button is clicked.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 * @param onError Callback invoked when an error occurs.
 */
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    mapState: W3WMapState,
    buttonState: W3WButtonsState,
    mapProvider: MapProvider,
    content: (@Composable () -> Unit)? = null,
    onMapTypeClicked: ((W3WMapType) -> Unit)? = null,
    onMyLocationClicked: (() -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit)? = null,
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    onError: ((W3WError) -> Unit)? = null,
) {
    W3WMapContent(
        modifier = modifier,
        layoutConfig = layoutConfig,
        mapConfig = mapConfig,
        mapProvider = mapProvider,
        content = content,
        mapState = mapState,
        buttonState = buttonState,
        onMapClicked = {
            onMapClicked?.invoke(it)
        },
        onMapTypeClicked = {
            onMapTypeClicked?.invoke(it)
        },
        onMyLocationClicked = {
            onMyLocationClicked?.invoke()
        },
        onCameraUpdated = {
            onCameraUpdated.invoke(it)
        },
        onError = onError
    )
}

/**
 * A composable function that displays Map and Buttons.
 *
 * This function is responsible for rendering the map, handling user interactions,
 * and managing the map's mapState. It provides options for customizing the map's
 * appearance, layout, and behavior.
 ** @param modifier Modifier for styling and layout.
 * @param layoutConfig [W3WMapDefaults.LayoutConfig] Configuration for the map's layout.
 * @param mapConfig [W3WMapDefaults.MapConfig] Configuration for the map's appearance.
 * @param mapState The [W3WMapState] object that holds the mapState of the map.
 * @param buttonState The [W3WButtonsState] object that holds the buttonState of the map.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param content Optional composable content to be displayed on the map.
 * @param onMapTypeClicked Callback invoked when the user clicks on the map type button.
 * @param onMyLocationClicked Callback invoked when the user clicks on the my location button.
 * @param onMapClicked Callback invoked when the user clicks on the map.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 * @param onError Callback invoked when an error occurs during map initialization or interaction.
 */
@Composable
internal fun W3WMapContent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    mapState: W3WMapState,
    buttonState: W3WButtonsState,
    mapProvider: MapProvider,
    content: (@Composable () -> Unit)? = null,
    onMapTypeClicked: ((W3WMapType) -> Unit),
    onMyLocationClicked: () -> Unit,
    onMapClicked: (W3WCoordinates) -> Unit,
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    onError: ((W3WError) -> Unit)? = null,
) {
    // Handles check location permissions, if isMyLocationEnabled enable
    MapPermissionsHandler(mapState = mapState, onError = onError) {

        // Fetch current location when launch
        LaunchedEffect(Unit) {
            if (mapState.isMyLocationEnabled) {
                onMyLocationClicked.invoke()
            }
        }

        Box(modifier = modifier) {
            W3WMapView(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                mapProvider = mapProvider,
                mapState = mapState,
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
                onMyLocationClicked = onMyLocationClicked,
                mapConfig = mapConfig,
                onMapTypeClicked = onMapTypeClicked,
                isLocationEnabled = mapState.isMyLocationEnabled,
                accuracyDistance = buttonState.accuracyDistance,
                isLocationActive = buttonState.isLocationActive,
            )
        }
    }
}

/**
 * A composable function that handles location permissions for the map.
 *
 * This function checks if the "My Location" feature is enabled in the map mapState
 * and requests the necessary location permissions if needed. If the permissions
 * are granted, it displays the provided content. Otherwise, it invokes the
 * `onError` callback with a [W3WError] indicating that permissions are required.
 *
 * @param mapState The [W3WMapState] object that holds the mapState of the map.
 * @param onError Callback invoked when an error occurs, such as when location
 *   permissions are denied.
 * @param content The composable content to be displayed if location permissions
 *   are granted or if the "My Location" feature is disabled.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun MapPermissionsHandler(
    mapState: W3WMapState,
    onError: ((W3WError) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (mapState.isMyLocationEnabled) {
        val permissionState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        LaunchedEffect(Unit) {
            permissionState.launchMultiplePermissionRequest()
        }

        when {
            permissionState.allPermissionsGranted -> {
                content()
            }

            else -> {
                onError?.invoke(W3WError(message = "Map component needs permission"))
            }
        }
    } else {
        content()
    }
}

/**
 * A composable function that displays the core What3Words (W3W) map view.
 *
 * This function is responsible for rendering the map surface, handling map clicks,
 * and updating the camera position by map
 *
 * @param modifier Modifier for styling and layout of the map view.
 * @param layoutConfig [W3WMapDefaults.LayoutConfig] Configuration for the map's layout.
 * @param mapConfig [W3WMapDefaults.MapConfig] Configuration for the map's appearance.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param mapState The [W3WMapState] object that holds the mapState of the map.
 * @param content Optional composable content to be displayed on the map.
 * @param onMapClicked Callback invoked when the user clicks on the map.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 */
@Composable
internal fun W3WMapView(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    mapProvider: MapProvider,
    mapState: W3WMapState,
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
                state = mapState,
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
                state = mapState,
                onMapClicked = onMapClicked,
                content = content,
                onCameraUpdated = {
                    onCameraUpdated.invoke(it)
                }
            )
        }
    }
}

/**
 * This function is responsible for update camera position and button state based on current location
 *
 * @param locationSource An optional [W3WLocationSource] used to fetch the user's location.
 * @param mapManager The [W3WMapManager] instance that manages the map's mapState and interactions.
 * @param onError Callback invoked when an error occurs during map initialization or interaction.
 */
private fun fetchCurrentLocation(
    locationSource: W3WLocationSource?,
    mapManager: W3WMapManager,
    onError: ((W3WError) -> Unit)? = null
) {
    locationSource?.let {
        CoroutineScope(IO).launch {
            try {
                val location = it.fetchLocation()
                // Update camera state
                mapManager.moveToPosition(
                    coordinates = W3WCoordinates(location.latitude, location.longitude),
                    animate = true
                )
                //TODO: Update button state
            } catch (e: Exception) {
                onError?.invoke(W3WError("Location fetch failed: ${e.message}"))
            }
        }
    }
}





