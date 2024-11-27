package com.what3words.components.compose.maps.utils

import android.content.Context
import com.what3words.map.components.compose.R

fun Float.getGridSelectedBorderSizeBasedOnZoomLevel(
    context: Context,
    zoomSwitchLevel: Float
): Float {
    return when {
        this < zoomSwitchLevel -> context.resources.getDimension(R.dimen.grid_width_gone)
        this >= zoomSwitchLevel && this < 19f -> context.resources.getDimension(R.dimen.grid_selected_width_1dp)
        this in 19f..20f -> context.resources.getDimension(R.dimen.grid_selected_width_1_5dp)
        else -> context.resources.getDimension(R.dimen.grid_selected_width_2dp)
    }
}