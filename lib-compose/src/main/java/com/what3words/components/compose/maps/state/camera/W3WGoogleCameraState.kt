package com.what3words.components.compose.maps.state.camera

import androidx.annotation.UiThread
import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

@Immutable
class W3WGoogleCameraState(override val cameraState: CameraPositionState) :
    W3WCameraState<CameraPositionState> {

    companion object {
        const val MY_LOCATION_ZOOM = 20f
    }

    override var gridBound: W3WRectangle? = null

    @UiThread
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

    @UiThread
    override suspend fun moveToPosition(
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

    @UiThread
    override suspend fun moveToPosition(
        listCoordinates: List<W3WCoordinates>,
    ) {
        val latLngBounds = LatLngBounds.Builder()
        listCoordinates.forEach {
            latLngBounds.include(LatLng(it.lat, it.lng))
        }

        cameraState.animate(
            update =
            CameraUpdateFactory.newLatLngBounds(
                latLngBounds.build(), 0
            )
        )
    }

    override fun getZoomLevel(): Float {
        return cameraState.position.zoom
    }

    @UiThread
    private suspend fun updateCameraPosition(cameraPosition: CameraPosition, animate: Boolean) {
        if (animate) {
            cameraState.animate(update = CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            cameraState.position = cameraPosition
        }
    }
}