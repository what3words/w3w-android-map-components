package com.what3words.components.compose.maps.providers.mapbox

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.PointListValue
import com.mapbox.maps.extension.compose.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.compose.style.sources.generated.rememberImageSourceState
import com.mapbox.maps.extension.style.sources.generated.ImageSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.updateImage
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapDefaults.defaultMarkerConfig
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.utils.getFillGridMarkerBitmap
import com.what3words.components.compose.maps.utils.getMarkerBitmap
import com.what3words.components.compose.maps.utils.getPinBitmap
import com.what3words.map.components.compose.R
import kotlinx.collections.immutable.ImmutableList

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawer(
    state: W3WMapState,
    mapConfig: W3WMapDefaults.MapConfig,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    state.cameraState?.let { cameraState ->
        if (mapConfig.gridLineConfig.isGridEnabled && cameraState.getZoomLevel() >= mapConfig.gridLineConfig.zoomSwitchLevel) {
            W3WMapBoxDrawGridLines(
                verticalLines = state.gridLines.verticalLines,
                horizontalLines = state.gridLines.horizontalLines,
                gridColor = mapConfig.gridLineConfig.gridColor,
                gridLineWidth = mapConfig.gridLineConfig.gridLineWidth
            )
        }

        if (state.markers.isNotEmpty()) {
            W3WMapBoxDrawMarkers(
                markerConfig = mapConfig.markerConfig,
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                markers = state.markers,
                onMarkerClicked = onMarkerClicked
            )
        }

        if (state.selectedAddress != null) {
            //Draw the selected address
            W3WMapBoxDrawSelectedAddress(
                markerConfig = mapConfig.markerConfig,
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                selectedMarker = state.selectedAddress
            )
        }
    }
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawGridLines(
    verticalLines: List<W3WLatLng>,
    horizontalLines: List<W3WLatLng>,
    gridColor: Color,
    gridLineWidth: Dp,
) {
    val polylines = remember(verticalLines, horizontalLines, gridColor, gridLineWidth) {
        listOf(
            PolylineAnnotationOptions()
                .withPoints(verticalLines.map { Point.fromLngLat(it.lng, it.lat) })
                .withLineColor(gridColor.toArgb())
                .withLineWidth(gridLineWidth.value.toDouble()),
            PolylineAnnotationOptions()
                .withPoints(horizontalLines.map { Point.fromLngLat(it.lng, it.lat) })
                .withLineColor(gridColor.toArgb())
                .withLineWidth(gridLineWidth.value.toDouble())
        )
    }

    PolylineAnnotationGroup(
        annotations = polylines
    )
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig = defaultMarkerConfig(),
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedMarker: W3WMarker
) {
    if (zoomLevel < zoomSwitchLevel) {
        DrawZoomOutSelectedAddress(
            markerConfig,
            selectedMarker
        )
    } else {
        DrawZoomInSelectedAddress(
            zoomLevel = zoomLevel,
            zoomSwitchLevel = zoomSwitchLevel,
            selectedMarker = selectedMarker
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
    onMarkerClicked: (W3WMarker) -> Unit
) {
    if (zoomLevel < zoomSwitchLevel) {
        DrawZoomOutMarkers(
            markerConfig,
            markers,
            onMarkerClicked
        )
    } else {
        DrawZoomInMarkers(
            markerConfig,
            markers
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

    val annotations = remember(markers) {
        markers.map { marker ->
            val color = if(marker.hasMultipleLists) markerConfig.multiMarkersColor else marker.color
            val bitmap = bitmapCache.getOrPut(color.id) {
                getPinBitmap(
                    context,
                    density,
                    color
                )
            }

            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(marker.latLng.lng, marker.latLng.lat))
                .withIconImage(bitmap)
                .withData(JsonPrimitive(marker.id))
        }
    }

    PointAnnotationGroup(
        annotations = annotations,
    ) {
        interactionsState.onClicked { it ->
            val markerId = it.getData()?.asLong
            val marker = markers.find { it.id == markerId }
            marker?.let(currentOnMarkerClicked)
            true
        }
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

    markers.forEach { marker ->
        val color = if(marker.hasMultipleLists) markerConfig.markerColor else marker.color
        val bitmap = bitmapCache.getOrPut(color.id) {
            getFillGridMarkerBitmap(
                context, 
                density,
                color
            )
        }

        val square = marker.square

        MapEffect(bitmap) { mapView ->
            val imageSource: ImageSource = mapView.mapboxMap.getSourceAs(marker.id.toString())!!
            imageSource.updateImage(bitmap)
        }

        RasterLayer(
            sourceState = rememberImageSourceState(sourceId = marker.id.toString()) {
                coordinates = PointListValue(
                    Point.fromLngLat(square.southwest.lng, square.northeast.lat),
                    Point.fromLngLat(square.northeast.lng, square.northeast.lat),
                    Point.fromLngLat(square.northeast.lng, square.southwest.lat),
                    Point.fromLngLat(square.southwest.lng, square.southwest.lat)
                )
            }
        )
    }
}

@Composable
@MapboxMapComposable
private fun DrawZoomOutSelectedAddress(
    markerConfig: W3WMapDefaults.MarkerConfig,
    selectedMarker: W3WMarker
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val color = if(selectedMarker.hasMultipleLists) markerConfig.multiMarkersColor else selectedMarker.color

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
            selectedMarker.latLng.lng,
            selectedMarker.latLng.lat
        )
    ) {
        iconImage = marker
    }
}

@Composable
@MapboxMapComposable
private fun DrawZoomInSelectedAddress(
    selectedMarker: W3WMarker,
    zoomLevel: Float,
    zoomSwitchLevel: Float
) {
    val context = LocalContext.current
    selectedMarker.square.let { square ->
        PolylineAnnotation(
            points = listOf(
                Point.fromLngLat(square.southwest.lng, square.northeast.lat),
                Point.fromLngLat(square.northeast.lng, square.northeast.lat),
                Point.fromLngLat(square.northeast.lng, square.southwest.lat),
                Point.fromLngLat(square.southwest.lng, square.southwest.lat),
                Point.fromLngLat(square.southwest.lng, square.northeast.lat)

            )
        ) {
            lineColor = Color.Black
            lineWidth =
                getGridSelectedBorderSizeBasedOnZoomLevel(context, zoomLevel, zoomSwitchLevel)
        }
    }
}

private fun getGridSelectedBorderSizeBasedOnZoomLevel(
    context: Context,
    zoomLevel: Float,
    zoomSwitchLevel: Float
): Double {
    return when {
        zoomLevel < zoomSwitchLevel -> context.resources.getDimension(R.dimen.grid_width_gone)
            .toDouble()

        zoomLevel >= zoomSwitchLevel && zoomLevel < 19f -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_1px)
            .toDouble()

        zoomLevel in 19f..20f -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_1_5px)
            .toDouble()

        else -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_2px).toDouble()
    }
}