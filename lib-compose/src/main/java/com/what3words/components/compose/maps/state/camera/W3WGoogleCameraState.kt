package com.what3words.components.compose.maps.state.camera

import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WSquare
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext

@Immutable
class W3WGoogleCameraState(override val cameraState: CameraPositionState) :
    W3WCameraState<CameraPositionState> {

    companion object {
        const val MY_LOCATION_ZOOM = 20f
    }

    override var gridBound: W3WSquare? = null

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
        latLng: W3WLatLng,
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
        listLatLng: List<W3WLatLng>,
    ) {
        val latLngBounds = LatLngBounds.Builder()
        listLatLng.forEach {
            latLngBounds.include(LatLng(it.lat, it.lng))
        }

        withContext(Main) {
            cameraState.animate(
                update =
                CameraUpdateFactory.newLatLngBounds(
                    latLngBounds.build(), 0
                )
            )
        }
    }

    override fun getZoomLevel(): Float {
        return cameraState.position.zoom
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