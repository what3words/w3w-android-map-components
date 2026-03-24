package com.what3words.components.compose.maps.extensions

import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WLine
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.javawrapper.response.Coordinates
import com.what3words.javawrapper.response.Line

/**
 * Utility method for checking if coordinates are inside a W3W rectangle.
 *
 * @param coordinates The coordinates to check if they are within the rectangle.
 * @return `true` if the coordinates are inside the rectangle, `false` otherwise or if coordinates are null.
 */
internal fun W3WRectangle.contains(coordinates: W3WCoordinates?): Boolean {
    if (coordinates == null) return false
    return if (coordinates.lat >= this.southwest.lat && coordinates.lat <= this.northeast.lat && coordinates.lng >= this.southwest.lng && coordinates.lng <= this.northeast.lng) return true
    else false
}

/**
 * Generates a unique 64-bit ID using SplitMix64-based hash function.
 * 
 * Based on "Fast Splittable Pseudorandom Number Generators" (Steele et al., OOPSLA 2014)
 * and java.util.SplittableRandom. Uses doubleToLongBits() for exact precision,
 * XOR-shift-multiplication mixing for avalanche effect, and prime multipliers
 * to prevent coordinate swap collisions.
 */
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

        val swLat = java.lang.Double.doubleToLongBits(sw.lat)
        val swLng = java.lang.Double.doubleToLongBits(sw.lng)
        val neLat = java.lang.Double.doubleToLongBits(ne.lat)
        val neLng = java.lang.Double.doubleToLongBits(ne.lng)

        var h1 = swLat * -7046029254386353131L
        h1 = (h1 xor (h1 ushr 30)) * -4658895280553007687L
        h1 = (h1 xor (h1 ushr 27)) * -7723592293110705685L
        h1 = h1 xor (h1 ushr 31)

        var h2 = swLng * -7046029254386353131L
        h2 = (h2 xor (h2 ushr 30)) * -4658895280553007687L
        h2 = (h2 xor (h2 ushr 27)) * -7723592293110705685L
        h2 = h2 xor (h2 ushr 31)

        var h3 = neLat * -7046029254386353131L
        h3 = (h3 xor (h3 ushr 30)) * -4658895280553007687L
        h3 = (h3 xor (h3 ushr 27)) * -7723592293110705685L
        h3 = h3 xor (h3 ushr 31)

        var h4 = neLng * -7046029254386353131L
        h4 = (h4 xor (h4 ushr 30)) * -4658895280553007687L
        h4 = (h4 xor (h4 ushr 27)) * -7723592293110705685L
        h4 = h4 xor (h4 ushr 31)

        var result = h1 + h2 * -4417273771661082387L + h3 * -2912652041462636481L + h4 * -744332891524214833L
        result = (result xor (result ushr 33)) * -1110381560719973571L
        result = (result xor (result ushr 33)) * -3956934367972663973L
        result = result xor (result ushr 33)

        return result
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