package com.what3words.components.compose.maps.providers.googlemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.extensions.id
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
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
    mapColor: W3WMapDefaults.MapColor,
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
                gridLineColor = mapColor.gridLineColor
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
                defaultMarkerColor = mapColor.markerColors.defaultMarkerColor,
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
                markerColors = mapColor.markerColors,
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                selectedAddress = state.selectedAddress,
                markersInSelectedAddress = markersInSelectedAddress
            )
        }
    }
}

/**
 * Renders grid lines on a Google Map.
 *
 * This composable function draws the what3words grid overlay on the map, consisting of both
 * horizontal and vertical lines that form the what3words square boundaries. The grid lines
 * provide visual reference for the what3words addressing system's geographic divisions.
 *
 * @param verticalLines List of coordinates representing vertical grid lines
 * @param horizontalLines List of coordinates representing horizontal grid lines
 * @param gridLineColor Color to use when drawing the grid lines
 */
@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawGridLines(
    verticalLines: List<W3WCoordinates>,
    horizontalLines: List<W3WCoordinates>,
    gridLineColor: Color
) {
    val horizontalPolylines = remember(horizontalLines) {
        horizontalLines.map { LatLng(it.lat, it.lng) }
    }

    val verticalPolylines = remember(verticalLines) {
        verticalLines.map { LatLng(it.lat, it.lng) }
    }

    Polyline(
        points = horizontalPolylines,
        color = gridLineColor,
        width = 1f,
        clickable = false,
        zIndex = 1f
    )

    Polyline(
        points = verticalPolylines,
        color = gridLineColor,
        width = 1f,
        clickable = false,
        zIndex = 1f
    )
}


/**
 * Renders the selected what3words address on a Google Map.
 *
 * This composable handles the display of the selected address, switching between
 * zoom in and zoom out rendering modes based on the current zoom level.
 *
 * @param markerColors Configuration for marker colors
 * @param zoomLevel Current zoom level of the map
 * @param zoomSwitchLevel Zoom level at which to switch between detail modes
 * @param selectedAddress The currently selected what3words address
 * @param markersInSelectedAddress List of markers that exist in the selected address's square
 */
@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawSelectedAddress(
    markerColors: W3WMapDefaults.MarkerColors,
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedAddress: W3WAddress,
    markersInSelectedAddress: ImmutableList<W3WMarker>
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
            selectedMarkerColor = markerColors.selectedColor,
            gridLineWidth = gridLineWidth.value,
            selectedAddress = selectedAddress,
        )
    } else {
        DrawZoomOutSelectedAddress(markerColors, selectedAddress, markersInSelectedAddress)
    }
}


/**
 * Renders a selected what3words address when zoomed out.
 *
 * This composable displays a marker at the center of the selected address. It uses
 * different colors based on whether there are markers within the selected square.
 *
 * @param markerColors Configuration for marker colors
 * @param selectedAddress The currently selected what3words address
 * @param markersInSelectedSquare List of markers that exist in the selected square
 */
@Composable
@GoogleMapComposable
private fun DrawZoomOutSelectedAddress(
    markerColors: W3WMapDefaults.MarkerColors,
    selectedAddress: W3WAddress,
    markersInSelectedSquare: ImmutableList<W3WMarker>
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val color = when (markersInSelectedSquare.size) {
        0 -> markerColors.selectedZoomOutColor
        1 -> markersInSelectedSquare.first().color
        else -> markerColors.defaultMarkerColor
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
        anchor = Offset(0.5f, 0.8f)
    )
}


/**
 * Renders a selected what3words address when zoomed in.
 *
 * This composable draws an outline around the selected what3words square using a polyline.
 *
 * @param selectedMarkerColor Color for the selected square's outline
 * @param selectedAddress The currently selected what3words address
 * @param gridLineWidth Width of the outline around the selected square
 */
@Composable
@GoogleMapComposable
private fun DrawZoomInSelectedAddress(
    selectedMarkerColor: Color,
    selectedAddress: W3WAddress,
    gridLineWidth: Float
) {
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
            color = selectedMarkerColor,
            width = gridLineWidth,
            clickable = false,
            zIndex = 1f
        )
    }
}


/**
 * Renders markers and the selected address on a Google Map.
 *
 * This composable function handles drawing all markers and the selected address,
 * switching between zoom in and zoom out rendering modes based on the current zoom level.
 *
 * @param defaultMarkerColor Default color for markers
 * @param zoomLevel Current zoom level of the map
 * @param zoomSwitchLevel Zoom level at which to switch between detail modes
 * @param markers List of all markers to display
 * @param selectedAddress Currently selected what3words address, if any
 * @param onMarkerClicked Callback invoked when a marker is clicked
 */
@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawMarkers(
    defaultMarkerColor: W3WMarkerColor,
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
            defaultMarkerColor = defaultMarkerColor,
            markers = markers
        )
    } else {
        DrawZoomOutMarkers(
            defaultMarkerColor = defaultMarkerColor,
            markers = zoomOutMarkers.value,
            onMarkerClicked = onMarkerClicked
        )
    }
}


/**
 * Renders markers on a Google Map when zoomed in.
 *
 * This composable draws filled markers at a higher zoom level, using ground overlays to
 * highlight entire what3words squares. When multiple markers share the same square,
 * it uses a default color to indicate multiple points of interest.
 *
 * @param defaultMarkerColor The default color to use for squares with multiple markers
 * @param markers The list of markers to display on the map
 */
@Composable
private fun DrawZoomInMarkers(
    defaultMarkerColor: W3WMarkerColor,
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
            if (markers.size == 1) markers.first().color else defaultMarkerColor

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


/**
 * Renders markers on a Google Map when zoomed out.
 *
 * This composable draws markers at a lower zoom level, using pin icons to represent
 * what3words addresses. When multiple markers share the same square, it uses a default
 * color to indicate multiple points of interest.
 *
 * @param defaultMarkerColor The default color to use for markers with multiple items
 * @param markers The list of markers to display on the map
 * @param onMarkerClicked Callback invoked when a marker is clicked
 */
@Composable
private fun DrawZoomOutMarkers(
    defaultMarkerColor: W3WMarkerColor,
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
            if (markers.size == 1) markers.first().color else defaultMarkerColor

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
            },
            anchor = Offset(0.5f, 0.5f)
        )
    }
}

/**
 * Remembers and updates a MarkerState with the given position.
 *
 * This is a workaround solution for issues with the standard rememberMarkerState() function.
 * It creates a MarkerState instance and updates its position to ensure proper rendering.
 *
 * @param newPosition The LatLng position for the marker
 * @return A MarkerState object with the updated position
 */
@Composable
fun rememberUpdatedMarkerState(newPosition: LatLng) =
    remember { MarkerState(position = newPosition) }
        .apply { position = newPosition }


/**
 * Calculates the appropriate width for the selected grid based on the zoom level.
 *
 * This function adjusts the grid line width according to the current zoom level
 * to ensure proper visibility at different scales.
 *
 * @param zoomLevel The current map zoom level
 * @param density The display density factor
 * @return The calculated width for the selected grid lines
 */
private fun getSelectedGridWidth(zoomLevel: Float, density: Float): Float {
    return density * if (zoomLevel < 19) {
        1f
    } else {
        1.5f
    }
}