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

@Stable
class W3WGoogleCameraState(initialCameraState: CameraPositionState) :
    W3WCameraState<CameraPositionState> {

    companion object {
        const val MY_LOCATION_ZOOM = 20f
    }

    override val cameraState: CameraPositionState by mutableStateOf(initialCameraState)

    override var gridBound: W3WRectangle? by mutableStateOf(null)

    override var visibleBound: W3WRectangle? by mutableStateOf(null)

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