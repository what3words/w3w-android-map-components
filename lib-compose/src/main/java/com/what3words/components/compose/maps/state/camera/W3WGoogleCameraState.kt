package com.what3words.components.compose.maps.state.camera

import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Immutable
class W3WGoogleCameraState(override val cameraState: CameraPositionState) :
    W3WCameraState<CameraPositionState> {

    companion object {
        const val MY_LOCATION_ZOOM = 20f
    }

    override var gridBound: W3WRectangle? = null

    override fun orientCamera() {
        updateCameraPosition(
            CameraPosition(
                cameraState.position.target,
                cameraState.position.zoom,
                0f,
                0f
            ), true
        )
    }

    override fun moveToPosition(
        coordinates: W3WCoordinates,
        zoom: Float?,
        bearing: Float?,
        tilt: Float?,
        animate: Boolean
    ) {
        updateCameraPosition(
            CameraPosition(
                LatLng(coordinates.lat, coordinates.lng),
                zoom ?: cameraState.position.zoom,
                bearing ?: cameraState.position.tilt,
                tilt ?: cameraState.position.bearing
            ), animate
        )
    }

    override fun moveToPosition(
        listCoordinates: List<W3WCoordinates>,
        zoom: Float?,
        bearing: Float?,
        tilt: Float?,
        animate: Boolean
    ) {
        val latLngBounds = LatLngBounds.Builder()
        listCoordinates.forEach {
            latLngBounds.include(LatLng(it.lat,it.lng))
        }


        CoroutineScope(Dispatchers.Main).launch {
            cameraState.animate(update = CameraUpdateFactory.newLatLngBounds( latLngBounds.build(), 0))
        }
    }

    override fun getZoomLevel(): Float {
        return cameraState.position.zoom
    }

    private fun updateCameraPosition(cameraPosition: CameraPosition, animate: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            if (animate) {
                cameraState.animate(update = CameraUpdateFactory.newCameraPosition(cameraPosition))
            } else {
                cameraState.position = cameraPosition
            }
        }
    }
}