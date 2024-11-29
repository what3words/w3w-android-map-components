package com.what3words.components.compose.maps.state.camera

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapConstants.MAX_ZOOM
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

class W3WMapboxCameraState(override val cameraState: MapViewportState) :
    W3WCameraState<MapViewportState> {

    companion object{
        const val MY_LOCATION_ZOOM = MAX_ZOOM
    }

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

    override fun moveToPosition(
        coordinates: W3WCoordinates,
        zoom: Float?,
        bearing: Float?,
        tilt: Float?,
        animate: Boolean
    ) {
        val cameraOptions = CameraOptions.Builder()
            .pitch(tilt?.toDouble()?:cameraState.cameraState?.pitch)
            .bearing(bearing?.toDouble()?:cameraState.cameraState?.bearing)
            .center(Point.fromLngLat(coordinates.lng, coordinates.lat))
            .zoom(zoom?.toDouble()?:cameraState.cameraState?.zoom)
            .build()

        updateCameraPosition(cameraOptions, animate)
    }

    override fun getZoomLevel(): Float {
        return cameraState.cameraState?.zoom?.toFloat() ?: run { 0f }
    }

    override fun moveToMyLocation(coordinates: W3WCoordinates) {
        val cameraOptions = CameraOptions.Builder()
            .pitch(cameraState.cameraState?.pitch)
            .bearing(cameraState.cameraState?.bearing)
            .center(Point.fromLngLat(coordinates.lng, coordinates.lat))
            .zoom(MY_LOCATION_ZOOM)
            .build()

        updateCameraPosition(cameraOptions, true)
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