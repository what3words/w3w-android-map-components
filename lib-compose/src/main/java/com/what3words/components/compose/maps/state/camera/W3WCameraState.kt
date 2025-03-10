package com.what3words.components.compose.maps.state.camera

import androidx.compose.runtime.Immutable
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

/**
 * W3WCameraState the interface to define the camera state of the map.
 *
 * @param T The type of the underlying camera state object can be Google Map, MapBox
 *
 * @property cameraState The underlying camera state object.
 * @property gridBound The bounding rectangle of the grid lines displayed on the map.
 * @property visibleBound The bounding rectangle of the visible area on the map.
 * @property isCameraMoving A boolean flag indicating whether the camera is currently moving.
 *
 */
@Immutable
interface W3WCameraState<T> {

    val cameraState: T

    var gridBound: W3WRectangle?

    var visibleBound: W3WRectangle?

    var isCameraMoving: Boolean

    /**
     *  Adjust camera bearing to 0
     */
    suspend fun orientCamera()

    /**
     * Moves the camera to the specified latLng.
     *
     * @param latLng The W3W latLng to move the camera to.
     * @param animate Whether to animate the camera movement.
     */
    suspend fun moveToPosition(
        latLng: W3WCoordinates,
        zoom: Float? = null,
        bearing: Float? = null,
        tilt: Float? = null,
        animate: Boolean = false,
    )

    /**
     * Moves the camera to the specified latLng.
     *
     * @param listLatLng The list of W3W latLng to move the camera to.
     */
    suspend fun moveToPosition(
        listLatLng: List<W3WCoordinates>,
    )

    /**
     * Returns the current zoom level of the camera.
     *
     * @return The current zoom level.
     */
    fun getZoomLevel(): Float

    /**
     * Returns the current tilt angle of the camera in degrees.
     *
     * @return The current tilt angle in degrees from the nadir (directly facing the Earth).
     */
    fun getBearing(): Float

    /**
     * Returns the current bearing angle of the camera in degrees.
     *
     * @return The current bearing angle in degrees (clockwise rotation from north).
     */
    fun getTilt(): Float

    /**
     * Returns the coordinates of the center point of the current camera view.
     *
     * @return The W3WCoordinates representing the center of the map view, or null if not available.
     */
    fun getCenter(): W3WCoordinates?
}