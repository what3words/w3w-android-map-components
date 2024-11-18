package com.what3words.components.compose.maps.state

import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

interface W3WCameraState<T> {

    val cameraState: T

    var gridBound: W3WRectangle?

    fun orientCamera()

    fun moveToPosition(coordinates: W3WCoordinates, animated: Boolean)

    fun getZoomLevel(): Float
}