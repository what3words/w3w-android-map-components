package com.what3words.components.compose.maps.state.camera

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

/**
 * Implementation of [W3WCameraState] for Mapbox Maps in Jetpack Compose.
 *
 * This class manages the camera state for Mapbox Maps, including position, zoom, bearing, and tilt.
 * It provides methods to control camera movement, orientation, and to retrieve current camera properties.
 *
 * @property cameraState The underlying Mapbox [MapViewportState] that this class wraps
 * @property gridBound The What3Words grid boundary rectangle shown on the map
 * @property visibleBound The currently visible map area as a What3Words rectangle
 * @property isCameraMoving Whether the camera is currently in motion
 * @property cameraForCoordinates Temporary storage for coordinates when using cameraForCoordinates functionality
 *
 * @param initialCameraState The initial camera state to use
 */
@Immutable
class W3WMapboxCameraState(initialCameraState: MapViewportState) :
    W3WCameraState<MapViewportState> {

    companion object {
        const val MY_LOCATION_ZOOM = 20.0
    }

    override var cameraState: MapViewportState by mutableStateOf(initialCameraState)

    override var gridBound: W3WRectangle? by mutableStateOf(null)

    override var visibleBound: W3WRectangle? by mutableStateOf(null)

    override var isCameraMoving: Boolean by mutableStateOf(true)

    //TODO: This is work around for the function cameraForCoordinates not support in compose
    var cameraForCoordinates: MutableList<Point>? = mutableListOf()

    override suspend fun orientCamera() {
        updateCameraPosition(
            CameraOptions.Builder()
                .pitch(cameraState.cameraState?.pitch)
                .bearing(0.0)
                .center(cameraState.cameraState?.center)
                .zoom(cameraState.cameraState?.zoom)
                .build(), true
        )
    }

    override suspend fun moveToPosition(
        latLng: W3WCoordinates,
        zoom: Float?,
        bearing: Float?,
        tilt: Float?,
        animate: Boolean
    ) {
        val cameraOptions = CameraOptions.Builder()
            .pitch(tilt?.toDouble() ?: cameraState.cameraState?.pitch)
            .bearing(bearing?.toDouble() ?: cameraState.cameraState?.bearing)
            .center(Point.fromLngLat(latLng.lng, latLng.lat))
            .zoom(zoom?.toDouble() ?: cameraState.cameraState?.zoom)
            .build()

        updateCameraPosition(cameraOptions, animate)
    }

    override suspend fun moveToPosition(
        listLatLng: List<W3WCoordinates>,
    ) {
        cameraForCoordinates = listLatLng.map { Point.fromLngLat(it.lng, it.lat) }.toMutableList()
    }

    override fun getZoomLevel(): Float {
        return cameraState.cameraState?.zoom?.toFloat() ?: run { 0f }
    }

    override fun getBearing(): Float {
        return cameraState.cameraState?.bearing?.toFloat() ?: run { 0f }
    }

    override fun getTilt(): Float {
        return cameraState.cameraState?.pitch?.toFloat() ?: run { 0f }
    }

    override fun getCenter(): W3WCoordinates? {
        return cameraState.cameraState?.center?.let {
            W3WCoordinates(
                lat = it.latitude(),
                lng = it.longitude()
            )
        }
    }

    /**
     * Updates the camera position with new settings.
     *
     * This private function handles the camera positioning for both animated and non-animated
     * camera movements. It uses either flyTo for animated transitions or setCameraOptions for
     * instant updates.
     *
     * @param cameraOptions The new [CameraOptions] to apply to the map
     * @param animate Whether to animate the transition to the new camera position (true) or
     *                update instantly (false)
     */
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