package com.what3words.components.compose.maps.providers.mapbox

import android.graphics.PointF
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.what3words.components.compose.maps.mapper.toMapBoxPoint
import com.what3words.components.compose.maps.mapper.toW3WLatLng
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMapProjection

class W3WMapBoxMapProjection(private val map: MapboxMap) : W3WMapProjection {

    override fun toScreenLocation(w3wLatLng: W3WLatLng): PointF {
        val point = map.toScreenLocation(w3wLatLng.toMapBoxPoint())
        return PointF(point.x.toFloat(), point.y.toFloat())
    }

    override fun fromScreenLocation(point: PointF): W3WLatLng {
        val screenCoordinate = ScreenCoordinate(point.x.toDouble(), point.y.toDouble())
        val mapboxPoint = map.fromScreenLocation(screenCoordinate)
        return mapboxPoint.toW3WLatLng()
    }

    // Worked with Mapbox v11.8.0
    // Reference: https://github.com/mapbox/mapbox-maps-android/issues/2131#issuecomment-2443260596
    // TODO: Check the getDeclaredField and class name every time Mapbox is updated to newer version
    private fun MapboxMap.toScreenLocation(coordinate: Point): ScreenCoordinate {
        val field = this.javaClass.getDeclaredField("nativeMap")
        field.isAccessible = true
        val nativeMap = field.get(this)
        val nativeMapClass = Class.forName("com.mapbox.maps.NativeMapImpl")
        val method = nativeMapClass.getDeclaredMethod("pixelForCoordinate", Point::class.java)
        method.isAccessible = true
        return method.invoke(nativeMap, coordinate) as ScreenCoordinate
    }

    private fun MapboxMap.fromScreenLocation(screenCoordinate: ScreenCoordinate): Point {
        val field = this.javaClass.getDeclaredField("nativeMap")
        field.isAccessible = true
        val nativeMap = field.get(this)
        val nativeMapClass = Class.forName("com.mapbox.maps.NativeMapImpl")
        val method = nativeMapClass.getDeclaredMethod("coordinateForPixel", ScreenCoordinate::class.java)
        method.isAccessible = true
        return method.invoke(nativeMap, screenCoordinate) as Point
    }
}