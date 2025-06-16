package com.what3words.components.compose.maps.providers.googlemap

import android.graphics.PointF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import com.google.android.gms.maps.Projection
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.core.types.geometry.W3WCoordinates

/**
 * An implementation of [W3WMapProjection] for Google Maps.
 *
 * This class wraps Google Maps' [Projection] to provide coordinate conversion between
 * screen coordinates and geographical coordinates using what3words system.
 *
 * @property projection The Google Maps Projection object used for coordinate conversions
 */
class W3WGoogleMapProjection(projection: Projection) : W3WMapProjection {

    var projection by mutableStateOf(projection)

    override fun toScreenLocation(coordinates: W3WCoordinates): PointF {
        val point = projection.toScreenLocation(coordinates.toGoogleLatLng())
        return point.toPointF()
    }

    override fun fromScreenLocation(point: PointF): W3WCoordinates {
        val location = projection.fromScreenLocation(point.toPoint())
        return W3WCoordinates(lat = location.latitude, lng = location.longitude)
    }
}