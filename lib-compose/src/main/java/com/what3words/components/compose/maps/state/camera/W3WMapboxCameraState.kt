package com.what3words.components.compose.maps.state.camera

import androidx.compose.runtime.Immutable
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

@Immutable
class W3WMapboxCameraState(override val cameraState: MapViewportState) :
    W3WCameraState<MapViewportState> {

    companion object {
        const val MY_LOCATION_ZOOM = 20.0
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
            .pitch(tilt?.toDouble() ?: cameraState.cameraState?.pitch)
            .bearing(bearing?.toDouble() ?: cameraState.cameraState?.bearing)
            .center(Point.fromLngLat(coordinates.lng, coordinates.lat))
            .zoom(zoom?.toDouble() ?: cameraState.cameraState?.zoom)
            .build()

        updateCameraPosition(cameraOptions, animate)
    }

    override fun moveToPosition(
        listCoordinates: List<W3WCoordinates>,
        zoom: Float?,
        bearing: Float?,
        tilt: Float?,
        animate: Boolean
    ) {
//        val points = listCoordinates.map { Point.fromLngLat(it.lng,it.lat) }
//        val southwest = points.minByOrNull { it.latitude() to it.longitude() }
//        val northeast = points.maxByOrNull { it.latitude() to it.longitude() }
//        val bounds = Bounds(southwest,northeast)
//
//
//
//        val cameraOptions = CameraOptions.Builder()
//            .center(bounds.)
//            .pitch(tilt?.toDouble() ?: cameraState.cameraState?.pitch)
//            .bearing(bearing?.toDouble() ?: cameraState.cameraState?.bearing)
//            .zoom(zoom?.toDouble() ?: cameraState.cameraState?.zoom)
//            .build()
//
//        updateCameraPosition(cameraOptions, animate)
    }

    override fun getZoomLevel(): Float {
        return cameraState.cameraState?.zoom?.toFloat() ?: run { 0f }
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