package com.what3words.components.compose.maps.providers.mapbox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.PointListValue
import com.mapbox.maps.extension.compose.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.compose.style.sources.generated.rememberImageSourceState
import com.mapbox.maps.extension.style.sources.generated.ImageSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.updateImage
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WListMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.utils.getFillGridMarkerBitmap
import com.what3words.components.compose.maps.utils.getMarkerBitmap
import com.what3words.components.compose.maps.utils.getPinBitmap
import com.what3words.map.components.compose.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap


@Composable
@MapboxMapComposable
fun W3WMapBoxDrawer(state: W3WMapState, mapConfig: W3WMapDefaults.MapConfig) {

    state.cameraState?.let { cameraState ->
        if (mapConfig.gridLineConfig.isGridEnabled && cameraState.getZoomLevel() >= mapConfig.gridLineConfig.zoomSwitchLevel) {
            W3WMapBoxDrawGridLines(
                verticalLines = state.gridLines.verticalLines,
                horizontalLines = state.gridLines.horizontalLines,
                gridColor = mapConfig.gridLineConfig.gridColor,
                gridLineWidth = mapConfig.gridLineConfig.gridLineWidth
            )
        }

        if (state.selectedAddress != null) {
            //Draw the selected address
            W3WMapBoxDrawSelectedAddress(
                zoomLevel = cameraState.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
                selectedMarker = state.selectedAddress
            )
        }

        if (state.listMakers.isNotEmpty()) {
            W3WMapBoxDrawSavedAddress(
                cameraState.getZoomLevel(),
                mapConfig.gridLineConfig.zoomSwitchLevel,
                state.listMakers
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
    PolylineAnnotation(
        points = verticalLines.map { Point.fromLngLat(it.lng, it.lat) }
    ) {
        lineColor = gridColor
        lineWidth = gridLineWidth.value.toDouble()
    }

    PolylineAnnotation(
        points = horizontalLines.map { Point.fromLngLat(it.lng, it.lat) }
    ) {
        lineColor = gridColor
        lineWidth = gridLineWidth.value.toDouble()
    }
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawSelectedAddress(
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedMarker: W3WMarker
) {
    if (zoomLevel < zoomSwitchLevel) {
        DrawZoomOutSelectedMarker(selectedMarker)
    } else {
        DrawZoomInSelectedMarker(
            zoomLevel = zoomLevel,
            zoomSwitchLevel = zoomSwitchLevel,
            selectedMarker = selectedMarker
        )
    }
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawSavedAddress(
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    listMakers: Map<String, W3WListMarker>
) {
    if (zoomLevel < zoomSwitchLevel) {
        DrawZoomOutSavedMarkers(listMakers)
    } else {
        DrawZoomInSavedMarkers(listMakers)
    }
}

@Composable
@MapboxMapComposable
fun DrawZoomOutSavedMarkers(listMakers: Map<String, W3WListMarker>) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    listMakers.forEach {
        it.value.markers.forEach { marker ->
            val icon = rememberIconImage(
                key = marker.words,
                painter = BitmapPainter(
                    getPinBitmap(context, density, marker.color!!).asImageBitmap()
                )
            )

            PointAnnotation(
                point = Point.fromLngLat(
                    marker.latLng.lng,
                    marker.latLng.lat
                )
            ) {
                iconImage = icon
            }
        }
    }
}

@Composable
@MapboxMapComposable
fun DrawZoomInSavedMarkers(listMakers: Map<String, W3WListMarker>) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    listMakers.forEach { markers ->
        markers.value.markers.forEach { marker ->
            val id = String.format(
                ID_SAVED_ADDRESS_IMAGE_SOURCE,
                marker.words
            )

            val square = marker.square

            val bitmap = getFillGridMarkerBitmap(
                context,
                density,
                marker.color
            )

            MapEffect(Unit) {
                val imageSource: ImageSource = it.mapboxMap.getSourceAs(id)!!
                imageSource.updateImage(bitmap)
            }

            RasterLayer(
                sourceState = rememberImageSourceState(sourceId = id) {
                    coordinates = PointListValue(
                        Point.fromLngLat(square.southwest.lng, square.northeast.lat),
                        Point.fromLngLat(square.northeast.lng, square.northeast.lat),
                        Point.fromLngLat(square.northeast.lng, square.southwest.lat),
                        Point.fromLngLat(square.southwest.lng, square.southwest.lat),
                    )
                }
            )
        }
    }
}

@Composable
private fun rememberImageBitmap(bitmap: Bitmap): ImageBitmap {
    return remember(bitmap) { bitmap.asImageBitmap() }
}

@Composable
@MapboxMapComposable
private fun DrawZoomOutSelectedMarker(selectedMarker: W3WMarker) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val marker = rememberIconImage(
        key = selectedMarker.words,
        painter = BitmapPainter(
            getMarkerBitmap(context, density, selectedMarker.color!!).asImageBitmap()
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
private fun DrawZoomInSelectedMarker(
    selectedMarker: W3WMarker,
    zoomLevel: Float,
    zoomSwitchLevel: Float
) {
    val context = LocalContext.current
    selectedMarker.square?.let { square ->
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

private const val ID_SAVED_ADDRESS_IMAGE_SOURCE = "image_source-saved-address-%s"