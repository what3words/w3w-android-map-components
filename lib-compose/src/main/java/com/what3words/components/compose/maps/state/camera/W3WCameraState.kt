package com.what3words.components.compose.maps.state.camera

import com.what3words.components.compose.maps.models.W3WCameraPosition
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

/**
 * W3WCameraState the interface to define the camera state of the map.
 *
 * @param T The type of the underlying camera state object can be Google Map, MapBox
 *
 * @property cameraState The underlying camera state object.
 * @property gridBound The bounding rectangle of the grid lines displayed on the map.
 *
 */
interface W3WCameraState<T> {

    val cameraState: T

    var gridBound: W3WRectangle?

    /**
     *  Adjust camera bearing to 0
     */
    fun orientCamera()

    /**
     * Moves the camera to the specified coordinates.
     *
     * @param coordinates The W3W coordinates to move the camera to.
     * @param animate Whether to animate the camera movement.
     */
    fun moveToPosition(coordinates: W3WCoordinates, animate: Boolean)

    /**
     * Returns the current zoom level of the camera.
     *
     * @return The current zoom level.
     */
    fun getZoomLevel(): Float

    /**
     * Sets the camera position to the specified camera position.
     *
     * @param cameraPosition The new camera position.
     * @param animate Whether to animate the camera movement.
     */
    fun setCameraPosition(cameraPosition: W3WCameraPosition, animate: Boolean)
}