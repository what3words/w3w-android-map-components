package com.what3words.components.compose.maps.providers.googlemap

import android.graphics.PointF
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import com.google.android.gms.maps.Projection
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.mapper.toW3WCoordinates
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.core.types.geometry.W3WCoordinates

class W3WGoogleMapProjection(private val projection: Projection) : W3WMapProjection {
    override fun toScreenLocation(coordinates: W3WCoordinates): PointF {
        val point = projection.toScreenLocation(coordinates.toGoogleLatLng())
        return point.toPointF()
    }

    override fun fromScreenLocation(point: PointF): W3WCoordinates {
        val location = projection.fromScreenLocation(point.toPoint())
        return location.toW3WCoordinates()
    }
}