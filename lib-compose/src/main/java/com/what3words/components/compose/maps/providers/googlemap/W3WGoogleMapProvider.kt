package com.what3words.components.compose.maps.providers.googlemap

import android.graphics.Point
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.components.compose.maps.mapper.toMapType
import com.what3words.components.compose.maps.providers.W3WMapProvider
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.map.components.compose.R
import kotlin.math.roundToInt

class W3WGoogleMapProvider : W3WMapProvider {
    override val defaultZoomLevel: Float
        get() = 19f

    @OptIn(MapsComposeExperimentalApi::class)
    @Composable
    override fun What3WordsMap(
        modifier: Modifier,
        contentPadding: PaddingValues,
        state: W3WMapState,
        onMapClicked: ((W3WCoordinates) -> Unit),
        onMapUpdate: (W3WRectangle?) -> Unit
    ) {
        val cameraPositionState = rememberCameraPositionState {
            state.zoomSwitchLevel?.let {
                position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), it)
            }
            calculateGridScaledBoundingBox(state.zoomSwitchLevel, state.gridScale)
        }

        val uiSettings by remember {
            mutableStateOf(
                MapUiSettings(
                    zoomControlsEnabled = false
                )
            )
        }

        val mapProperties by remember {
            mutableStateOf(
                MapProperties(
                    mapType = state.mapType.toMapType(),
                )
            )
        }

        //TODO:
        // cameraPositionState: animate camera
        // uiSetting: turn off some buttons control
        // mapProperties: switch mapType

        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            contentPadding = contentPadding,
            uiSettings = uiSettings,
            properties = mapProperties,
            onMapClick = {
                onMapClicked.invoke(W3WCoordinates(it.latitude, it.longitude))
            }
        ) {

            MapEffect { map ->
                map.setOnCameraIdleListener {
                    onMapUpdate.invoke(cameraPositionState.calculateGridScaledBoundingBox(zoomSwitchLevel = state.zoomSwitchLevel, scaleBy = state.gridScale))
                }
            }

            W3WMapDrawer(state = state)
        }
    }



    @Composable
    override fun W3WMapDrawer(state: W3WMapState) {
        val zoomLevel = state.zoomSwitchLevel ?: defaultZoomLevel

        //Draw the grid lines by zoom in state
        DrawGridLines(state.gridLines, state.gridColor ?: Color.Black)

        //Draw the markers
        DrawMarkers(zoomLevel, state.listMakers)

        //Draw the selected address
        state.selectedAddress?.let { DrawSelectedAddress(zoomLevel, it) }
    }

    @Composable
    override fun DrawGridLines(gridLines: W3WMapState.GridLines?, gridColor: Color) {
        //The code below is an example of how to draw a polyline on the map.
        var polylineCoordinates = remember {
            emptyList<LatLng>()
        }

        LaunchedEffect(gridLines) {
            polylineCoordinates = gridLines?.let { gridLines ->
                gridLines.verticalLines.map { LatLng(it.lat, it.lng) } +
                        gridLines.horizontalLines.map { LatLng(it.lat, it.lng) }
            } ?: emptyList()
        }

        Polyline(
            points = polylineCoordinates,
            color = gridColor,
            width = 1f
        )
    }

    @Composable
    override fun DrawSelectedAddress(zoomLevel: Float, address: W3WAddress) {
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
    override fun DrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMapState.Marker>>) {
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
}

fun CameraPositionState.calculateGridScaledBoundingBox(
    zoomSwitchLevel: Float?,
    scaleBy: Float,
): W3WRectangle? {
    return if (zoomSwitchLevel == null || this.position.zoom < zoomSwitchLevel) {
        null
    } else {
        this.projection?.let {
            try {
                val center = it.visibleRegion.latLngBounds.center
                val centerPoint: Point = it.toScreenLocation(center)
                val screenPositionNortheast: Point =
                    it.toScreenLocation(it.visibleRegion.latLngBounds.northeast)
                screenPositionNortheast.x =
                    ((scaleBy * (screenPositionNortheast.x - centerPoint.x) + centerPoint.x).roundToInt())
                screenPositionNortheast.y =
                    ((scaleBy * (screenPositionNortheast.y - centerPoint.y) + centerPoint.y).roundToInt())
                val scaledNortheast = it.fromScreenLocation(screenPositionNortheast)
                val screenPositionSouthwest: Point =
                    it.toScreenLocation(it.visibleRegion.latLngBounds.southwest)
                screenPositionSouthwest.x =
                    ((scaleBy * (screenPositionSouthwest.x - centerPoint.x) + centerPoint.x).roundToInt())
                screenPositionSouthwest.y =
                    ((scaleBy * (screenPositionSouthwest.y - centerPoint.y) + centerPoint.y).roundToInt())
                val scaledSouthwest = it.fromScreenLocation(screenPositionSouthwest)
                val scaledBounds = LatLngBounds(scaledSouthwest, scaledNortheast)

                return W3WRectangle(
                    W3WCoordinates(
                        scaledBounds.southwest.latitude,
                        scaledBounds.southwest.longitude
                    ),
                    W3WCoordinates(
                        scaledBounds.northeast.latitude,
                        scaledBounds.northeast.longitude
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return W3WRectangle(
                    W3WCoordinates(
                        it.visibleRegion.latLngBounds.southwest.latitude,
                        it.visibleRegion.latLngBounds.southwest.longitude
                    ),
                    W3WCoordinates(
                        it.visibleRegion.latLngBounds.northeast.latitude,
                        it.visibleRegion.latLngBounds.northeast.longitude
                    )
                )
            }
        }
    }
}