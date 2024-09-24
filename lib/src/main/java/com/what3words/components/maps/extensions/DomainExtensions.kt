package com.what3words.components.maps.extensions

import com.google.maps.android.collections.PolylineManager
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.javawrapper.response.ConvertTo3WA
import com.what3words.javawrapper.response.ConvertToCoordinates
import com.what3words.javawrapper.response.Coordinates
import com.what3words.javawrapper.response.Line
import com.what3words.javawrapper.response.Square
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates

//To be moved to core library extensions?
internal fun Square.contains(lat: Double, lng: Double): Boolean {
    return if (lat >= this.southwest.lat && lat <= this.northeast.lat && lng >= this.southwest.lng && lng <= this.northeast.lng) return true
    else false
}

internal fun ConvertTo3WA.toSuggestionWithCoordinates(): SuggestionWithCoordinates {
    val suggestion = Suggestion(this.words, this.nearestPlace, this.country, null, 0, this.language)
    return SuggestionWithCoordinates(suggestion, this)
}

internal fun ConvertToCoordinates.toSuggestionWithCoordinates(): SuggestionWithCoordinates {
    val suggestion = Suggestion(this.words, this.nearestPlace, this.country, null, 0, this.language)
    return SuggestionWithCoordinates(suggestion, this)
}

fun Coordinates.generateUniqueId(): Long {
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

fun Long.toCoordinates(): Coordinates {
    val lat = (this ushr 32).toDouble() / 1e6
    val lng = (this and 0xffffffff).toDouble() / 1e6

    if (lat < -90 || lat > 90) {
        throw IllegalArgumentException("Invalid latitude value: must be between -90 and 90")
    }
    if (lng < -180 || lng > 180) {
        throw IllegalArgumentException("Invalid longitude value: must be between -180 and 180")
    }

    return Coordinates(lat, lng)
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
internal fun List<Line>.computeVerticalLines(): List<Coordinates> {
    val computedVerticalLines =
        mutableListOf<Coordinates>()

    // all vertical lines
    val verticalLines = mutableListOf<Line>()
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
internal fun List<Line>.computeHorizontalLines(): List<Coordinates> {
    val computedHorizontalLines =
        mutableListOf<Coordinates>()

    // all horizontal lines
    val horizontalLines = mutableListOf<Line>()
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