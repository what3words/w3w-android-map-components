package com.what3words.components.compose.maps.providers.googlemap

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.what3words.components.compose.maps.W3WMapDefaults.defaultMarkerConfig
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.utils.getFillGridMarkerBitmap
import com.what3words.components.compose.maps.utils.getMarkerBitmap
import com.what3words.components.compose.maps.utils.getPinBitmap
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.map.components.compose.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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
        if (mapConfig.gridLineConfig.isGridEnabled) {
            // Draw grid lines
            W3WGoogleMapDrawGridLines(
                verticalLines = state.gridLines.verticalLines,
                horizontalLines = state.gridLines.horizontalLines,
                zoomLevel = cameraState.getZoomLevel(),
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
                            cameraBound.contains(W3WCoordinates(it.latLng.lat, it.latLng.lng)) &&
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
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                markers = state.markers,
                selectedMarker = state.selectedAddress,
                onMarkerClicked = onMarkerClicked
            )
        }

        if (state.selectedAddress != null) {
            //Draw the selected address
            W3WGoogleMapDrawSelectedAddress(
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                selectedMarker = state.selectedAddress
            )
        }
    }
}


@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawGridLines(
    verticalLines: List<W3WLatLng>,
    horizontalLines: List<W3WLatLng>,
    zoomLevel: Float,
    gridLinesConfig: W3WMapDefaults.GridLinesConfig,
    isDarkMode: Boolean
) {
    val shouldDrawGrid = remember(zoomLevel) {
        derivedStateOf {
            zoomLevel > gridLinesConfig.zoomSwitchLevel
        }
    }

    if (shouldDrawGrid.value) {
        val horizontalPolylines = horizontalLines.map { coordinate ->
            LatLng(coordinate.lat, coordinate.lng)
        }

        val verticalPolylines = verticalLines.map { coordinate ->
            LatLng(coordinate.lat, coordinate.lng)
        }

        val gridLineColor = remember(isDarkMode) {
            if(isDarkMode) gridLinesConfig.gridColorDarkMode else gridLinesConfig.gridColor
        }

        Polyline(
            points = horizontalPolylines,
            color = gridLineColor,
            width = gridLinesConfig.gridLineWidth.value,
            clickable = false,
            zIndex = 1f
        )

        Polyline(
            points = verticalPolylines,
            color = gridLineColor,
            width = gridLinesConfig.gridLineWidth.value,
            clickable = false,
            zIndex = 1f
        )
    }
}

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig = defaultMarkerConfig(),
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedMarker: W3WMarker
) {
    val drawZoomIn = remember(zoomLevel) {
        derivedStateOf {
            zoomLevel > zoomSwitchLevel
        }
    }

    if (drawZoomIn.value) {
        DrawZoomInSelectedAddress(
            zoomLevel = zoomLevel,
            zoomSwitchLevel = zoomSwitchLevel,
            selectedMarker = selectedMarker
        )
    } else {
        DrawZoomOutSelectedAddress(markerConfig, selectedMarker)

    }
}

@Composable
@GoogleMapComposable
private fun DrawZoomOutSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig,
    selectedMarker: W3WMarker,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val color =
        if (selectedMarker.isInMultipleList) markerConfig.multiListMarkersColor else selectedMarker.color

    val markerState = rememberUpdatedMarkerState(selectedMarker.latLng.toGoogleLatLng())

    LaunchedEffect(selectedMarker.latLng) {
        markerState.position = selectedMarker.latLng.toGoogleLatLng()
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
    selectedMarker: W3WMarker,
    zoomLevel: Float,
    zoomSwitchLevel: Float
) {
    val context = LocalContext.current
    selectedMarker.square.let { square ->
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
            color = Color.Black,
            width = getGridSelectedBorderSizeBasedOnZoomLevel(
                context,
                zoomLevel,
                zoomSwitchLevel,
            ),
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
    selectedMarker: W3WMarker?,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    val drawZoomIn = remember(zoomLevel) {
        derivedStateOf {
            zoomLevel > zoomSwitchLevel
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
            markers = markers,
            selectedMarker = selectedMarker,
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

    markers.forEach { marker ->
        val square = marker.square
        val color =
            if (marker.isInMultipleList) markerConfig.multiListMarkersColor else marker.color

        val icon = bitmapCache.getOrPut(color.id) {
            BitmapDescriptorFactory.fromBitmap(
                getFillGridMarkerBitmap(
                    context,
                    density,
                    color
                )
            )
        }

        GroundOverlay(
            position = GroundOverlayPosition.create(
                LatLngBounds(
                    LatLng(
                        square.southwest.lat,
                        square.southwest.lng
                    ),
                    LatLng(square.northeast.lat, square.northeast.lng)
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
    selectedMarker: W3WMarker?,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val currentOnMarkerClicked by rememberUpdatedState(onMarkerClicked)

    // Map of cached bitmap with key is the ID of the W3WColor
    val bitmapCache = remember { mutableMapOf<Long, BitmapDescriptor>() }

    markers.filter { it != selectedMarker }.forEach { marker ->
        val color =
            if (marker.isInMultipleList) markerConfig.multiListMarkersColor else marker.color

        val icon = bitmapCache.getOrPut(color.id) {
            BitmapDescriptorFactory.fromBitmap(
                getPinBitmap(
                    context,
                    density,
                    color
                )
            )
        }

        val position = LatLng(marker.latLng.lat, marker.latLng.lng)
        val state = rememberUpdatedMarkerState(position)

        Marker(
            state = state,
            icon = icon,
            onClick = {
                currentOnMarkerClicked(marker)
                true
            },
            title = marker.title,
            snippet = marker.snippet
        )
    }
}

private fun getGridSelectedBorderSizeBasedOnZoomLevel(
    context: Context,
    zoomLevel: Float,
    zoomSwitchLevel: Float
): Float {
    return when {
        zoomLevel < zoomSwitchLevel -> context.resources.getDimension(R.dimen.grid_width_gone)
        zoomLevel >= zoomSwitchLevel && zoomLevel < 19f -> context.resources.getDimension(R.dimen.grid_selected_width_google_map_1dp)
        zoomLevel in 19f..20f -> context.resources.getDimension(R.dimen.grid_selected_width_google_map_1_5dp)
        else -> context.resources.getDimension(R.dimen.grid_selected_width_google_map_2dp)
    }
}

// Workaround solution for the issue with rememberMarkerState(): https://stackoverflow.com/questions/75920971/how-to-make-remembermarkerstate-work-correctly-in-jetpack-compose
@Composable
fun rememberUpdatedMarkerState(newPosition: LatLng) =
    remember { MarkerState(position = newPosition) }
        .apply { position = newPosition }