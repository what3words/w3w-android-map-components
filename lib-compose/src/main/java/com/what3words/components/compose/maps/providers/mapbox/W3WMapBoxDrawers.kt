package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WListMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.utils.getGridSelectedBorderSizeBasedOnZoomLevel
import com.what3words.components.compose.maps.utils.getMarkerBitmap
import com.what3words.core.types.geometry.W3WCoordinates


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
    }
}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawGridLines(
    verticalLines: List<W3WCoordinates>,
    horizontalLines: List<W3WCoordinates>,
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
fun W3WMapBoxDrawMarkers(zoomLevel: Float, listMakers: Map<String, W3WListMarker>) {
    //TODO: Draw select for zoom in: filled square

    //TODO: Draw select for zoom out: circle
}

@Composable
@MapboxMapComposable
private fun DrawZoomOutSelectedMarker(selectedMarker: W3WMarker) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val marker = rememberIconImage(
        key = selectedMarker.address.words,
        painter = BitmapPainter(
            getMarkerBitmap(context, density, selectedMarker.color!!).asImageBitmap()
        )
    )

    PointAnnotation(
        point = Point.fromLngLat(
            selectedMarker.address.center!!.lng,
            selectedMarker.address.center!!.lat
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
    selectedMarker.address.square?.let { square ->
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
                zoomLevel.getGridSelectedBorderSizeBasedOnZoomLevel(context, zoomSwitchLevel)
                    .toDouble()
        }
    }
}