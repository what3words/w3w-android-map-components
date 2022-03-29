package com.what3words.components.maps.extensions

import com.what3words.javawrapper.response.ConvertTo3WA
import com.what3words.javawrapper.response.ConvertToCoordinates
import com.what3words.javawrapper.response.Square
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates

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
