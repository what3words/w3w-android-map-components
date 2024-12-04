package com.what3words.components.compose.maps.models

import android.graphics.PointF
import com.what3words.core.types.geometry.W3WCoordinates


/**
 * Interface defining a projection for mapping geographical coordinates to screen coordinates and vice versa.
 */

interface W3WMapProjection {

    /**
     * Converts a [W3WCoordinates] to a screen location.
     * @param coordinates the coordinates to convert
     * @return the screen location corresponding to the given coordinates
     */
    fun toScreenLocation(coordinates: W3WCoordinates): PointF


    /**
     * Converts a screen location to a [W3WCoordinates].
     * @param point the screen location to convert
     * @return the coordinates corresponding to the given screen location
     */
    fun fromScreenLocation(point: PointF): W3WCoordinates

}