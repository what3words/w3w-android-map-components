//package com.what3words.components.compose.maps.state
//
//import com.what3words.core.types.geometry.W3WCoordinates
//import com.what3words.core.types.geometry.W3WRectangle
//
//data class W3WCameraPosition(
//    val zoomLevel: Float,
//    val coordinates: W3WCoordinates,
//    val bearing: Float,
//    val isAnimated: Boolean = false,
//    val isMoving: Boolean = false,
//    val tilt: Float,
//    val gridBound: W3WRectangle? = null
//) {
//    override fun hashCode(): Int {
//        var result = zoomLevel.hashCode()
//        result = 31 * result + coordinates.hashCode()
//        result = 31 * result + bearing.hashCode()
//        result = 31 * result + tilt.hashCode()
//        result = 31 * result + isAnimated.hashCode()
//        result = 31 * result + isMoving.hashCode()
//        return result
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as W3WCameraPosition
//
//        if (zoomLevel != other.zoomLevel) return false
//        if (coordinates != other.coordinates) return false
//        if (bearing != other.bearing) return false
//        if (tilt != other.tilt) return false
//
//        return true
//    }
//}