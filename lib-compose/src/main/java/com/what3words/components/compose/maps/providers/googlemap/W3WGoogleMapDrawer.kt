package com.what3words.components.compose.maps.providers.googlemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.GroundOverlay
import com.google.maps.android.compose.GroundOverlayPosition
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapDefaults.MIN_SUPPORT_GRID_ZOOM_LEVEL_GOOGLE
import com.what3words.components.compose.maps.W3WMapDefaults.defaultMarkerConfig
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.extensions.id
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.utils.getFillGridMarkerBitmap
import com.what3words.components.compose.maps.utils.getMarkerBitmap
import com.what3words.components.compose.maps.utils.getPinBitmap
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * A composable function that draws What3Words component support:
 * the grid lines, markers, and selected address on a Google Map.
 *
 * @param state The [W3WMapState] object that holds the state of the map, including grid lines configuration.
 * @param mapConfig The [W3WMapDefaults.MapConfig] object that holds the configuration for the map, including styling options.
 *
 */
@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawer(
    state: W3WMapState,
    mapConfig: W3WMapDefaults.MapConfig,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    state.cameraState?.let { cameraState ->
        val shouldDrawGrid = remember(mapConfig, cameraState.getZoomLevel()) {
            derivedStateOf {
                mapConfig.gridLineConfig.isGridEnabled && cameraState.getZoomLevel() >= mapConfig.gridLineConfig.zoomSwitchLevel && cameraState.getZoomLevel() >= MIN_SUPPORT_GRID_ZOOM_LEVEL_GOOGLE
            }
        }

        if (shouldDrawGrid.value) {
            // Draw grid lines
            W3WGoogleMapDrawGridLines(
                verticalLines = state.gridLines.verticalLines,
                horizontalLines = state.gridLines.horizontalLines,
                gridLinesConfig = mapConfig.gridLineConfig,
                isDarkMode = state.isDarkMode
            )
        }


        /**
         * Optimized marker rendering for W3WGoogleMapDrawer.
         *
         * This implementation provides an efficient way to render markers on a map, optimizing for
         * performance and responsiveness, especially when dealing with a large number of markers.
         *
         * Key features:
         * 1. Progressive marker accumulation: Markers are added to the visible set gradually,
         *    reducing the workload in any single frame and improving responsiveness.
         * 2. Zoom level sensitivity: At high zoom levels (>= 19f), the visible marker set is reset,
         *    ensuring smooth transitions between zoom in and zoom out drawer.
         * 3. Efficient recomposition: Uses remember and derivedStateOf to minimize unnecessary
         *    recompositions and calculations.
         *
         * The algorithm works as follows:
         * - If markers exist, it starts accumulating visible markers based on the current camera bounds.
         * - It keeps track of whether all markers have been drawn to avoid unnecessary calculations.
         * - At zoomLevelThreshold, it resets the process to ensure the smooth transition between zoom in and zoom out drawers.
         * - It only adds new markers that are both within the camera bounds and not already visible.
         * - Once all markers are drawn, it stops the accumulation process to save resources.
         */
        if (state.markers.isNotEmpty()) {
            val zoomLevelThreshold =
                mapConfig.gridLineConfig.zoomSwitchLevel - 2f // buffer 2 zoom levels to ensure it can start calculate the visible markers on time before switching to zoom in drawer.
            var allMarkersDrawn by remember { mutableStateOf(false) }
            var visibleMarkers by remember {
                mutableStateOf<ImmutableList<W3WMarker>>(
                    persistentListOf()
                )
            }

            LaunchedEffect(cameraState.gridBound, state.markers, cameraState.getZoomLevel()) {
                val currentZoomLevel = cameraState.getZoomLevel()

                if (currentZoomLevel >= zoomLevelThreshold) {
                    allMarkersDrawn = false
                    visibleMarkers = persistentListOf()
                }

                if (!allMarkersDrawn) {
                    val cameraBound = cameraState.gridBound
                    if (cameraBound != null) {
                        val newVisibleMarkers = state.markers.filter {
                            cameraBound.contains(it.center) &&
                                    !visibleMarkers.contains(it)
                        }

                        visibleMarkers = (visibleMarkers + newVisibleMarkers).toImmutableList()

                        if (visibleMarkers.size == state.markers.size) {
                            allMarkersDrawn = true
                        }
                    }
                }
            }

            //Draw the markers
            W3WGoogleMapDrawMarkers(
                markerConfig = mapConfig.markerConfig,
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                markers = state.markers,
                selectedAddress = state.selectedAddress,
                onMarkerClicked = onMarkerClicked
            )
        }

        if (state.selectedAddress != null) {
            val markersInSelectedAddress by remember(state.selectedAddress, state.markers) {
                mutableStateOf(state.markers.filter {
                    it.square == state.selectedAddress.square
                }.toPersistentList())
            }

            //Draw the selected address
            W3WGoogleMapDrawSelectedAddress(
                markerConfig = mapConfig.markerConfig,
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                selectedAddress = state.selectedAddress,
                isDarkMode = state.isDarkMode,
                markersInSelectedAddress = markersInSelectedAddress
            )
        }
    }
}


