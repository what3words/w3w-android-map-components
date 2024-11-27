package com.what3words.components.compose.maps.extensions

import com.google.maps.android.collections.PolylineManager
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.components.compose.maps.mapper.toW3WLatLong
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WLine
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.javawrapper.response.Coordinates
import com.what3words.javawrapper.response.Line

//To be moved to core library extensions?
internal fun W3WRectangle.contains(coordinates: W3WCoordinates?): Boolean {
    if (coordinates == null) return false
    return if (coordinates.lat >= this.southwest.lat && coordinates.lat <= this.northeast.lat && coordinates.lng >= this.southwest.lng && coordinates.lng <= this.northeast.lng) return true
    else false
}

fun W3WCoordinates.generateUniqueId(): Long {
    if (lat < -90 || lat > 90) {
        throw IllegalArgumentException("Invalid latitude value: must be between -90 and 90")
    }
    if (lng < -180 || lng > 180) {
        throw IllegalArgumentException("Invalid longitude value: must be between -180 and 180")
    }
    val latBits = (lat * 1e6).toLong() shl 32
    val lngBits = (lng * 1e6).toLong() and 0xffffffff
    return (latBits or lngBits)
}

fun Long.toCoordinates(): W3WCoordinates {
    val lat = (this ushr 32).toDouble() / 1e6
    val lng = (this and 0xffffffff).toDouble() / 1e6

    if (lat < -90 || lat > 90) {
        throw IllegalArgumentException("Invalid latitude value: must be between -90 and 90")
    }
    if (lng < -180 || lng > 180) {
        throw IllegalArgumentException("Invalid longitude value: must be between -180 and 180")
    }

    return W3WCoordinates(lat, lng)
}

/** [computeVerticalLines] will compute vertical lines to work with [PolylineManager], it will invert every odd line to avoid diagonal connection.
 * List of [Line]'s will come from [What3WordsAndroidWrapper] with the following logic:
 * 1     3      5
 * |  /  |  /  |
 * 2     4     6 ..
 *
 * @return will return list of [Coordinates] with the following logic:
 * 1     4-----5
 * |     |     |
 * 2-----3     6 ..
 */
internal fun List<W3WLine>.computeVerticalLines(): List<W3WLatLng> {
    val computedVerticalLines =
        mutableListOf<W3WLatLng>()

    // all vertical lines
    val verticalLines = mutableListOf<W3WLine>()
    verticalLines.addAll(this.filter { it.start.lng == it.end.lng })

    var t = 0
    while (verticalLines.isNotEmpty()) {
        verticalLines.maxByOrNull { it.start.lat }?.let { topLeftGrid ->
            if (t % 2 == 0) {
                computedVerticalLines.add(topLeftGrid.start.toW3WLatLong())
                computedVerticalLines.add(topLeftGrid.end.toW3WLatLong())
            } else {
                computedVerticalLines.add(topLeftGrid.end.toW3WLatLong())
                computedVerticalLines.add(topLeftGrid.start.toW3WLatLong())
            }
            verticalLines.remove(topLeftGrid)
        }
        t += 1
    }
    return computedVerticalLines
}

/** [computeHorizontalLines] will compute horizontal lines to work with [PolylineManager], it will invert every odd line to avoid diagonal connection.
 * List of [Line]'s will come from [What3WordsAndroidWrapper] with the following logic:
 * A-----B
 *    /
 * C-----D
 *    /
 * E-----F
 *
 * @return will return list of [Coordinates] with the following logic:
 * A-----B
 *       |
 * D-----C
 * |
 * E-----F
 */
internal fun List<W3WLine>.computeHorizontalLines(): List<W3WLatLng> {
    val computedHorizontalLines =
        mutableListOf<W3WLatLng>()

    // all horizontal lines
    val horizontalLines = mutableListOf<W3WLine>()
    horizontalLines.addAll(this.filter { it.start.lat == it.end.lat })

    var t = 0
    while (horizontalLines.isNotEmpty()) {
        horizontalLines.minByOrNull { it.start.lng }?.let { topLeftGrid ->
            if (t % 2 == 0) {
                computedHorizontalLines.add(topLeftGrid.start.toW3WLatLong())
                computedHorizontalLines.add(topLeftGrid.end.toW3WLatLong())
            } else {
                computedHorizontalLines.add(topLeftGrid.end.toW3WLatLong())
                computedHorizontalLines.add(topLeftGrid.start.toW3WLatLong())
            }
            horizontalLines.remove(topLeftGrid)
            t += 1
        }
    }
    return computedHorizontalLines
}