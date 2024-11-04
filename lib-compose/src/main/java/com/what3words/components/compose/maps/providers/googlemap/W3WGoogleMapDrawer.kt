package com.what3words.components.compose.maps.providers.googlemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.what3words.components.compose.maps.models.W3WMapMarker
import com.what3words.components.compose.maps.models.W3WMapState
import com.what3words.core.types.domain.W3WAddress
import com.what3words.map.components.compose.R


@Composable
private fun DrawGridLines(zoomLevel: Float) {
    //TODO: Draw visible grid lines based on zoomLevel


    //The code below is an example of how to draw a polyline on the map.
    val polylineCoordinates = remember {
        listOf(
            LatLng(37.7749, -122.4194), // San Francisco
            LatLng(34.0522, -118.2437), // Los Angeles
            LatLng(40.7128, -74.0060)  // New York City
        )
    }

    Polyline(
        points = polylineCoordinates,
        color = Color.Blue,
        width = 10f
    )
}

@Composable
private fun DrawSelectedAddress(zoomLevel: Float, address: W3WAddress) {
    //TODO: Draw select for zoom in: grid, square

    //TODO: Draw select for zoom out: pin (maker)


    // The code below is an example of how to draw a marker on the map.
    val context = LocalContext.current

    val markerState = rememberMarkerState(position = LatLng(address.center!!.lat, address.center!!.lng))
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
private fun DrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMapMarker>>) {
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
fun W3WGoogleMapDrawer(state: W3WMapState) {
    val zoomLevel = state.zoom ?: W3WGoogleMapProvider.DEFAULT_ZOOM_SWITCH_LEVEL

    //Draw the grid lines by zoom in state
    DrawGridLines(zoomLevel)

    //Draw the markers
    DrawMarkers(zoomLevel, state.listMakers)

    //Draw the selected address
    state.selectedAddress?.let { DrawSelectedAddress(zoomLevel, it) }
}