@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawGridLines(
    verticalLines: List<W3WCoordinates>,
    horizontalLines: List<W3WCoordinates>,
    gridLinesConfig: W3WMapDefaults.GridLinesConfig,
    isDarkMode: Boolean
) {
    val gridLineColor = remember(isDarkMode) {
        derivedStateOf {
            if (isDarkMode) gridLinesConfig.gridColorDarkMode else gridLinesConfig.gridColor
        }
    }

    val horizontalPolylines = remember(horizontalLines) {
        horizontalLines.map { LatLng(it.lat, it.lng) }
    }

    val verticalPolylines = remember(verticalLines) {
        verticalLines.map { LatLng(it.lat, it.lng) }
    }

    Polyline(
        points = horizontalPolylines,
        color = gridLineColor.value,
        width = 1f,
        clickable = false,
        zIndex = 1f
    )

    Polyline(
        points = verticalPolylines,
        color = gridLineColor.value,
        width = 1f,
        clickable = false,
        zIndex = 1f
    )
}

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig = defaultMarkerConfig(),
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedAddress: W3WAddress,
    markersInSelectedAddress: ImmutableList<W3WMarker>,
    isDarkMode: Boolean
) {

    val density = LocalDensity.current.density

    val drawZoomIn = remember(zoomLevel) {
        derivedStateOf {
            zoomLevel > zoomSwitchLevel && zoomLevel >= MIN_SUPPORT_GRID_ZOOM_LEVEL_GOOGLE
        }
    }

    val gridLineWidth = remember(zoomLevel) {
        derivedStateOf {
            getSelectedGridWidth(zoomLevel, density)
        }
    }

    if (drawZoomIn.value) {
        DrawZoomInSelectedAddress(
            markerConfig = markerConfig,
            gridLineWidth = gridLineWidth.value,
            selectedAddress = selectedAddress,
            isDarkMode = isDarkMode
        )
    } else {
        DrawZoomOutSelectedAddress(
            markerConfig = markerConfig,
            selectedAddress = selectedAddress,
            markersInSelectedSquare = markersInSelectedAddress,
            isDarkMode = isDarkMode
        )
    }
}

@Composable
@GoogleMapComposable
private fun DrawZoomOutSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig,
    selectedAddress: W3WAddress,
    markersInSelectedSquare: ImmutableList<W3WMarker>,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val selectedZoomOutColor = if (isDarkMode) {
        markerConfig.selectedZoomOutColorDarkMode
    } else {
        markerConfig.selectedZoomOutColorLightMode
    }

    val color = when (markersInSelectedSquare.size) {
        0 -> selectedZoomOutColor
        1 -> markersInSelectedSquare.first().color
        else -> markerConfig.defaultMarkerColor
    }

    val markerState = rememberUpdatedMarkerState(selectedAddress.center!!.toGoogleLatLng())

    LaunchedEffect(selectedAddress.center) {
        markerState.position = selectedAddress.center!!.toGoogleLatLng()
    }

    val icon =
        BitmapDescriptorFactory.fromBitmap(
            getMarkerBitmap(
                context,
                density,
                color
            )
        )

    Marker(
        state = markerState,
        icon = icon,
        zIndex = 1f,
    )
}

@Composable
@GoogleMapComposable
private fun DrawZoomInSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig,
    selectedAddress: W3WAddress,
    gridLineWidth: Float,
    isDarkMode: Boolean
) {
    val color = remember(isDarkMode) {
        derivedStateOf { if (isDarkMode) markerConfig.selectedZoomInColorDarkMode else markerConfig.selectedZoomInColor }
    }

    selectedAddress.square?.let { square ->
        Polyline(
            points = listOf(
                LatLng(
                    square.northeast.lat,
                    square.southwest.lng
                ),
                LatLng(
                    square.northeast.lat,
                    square.northeast.lng
                ),
                LatLng(
                    square.southwest.lat,
                    square.northeast.lng
                ),
                LatLng(
                    square.southwest.lat,
                    square.southwest.lng
                ),
                LatLng(
                    square.northeast.lat,
                    square.southwest.lng
                )
            ),
            color = color.value,
            width = gridLineWidth,
            clickable = false,
            zIndex = 1f
        )
    }
}


