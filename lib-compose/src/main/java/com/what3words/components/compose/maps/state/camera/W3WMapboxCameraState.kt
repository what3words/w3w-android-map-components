package com.what3words.components.compose.maps.state.camera

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.components.compose.maps.mapper.toMapBoxCameraOptions
import com.what3words.components.compose.maps.models.W3WCameraPosition
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

class W3WMapboxCameraState(override val cameraState: MapViewportState) :
    W3WCameraState<MapViewportState> {

    override var gridBound: W3WRectangle? = null

    override fun orientCamera() {
        updateCameraPosition(
            CameraOptions.Builder()
                .pitch(cameraState.cameraState?.pitch)
                .bearing(0.0)
                .center(cameraState.cameraState?.center)
                .zoom(cameraState.cameraState?.zoom)
                .build(), true
        )
    }

    override fun moveToPosition(coordinates: W3WCoordinates, animate: Boolean) {
        val cameraOptions = CameraOptions.Builder()
            .pitch(cameraState.cameraState?.pitch)
            .bearing(cameraState.cameraState?.bearing)
            .center(Point.fromLngLat(coordinates.lng, coordinates.lat))
            .zoom(cameraState.cameraState?.zoom)
            .build()

        updateCameraPosition(cameraOptions, animate)
    }

    override fun getZoomLevel(): Float {
        return cameraState.cameraState?.zoom?.toFloat() ?: run { 0f }
    }

    override fun setCameraPosition(cameraPosition: W3WCameraPosition, animate: Boolean) {
        updateCameraPosition(cameraPosition.toMapBoxCameraOptions(),animate)
    }

    private fun updateCameraPosition(cameraOptions: CameraOptions, animate: Boolean) {
        if (animate) {
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