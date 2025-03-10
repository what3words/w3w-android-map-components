package com.what3words.components.compose.maps.providers.mapbox

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotationGroupState
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.PointListValue
import com.mapbox.maps.extension.compose.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.compose.style.layers.generated.RasterLayerState
import com.mapbox.maps.extension.compose.style.sources.generated.rememberImageSourceState
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.generated.ImageSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.updateImage
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapDefaults.MIN_SUPPORT_GRID_ZOOM_LEVEL_MAP_BOX
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.extensions.id
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
 * Main drawer component for rendering what3words elements on a Mapbox map.
 *
 * This composable handles the rendering of all what3words visual elements including grid lines,
 * markers, and selected addresses. It manages rendering optimization with progressive loading
 * of markers based on the current viewport.
 *
 * @param state The current state of the what3words map
 * @param mapConfig Configuration settings for the map
 * @param mapColor Color settings for map elements
 * @param onMarkerClicked Callback invoked when a marker is clicked
 */
@Composable
@MapboxMapComposable
fun W3WMapBoxDrawer(
    state: W3WMapState,
    mapConfig: W3WMapDefaults.MapConfig,
    mapColor: W3WMapDefaults.MapColor,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    state.cameraState?.let { cameraState ->
        val shouldDrawGrid = remember(mapConfig, cameraState.getZoomLevel()) {
            derivedStateOf {
                mapConfig.gridLineConfig.isGridEnabled && cameraState.getZoomLevel() >= mapConfig.gridLineConfig.zoomSwitchLevel && cameraState.getZoomLevel() >= MIN_SUPPORT_GRID_ZOOM_LEVEL_MAP_BOX
            }
        }

        if (shouldDrawGrid.value) {
            W3WMapBoxDrawGridLines(
                verticalLines = state.gridLines.verticalLines,
                horizontalLines = state.gridLines.horizontalLines,
                gridLineColor = mapColor.gridLineColor,
            )
        }

        /**
         * Optimized marker rendering for W3WMapBoxDrawer.
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

            W3WMapBoxDrawMarkers(
                defaultMarkerColor = mapColor.markerColors.defaultMarkerColor,
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                markers = visibleMarkers,
                selectedAddress = state.selectedAddress,
                onMarkerClicked = onMarkerClicked
            )
        }

        if (state.selectedAddress != null) {
            val markersInSelectedSquare by remember(state.selectedAddress, state.markers) {
                mutableStateOf(state.markers.filter {
                    it.square == state.selectedAddress.square
                }
                    .toPersistentList())
            }

            //Draw the selected address
            W3WMapBoxDrawSelectedAddress(
                markerColors = mapColor.markerColors,
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                selectedAddress = state.selectedAddress,
                markersInSelectedSquare = markersInSelectedSquare
            )
        }
    }
}

/**
 * Renders grid lines on a Mapbox map.
 *
 * This composable draws the what3words grid overlay consisting of horizontal and vertical
 * lines that form the what3words square boundaries.
 *
 * @param verticalLines List of coordinates representing vertical grid lines
 * @param horizontalLines List of coordinates representing horizontal grid lines
 * @param gridLineColor Color to use when drawing the grid lines
 */
@Composable
@MapboxMapComposable
fun W3WMapBoxDrawGridLines(
    verticalLines: List<W3WCoordinates>,
    horizontalLines: List<W3WCoordinates>,
    gridLineColor: Color
) {
    val polylines = remember(verticalLines, horizontalLines) {
        listOf(
            PolylineAnnotationOptions()
                .withPoints(verticalLines.map { Point.fromLngLat(it.lng, it.lat) }),
            PolylineAnnotationOptions()
                .withPoints(horizontalLines.map { Point.fromLngLat(it.lng, it.lat) })
        )
    }

    PolylineAnnotationGroup(
        annotations = polylines,
        polylineAnnotationGroupState = remember {
            PolylineAnnotationGroupState().apply {
                lineOcclusionOpacity = 0.0
                lineEmissiveStrength = 1.0
                lineWidth = 1.0
                lineColor = gridLineColor
            }
        }
    )
}

/**
 * Renders the selected what3words address on a Mapbox map.
 *
 * This composable handles the display of the selected address, switching between
 * zoom in and zoom out rendering modes based on the current zoom level.
 *
 * @param markerColors Configuration for marker colors
 * @param zoomLevel Current zoom level of the map
 * @param zoomSwitchLevel Zoom level at which to switch between detail modes
 * @param selectedAddress The currently selected what3words address
 * @param markersInSelectedSquare List of markers that exist in the selected square
 */
@Composable
@MapboxMapComposable
fun W3WMapBoxDrawSelectedAddress(
    markerColors: W3WMapDefaults.MarkerColors,
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedAddress: W3WAddress,
    markersInSelectedSquare: ImmutableList<W3WMarker>,
) {
    val drawZoomIn = remember(zoomLevel) {
        derivedStateOf {
            zoomLevel > zoomSwitchLevel && zoomLevel >= MIN_SUPPORT_GRID_ZOOM_LEVEL_MAP_BOX
        }
    }

    if (drawZoomIn.value) {
        val gridLineWidth = remember(zoomLevel) {
            derivedStateOf {
                getSelectedGridWidth(zoomLevel)
            }
        }

        DrawZoomInSelectedAddress(
            selectedMarkerColor = markerColors.selectedColor,
            gridLineWidth = gridLineWidth.value,
            selectedAddress = selectedAddress,
        )
    } else {
        DrawZoomOutSelectedAddress(
            markerColors,
            selectedAddress,
            markersInSelectedSquare
        )
    }
}

/**
 * Renders markers on a Mapbox map.
 *
 * This composable function handles drawing all markers, switching between zoom in and zoom out
 * rendering modes based on the current zoom level.
 *
 * @param defaultMarkerColor Default color for markers
 * @param zoomLevel Current zoom level of the map
 * @param zoomSwitchLevel Zoom level at which to switch between detail modes
 * @param markers List of all markers to display
 * @param selectedAddress Currently selected what3words address, if any
 * @param onMarkerClicked Callback invoked when a marker is clicked
 */
@Composable
@MapboxMapComposable
fun W3WMapBoxDrawMarkers(
    defaultMarkerColor: W3WMarkerColor,
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    markers: ImmutableList<W3WMarker>,
    selectedAddress: W3WAddress?,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    val drawZoomIn = remember(zoomLevel) {
        derivedStateOf {
            zoomLevel > zoomSwitchLevel && zoomLevel >= MIN_SUPPORT_GRID_ZOOM_LEVEL_MAP_BOX
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
            defaultMarkerColor,
            markers
        )
    } else {
        DrawZoomOutMarkers(
            defaultMarkerColor,
            zoomOutMarkers.value,
            onMarkerClicked,
        )
    }
}

/**
 * Renders markers on a Mapbox map when zoomed out.
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
@MapboxMapComposable
private fun DrawZoomOutMarkers(
    defaultMarkerColor: W3WMarkerColor,
    markers: ImmutableList<W3WMarker>,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val currentOnMarkerClicked by rememberUpdatedState(onMarkerClicked)

    // Map of cached bitmap with key is the ID of the W3WColor
    val bitmapCache = remember { mutableMapOf<Long, Bitmap>() }

    val markersBySquareId by remember(markers) {
        mutableStateOf(markers.groupBy { it.square.id })
    }

    val annotations = remember(markers) {
        markersBySquareId.keys.map { squareId ->
            val color = if (markersBySquareId[squareId]!!.size == 1) markersBySquareId[squareId]!!
                .first().color else defaultMarkerColor

            val bitmap = bitmapCache.getOrPut(color.id) {
                getPinBitmap(
                    context,
                    density,
                    color
                )
            }

            val marker =
                markersBySquareId[squareId]!!.first() // Get the information from the first marker in the list
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(marker.center.lng, marker.center.lat))
                .withIconImage(bitmap)
                .withData(JsonPrimitive(squareId))
        }
    }

    PointAnnotationGroup(
        annotations = annotations,
    ) {
        interactionsState.onClicked { it ->
            val squareId = it.getData()?.asLong
            val marker = markers.find { it.square.id == squareId }
            marker?.let(currentOnMarkerClicked)
            true
        }
        iconEmissiveStrength = 1.0
        iconAllowOverlap = true
    }
}

/**
 * Renders markers on a Mapbox map when zoomed in.
 *
 * This composable draws filled markers at a higher zoom level, using raster image overlays to
 * highlight entire what3words squares. When multiple markers share the same square,
 * it uses a default color to indicate multiple points of interest.
 *
 * @param defaultMarkerColor The default color to use for squares with multiple markers
 * @param markers The list of markers to display on the map
 */
@Composable
@MapboxMapComposable
private fun DrawZoomInMarkers(
    defaultMarkerColor: W3WMarkerColor,
    markers: ImmutableList<W3WMarker>
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    // Map of cached bitmap with key is the ID of the W3WColor
    val bitmapCache = remember { mutableMapOf<Long, Bitmap>() }

    val markersBySquareId by remember(markers) {
        mutableStateOf(markers.groupBy { it.square.id })
    }

    markersBySquareId.forEach { (squareId, markers) ->
        val color =
            if (markers.size == 1) markers.first().color else defaultMarkerColor

        val bitmap = bitmapCache.getOrPut(color.id) {
            getFillGridMarkerBitmap(
                context,
                density,
                color
            )
        }

        MapEffect(bitmap) { mapView ->
            val imageSource: ImageSource? = mapView.mapboxMap.getSourceAs(squareId.toString())
            imageSource?.updateImage(bitmap)
        }

        val marker = markers.first() // Get the information from the first marker in the list
        key(squareId) {
            RasterLayer(
                layerId = remember { squareId.toString() },
                sourceState = rememberImageSourceState(sourceId = squareId.toString()) {
                    coordinates = PointListValue(
                        Point.fromLngLat(marker.square.southwest.lng, marker.square.northeast.lat),
                        Point.fromLngLat(marker.square.northeast.lng, marker.square.northeast.lat),
                        Point.fromLngLat(marker.square.northeast.lng, marker.square.southwest.lat),
                        Point.fromLngLat(marker.square.southwest.lng, marker.square.southwest.lat)
                    )
                },
                rasterLayerState = remember {
                    RasterLayerState().apply {
                        rasterContrast = DoubleValue(1.0)
                    }
                }
            )
        }
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
@MapboxMapComposable
private fun DrawZoomOutSelectedAddress(
    markerColors: W3WMapDefaults.MarkerColors,
    selectedAddress: W3WAddress,
    markersInSelectedSquare: ImmutableList<W3WMarker>,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val color = when (markersInSelectedSquare.size) {
        0 -> markerColors.selectedZoomOutColor
        1 -> markersInSelectedSquare.first().color
        else -> markerColors.defaultMarkerColor
    }

    val marker = rememberIconImage(
        key = color.id,
        painter = BitmapPainter(
            getMarkerBitmap(
                context,
                density,
                color
            ).asImageBitmap()
        )
    )

    PointAnnotation(
        point = Point.fromLngLat(
            selectedAddress.center!!.lng,
            selectedAddress.center!!.lat
        )
    ) {
        iconImage = marker
        iconEmissiveStrength = 1.0
        iconAnchor =
            IconAnchor.BOTTOM // This makes the arrow part of the icon to be at the center of the selected square
    }
}

/**
 * Renders a selected what3words address when zoomed in.
 *
 * This composable draws an outline around the selected what3words square using a polyline.
 *
 * @param selectedMarkerColor Color for the selected square's outline
 * @param gridLineWidth Width of the outline around the selected square
 * @param selectedAddress The currently selected what3words address
 */
@Composable
@MapboxMapComposable
private fun DrawZoomInSelectedAddress(
    selectedMarkerColor: Color,
    selectedAddress: W3WAddress,
    gridLineWidth: Double
) {
    selectedAddress.square?.let { square ->
        PolylineAnnotationGroup(
            annotations = listOf(
                PolylineAnnotationOptions()
                    .withPoints(
                        listOf(
                            Point.fromLngLat(square.southwest.lng, square.northeast.lat),
                            Point.fromLngLat(square.northeast.lng, square.northeast.lat),
                            Point.fromLngLat(square.northeast.lng, square.southwest.lat),
                            Point.fromLngLat(square.southwest.lng, square.southwest.lat),
                            Point.fromLngLat(square.southwest.lng, square.northeast.lat)
                        )
                    )
            ),
            polylineAnnotationGroupState = remember {
                PolylineAnnotationGroupState().apply {
                    lineOcclusionOpacity = 0.0
                    lineEmissiveStrength = 1.0
                    lineColor = selectedMarkerColor
                    lineWidth = gridLineWidth
                }
            }
        )
    }
}

/**
 * Calculates the appropriate width for the selected grid based on the zoom level.
 *
 * This function adjusts the grid line width according to the current zoom level
 * to ensure proper visibility at different scales.
 *
 * @param zoomLevel The current map zoom level
 * @return The calculated width for the selected grid lines
 */
private fun getSelectedGridWidth(zoomLevel: Float): Double {
    return if (zoomLevel < 20) {
        2.0
    } else {
        2.5
    }
}