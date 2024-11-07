package com.what3words.components.compose.maps.providers.mapbox

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.layers.generated.LineLayer
import com.mapbox.maps.extension.compose.style.layers.generated.LineLayerState
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.toCameraOptions
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.components.compose.maps.providers.W3WMapProvider
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

class W3WMapBoxProvider : W3WMapProvider {
    override val defaultZoomLevel: Float
        get() = 19f

    @Composable
    override fun What3WordsMap(
        modifier: Modifier,
        contentPadding: PaddingValues,
        state: W3WMapState,
        onMapClicked: ((W3WCoordinates) -> Unit),
        onMapUpdate: (W3WRectangle?) -> Unit,
        onMapMove: (W3WRectangle?) -> Unit
    ) {
        val mapViewportState = rememberMapViewportState {
            state.zoomSwitchLevel?.let {
                setCameraOptions {
                    center(Point.fromLngLat(0.0, 0.0))
                    zoom(it.toDouble())
                    pitch(0.0)
                    bearing(0.0)
                }
            }
        }

        //TODO:
        // cameraPositionState: animate camera
        // uiSetting: turn off some buttons control
        // mapProperties: switch mapType

        MapboxMap(
            modifier = modifier,
            mapViewportState = mapViewportState,
            onMapClickListener = {
                onMapClicked.invoke(W3WCoordinates(it.latitude(), it.longitude()))
                false
            }
        ) {
            W3WMapDrawer(state)

            MapEffect { mapView ->
                mapView.mapboxMap.subscribeMapIdle {
                    Log.d("MapIdle", "MapIdle")
                    onMapUpdate.invoke(
                        mapView.mapboxMap.calculateGridScaledBoundingBox(
                            zoomSwitchLevel = state.zoomSwitchLevel,
                            scaleBy = state.gridScale
                        )
                    )
                }
                mapView.mapboxMap.subscribeCameraChanged {
                    Log.d("CameraChanged", "CameraChanged")
                    onMapMove.invoke(
                        mapView.mapboxMap.calculateGridScaledBoundingBox(
                            zoomSwitchLevel = state.zoomSwitchLevel,
                            scaleBy = state.gridScale
                        )
                    )
                }
            }
        }
    }

    @Composable
    override fun DrawSelectedAddress(zoomLevel: Float, address: W3WAddress) {
        //TODO: Draw select for zoom in: grid, square

        //TODO: Draw select for zoom out: pin (maker)
    }

    @Composable
    override fun DrawMarkers(zoomLevel: Float, listMakers: Map<String, List<W3WMapState.Marker>>) {
        //TODO: Draw select for zoom in: filled square

        //TODO: Draw select for zoom out: circle
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
        val geoJsonSource = rememberGeoJsonSourceState()
        gridLines?.geoJSON?.let {
            geoJsonSource.data = GeoJSONData(it)
        } ?: run {
            geoJsonSource.data = GeoJSONData("")
        }
        LineLayer(
            sourceState = geoJsonSource,
            lineLayerState = remember {
                LineLayerState().apply {
                    lineColor = ColorValue(gridColor)
                }
            }
        )
    }
}

fun MapboxMap.calculateGridScaledBoundingBox(
    zoomSwitchLevel: Float?,
    scaleBy: Float
): W3WRectangle? {
    return if (zoomSwitchLevel == null || this.cameraState.zoom < zoomSwitchLevel) {
        null
    } else {
        val bounds = this
            .coordinateBoundsForCamera(this.cameraState.toCameraOptions())
        val center = bounds.center()
        val finalNELat =
            ((scaleBy * (bounds.northeast.latitude() - center.latitude()) + center.latitude()))
        val finalNELng =
            ((scaleBy * (bounds.northeast.longitude() - center.longitude()) + center.longitude()))
        val finalSWLat =
            ((scaleBy * (bounds.southwest.latitude() - center.latitude()) + center.latitude()))
        val finalSWLng =
            ((scaleBy * (bounds.southwest.longitude() - center.longitude()) + center.longitude()))

        return W3WRectangle(
            W3WCoordinates(
                finalSWLat,
                finalSWLng
            ),
            W3WCoordinates(finalNELat, finalNELng)
        )
    }
}