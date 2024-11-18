package com.what3words.components.compose.maps.state

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class W3WMapboxCameraState(override val cameraState: MapViewportState) :
    W3WCameraState<MapViewportState> {

    override var gridBound: W3WRectangle?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun orientCamera() {
        TODO("Not yet implemented")
    }

    override fun moveToPosition(coordinates: W3WCoordinates, animated: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val cameraOptions = CameraOptions.Builder()
                .pitch(cameraState.cameraState?.pitch)
                .bearing(cameraState.cameraState?.bearing)
                .center(Point.fromLngLat(coordinates.lng, coordinates.lat))
                .zoom(cameraState.cameraState?.zoom)
                .build()

            if (animated) {
                cameraState.flyTo(
                    cameraOptions
                )
            } else {
                cameraState.setCameraOptions(
                    cameraOptions
                )
            }
        }
    }

    override fun getZoomLevel(): Float {
        TODO("Not yet implemented")
    }
}