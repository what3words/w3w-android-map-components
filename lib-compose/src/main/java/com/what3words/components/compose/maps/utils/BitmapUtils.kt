package com.what3words.components.compose.maps.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.PathParser
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.what3words.components.compose.maps.models.W3WMarkerColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun getMarkerBitmap(
    scale: Float,
    colorMarker: W3WMarkerColor
): Bitmap {
    val pathBackground =
        "M4,3H44V43H32L25.598,50.317C25.218,50.751 24.669,51 24.093,51H23.907C23.331,51 22.782,50.751 22.402,50.317L16,43H4V3Z"
    val pathSplash =
        "M19.875,14.787C20.592,15.048 20.969,15.821 20.717,16.513L15.656,30.42C15.404,31.112 14.618,31.462 13.901,31.201C13.184,30.94 12.806,30.167 13.059,29.475L18.12,15.568C18.372,14.875 19.158,14.526 19.875,14.787ZM26.622,14.799C27.339,15.06 27.716,15.833 27.464,16.525L22.403,30.432C22.15,31.124 21.365,31.474 20.648,31.213C19.93,30.952 19.553,30.179 19.805,29.487L24.867,15.58C25.119,14.887 25.905,14.538 26.622,14.799ZM34.243,16.525C34.495,15.832 34.118,15.059 33.401,14.798C32.683,14.537 31.898,14.887 31.646,15.579L26.584,29.486C26.332,30.179 26.709,30.952 27.426,31.213C28.143,31.474 28.929,31.124 29.181,30.431L34.243,16.525Z"
    val markerWidth = 48
    val markerHeight = 56

    return getBitMapFromPathData(
        listOf(
            DrawPath(pathBackground,
                Paint().apply {
                    color = colorMarker.background.toArgb()
                }
            ),
            DrawPath(pathSplash,
                Paint().apply {
                    color = colorMarker.slash.toArgb()
                }
            )
        ),
        markerWidth,
        markerHeight,
        scale
    )
}

fun getPinBitmap(
    scale: Float,
    colorMarker: W3WMarkerColor
): Bitmap {
    val pathCircleBorder =
        "M12,1L12,1A11,11 0,0 1,23 12L23,12A11,11 0,0 1,12 23L12,23A11,11 0,0 1,1 12L1,12A11,11 0,0 1,12 1z"
    val pathCircleFill =
        "M12,1L12,1A11,11 0,0 1,23 12L23,12A11,11 0,0 1,12 23L12,23A11,11 0,0 1,1 12L1,12A11,11 0,0 1,12 1z"
    val pathSplash =
        "M9.938,7.893C10.296,8.023 10.485,8.41 10.359,8.756L7.828,15.71C7.702,16.056 7.309,16.231 6.95,16.1C6.592,15.97 6.403,15.583 6.529,15.237L9.06,8.283C9.186,7.937 9.579,7.762 9.938,7.893ZM13.311,7.899C13.67,8.03 13.858,8.416 13.732,8.762L11.201,15.716C11.075,16.062 10.683,16.237 10.324,16.106C9.965,15.976 9.777,15.589 9.903,15.243L12.434,8.29C12.56,7.943 12.953,7.769 13.311,7.899ZM17.122,8.762C17.247,8.416 17.059,8.029 16.7,7.899C16.342,7.768 15.949,7.943 15.823,8.289L13.292,15.243C13.166,15.589 13.355,15.975 13.713,16.106C14.072,16.236 14.465,16.061 14.591,15.715L17.122,8.762Z"
    val pinSize = 24

    return getBitMapFromPathData(
        listOf(
            DrawPath(pathCircleBorder,
                Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * scale
                    isAntiAlias = true
                    color = Color.White.toArgb()
                }
            ),
            DrawPath(pathCircleFill,
                Paint().apply {
                    color = colorMarker.background.toArgb()
                }
            ),
            DrawPath(pathSplash,
                Paint().apply {
                    color = colorMarker.slash.toArgb()
                }
            )
        ),
        pinSize,
        pinSize,
        scale
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
    // Create a Bitmap of the target size
    val widthScaled = width * scaleFactor
    val heightScaled = height * scaleFactor
    val bitmap =
        Bitmap.createBitmap(widthScaled.toInt(), heightScaled.toInt(), Bitmap.Config.ARGB_8888)

    // Create a Canvas to draw on the Bitmap
    val canvas = Canvas(bitmap)

    // Apply scaling transformation to the path
    val matrix = Matrix()
    matrix.setScale(scaleFactor, scaleFactor)

    listDrawPath.forEach {
        val path = PathParser.createPathFromPathData(it.pathData)
        path.transform(matrix)
        canvas.drawPath(
            path,
            it.paint
        )
    }


    return bitmap
}

suspend fun getPin(context: Context, color: W3WMarkerColor?, density: Float = 1f): Bitmap? {
    return if (color == null) {
        null
    } else {
        loadBitmapWithGlide(context, getPinBitmap(density, color))
    }
}

suspend fun getMarker(context: Context, color: W3WMarkerColor?, density: Float = 1f): Bitmap? {
    return if (color == null) {
        null
    } else {
        loadBitmapWithGlide(context, getMarkerBitmap(density, color))
    }
}

// Load the bitmap with Glide and return the result asynchronously
suspend fun loadBitmapWithGlide(context: Context, bitmap: Bitmap): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            // Glide will asynchronously load the bitmap
            val futureTarget = Glide.with(context)
                .asBitmap()
                .load(bitmap)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .submit()

            // Wait for Glide to load the image asynchronously
            return@withContext futureTarget.get()
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null or provide a fallback bitmap
        }
    }
}