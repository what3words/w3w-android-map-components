package com.what3words.components.compose.maps.providers.googlemap

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.state.W3WListMarker
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.isExistInOtherList
import com.what3words.components.compose.maps.utils.getGridSelectedBorderSizeBasedOnZoomLevel
import com.what3words.components.compose.maps.utils.getMarkerBitmap
import com.what3words.components.compose.maps.utils.getPin
import com.what3words.core.types.geometry.W3WCoordinates

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
    mapConfig: W3WMapDefaults.MapConfig
) {
    state.cameraState?.let { cameraState ->
        if (mapConfig.gridLineConfig.isGridEnabled) {
            // Draw grid lines
            W3WGoogleMapDrawGridLines(
                verticalLines = state.gridLines.verticalLines,
                horizontalLines = state.gridLines.horizontalLines,
                zoomLevel = cameraState.getZoomLevel(),
                gridLinesConfig = mapConfig.gridLineConfig
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

    //Draw the markers
//    W3WGoogleMapDrawMarkers(
//        cameraState = state.cameraState?.cameraState as CameraPositionState,
//        state.listMakers
//    )

}


@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawGridLines(
    verticalLines: List<W3WCoordinates>,
    horizontalLines: List<W3WCoordinates>,
    zoomLevel: Float,
    gridLinesConfig: W3WMapDefaults.GridLinesConfig,
) {
    if (zoomLevel < gridLinesConfig.zoomSwitchLevel) {
        return
    }

    val horizontalPolylines = horizontalLines.map { coordinate ->
        LatLng(coordinate.lat, coordinate.lng)
    }

    val verticalPolylines = verticalLines.map { coordinate ->
        LatLng(coordinate.lat, coordinate.lng)
    }

    Polyline(
        points = horizontalPolylines,
        color = gridLinesConfig.gridColor,
        width = gridLinesConfig.gridLineWidth.value,
        clickable = false
    )

    Polyline(
        points = verticalPolylines,
        color = gridLinesConfig.gridColor,
        width = gridLinesConfig.gridLineWidth.value,
        clickable = false
    )
}

@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawSelectedAddress(
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedMarker: W3WMarker,
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
@GoogleMapComposable
private fun DrawZoomOutSelectedMarker(selectedMarker: W3WMarker) {
    val context = LocalContext.current
    val markerState =
        rememberMarkerState(
            position = LatLng(
                selectedMarker.address.center!!.lat,
                selectedMarker.address.center!!.lng
            )
        )
    LaunchedEffect(selectedMarker.address) {
        markerState.position =
            LatLng(selectedMarker.address.center!!.lat, selectedMarker.address.center!!.lng)
    }

    val density = LocalDensity.current.density
    var icon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(key1 = Unit) {
        icon = selectedMarker.color?.let {
            BitmapDescriptorFactory.fromBitmap(getMarkerBitmap(context, density, it))
        }
    }

    Marker(
        state = markerState,
        icon = icon
    )
}

@Composable
@GoogleMapComposable
private fun DrawZoomInSelectedMarker(
    selectedMarker: W3WMarker,
    zoomLevel: Float,
    zoomSwitchLevel: Float
) {
    val context = LocalContext.current
    selectedMarker.address.square?.let { square ->
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
            width = zoomLevel.getGridSelectedBorderSizeBasedOnZoomLevel(context, zoomSwitchLevel),
            clickable = false,
            zIndex = 5f
        )
    }
}


@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawMarkers(
    cameraState: CameraPositionState,
    listMakers: Map<String, W3WListMarker>,
    onMarkerClicked: ((W3WMarker) -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val icons = rememberMarkerIcons(listMakers, context, density)
    val zoomLevel = remember {
        mutableFloatStateOf(cameraState.position.zoom)
    }

    LaunchedEffect(key1 = cameraState.position.zoom) {
        zoomLevel.floatValue = cameraState.position.zoom
    }

    listMakers.forEach { (_, listMarker) ->
        listMarker.markers.forEach { marker ->
            val icon = icons[marker.address.words]
            val position = LatLng(marker.address.center!!.lat, marker.address.center!!.lng)

            Marker(
                state = rememberMarkerState(position = position),
                icon = icon,
                onClick = {
                    onMarkerClicked?.invoke(marker)
                    true
                },
                title = marker.title,
                snippet = marker.snippet
            )
        }
    }
}


@Composable
fun rememberMarkerIcons(
    listMakers: Map<String, W3WListMarker>,
    context: Context,
    density: Float
): Map<String, BitmapDescriptor?> {
    val icons = remember { mutableStateMapOf<String, BitmapDescriptor?>() }

    // LaunchedEffect with a stable key (listMakers)
    LaunchedEffect(key1 = listMakers) {
        listMakers.forEach { (listId, listMarker) ->
            listMarker.markers.forEach { marker ->
                val color = if (isExistInOtherList(listId, marker, listMakers)) {
                    W3WMarkerColor(Color.Blue, Color.White)
                } else {
                    listMarker.listColor ?: marker.color ?: W3WMarkerColor(
                        Color.Red,
                        Color.White
                    )
                }

                // Load the icon for the marker if not already loaded
                if (icons[marker.address.words] == null) {
                    val iconBitmap = getPin(context, color = color, density = density)
                    icons[marker.address.words] = iconBitmap?.let {
                        BitmapDescriptorFactory.fromBitmap(it)
                    }
                }
            }
        }
    }

    return icons
}