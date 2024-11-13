package com.what3words.components.compose.maps.providers.googlemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.components.compose.maps.mapper.toGoogleCameraPosition
import com.what3words.components.compose.maps.mapper.toGoogleMapType
import com.what3words.components.compose.maps.mapper.toW3WMapStateCameraPosition
import com.what3words.components.compose.maps.providers.mapbox.W3WMapBoxDrawGridLines
import com.what3words.components.compose.maps.providers.mapbox.W3WMapBoxDrawMarkers
import com.what3words.components.compose.maps.providers.mapbox.W3WMapBoxDrawSelectedAddress
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.map.components.compose.R

@Composable
fun W3WGoogleMap(
    modifier: Modifier,
    layoutConfig: W3WMapDefaults.LayoutConfig,
    mapConfig: W3WMapDefaults.MapConfig,
    state: W3WMapState,
    content: (@Composable () -> Unit)? = null,
    onMapClicked: ((W3WCoordinates) -> Unit),
    onCameraUpdated: ((W3WMapState.CameraPosition) -> Unit)
) {
    val mapProperties = remember(state.mapType, state.isMyLocationEnabled, state.isDarkMode) {
        MapProperties(
            mapType = state.mapType.toGoogleMapType(),
            isMyLocationEnabled = state.isMyLocationEnabled,
            mapStyleOptions = if (state.isDarkMode) MapStyleOptions(mapConfig.darkModeCustomJsonStyle) else null
        )
    }

    val uiSettings = remember(state.isMyLocationButtonEnabled, state.isMapGestureEnable) {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            scrollGesturesEnabled = state.isMapGestureEnable,
            tiltGesturesEnabled = state.isMapGestureEnable,
            zoomGesturesEnabled = state.isMapGestureEnable,
            rotationGesturesEnabled = state.isMapGestureEnable,
            scrollGesturesEnabledDuringRotateOrZoom = state.isMapGestureEnable
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        state.cameraPosition?.let {
            position = it.toGoogleCameraPosition()
        }
    }

    LaunchedEffect(state.cameraPosition) {
        if (state.cameraPosition?.toGoogleCameraPosition() != cameraPositionState.position) {
            state.cameraPosition?.let {
                if (it.isAnimated) {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newCameraPosition(it.toGoogleCameraPosition())
                    )
                } else {
                    cameraPositionState.position = it.toGoogleCameraPosition()
                }
            }
        }
    }

    LaunchedEffect(cameraPositionState.position, cameraPositionState.isMoving) {
//        cameraPositionState.projection.visibleRegion.latLngBounds to Box
        onCameraUpdated.invoke(cameraPositionState.toW3WMapStateCameraPosition())
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentPadding = layoutConfig.contentPadding,
        uiSettings = uiSettings,
        properties = mapProperties,
        onMapClick = {
            onMapClicked.invoke(W3WCoordinates(it.latitude, it.longitude))
        },
    ) {
        W3WGoogleMapDrawer(state = state, mapConfig)
        content?.invoke()
    }
}

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawer(
    state: W3WMapState,
    mapConfig: W3WMapDefaults.MapConfig
) {

    //Draw the grid lines by zoom in state
    W3WMapBoxDrawGridLines(mapConfig.zoomSwitchLevel)

    //Draw the markers
    W3WMapBoxDrawMarkers(mapConfig.zoomSwitchLevel, state.listMakers)

    //Draw the selected address
    state.selectedAddress?.let { W3WMapBoxDrawSelectedAddress(mapConfig.zoomSwitchLevel, it) }
}

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawGridLines(zoomLevel: Float) {
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
fun W3WGoogleMapDrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMapState.Marker>>) {
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