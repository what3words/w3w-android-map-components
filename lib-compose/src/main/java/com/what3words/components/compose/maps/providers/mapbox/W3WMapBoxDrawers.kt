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
import com.what3words.components.compose.maps.W3WMapDefaults.defaultMarkerConfig
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.extensions.id
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

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawer(
    state: W3WMapState,
    mapConfig: W3WMapDefaults.MapConfig,
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
                gridLinesConfig = mapConfig.gridLineConfig,
                isDarkMode = state.isDarkMode
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
                markerConfig = mapConfig.markerConfig,
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
                markerConfig = mapConfig.markerConfig,
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                selectedAddress = state.selectedAddress,
                isDarkMode = state.isDarkMode,
                markersInSelectedSquare = markersInSelectedSquare
            )
        }
    }
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawGridLines(
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
                lineColor = gridLineColor.value
            }
        }
    )
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig = defaultMarkerConfig(),
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedAddress: W3WAddress,
    markersInSelectedSquare: ImmutableList<W3WMarker>,
    isDarkMode: Boolean,
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
            markerConfig = markerConfig,
            gridLineWidth = gridLineWidth.value,
            selectedAddress = selectedAddress,
            isDarkMode = isDarkMode,
        )
    } else {
        DrawZoomOutSelectedAddress(
            markerConfig,
            selectedAddress,
            markersInSelectedSquare
        )
    }
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawMarkers(
    markerConfig: W3WMapDefaults.MarkerConfig = defaultMarkerConfig(),
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
            markerConfig,
            markers
        )
    } else {
        DrawZoomOutMarkers(
            markerConfig,
            zoomOutMarkers.value,
            onMarkerClicked,
        )
    }
}

@Composable
@MapboxMapComposable
private fun DrawZoomOutMarkers(
    markerConfig: W3WMapDefaults.MarkerConfig,
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
                .first().color else markerConfig.defaultMarkerColor

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

@Composable
@MapboxMapComposable
private fun DrawZoomInMarkers(
    markerConfig: W3WMapDefaults.MarkerConfig,
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
            if (markers.size == 1) markers.first().color else markerConfig.defaultMarkerColor

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

@Composable
@MapboxMapComposable
private fun DrawZoomOutSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig,
    selectedAddress: W3WAddress,
    markersInSelectedSquare: ImmutableList<W3WMarker>,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val color = when (markersInSelectedSquare.size) {
        0 -> markerConfig.selectedZoomOutColor
        1 -> markersInSelectedSquare.first().color
        else -> markerConfig.defaultMarkerColor
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

@Composable
@MapboxMapComposable
private fun DrawZoomInSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig,
    selectedAddress: W3WAddress,
    gridLineWidth: Double,
    isDarkMode: Boolean
) {
    val color = remember(isDarkMode) {
        derivedStateOf { if (isDarkMode) markerConfig.selectedZoomInColorDarkMode else markerConfig.selectedZoomInColor }
    }

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
                    lineColor = color.value
                    lineWidth = gridLineWidth
                }
            }
        )
    }
}

private fun getSelectedGridWidth(zoomLevel: Float): Double {
    return if (zoomLevel < 20) {
        2.0
    } else {
        2.5
    }
}