package com.what3words.components.maps.views

import androidx.core.util.Consumer
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates

/** [W3WMap] A basic set of functions common to all W3W map objects */
interface W3WMap {
    /** Set the language of [SuggestionWithCoordinates.words] that onSuccess callbacks should return.
     *
     * @param language a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).
     */
    fun setLanguage(language: String)

    /** A callback for when an existing marker on the map is clicked.
     *
     * @param callback it will be invoked when an existing marker on the map is clicked by the user.
     */
    fun onMarkerClicked(callback: Consumer<SuggestionWithCoordinates>)

    /** Add [Suggestion] to the map. This method will add a marker/square to the map after getting the [Suggestion] from our W3WAutosuggestEditText.
     *
     * @param suggestion the [Suggestion] returned by our text/voice autosuggest component.
     * @param markerColor is the [W3WMarkerColor] for the [Suggestion] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtSuggestion(
        suggestion: Suggestion,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add a list of [Suggestion] to the map. This method will add multiple markers/squares to the map after getting the suggestions from our W3WAutosuggestEditText.
     *
     * @param listSuggestions list of [Suggestion]s returned by our text/voice autosuggest component.
     * @param markerColor the [W3WMarkerColor] for the [listSuggestions] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtSuggestion(
        listSuggestions: List<Suggestion>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Remove [Suggestion] from the map.
     *
     * @param suggestion the [Suggestion] to be removed.
     */
    fun removeMarkerAtSuggestion(suggestion: Suggestion)

    /** Remove [Suggestion]s from the map.
     *
     * @param listSuggestions the list of [Suggestion]s to remove.
     */
    fun removeMarkerAtSuggestion(listSuggestions: List<Suggestion>)

    /** Set [Suggestion] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param suggestion the [Suggestion] returned by our text/voice autosuggest component.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtSuggestion(
        suggestion: Suggestion,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add a three word address to the map. This method will add a marker/square to the map if [words] are a valid three word address, e.g., filled.count.soap. If it's not a valid three word address, [onError] will be called returning [APIResponse.What3WordsError.BAD_WORDS].
     *
     * @param words three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [words] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words and coordinates info needed for those [words].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add a list of three word addresses to the map. This method will add a marker/square to the map if all [listWords] are a valid three word addresses, e.g., filled.count.soap. If any valid three word address is not valid, [onError] will be called returning [APIResponse.What3WordsError.BAD_WORDS].
     *
     * @param listWords, list of three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [listWords] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess an success callback will return a list of [SuggestionWithCoordinates] with all the what3words and coordinate info for all added [listWords].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Set [words] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param words three word address to be added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtWords(
        words: String,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Remove three word address from the map.
     *
     * @param words the three word address to be removed.
     */
    fun removeMarkerAtWords(words: String)

    /** Remove a list of three word addresses from the map.
     *
     * @param listWords the list of three word addresses to remove.
     */
    fun removeMarkerAtWords(listWords: List<String>)

    /** Add [Coordinates] to the map. This method will add a marker/square to the map based on each of the [Coordinates] provided latitude and longitude.
     *
     * @param coordinates [Coordinates] to be added.
     * @param markerColor the [W3WMarkerColor] for the [Coordinates] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtCoordinates(
        coordinates: Coordinates,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add a list of [Coordinates] to the map. This method will add multiple markers/squares to the map based on the latitude and longitude of each [Coordinates] on the list.
     *
     * @param listCoordinates list of [Coordinates]s to be added.
     * @param markerColor the [W3WMarkerColor] for the [listCoordinates] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess an success callback will return a list of [SuggestionWithCoordinates] with all the what3words and coordinate info for all added [listCoordinates].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtCoordinates(
        listCoordinates: List<Coordinates>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Set [Coordinates] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param coordinates [Coordinates] to be added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtCoordinates(
        coordinates: Coordinates,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    fun findMarkerByCoordinates(
        coordinates: Coordinates
    ): SuggestionWithCoordinates?

    /** Remove [Coordinates] from the map.
     *
     * @param coordinates the [Coordinates] to be removed.
     */
    fun removeMarkerAtCoordinates(coordinates: Coordinates)

    /** Remove a list of [Coordinates] from the map.
     *
     * @param listCoordinates the list of [Coordinates] to remove.
     */
    fun removeMarkerAtCoordinates(listCoordinates: List<Coordinates>)

    /** Remove all markers from the map. */
    fun removeAllMarkers()

    /** Get all added [SuggestionWithCoordinates] from the map.
     *
     * @return list of [SuggestionWithCoordinates] with all items added to the map.
     */
    fun getAllMarkers(): List<SuggestionWithCoordinates>

    /** Remove selected marker from the map. */
    fun unselect()
}