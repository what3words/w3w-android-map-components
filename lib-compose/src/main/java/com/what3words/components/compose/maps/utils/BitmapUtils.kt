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
import androidx.core.graphics.createBitmap

/**
 * Creates a bitmap for the standard what3words marker.
 *
 * This function generates a bitmap with the what3words marker design using SVG path data.
 * The marker includes the iconic what3words slashes and can be customized with different colors.
 *
 * @param context Android context used to access resources
 * @param density The display density factor for proper scaling
 * @param colorMarker The color scheme to use for the marker
 * @param width The desired width of the bitmap in pixels
 * @param height The desired height of the bitmap in pixels
 * @return A Bitmap of the what3words marker
 */
fun getMarkerBitmap(
    context: Context,
    density: Float = 1f,
    colorMarker: W3WMarkerColor,
    width: Int = 48,
    height: Int = 56
): Bitmap {
    return getBitMapFromPathData(
        listOf(
            DrawPath(
                context.getString(R.string.path_marker_background),
                Paint().apply {
                    color = colorMarker.background.toArgb()
                    // Draw shadow with radius 4dp = 4 * density
                    setShadowLayer(4 * density, 0f, 0f, Color(0x29000000).toArgb())
                }
            ),
            DrawPath(
                context.getString(R.string.path_marker_slashes),
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

/**
 * Creates a bitmap for the filled grid what3words marker.
 *
 * This function generates a bitmap for markers used when displaying the what3words grid.
 * It represents a filled square with the what3words slashes.
 *
 * @param context Android context used to access resources
 * @param scale The scaling factor to apply to the bitmap
 * @param colorMarker The color scheme to use for the marker
 * @param size The desired width and height of the bitmap in pixels
 * @return A Bitmap of the filled grid marker
 */
fun getFillGridMarkerBitmap(
    context: Context,
    scale: Float,
    colorMarker: W3WMarkerColor,
    size: Int = 32,
): Bitmap {

    return getBitMapFromPathData(
        listOf(
            DrawPath(
                context.getString(R.string.path_fill_grid_marker_background),
                Paint().apply {
                    color = colorMarker.background.toArgb()
                }
            ),
            DrawPath(
                context.getString(R.string.path_fill_grid_marker_slashes),
                Paint().apply {
                    color = colorMarker.slash.toArgb()
                }
            )
        ),
        size,
        size,
        scale,
        0
    )
}

/**
 * Creates a bitmap for the what3words pin marker.
 *
 * This function generates a bitmap with the what3words pin design, which is a circular marker
 * with slashes and a white border. The pin includes multiple shadow layers for depth.
 *
 * @param context Android context used to access resources
 * @param density The display density factor for proper scaling
 * @param colorMarker The color scheme to use for the pin
 * @return A Bitmap of the what3words pin
 */
fun getPinBitmap(
    context: Context,
    density: Float = 1f,
    colorMarker: W3WMarkerColor
): Bitmap {
    val pinSize = 24

    return getBitMapFromPathData(
        listOf(
            DrawPath(
                context.getString(R.string.path_pin_circle),
                Paint().apply {
                    color = colorMarker.background.toArgb()
                    // Draw shadow with radius 3dp (+2dp) of the stroke
                    // The design has 2 shadow layers
                    setShadowLayer(5 * density, 0f, 0f, Color(0x26000000).toArgb())
                    setShadowLayer(4 * density, 0f, 0f, Color(0x4D000000).toArgb())
                }
            ),
            DrawPath(
                context.getString(R.string.path_pin_slashes),
                Paint().apply {
                    color = colorMarker.slash.toArgb()
                }
            ),
            DrawPath(
                context.getString(R.string.path_pin_circle),
                Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * density
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                }
            )
        ),
        pinSize,
        pinSize,
        density
    )
}

/**
 * Data class representing an SVG path and its associated paint style.
 *
 * @property pathData The SVG path data string
 * @property paint The Paint object defining how to render the path
 */
data class DrawPath(
    val pathData: String,
    val paint: Paint
)

/**
 * Creates a bitmap from SVG path data.
 *
 * This function renders SVG paths onto a bitmap with the specified dimensions and scaling.
 * It supports multiple layers of paths with different paint styles, including shadows.
 *
 * @param listDrawPath List of paths and their painting styles to render
 * @param width The desired width of the output bitmap in dp
 * @param height The desired height of the output bitmap in dp
 * @param scaleFactor The scaling factor to apply to the paths and bitmap
 * @param padding Additional padding in dp to add around the bitmap for shadows
 * @return A Bitmap with the rendered SVG paths
 * @throws IllegalArgumentException If the resulting bitmap would be too large to allocate
 */
fun getBitMapFromPathData(
    listDrawPath: List<DrawPath>,
    width: Int,
    height: Int,
    scaleFactor: Float,
    padding: Int = 4, // Default padding for shadow in dp
): Bitmap {
    // Calculate scaled dimensions
    val widthScaled = (width * scaleFactor).toInt()
    val heightScaled = (height * scaleFactor).toInt()
    val paddingPx = (padding * scaleFactor).toInt()

    // Create a Bitmap of the target size
    val bitmap = try {
        createBitmap(widthScaled + paddingPx * 2, heightScaled + paddingPx * 2)
    } catch (e: OutOfMemoryError) {
        throw IllegalArgumentException("Bitmap too large to allocate", e)
    }

    // Create a Canvas to draw on the Bitmap
    val canvas = Canvas(bitmap)

    // Create and apply scaling transformation
    val matrix = Matrix().apply { setScale(scaleFactor, scaleFactor) }

    // Translate canvas to account for padding
    canvas.translate(paddingPx.toFloat(), paddingPx.toFloat())

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