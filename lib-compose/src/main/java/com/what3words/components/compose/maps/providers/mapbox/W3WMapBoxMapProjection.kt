package com.what3words.components.compose.maps.providers.mapbox

import android.graphics.PointF
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.what3words.components.compose.maps.mapper.toMapBoxPoint
import com.what3words.components.compose.maps.mapper.toW3WCoordinates
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.core.types.geometry.W3WCoordinates

class W3WMapBoxMapProjection(private val map: MapboxMap) : W3WMapProjection {

    override fun toScreenLocation(coordinates: W3WCoordinates): PointF {
        val point = map.pixelForCoordinate(coordinates.toMapBoxPoint())
        return PointF(point.x.toFloat(), point.y.toFloat())
    }

    override fun fromScreenLocation(point: PointF): W3WCoordinates {
        val screenCoordinate = ScreenCoordinate(point.x.toDouble(), point.y.toDouble())
        val mapboxPoint = map.coordinateForPixel(screenCoordinate)
        return mapboxPoint.toW3WCoordinates()
    }
}