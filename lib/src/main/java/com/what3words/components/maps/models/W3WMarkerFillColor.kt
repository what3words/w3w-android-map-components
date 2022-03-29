package com.what3words.components.maps.models

import com.what3words.map.components.R

enum class W3WMarkerColor {
    RED,
    CORAL,
    TURQUOISE,
    SKY_BLUE,
    PURPLE,
    YELLOW,
    BRIGHT_ORANGE,
    BLUE,
    ORANGE,
    GREEN,
    FUCHSIA
}

internal fun W3WMarkerColor.toPin(): Int? {
    return when (this) {
        W3WMarkerColor.RED -> R.drawable.ic_marker_pin_red
        W3WMarkerColor.CORAL -> R.drawable.ic_marker_pin_coral
        W3WMarkerColor.TURQUOISE -> R.drawable.ic_marker_pin_turquoise
        W3WMarkerColor.SKY_BLUE -> R.drawable.ic_marker_pin_sky_blue
        W3WMarkerColor.PURPLE -> R.drawable.ic_marker_pin_purple
        W3WMarkerColor.YELLOW -> R.drawable.ic_marker_pin_yellow
        W3WMarkerColor.BRIGHT_ORANGE -> R.drawable.ic_marker_pin_bright_orange
        W3WMarkerColor.BLUE -> R.drawable.ic_marker_pin_blue
        W3WMarkerColor.ORANGE -> R.drawable.ic_marker_pin_orange
        W3WMarkerColor.GREEN -> R.drawable.ic_marker_pin_green
        W3WMarkerColor.FUCHSIA -> R.drawable.ic_marker_pin_fuchsia
    }
}

internal fun W3WMarkerColor.toCircle(): Int {
    return when (this) {
        W3WMarkerColor.RED -> R.drawable.ic_marker_circle_red
        W3WMarkerColor.CORAL -> R.drawable.ic_marker_circle_coral
        W3WMarkerColor.TURQUOISE -> R.drawable.ic_marker_circle_turquoise
        W3WMarkerColor.SKY_BLUE -> R.drawable.ic_marker_circle_sky_blue
        W3WMarkerColor.PURPLE -> R.drawable.ic_marker_circle_purple
        W3WMarkerColor.YELLOW -> R.drawable.ic_marker_circle_yellow
        W3WMarkerColor.BRIGHT_ORANGE -> R.drawable.ic_marker_circle_bright_orange
        W3WMarkerColor.BLUE -> R.drawable.ic_marker_circle_blue
        W3WMarkerColor.ORANGE -> R.drawable.ic_marker_circle_orange
        W3WMarkerColor.GREEN -> R.drawable.ic_marker_circle_green
        W3WMarkerColor.FUCHSIA -> R.drawable.ic_marker_circle_fuchsia
    }
}

internal fun W3WMarkerColor.toGridFill(): Int {
    return when (this) {
        W3WMarkerColor.RED -> R.drawable.ic_grid_red
        W3WMarkerColor.CORAL -> R.drawable.ic_grid_coral
        W3WMarkerColor.TURQUOISE -> R.drawable.ic_grid_turquoise
        W3WMarkerColor.SKY_BLUE -> R.drawable.ic_grid_sky_blue
        W3WMarkerColor.PURPLE -> R.drawable.ic_grid_purple
        W3WMarkerColor.YELLOW -> R.drawable.ic_grid_yellow
        W3WMarkerColor.BRIGHT_ORANGE -> R.drawable.ic_grid_bright_orange
        W3WMarkerColor.BLUE -> R.drawable.ic_grid_blue
        W3WMarkerColor.ORANGE -> R.drawable.ic_grid_orange
        W3WMarkerColor.GREEN -> R.drawable.ic_grid_green
        W3WMarkerColor.FUCHSIA -> R.drawable.ic_grid_fuchsia
    }
}
