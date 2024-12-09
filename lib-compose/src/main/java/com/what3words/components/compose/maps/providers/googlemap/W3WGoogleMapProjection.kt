package com.what3words.components.compose.maps.providers.googlemap

import android.graphics.PointF
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import com.google.android.gms.maps.Projection
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.mapper.toW3WLatLng
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMapProjection

class W3WGoogleMapProjection(private val projection: Projection) : W3WMapProjection {
    override fun toScreenLocation(w3wLatLng: W3WLatLng): PointF {
        val point = projection.toScreenLocation(w3wLatLng.toGoogleLatLng())
        return point.toPointF()
    }

    override fun fromScreenLocation(point: PointF): W3WLatLng {
        val location = projection.fromScreenLocation(point.toPoint())
        return location.toW3WLatLng()
    }
}