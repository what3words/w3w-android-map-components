package com.what3words.components.compose.maps.utils

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * Find the closest point to target from the given points list.
 */
fun findClosestTo(target: PointF, vararg points: PointF): PointF {
    val closest = points.minWithOrNull { o1, o2 ->
        val distance1 = distanceBetween(target, o1)
        val distance2 = distanceBetween(target, o2)

        distance1.compareTo(distance2)
    }

    return closest ?: points[0]
}

/**
 * Computes the distance between two points.
 */
fun distanceBetween(start: PointF?, end: PointF?): Float {
    if (start == null || end == null) return 0f

    val distance = sqrt((end.x.toDouble() - start.x.toDouble()).pow(2.0) + (end.y.toDouble() - start.y.toDouble()).pow(2.0))
    return distance.toFloat()
}

/**
 * Translates the given point with the offset defined by the given RectF, as it follows:
 *
 * - vertical offset is RectF height
 * - horizontal offset is RectF width
 */
fun offsetPoint(offset: RectF, point: PointF): PointF {
    val result = PointF(point.x, point.y)
    val offsetY = offset.bottom - offset.top
    val offsetX = offset.right - offset.left

    result.x = point.x + offsetX
    result.y = point.y + offsetY
    return result
}

/**
 * Computes the angle between points p1 and p2.
 *
 * The angle is expressed in degrees, in the range [0, 360]
 */
fun angleOfPoints(p1: PointF, p2: PointF): Float {
    // NOTE: Remember that most math has the Y axis as positive above the X.
    // However, for screens we have Y as positive below. For this reason,
    // the Y values are inverted to get the expected results.
    val deltaY = (p1.y - p2.y).toDouble()
    val deltaX = (p2.x - p1.x).toDouble()
    val result = Math.toDegrees(atan2(deltaY, deltaX)).toFloat()
    return if (result < 0) 360 + result else result
}