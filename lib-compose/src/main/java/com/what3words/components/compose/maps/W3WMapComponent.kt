package com.what3words.components.compose.maps

import android.Manifest
import android.graphics.PointF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.what3words.components.compose.maps.W3WMapDefaults.MapColors
import com.what3words.components.compose.maps.W3WMapDefaults.defaultMapColors
import com.what3words.components.compose.maps.buttons.MapButtons
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault.defaultLocationButtonColor
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.models.W3WGridScreenCell
import com.what3words.components.compose.maps.models.W3WLocationSource
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.providers.googlemap.W3WGoogleMap
import com.what3words.components.compose.maps.providers.mapbox.W3WMapBox
import com.what3words.components.compose.maps.state.W3WButtonsState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WGoogleCameraState
import com.what3words.components.compose.maps.state.camera.W3WMapboxCameraState
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * Determines the map's color scheme based on the current map type and dark mode setting.
 * - If the `mapType` is `NORMAL` or `TERRAIN`:
 *     - If `isDarkMode` is `true`, the dark color scheme (`darkMapColor`) is used.
 *     - If `isDarkMode` is `false`, the normal color scheme (`normalMapColor`) is used.
 * - If the `mapType` is `HYBRID` or `SATELLITE`, the satellite color scheme (`satelliteMapColor`) is used, regardless of the dark mode setting.
 * @param mapColors [W3WMapDefaults.MapColor] determines the map's color scheme based on mapType and dark mode.
 * @param locationButtonColor specifies color settings for the location button.
 * @param mapManager manages the map's state and interactions.
 * @param textDataSource data source for text related to the map.
 * @param onSelectedSquareChanged callback for when the selected square changes.
 * @param locationSource optional source for fetching user location.
 * @param content composable content to display over the map.
 * @param onError callback for error occurrences.
 */
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    locationButtonColor: W3WMapButtonsDefault.LocationButtonColor = defaultLocationButtonColor(),
    mapColors: MapColors = defaultMapColors(),
    mapManager: W3WMapManager,
    textDataSource: W3WTextDataSource,
    onSelectedSquareChanged: (W3WAddress) -> Unit,
    locationSource: W3WLocationSource? = null,
    content: (@Composable () -> Unit)? = null,
    onError: ((W3WError) -> Unit)? = null,
) {

    val mapState by mapManager.mapState.collectAsState()

    val buttonState by mapManager.buttonState.collectAsState()

    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }

    val currentLayoutConfig by remember(layoutConfig) {
        mutableStateOf(layoutConfig)
    }

    val currentMapConfig by remember(mapConfig) {
        mutableStateOf(mapConfig)
    }

    LaunchedEffect(textDataSource) {
        mapManager.setTextDataSource(textDataSource)
    }

    LaunchedEffect(currentMapConfig) {
        mapManager.setMapConfig(currentMapConfig)
    }

    LaunchedEffect(Unit) {
        locationSource?.locationStatus?.collect { status ->
            mapManager.updateLocationStatus(status)
        }
    }

    LaunchedEffect(mapState.selectedAddress) {
        mapState.selectedAddress?.let {
            onSelectedSquareChanged.invoke(it)
        }
    }

    W3WMapContent(
        modifier = modifier,
        layoutConfig = currentLayoutConfig,
        mapConfig = currentMapConfig,
        mapColors = mapColors,
        locationButtonColor = locationButtonColor,
        mapProvider = mapManager.mapProvider,
        content = content,
        mapState = mapState,
        buttonState = buttonState,
        onMapTypeClicked =
            { mapType ->
                mapManager.setMapType(mapType)
            },
        onMapClicked = {
            coroutineScope.launch {
                mapManager.setSelectedAt(it)
            }
        },
        onMyLocationClicked =
            {
                fetchCurrentLocation(
                    locationSource = locationSource,
                    mapManager = mapManager,
                    onError = onError,
                    coroutineScope = coroutineScope,
                    shouldSelect = mapConfig.buttonConfig.shouldSelectOnMyLocationClicked
                )
            },
        onMapProjectionUpdated = mapManager::setMapProjection,
        onMapViewPortProvided = mapManager::setMapViewPort,
        onRecallClicked = {
            mapState.selectedAddress?.center?.let {
                coroutineScope.launch {
                    mapManager.moveToPosition(it, animate = true)
                }
            }
        },
        onRecallButtonPositionProvided = mapManager::setRecallButtonPosition,
        onMarkerClicked =
            { marker ->
                coroutineScope.launch {
                    mapManager.setSelectedAt(marker.center)
                }
            },
        onCameraUpdated = {
            coroutineScope.launch {
                mapManager.updateCameraState(it)
            }
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
 * Determines the map's color scheme based on the current map type and dark mode setting.
 * - If the `mapType` is `NORMAL` or `TERRAIN`:
 *     - If `isDarkMode` is `true`, the dark color scheme (`darkMapColor`) is used.
 *     - If `isDarkMode` is `false`, the normal color scheme (`normalMapColor`) is used.
 * - If the `mapType` is `HYBRID` or `SATELLITE`, the satellite color scheme (`satelliteMapColor`) is used, regardless of the dark mode setting.
 * @param mapColors [W3WMapDefaults.MapColor] Configuration for the map's color such as light, dark and satellite mode will based on mapType and state isDarkMode
 * @param locationButtonColor Color settings for the location button (the button to center on the user's current location).
 * @param mapState The [W3WMapState] object that holds the mapState of the map.
 * @param buttonState The [W3WButtonsState] object that holds the buttonState of the map.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param content Optional composable content to be displayed on the map.
 * @param onMapTypeClicked Callback invoked when the map type is clicked.
 * @param onMapClicked Callback invoked when the map is clicked.
 * @param onMyLocationClicked Callback invoked when the my location button is clicked.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 * @param onError Callback invoked when an error occurs.
 * @param onMapProjectionProvided Callback providing map projection updates.
 * @param onMapViewPortProvided Callback providing map viewport updates.
 * @param onRecallButtonPositionProvided Callback providing recall button position.
 */
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig = W3WMapDefaults.defaultLayoutConfig(),
    mapConfig: W3WMapDefaults.MapConfig = W3WMapDefaults.defaultMapConfig(),
    locationButtonColor: W3WMapButtonsDefault.LocationButtonColor = defaultLocationButtonColor(),
    mapColors: MapColors = defaultMapColors(),
    mapState: W3WMapState,
    buttonState: W3WButtonsState,
    mapProvider: MapProvider,
    content: (@Composable () -> Unit)? = null,
    onMarkerClicked: ((W3WMarker) -> Unit)? = null,
    onMapTypeClicked: ((W3WMapType) -> Unit)? = null,
    onMyLocationClicked: (() -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit)? = null,
    onRecallClicked: (() -> Unit)? = null,
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    onError: ((W3WError) -> Unit)? = null,
    onMapProjectionProvided: ((W3WMapProjection) -> Unit)? = null,
    onMapViewPortProvided: ((W3WGridScreenCell) -> Unit)? = null,
    onRecallButtonPositionProvided: ((PointF) -> Unit)? = null,
) {
    W3WMapContent(
        modifier = modifier,
        layoutConfig = layoutConfig,
        mapConfig = mapConfig,
        mapColors = mapColors,
        locationButtonColor = locationButtonColor,
        mapProvider = mapProvider,
        content = content,
        mapState = mapState,
        buttonState = buttonState,
        onMapClicked = { onMapClicked?.invoke(it) },
        onMapTypeClicked = {
            onMapTypeClicked?.invoke(it)
        },
        onMyLocationClicked = {
            onMyLocationClicked?.invoke()
        },
        onCameraUpdated = {
            onCameraUpdated.invoke(it)
        },
        onMarkerClicked = {
            onMarkerClicked?.invoke(it)
        },
        onError = onError,
        onMapProjectionUpdated = {
            onMapProjectionProvided?.invoke(it)
        },
        onMapViewPortProvided = {
            onMapViewPortProvided?.invoke(it)
        },
        onRecallClicked = {
            onRecallClicked?.invoke()
        },
        onRecallButtonPositionProvided = {
            onRecallButtonPositionProvided?.invoke(it)
        }
    )
}

/**
 * A composable function that displays Map and Buttons.
 *
 * This function is responsible for rendering the map, handling user interactions,
 * and managing the map's mapState. It provides options for customizing the map's
 * appearance, layout, and behavior.
 * @param modifier Modifier for styling and layout.
 * @param layoutConfig [W3WMapDefaults.LayoutConfig] Configuration for the map's layout.
 * @param mapConfig [W3WMapDefaults.MapConfig] Configuration for the map's appearance.
 * Determines the map's color scheme based on the current map type and dark mode setting.
 * - If the `mapType` is `NORMAL` or `TERRAIN`:
 *     - If `isDarkMode` is `true`, the dark color scheme (`darkMapColor`) is used.
 *     - If `isDarkMode` is `false`, the normal color scheme (`normalMapColor`) is used.
 * - If the `mapType` is `HYBRID` or `SATELLITE`, the satellite color scheme (`satelliteMapColor`) is used, regardless of the dark mode setting.
 * @param mapColors [W3WMapDefaults.MapColors] Configuration for the map's color such as light, dark and satellite mode will based on mapType and state isDarkMode
 * @param locationButtonColor Color settings for the location button (the button to center on the user's current location).
 * @param mapState The [W3WMapState] object that holds the mapState of the map.
 * @param buttonState The [W3WButtonsState] object that holds the buttonState of the map.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param content Optional composable content to be displayed on the map.
 * @param onMarkerClicked Callback invoked when a marker on the map is clicked.
 * @param onMapTypeClicked Callback invoked when the user clicks on the map type button.
 * @param onMyLocationClicked Callback invoked when the user clicks on the my location button.
 * @param onMapClicked Callback invoked when the user clicks on the map.
 * @param onRecallClicked Callback invoked when the user clicks on the recall button.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 * @param onError Callback invoked when an error occurs during map initialization or interaction.
 * @param onMapProjectionUpdated Callback providing map projection updates.
 * @param onMapViewPortProvided Callback providing map viewport updates.
 * @param onRecallButtonPositionProvided Callback providing recall button position.
 */
@Composable
internal fun W3WMapContent(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    locationButtonColor: W3WMapButtonsDefault.LocationButtonColor,
    mapColors: MapColors = defaultMapColors(),
    mapState: W3WMapState,
    buttonState: W3WButtonsState,
    mapProvider: MapProvider,
    content: (@Composable () -> Unit)? = null,
    onMarkerClicked: ((W3WMarker) -> Unit),
    onMapTypeClicked: ((W3WMapType) -> Unit),
    onMyLocationClicked: () -> Unit,
    onRecallClicked: () -> Unit,
    onMapClicked: (W3WCoordinates) -> Unit,
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    onError: ((W3WError) -> Unit)? = null,
    onMapProjectionUpdated: (W3WMapProjection) -> Unit,
    onMapViewPortProvided: (W3WGridScreenCell) -> Unit,
    onRecallButtonPositionProvided: ((PointF) -> Unit),
) {
    // Handles check location permissions, if isMyLocationEnabled enable
    MapPermissionsHandler(mapState = mapState, onError = onError) {

        var bounds by remember { mutableStateOf(Rect.Zero) }

        // Check if the map is initialized, use to prevent LaunchedEffect to re-run on configuration changes
        val isInitialized = rememberSaveable { mutableStateOf(false) }

        // // Fetch current location when launch, but not on configuration changes
        LaunchedEffect(isInitialized) {
            if (!isInitialized.value && mapState.isMyLocationEnabled) {
                onMyLocationClicked.invoke()
                isInitialized.value = true
            }
        }

        val mapColor = remember(mapState.mapType, mapState.isDarkMode) {
            when (mapState.mapType) {
                W3WMapType.NORMAL,
                W3WMapType.TERRAIN -> {
                    if (mapState.isDarkMode) mapColors.darkMapColor else mapColors.normalMapColor
                }

                W3WMapType.HYBRID,
                W3WMapType.SATELLITE -> {
                    mapColors.satelliteMapColor
                }
            }
        }

        val recallButtonColor = remember(mapState.selectedAddress, mapState.markers, mapColor) {
            derivedStateOf {
                val markersAtSelectedSquare =
                    mapState.markers.filter { it.square.contains(mapState.selectedAddress?.center) }
                val color = when (markersAtSelectedSquare.size) {
                    0 -> mapColor.markerColors.selectedZoomOutColor
                    1 -> markersAtSelectedSquare.first().color
                    else -> mapColor.markerColors.defaultMarkerColor
                }
                W3WMapButtonsDefault.RecallButtonColor(
                    recallArrowColor = color.slash,
                    recallBackgroundColor = color.background
                )
            }
        }

        val buttonsLayoutConfig = layoutConfig.buttonsLayoutConfig

        LaunchedEffect(bounds) {
            bounds.let {
                val leftTop = PointF(it.left, it.top)
                val rightTop = PointF(it.right, it.top)
                val rightBottom = PointF(it.right, it.bottom)
                val leftBottom = PointF(it.left, it.bottom)
                onMapViewPortProvided.invoke(
                    W3WGridScreenCell(
                        leftTop,
                        rightTop,
                        rightBottom,
                        leftBottom
                    )
                )
            }
        }

        Box(
            modifier = modifier
                .onGloballyPositioned { coordinates ->
                    bounds = coordinates.boundsInParent()
                }
        ) {
            W3WMapView(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                mapColor = mapColor,
                mapProvider = mapProvider,
                mapState = mapState,
                onMarkerClicked = onMarkerClicked,
                onMapClicked = onMapClicked,
                content = content,
                onCameraUpdated = onCameraUpdated,
                onMapProjectionUpdated = onMapProjectionUpdated
            )

            MapButtons(
                modifier = Modifier
                    .align(buttonsLayoutConfig.buttonAlignment.toComposeAlignment())
                    .padding(buttonsLayoutConfig.buttonPadding),
                buttonConfig = mapConfig.buttonConfig,
                buttonState = buttonState,
                mapType = mapState.mapType,
                locationButtonColor = locationButtonColor,
                recallButtonColor = recallButtonColor.value,
                onMapTypeClicked = onMapTypeClicked,
                onMyLocationClicked = onMyLocationClicked,
                onRecallClicked = onRecallClicked,
                onRecallButtonPositionProvided = onRecallButtonPositionProvided,
            )
        }
    }
}

/**
 * A composable function that handles location permissions for the map.
 *
 * This function checks if the "My Location" feature is enabled in the map state
 * and requests the necessary location permissions if needed. If the permissions
 * are granted, it displays the provided content. Otherwise, it invokes the
 * `onError` callback with a [W3WError] indicating that permissions are required.
 *
 * @param mapState The [W3WMapState] object that holds the state of the map.
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
 * @param mapColor [W3WMapDefaults.MapColor] Configuration for the map's appearance color such gridLine, marker.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param mapState The [W3WMapState] object that holds the mapState of the map.
 * @param content Optional composable content to be displayed on the map.
 * @param onMarkerClicked Callback invoked when a marker on the map is clicked.
 * @param onMapClicked Callback invoked when the user clicks on the map.
 * @param onCameraUpdated Callback invoked when the camera position is updated.
 * @param onMapProjectionUpdated Callback invoked when the map projection is updated.
 */
@Composable
internal fun W3WMapView(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    mapColor: W3WMapDefaults.MapColor,
    mapProvider: MapProvider,
    mapState: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMarkerClicked: ((W3WMarker) -> Unit),
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: (W3WCameraState<*>) -> Unit,
    onMapProjectionUpdated: (W3WMapProjection) -> Unit,
) {
    when (mapProvider) {
        MapProvider.GOOGLE_MAP -> {
            W3WGoogleMap(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                mapColor = mapColor,
                state = mapState,
                content = content,
                onMapClicked = onMapClicked,
                onMarkerClicked = onMarkerClicked,
                onCameraUpdated = onCameraUpdated,
                onMapProjectionUpdated = onMapProjectionUpdated
            )
        }

        MapProvider.MAPBOX -> {
            W3WMapBox(
                modifier = modifier,
                layoutConfig = layoutConfig,
                mapConfig = mapConfig,
                mapColor = mapColor,
                state = mapState,
                content = content,
                onMapClicked = onMapClicked,
                onMarkerClicked = onMarkerClicked,
                onCameraUpdated = onCameraUpdated,
                onMapProjectionUpdated = onMapProjectionUpdated
            )
        }
    }
}

/**
 * This function is responsible for updating camera position and button state based on current location
 *
 * @param locationSource An optional [W3WLocationSource] used to fetch the user's location.
 * @param mapManager The [W3WMapManager] instance that manages the map's state and interactions.
 * @param coroutineScope The [CoroutineScope] used to launch coroutines for asynchronous operations.
 * @param onError Callback invoked when an error occurs during location fetching or map interaction.
 */
private fun fetchCurrentLocation(
    locationSource: W3WLocationSource?,
    mapManager: W3WMapManager,
    coroutineScope: CoroutineScope,
    shouldSelect: Boolean,
    onError: ((W3WError) -> Unit)? = null
) {
    locationSource?.let {
        coroutineScope.launch {
            try {
                val location = it.fetchLocation()
                // Update camera state
                mapManager.moveToPosition(
                    coordinates = W3WCoordinates(location.latitude, location.longitude),
                    zoom = when (mapManager.mapProvider) {
                        MapProvider.GOOGLE_MAP -> {
                            W3WGoogleCameraState.MY_LOCATION_ZOOM
                        }

                        MapProvider.MAPBOX -> {
                            W3WMapboxCameraState.MY_LOCATION_ZOOM.toFloat()
                        }
                    },
                    animate = true
                )

                if (shouldSelect) {
                    mapManager.setSelectedAt(W3WCoordinates(location.latitude, location.longitude))
                }

                if (location.hasAccuracy()) {
                    mapManager.updateAccuracyDistance(location.accuracy)
                }
            } catch (e: Exception) {
                onError?.invoke(W3WError("Location fetch failed: ${e.message}"))
            }
        }
    }
}