@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawMarkers(
    markerConfig: W3WMapDefaults.MarkerConfig = defaultMarkerConfig(),
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    markers: ImmutableList<W3WMarker>,
    selectedAddress: W3WAddress?,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    val drawZoomIn = remember(zoomLevel) {
        derivedStateOf {
            zoomLevel > zoomSwitchLevel && zoomLevel >= MIN_SUPPORT_GRID_ZOOM_LEVEL_GOOGLE
        }
    }

    val zoomOutMarkers = remember(markers, selectedAddress) {
        derivedStateOf {
            selectedAddress?.let {
                markers.filter { it.square != selectedAddress.square }.toImmutableList()
            } ?: run {
                markers
            }
        }
    }

    if (drawZoomIn.value) {
        DrawZoomInMarkers(
            markerConfig = markerConfig,
            markers = markers
        )
    } else {
        DrawZoomOutMarkers(
            markerConfig = markerConfig,
            markers = zoomOutMarkers.value,
            onMarkerClicked = onMarkerClicked
        )
    }
}

@Composable
private fun DrawZoomInMarkers(
    markerConfig: W3WMapDefaults.MarkerConfig,
    markers: ImmutableList<W3WMarker>
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    // Map of cached bitmap with key is the ID of the W3WColor
    val bitmapCache = remember { mutableMapOf<Long, BitmapDescriptor>() }

    val markersBySquareId by remember(markers) {
        mutableStateOf(markers.groupBy { it.square.id })
    }

    markersBySquareId.forEach { (_, markers) ->
        val color =
            if (markers.size == 1) markers.first().color else markerConfig.defaultMarkerColor

        val icon = bitmapCache.getOrPut(color.id) {
            BitmapDescriptorFactory.fromBitmap(
                getFillGridMarkerBitmap(
                    context,
                    density,
                    color
                )
            )
        }

        val marker = markers.first() // Get the information from the first marker in the list
        GroundOverlay(
            position = GroundOverlayPosition.create(
                LatLngBounds(
                    LatLng(
                        marker.square.southwest.lat,
                        marker.square.southwest.lng
                    ),
                    LatLng(marker.square.northeast.lat, marker.square.northeast.lng)
                )
            ),
            image = icon,
        )
    }
}

@Composable
private fun DrawZoomOutMarkers(
    markerConfig: W3WMapDefaults.MarkerConfig,
    markers: ImmutableList<W3WMarker>,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val currentOnMarkerClicked by rememberUpdatedState(onMarkerClicked)

    // Map of cached bitmap with key is the ID of the W3WColor
    val bitmapCache = remember { mutableMapOf<Long, BitmapDescriptor>() }

    val markersBySquareId by remember(markers) {
        mutableStateOf(markers.groupBy { it.square.id })
    }

    markersBySquareId.forEach { (_, markers) ->
        val color =
            if (markers.size == 1) markers.first().color else markerConfig.defaultMarkerColor

        val icon = bitmapCache.getOrPut(color.id) {
            BitmapDescriptorFactory.fromBitmap(
                getPinBitmap(
                    context,
                    density,
                    color
                )
            )
        }

        val marker = markers.first() // Get the information from the first marker in the list
        val position = LatLng(marker.center.lat, marker.center.lng)
        val state = rememberUpdatedMarkerState(position)

        Marker(
            state = state,
            icon = icon,
            onClick = {
                currentOnMarkerClicked(marker)
                true
            }
        )
    }
}

// Workaround solution for the issue with rememberMarkerState(): https://stackoverflow.com/questions/75920971/how-to-make-remembermarkerstate-work-correctly-in-jetpack-compose
@Composable
fun rememberUpdatedMarkerState(newPosition: LatLng) =
    remember { MarkerState(position = newPosition) }
        .apply { position = newPosition }


private fun getSelectedGridWidth(zoomLevel: Float, density: Float): Float {
    return density * if (zoomLevel < 19) {
        1f
    } else {
        1.5f
    }
}