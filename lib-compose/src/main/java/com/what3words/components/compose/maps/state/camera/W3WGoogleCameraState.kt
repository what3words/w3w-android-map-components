package com.what3words.components.compose.maps.state.camera

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.what3words.components.compose.maps.mapper.toGoogleCameraPosition
import com.what3words.components.compose.maps.models.W3WCameraPosition
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class W3WGoogleCameraState(override val cameraState: CameraPositionState) :
    W3WCameraState<CameraPositionState> {

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

    override fun moveToPosition(coordinates: W3WCoordinates, animate: Boolean) {
        updateCameraPosition(
            CameraPosition(
                LatLng(coordinates.lat, coordinates.lng),
                cameraState.position.zoom,
                cameraState.position.tilt,
                cameraState.position.bearing
            ), animate
        )
    }

    override fun getZoomLevel(): Float {
        return cameraState.position.zoom
    }

    override fun setCameraPosition(cameraPosition: W3WCameraPosition, animate: Boolean) {
        updateCameraPosition(cameraPosition.toGoogleCameraPosition(), animate)
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