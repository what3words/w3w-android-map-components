package com.what3words.components.compose.maps.extensions

import com.google.maps.android.collections.PolylineManager
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WLine
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.javawrapper.response.Coordinates
import com.what3words.javawrapper.response.Line

internal fun W3WRectangle.contains(coordinates: W3WCoordinates?): Boolean {
    if (coordinates == null) return false
    return if (coordinates.lat >= this.southwest.lat && coordinates.lat <= this.northeast.lat && coordinates.lng >= this.southwest.lng && coordinates.lng <= this.northeast.lng) return true
    else false
}

val W3WRectangle.id: Long
    get() {
        val sw = this.southwest
        val ne = this.northeast

        if (sw.lat < -90 || sw.lat > 90 || ne.lat < -90 || ne.lat > 90) {
            throw IllegalArgumentException("Invalid latitude value: must be between -90 and 90")
        }
        if (sw.lng < -180 || sw.lng > 180 || ne.lng < -180 || ne.lng > 180) {
            throw IllegalArgumentException("Invalid longitude value: must be between -180 and 180")
        }

        val swLatBits = (sw.lat * 1e6).toLong() and 0x7FFFFFFF
        val swLngBits = (sw.lng * 1e6).toLong() and 0x7FFFFFFF
        val neLatBits = (ne.lat * 1e6).toLong() and 0x7FFFFFFF
        val neLngBits = (ne.lng * 1e6).toLong() and 0x7FFFFFFF

        return (swLatBits shl 48) or (swLngBits shl 32) or (neLatBits shl 16) or neLngBits
    }

/**
 * Area of the [W3WRectangle].
 */
val W3WRectangle.area: Double
    get() {
        val width = this.northeast.lng - this.southwest.lng
        val height = this.northeast.lat - this.southwest.lat
        return width * height
    }

/**
 * Calculate the area of overlap between two rectangles.
 */
fun W3WRectangle.calculateAreaOverlap(other: W3WRectangle): Double {
    val overlapRect = W3WRectangle(
        W3WCoordinates(
            maxOf(this.southwest.lat, other.southwest.lat),
            maxOf(this.southwest.lng, other.southwest.lng)
        ),
        W3WCoordinates(
            minOf(this.northeast.lat, other.northeast.lat),
            minOf(this.northeast.lng, other.northeast.lng)
        )
    )

    if (overlapRect.southwest.lat >= overlapRect.northeast.lat ||
        overlapRect.southwest.lng >= overlapRect.northeast.lng
    ) {
        return 0.0
    }

    val thisRectArea = this.area
    val otherRectArea = other.area
    val overlapArea = overlapRect.area

    if (thisRectArea == 0.0 || otherRectArea == 0.0) {
        return 0.0 // Consider zero-area rectangles as having low overlap
    }

    val overlapPercentage = overlapArea / minOf(thisRectArea, otherRectArea)
    return overlapPercentage
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
internal fun List<W3WLine>.computeVerticalLines(): List<W3WCoordinates> {
    val computedVerticalLines =
        mutableListOf<W3WCoordinates>()

    // all vertical lines
    val verticalLines = mutableListOf<W3WLine>()
    verticalLines.addAll(this.filter { it.start.lng == it.end.lng })

    var t = 0
    while (verticalLines.isNotEmpty()) {
        verticalLines.maxByOrNull { it.start.lat }?.let { topLeftGrid ->
            if (t % 2 == 0) {
                computedVerticalLines.add(topLeftGrid.start)
                computedVerticalLines.add(topLeftGrid.end)
            } else {
                computedVerticalLines.add(topLeftGrid.end)
                computedVerticalLines.add(topLeftGrid.start)
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
internal fun List<W3WLine>.computeHorizontalLines(): List<W3WCoordinates> {
    val computedHorizontalLines =
        mutableListOf<W3WCoordinates>()

    // all horizontal lines
    val horizontalLines = mutableListOf<W3WLine>()
    horizontalLines.addAll(this.filter { it.start.lat == it.end.lat })

    var t = 0
    while (horizontalLines.isNotEmpty()) {
        horizontalLines.minByOrNull { it.start.lng }?.let { topLeftGrid ->
            if (t % 2 == 0) {
                computedHorizontalLines.add(topLeftGrid.start)
                computedHorizontalLines.add(topLeftGrid.end)
            } else {
                computedHorizontalLines.add(topLeftGrid.end)
                computedHorizontalLines.add(topLeftGrid.start)
            }
            horizontalLines.remove(topLeftGrid)
            t += 1
        }
    }
    return computedHorizontalLines
}