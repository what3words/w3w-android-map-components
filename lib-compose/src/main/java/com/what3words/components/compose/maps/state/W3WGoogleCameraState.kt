package com.what3words.components.compose.maps.state

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class W3WGoogleCameraState(override val cameraState: CameraPositionState) :
    W3WCameraState<CameraPositionState> {

    override var gridBound: W3WRectangle? = null

    override fun orientCamera() {
        TODO("Not yet implemented")
    }

    override fun moveToPosition(coordinates: W3WCoordinates, animated: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            if (animated) {
                cameraState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(coordinates.lat, coordinates.lng),
                        cameraState.position.zoom
                    )
                )
            } else {
                cameraState.position = CameraPosition(
                    LatLng(coordinates.lat, coordinates.lng),
                    cameraState.position.zoom,
                    cameraState.position.tilt,
                    cameraState.position.bearing
                )
            }
        }

    }

    override fun getZoomLevel(): Float {
        return cameraState.position.zoom
    }
}