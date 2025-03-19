package com.what3words.components.compose.maps.state.camera

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext

/**
 * Implementation of [W3WCameraState] for Google Maps.
 *
 * This class handles camera operations for Google Maps components, managing positioning,
 * zoom levels, bounds, and camera animations.
 *
 * @property cameraState The underlying [CameraPositionState] for Google Maps
 * @property gridBound Current grid bounds represented as a [W3WRectangle]
 * @property visibleBound Current visible bounds on the map represented as a [W3WRectangle]
 * @property isCameraMoving Boolean indicating if the camera is currently in motion
 *
 * @param initialCameraState The initial camera state to use when creating this instance
 */
@Stable
class W3WGoogleCameraState(initialCameraState: CameraPositionState) :
    W3WCameraState<CameraPositionState> {

    companion object {
        const val MY_LOCATION_ZOOM = 20f
    }

    override var cameraState: CameraPositionState by mutableStateOf(initialCameraState)

    override var gridBound: W3WRectangle? by mutableStateOf(null)

    override var visibleBound: W3WRectangle? by mutableStateOf(null)

    override var isCameraMoving: Boolean by mutableStateOf(true)

    override suspend fun orientCamera() {
        updateCameraPosition(
            CameraPosition(
                cameraState.position.target,
                cameraState.position.zoom,
                0f,
                0f
            ), true
        )
    }

    override suspend fun moveToPosition(
        latLng: W3WCoordinates,
        zoom: Float?,
        bearing: Float?,
        tilt: Float?,
        animate: Boolean
    ) {
        updateCameraPosition(
            CameraPosition(
                LatLng(latLng.lat, latLng.lng),
                zoom ?: cameraState.position.zoom,
                bearing ?: cameraState.position.tilt,
                tilt ?: cameraState.position.bearing
            ), animate
        )
    }

    override suspend fun moveToPosition(
        listLatLng: List<W3WCoordinates>,
    ) {
        if (listLatLng.isNotEmpty()) {
            val latLngBounds = LatLngBounds.Builder()
            listLatLng.forEach {
                latLngBounds.include(LatLng(it.lat, it.lng))
            }

            withContext(Main) {
                cameraState.animate(
                    update =
                        CameraUpdateFactory.newLatLngBounds(
                            latLngBounds.build(), 10
                        )
                )
            }
        }
    }

    override fun getZoomLevel(): Float {
        return cameraState.position.zoom
    }

    override fun getBearing(): Float {
        return cameraState.position.bearing
    }

    override fun getTilt(): Float {
        return cameraState.position.tilt
    }

    override fun getCenter(): W3WCoordinates? {
        return try {
            cameraState.position.target.let { W3WCoordinates(it.latitude, it.longitude) }
        } catch (ex: NullPointerException) {
            null
        }
    }

    /**
     * Updates the camera position with new settings.
     *
     * This private function handles the common camera positioning logic for both animated and
     * non-animated camera movements. It ensures the camera operation happens on the Main dispatcher
     * to properly update the UI.
     *
     * @param cameraPosition The new [CameraPosition] to apply to the map
     * @param animate Whether to animate the transition to the new camera position (true) or
     *                update instantly (false)
     */
    private suspend fun updateCameraPosition(cameraPosition: CameraPosition, animate: Boolean) {
        withContext(Main) {
            if (animate) {
                cameraState.animate(update = CameraUpdateFactory.newCameraPosition(cameraPosition))
            } else {
                cameraState.position = cameraPosition
            }
        }
    }
}