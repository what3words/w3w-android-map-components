package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.mapbox.geojson.Point
import androidx.compose.ui.platform.LocalContext
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WListMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates


@Composable
@MapboxMapComposable
fun W3WMapBoxDrawer(state: W3WMapState, mapConfig: W3WMapDefaults.MapConfig) {

    if (mapConfig.gridLineConfig.isGridEnabled) {
        state.cameraState?.let {
            if (it.getZoomLevel() >= mapConfig.gridLineConfig.zoomSwitchLevel) {
                W3WMapBoxDrawGridLines(
                    verticalLines = state.gridLines.verticalLines,
                    horizontalLines = state.gridLines.horizontalLines,
                    gridColor = mapConfig.gridLineConfig.gridColor,
                    gridLineWidth = mapConfig.gridLineConfig.gridLineWidth
                )
            }
        }
    }

    //Draw the markers
    W3WMapBoxDrawMarkers(mapConfig.gridLineConfig.zoomSwitchLevel, state.listMakers)

    //Draw the selected address
    state.selectedAddress?.let {
        W3WMapBoxDrawSelectedAddress(
            mapConfig.gridLineConfig.zoomSwitchLevel,
            it
        )
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
fun W3WMapBoxDrawSelectedAddress(zoomLevel: Float, selectedMarker: W3WMarker) {
    //TODO: Draw select for zoom in: grid, square

    //TODO: Draw select for zoom out: pin (maker)
    val context = LocalContext.current

    selectedMarker.address.center?.let { coordinate ->
//        val marker = remember {
//            getMarkerBitmap(context, selectedMarker.color)?.let { bitMap ->
//                IconImage(
//                    bitMap
//                )
//            }
//        }
//        PointAnnotation(point = Point.fromLngLat(coordinate.lng, coordinate.lat)) {
//            iconImage = marker
//        }
    }

}

@Composable
@MapboxMapComposable
fun W3WMapBoxDrawMarkers(zoomLevel: Float, listMakers: Map<String, W3WListMarker>) {
    //TODO: Draw select for zoom in: filled square

    //TODO: Draw select for zoom out: circle
}