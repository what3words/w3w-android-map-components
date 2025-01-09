package com.what3words.components.compose.maps.providers.mapbox

import android.graphics.PointF
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.core.types.geometry.W3WCoordinates

class W3WMapBoxMapProjection(private val map: MapboxMap) : W3WMapProjection {

    override fun toScreenLocation(coordinates: W3WCoordinates): PointF {
        val point = map.toScreenLocation(Point.fromLngLat(coordinates.lng, coordinates.lat))
        return PointF(point.x.toFloat(), point.y.toFloat())
    }

    override fun fromScreenLocation(point: PointF): W3WCoordinates {
        val screenCoordinate = ScreenCoordinate(point.x.toDouble(), point.y.toDouble())
        val mapboxPoint = map.coordinateForPixel(screenCoordinate)
        return W3WCoordinates(lat = mapboxPoint.latitude(), lng = mapboxPoint.longitude())
    }

    // Worked with Mapbox v11.8.0
    // Reference: https://github.com/mapbox/mapbox-maps-android/issues/2131#issuecomment-2443260596
    // TODO: Check the getDeclaredField and class name every time Mapbox is updated to newer version
    private fun MapboxMap.toScreenLocation(coordinate: Point): ScreenCoordinate {
        try {
            val field = this.javaClass.getDeclaredField("nativeMap")
            field.isAccessible = true
            val nativeMap = field.get(this)
            val nativeMapClass = Class.forName("com.mapbox.maps.NativeMapImpl")
            val method = nativeMapClass.getDeclaredMethod("pixelForCoordinate", Point::class.java)
            method.isAccessible = true
            return method.invoke(nativeMap, coordinate) as ScreenCoordinate
        } catch (e: Exception) {
            // Returns (0, 0) if something goes wrong, which will disable the recall button
            return ScreenCoordinate(0.0, 0.0)
        }
    }
}