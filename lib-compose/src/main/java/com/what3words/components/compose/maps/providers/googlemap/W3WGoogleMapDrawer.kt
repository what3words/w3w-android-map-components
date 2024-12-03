package com.what3words.components.compose.maps.providers.googlemap

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapDefaults.MUlTI_MAKERS_COLOR_DEFAULT
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.state.MarkerStatus
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.isExistInOtherList
import com.what3words.components.compose.maps.state.isMarkerInSavedList
import com.what3words.components.compose.maps.utils.getMarkerBitmap
import com.what3words.components.compose.maps.utils.getPinBitmap
import com.what3words.map.components.compose.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

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
    mapConfig: W3WMapDefaults.MapConfig,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    // Check if selectedMarker exists in the saved list
    val markerStatus = remember(state.listMakers, state.selectedAddress) {
        state.selectedAddress?.let { isMarkerInSavedList(state.listMakers, it) }
    }

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
                selectedMarker = state.selectedAddress.copy(
                    color = if (markerStatus == MarkerStatus.InMultipleList) MUlTI_MAKERS_COLOR_DEFAULT else state.selectedAddress.color
                ),
            )
        }


        //Draw the markers
        W3WGoogleMapDrawMarkers(
            zoomLevel = cameraState.getZoomLevel(),
            zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
            selectedMarkerID = if(markerStatus != MarkerStatus.NotSaved) state.selectedAddress?.id else null,
            listMarkers = state.listMakers,
            onMarkerClicked = onMarkerClicked
        )
    }
}


@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawGridLines(
    verticalLines: List<W3WLatLng>,
    horizontalLines: List<W3WLatLng>,
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
    selectedMarker: W3WMarker
) {
    if (zoomLevel < zoomSwitchLevel) {
        DrawZoomOutSelectedAddress(selectedMarker)
    } else {
        DrawZoomInSelectedAddress(
            zoomLevel = zoomLevel,
            zoomSwitchLevel = zoomSwitchLevel,
            selectedMarker = selectedMarker
        )
    }
}

@Composable
@GoogleMapComposable
private fun DrawZoomOutSelectedAddress(
    selectedMarker: W3WMarker,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val markerState =
        rememberMarkerState(
            position = selectedMarker.latLng.toGoogleLatLng()
        )

    LaunchedEffect(selectedMarker.latLng) {
        markerState.position = selectedMarker.latLng.toGoogleLatLng()
    }

    val icon = remember(selectedMarker.color) {
        BitmapDescriptorFactory.fromBitmap(
            getMarkerBitmap(
                context,
                density,
                selectedMarker.color
            )
        )
    }

    Marker(
        state = markerState,
        icon = icon
    )
}

@Composable
@GoogleMapComposable
private fun DrawZoomInSelectedAddress(
    selectedMarker: W3WMarker,
    zoomLevel: Float,
    zoomSwitchLevel: Float
) {
    val context = LocalContext.current
    selectedMarker.square.let { square ->
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
            width = getGridSelectedBorderSizeBasedOnZoomLevel(
                context,
                zoomLevel,
                zoomSwitchLevel,
            ),
            clickable = false,
            zIndex = 5f
        )
    }
}


@Composable
@GoogleMapComposable
fun W3WGoogleMapDrawMarkers(
    zoomLevel: Float,
    zoomSwitchLevel: Float,
    selectedMarkerID: Long? = null,
    listMarkers: ImmutableMap<String, ImmutableList<W3WMarker>>,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    if (zoomLevel < zoomSwitchLevel) {
        DrawZoomOutMarkers(
            selectedMarkerID = selectedMarkerID,
            listMarkers = listMarkers,
            onMarkerClicked = onMarkerClicked
        )
    } else {
        DrawZoomInMarkers(
            listMarkers = listMarkers,
            onMarkerClicked = onMarkerClicked
        )
    }
}

@Composable
private fun DrawZoomInMarkers(
    listMarkers: ImmutableMap<String, ImmutableList<W3WMarker>>,
    onMarkerClicked: ((W3WMarker) -> Unit)? = null,
) {
    //TODO: GroundOverlay for list markers bitmap square
}

@Composable
private fun DrawZoomOutMarkers(
    selectedMarkerID: Long? = null,
    listMarkers: ImmutableMap<String, ImmutableList<W3WMarker>>,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val currentOnMarkerClicked by rememberUpdatedState(onMarkerClicked)

    listMarkers.forEach { (listId, markerList) ->
        markerList.forEach { marker ->
            val icon = remember(marker, listId) {
                val color = if (isExistInOtherList(listId, marker, listMarkers)) {
                    MUlTI_MAKERS_COLOR_DEFAULT
                } else {
                    marker.color
                }

                BitmapDescriptorFactory.fromBitmap(getPinBitmap(context, density = density, colorMarker = color))
            }

            Marker(
                state = rememberMarkerState(position = marker.latLng.toGoogleLatLng()),
                icon = icon,
                visible = selectedMarkerID != marker.id,
                onClick = {
                    currentOnMarkerClicked(marker)
                    true
                },
                title = marker.title,
                snippet = marker.snippet
            )

        }
    }
}

private fun getGridSelectedBorderSizeBasedOnZoomLevel(
    context: Context,
    zoomLevel: Float,
    zoomSwitchLevel: Float
): Float {
    return when {
        zoomLevel < zoomSwitchLevel -> context.resources.getDimension(R.dimen.grid_width_gone)
        zoomLevel >= zoomSwitchLevel && zoomLevel < 19f -> context.resources.getDimension(R.dimen.grid_selected_width_google_map_1dp)
        zoomLevel in 19f..20f -> context.resources.getDimension(R.dimen.grid_selected_width_google_map_1_5dp)
        else -> context.resources.getDimension(R.dimen.grid_selected_width_google_map_2dp)
    }
}