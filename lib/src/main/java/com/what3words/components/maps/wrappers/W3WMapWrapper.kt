package com.what3words.components.maps.wrappers

import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import java.util.function.Consumer

interface W3WMapWrapper {

    fun setLanguage(language: String): W3WMapWrapper
    fun gridEnabled(isEnabled: Boolean): W3WMapWrapper
    fun onMarkerClicked(callback: Consumer<SuggestionWithCoordinates>): W3WMapWrapper

    fun addMarkerAtSuggestion(
        suggestion: Suggestion,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun addMarkerAtSuggestion(
        listSuggestions: List<Suggestion>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun removeMarkerAtSuggestion(suggestion: Suggestion)
    fun removeMarkerAtSuggestion(listSuggestions: List<Suggestion>)
    fun selectAtSuggestion(
        suggestion: Suggestion,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun selectAtWords(
        words: String,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun removeMarkerAtWords(listWords: List<String>)
    fun removeMarkerAtWords(words: String)

    fun addMarkerAtCoordinates(
        coordinates: Coordinates,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun addMarkerAtCoordinates(
        listCoordinates: List<Coordinates>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun selectAtCoordinates(
        coordinates: Coordinates,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun findMarkerByCoordinates(
        coordinates: Coordinates
    ): SuggestionWithCoordinates?

    fun removeMarkerAtCoordinates(coordinates: Coordinates)
    fun removeMarkerAtCoordinates(listCoordinates: List<Coordinates>)

    fun removeAllMarkers()
    fun getAllMarkers(): List<SuggestionWithCoordinates>
    fun unselect()

    fun updateMove()
    fun updateMap()
}
