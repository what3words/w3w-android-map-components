package com.what3words.components.compose.maps.models

import android.graphics.PointF

/**
 * Represents a quadrilateral grid cell on the screen defined by four [PointF] vertices.
 *
 * Cell vertices order
 *
 * 1 . . . 2
 * .       .
 * .       .
 * .       .
 * 4 . . . 3
 */

data class W3WGridScreenCell(val v1: PointF, val v2: PointF, val v3: PointF, val v4: PointF) {
    val center = computeCenterCoordinates()

    private fun computeCenterCoordinates(): PointF {
        val x = (v1.x + v3.x) * 0.5f
        val y = (v1.y + v3.y) * 0.5f

        return PointF(x, y)
    }

    fun containsPoint(pointF: PointF): Boolean {
        return containsPoint(pointF.x, pointF.y)
    }

    fun containsPoint(pointX: Float, pointY: Float): Boolean {
        return pointX in v1.x..v3.x && pointY in v1.y..v3.y
    }
}