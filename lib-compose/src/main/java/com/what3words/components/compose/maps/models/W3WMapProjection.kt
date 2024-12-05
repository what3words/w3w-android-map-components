package com.what3words.components.compose.maps.models

import android.graphics.PointF


/**
 * Interface defining a projection for mapping geographical coordinates to screen coordinates and vice versa.
 */

interface W3WMapProjection {

    /**
     * Converts a [W3WLatLng] to a screen location.
     * @param w3wLatLng the coordinates to convert
     * @return the screen location corresponding to the given coordinates
     */
    fun toScreenLocation(w3wLatLng: W3WLatLng): PointF


    /**
     * Converts a screen location to a [W3WLatLng].
     * @param point the screen location to convert
     * @return the coordinates corresponding to the given screen location
     */
    fun fromScreenLocation(point: PointF): W3WLatLng

}