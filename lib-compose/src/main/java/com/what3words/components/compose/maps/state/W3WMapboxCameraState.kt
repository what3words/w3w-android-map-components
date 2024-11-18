package com.what3words.components.compose.maps.state

import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

class W3WMapboxCameraState(override val cameraState: MapViewportState) :
    W3WCameraState<MapViewportState> {

    override var gridBound: W3WRectangle?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun orientCamera() {
        TODO("Not yet implemented")
    }

    override fun moveToPosition(coordinates: W3WCoordinates, animated: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getZoomLevel(): Float {
        TODO("Not yet implemented")
    }
}