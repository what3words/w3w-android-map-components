package com.what3words.components.compose.maps.providers.googlemap

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.W3WMapDefaults.MAKER_COLOR_DEFAULT
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
    val markerStatus = remember(state.savedListMakers, state.selectedAddress) {
        state.selectedAddress?.let { isMarkerInSavedList(state.savedListMakers, it) }
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
                selectedMarker = state.selectedAddress,
                isMarkerInSavedList = markerStatus == MarkerStatus.InMultipleList,
            )
        }


        //Draw the markers
        W3WGoogleMapDrawMarkers(
            zoomLevel = cameraState.getZoomLevel(),
            zoomSwitchLevel = mapConfig.gridLineConfig.zoomSwitchLevel,
            selectedMarker = if(markerStatus != MarkerStatus.NotSaved) state.selectedAddress else null,
            savedListMakers = state.savedListMakers,
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
    selectedMarker: W3WMarker,
    isMarkerInSavedList: Boolean
) {
    if (zoomLevel < zoomSwitchLevel) {
        DrawZoomOutSelectedMarker(selectedMarker,isMarkerInSavedList)
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
private fun DrawZoomOutSelectedMarker(
    selectedMarker: W3WMarker,
    isMarkerInSavedList: Boolean
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
                if (isMarkerInSavedList) MUlTI_MAKERS_COLOR_DEFAULT else selectedMarker.color?: MAKER_COLOR_DEFAULT
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
private fun DrawZoomInSelectedMarker(
    selectedMarker: W3WMarker,
    zoomLevel: Float,
    zoomSwitchLevel: Float
) {
    val context = LocalContext.current
    selectedMarker.square?.let { square ->
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
    selectedMarker: W3WMarker? = null,
    savedListMakers: ImmutableMap<String, ImmutableList<W3WMarker>>,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    if (zoomLevel < zoomSwitchLevel) {
        DrawZoomOutMarkers(
            selectedMarker = selectedMarker,
            savedListMakers = savedListMakers,
            onMarkerClicked = onMarkerClicked
        )
    } else {
        DrawZoomInMarkers(
            savedListMakers = savedListMakers,
            onMarkerClicked = onMarkerClicked
        )
    }
}

@Composable
fun DrawZoomInMarkers(
    savedListMakers: ImmutableMap<String, ImmutableList<W3WMarker>>,
    onMarkerClicked: ((W3WMarker) -> Unit)? = null,
) {
    //TODO: GroundOverlay for list markers bitmap square
}

@Composable
fun DrawZoomOutMarkers(
    selectedMarker: W3WMarker? = null,
    savedListMakers: ImmutableMap<String, ImmutableList<W3WMarker>>,
    onMarkerClicked: (W3WMarker) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val icons = rememberMarkerIcons(savedListMakers, context, density)

    val currentOnMarkerClicked by rememberUpdatedState(onMarkerClicked)

    savedListMakers.forEach { (_, markerList) ->
        markerList.forEach { marker ->
            key(marker.words) {
                val icon = icons[marker.words]
                Marker(
                    state = rememberMarkerState(position = marker.latLng.toGoogleLatLng()),
                    icon = icon,
                    visible = selectedMarker != marker,
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
}

@Composable
fun rememberMarkerIcons(
    savedListMakers: ImmutableMap<String, ImmutableList<W3WMarker>>,
    context: Context,
    density: Float
): Map<String, BitmapDescriptor?> {
    // Use rememberUpdatedState to hold the latest value of listMakers without triggering recomposition
    val latestListMakers = rememberUpdatedState(savedListMakers)

    // Remember the icon map so that it persists across recompositions
    val icons = remember { mutableStateMapOf<String, BitmapDescriptor?>() }

    // Use LaunchedEffect with listMakers as a key to load icons only when listMakers change
    LaunchedEffect(key1 = latestListMakers.value.size) {
        // Iterate over the listMakers and generate icons if not already cached
        savedListMakers.forEach { (listId, markerList) ->
            markerList.forEach { marker ->
                // Check if the icon for this marker already exists
                if (icons[marker.words] == null) {
                    // Determine the marker's color based on conditions
                    val color = if (isExistInOtherList(listId, marker, latestListMakers.value)) {
                        MUlTI_MAKERS_COLOR_DEFAULT
                    } else {
                        marker.color ?: MAKER_COLOR_DEFAULT
                    }

                    // Generate the icon and store it in the icons map
                    val iconBitmap = getPinBitmap(context, density = density, colorMarker = color)
                    icons[marker.words] = iconBitmap.let {
                        BitmapDescriptorFactory.fromBitmap(it)
                    }
                }
            }
        }
    }

    return icons
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