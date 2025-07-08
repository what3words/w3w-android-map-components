package com.what3words.components.compose.maps.utils

import android.graphics.PointF
import kotlin.math.atan2

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