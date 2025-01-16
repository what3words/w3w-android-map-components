package com.what3words.components.compose.maps.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.PathParser
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.map.components.compose.R

fun getMarkerBitmap(
    context: Context,
    density: Float = 1f,
    colorMarker: W3WMarkerColor,
    width: Int = 48,
    height: Int = 56
): Bitmap {
    return getBitMapFromPathData(
        listOf(
            DrawPath(context.getString(R.string.path_marker_background),
                Paint().apply {
                    color = colorMarker.background.toArgb()
                    // Draw shadow with radius 4dp = 4 * density
                    setShadowLayer(4 * density, 0f, 0f, Color(0x29000000).toArgb())
                }
            ),
            DrawPath(context.getString(R.string.path_marker_slashes),
                Paint().apply {
                    color = colorMarker.slash.toArgb()
                }
            )
        ),
        width,
        height,
        density
    )
}

fun getFillGridMarkerBitmap(
    context: Context,
    scale: Float,
    colorMarker: W3WMarkerColor,
    size: Int = 32,
): Bitmap {

    return getBitMapFromPathData(
        listOf(
            DrawPath(context.getString(R.string.path_fill_grid_marker_background),
                Paint().apply {
                    color = colorMarker.background.toArgb()
                }
            ),
            DrawPath(context.getString(R.string.path_fill_grid_marker_slashes),
                Paint().apply {
                    color = colorMarker.slash.toArgb()
                }
            )
        ),
        size,
        size,
        scale
    )
}

fun getPinBitmap(
    context: Context,
    density: Float = 1f,
    colorMarker: W3WMarkerColor
): Bitmap {
    val pinSize = 24

    return getBitMapFromPathData(
        listOf(
            DrawPath(context.getString(R.string.path_pin_circle),
                Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * density
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                }
            ),
            DrawPath(context.getString(R.string.path_pin_circle),
                Paint().apply {
                    color = colorMarker.background.toArgb()
                }
            ),
            DrawPath(context.getString(R.string.path_pin_slashes),
                Paint().apply {
                    color = colorMarker.slash.toArgb()
                }
            )
        ),
        pinSize,
        pinSize,
        density
    )
}

data class DrawPath(
    val pathData: String,
    val paint: Paint
)

fun getBitMapFromPathData(
    listDrawPath: List<DrawPath>,
    width: Int,
    height: Int,
    scaleFactor: Float
): Bitmap {
    // Calculate scaled dimensions
    val widthScaled = (width * scaleFactor).toInt()
    val heightScaled = (height * scaleFactor).toInt()

    // Create a Bitmap of the target size
    val bitmap = try {
        Bitmap.createBitmap(widthScaled, heightScaled, Bitmap.Config.ARGB_8888)
    } catch (e: OutOfMemoryError) {
        throw IllegalArgumentException("Bitmap too large to allocate", e)
    }

    // Create a Canvas to draw on the Bitmap
    val canvas = Canvas(bitmap)

    // Create and apply scaling transformation
    val matrix = Matrix().apply { setScale(scaleFactor, scaleFactor) }

    // Draw each path onto the canvas
    listDrawPath.forEach { drawPath ->
        try {
            val path = PathParser.createPathFromPathData(drawPath.pathData)
            path.transform(matrix)
            canvas.drawPath(path, drawPath.paint)
        } catch (e: Exception) {
            Log.e("BitmapUtils", "Error drawing path: ${e.message}")
        }
    }

    return bitmap
}