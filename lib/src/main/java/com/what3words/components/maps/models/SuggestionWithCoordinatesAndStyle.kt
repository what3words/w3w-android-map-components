package com.what3words.components.maps.models

import com.what3words.javawrapper.response.SuggestionWithCoordinates

internal data class SuggestionWithCoordinatesAndStyle(
    val suggestion: SuggestionWithCoordinates,
    val markerColor: W3WMarkerColor
)
