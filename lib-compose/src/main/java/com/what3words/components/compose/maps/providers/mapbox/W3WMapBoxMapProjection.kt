package com.what3words.components.compose.maps.providers.mapbox

import android.graphics.PointF
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.toCameraOptions
import com.what3words.components.compose.maps.mapper.toMapBoxPoint
import com.what3words.components.compose.maps.mapper.toW3WLatLng
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMapProjection

class W3WMapBoxMapProjection(private val map: MapboxMap) : W3WMapProjection {

    override fun toScreenLocation(w3wLatLng: W3WLatLng): PointF {
        val point = map.pixelForCoordinate(w3wLatLng.toMapBoxPoint())

        // If on-screen, return
        if (point.x >= 0 && point.y >= 0) {
            return PointF(point.x.toFloat(), point.y.toFloat())
        }

        // If off-screen, calculate the position manually
        val visibleBounds = map.coordinateBoundsForCamera(map.cameraState.toCameraOptions())
        val northWest = visibleBounds.northwest()
        val southEast = visibleBounds.southeast()

        // Get the screen positions of the corners
        val nwScreen = map.pixelForCoordinate(northWest)
        val seScreen = map.pixelForCoordinate(southEast)

        // Calculate the virtual screen coordinate for the off-screen point
        val mapWidth = seScreen.x - nwScreen.x
        val mapHeight = seScreen.y - nwScreen.y

        val longitudeDiff = w3wLatLng.lng - northWest.longitude()
        val latitudeDiff = northWest.latitude() - w3wLatLng.lat

        val x = nwScreen.x + (longitudeDiff / (southEast.longitude() - northWest.longitude())) * mapWidth
        val y = nwScreen.y + (latitudeDiff / (northWest.latitude() - southEast.latitude())) * mapHeight

        return PointF(x.toFloat(), y.toFloat())
    }

    override fun fromScreenLocation(point: PointF): W3WLatLng {
        val screenCoordinate = ScreenCoordinate(point.x.toDouble(), point.y.toDouble())
        val mapboxPoint = map.coordinateForPixel(screenCoordinate)
        return mapboxPoint.toW3WLatLng()
    }
}