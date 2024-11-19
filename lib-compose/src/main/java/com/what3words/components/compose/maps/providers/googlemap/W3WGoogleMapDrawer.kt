package com.what3words.components.compose.maps.providers.googlemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.map.components.compose.R

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawer(
    state: W3WMapState,
    mapConfig: W3WMapDefaults.MapConfig
) {

    if (mapConfig.gridLineConfig.isGridEnabled) {
        state.cameraState?.let {
            W3WGoogleMapDrawGridLines(
                verticalLines = state.gridLines.verticalLines,
                horizontalLines = state.gridLines.horizontalLines,
                zoomLevel = it.getZoomLevel(),
                zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel
            )
        }
    }

    //Draw the markers
    W3WGoogleMapDrawMarkers(mapConfig.gridLineConfig.zoomSwitchLevel, state.listMakers)

    //Draw the selected address
    state.selectedAddress?.let { W3WGoogleMapDrawSelectedAddress(mapConfig.gridLineConfig.zoomSwitchLevel, it) }
}

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawGridLines(
    verticalLines: List<W3WCoordinates>,
    horizontalLines: List<W3WCoordinates>,
    zoomSwitchLevel: Float,
    zoomLevel: Float
) {
    W3WGoogleMapGrid(
        verticalLines = verticalLines,
        horizontalLines = horizontalLines,
        zoomLevel = zoomLevel,
        zoomSwitchLevel = zoomSwitchLevel
    )
}

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawSelectedAddress(zoomLevel: Float, address: W3WAddress) {
    //TODO: Draw select for zoom in: grid, square

    //TODO: Draw select for zoom out: pin (maker)


    // The code below is an example of how to draw a marker on the map.
    val context = LocalContext.current

    val markerState =
        rememberMarkerState(position = LatLng(address.center!!.lat, address.center!!.lng))
    LaunchedEffect(address) {
        markerState.position = LatLng(address.center!!.lat, address.center!!.lng)
    }

    val icon = remember(context) {
        ContextCompat.getDrawable(context, R.drawable.ic_marker)?.let { drawable ->
            BitmapDescriptorFactory.fromBitmap(
                drawable.toBitmap(
                    width = drawable.intrinsicWidth,
                    height = drawable.intrinsicHeight
                )
            )
        }
    }

    Marker(
        state = markerState,
        icon = icon
    )
}

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMarker>>) {
    //TODO: Draw select for zoom in: filled square

    //TODO: Draw select for zoom out: circle

    // This code below is an example of how to draw a marker on the map.
    listMakers.forEach { (key, markers) ->
        markers.forEach { marker ->
            Marker(
                state = rememberMarkerState(
                    position = LatLng(
                        marker.address.center!!.lat,
                        marker.address.center!!.lng
                    )
                ),
                title = marker.title,
                snippet = marker.snippet,
            )
        }
    }
}

@Composable
@GoogleMapComposable
private fun W3WGoogleMapGrid(
    verticalLines: List<W3WCoordinates>,
    horizontalLines: List<W3WCoordinates>,
    zoomSwitchLevel: Float,
    zoomLevel: Float
) {

    val horizontalPolylines = horizontalLines.map { coordinate ->
        LatLng(coordinate.lat, coordinate.lng)
    }

    val verticalPolylines = verticalLines.map { coordinate ->
        LatLng(coordinate.lat, coordinate.lng)
    }

    Polyline(
        points = horizontalPolylines,
        color = Color.LightGray,
        width = getGridBorderSizeBasedOnZoomLevel(zoomLevel, zoomSwitchLevel).value,
        clickable = false
    )

    Polyline(
        points = verticalPolylines,
        color = Color.LightGray,
        width = getGridBorderSizeBasedOnZoomLevel(zoomLevel, zoomSwitchLevel).value,
        clickable = false
    )
}

private fun getGridBorderSizeBasedOnZoomLevel(zoomLevel: Float, zoomSwitchLevel: Float): Dp {
    return when {
        zoomLevel < zoomSwitchLevel -> 0.dp
        else -> 1.dp
    }
}