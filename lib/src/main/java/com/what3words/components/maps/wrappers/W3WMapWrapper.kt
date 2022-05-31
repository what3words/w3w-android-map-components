package com.what3words.components.maps.wrappers

import androidx.core.util.Consumer
import com.google.android.gms.maps.GoogleMap
import com.mapbox.maps.MapboxMap
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates

/** What3words abstract map wrapper interface, every new map provider wrapper added to the component should implement this interface. */
interface W3WMapWrapper {

    /** Set the language of [SuggestionWithCoordinates.words] that onSuccess callbacks should return.
     *
     * @param language a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).
     */
    fun setLanguage(language: String): W3WMapWrapper

    /** Enable grid overlay over map with all 3mx3m squares on the visible map bounds.
     *
     * @param isEnabled enable or disable grid, enabled by default.
     */
    fun gridEnabled(isEnabled: Boolean): W3WMapWrapper

    /** A callback for when an existing marker on the map is clicked.
     *
     * @param callback it will be invoked when an existing marker on the map is clicked by the user.
     */
    fun onMarkerClicked(callback: Consumer<SuggestionWithCoordinates>): W3WMapWrapper

    /** Add [Suggestion] to the map. This method will add a marker/square to the map after getting the [Suggestion] from our W3WAutosuggestEditText.
     *
     * @param suggestion the [Suggestion] returned by our text/voice autosuggest component.
     * @param markerColor is the [W3WMarkerColor] for the [Suggestion] added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtSuggestion(
        suggestion: Suggestion,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add a list of [Suggestion] to the map. This method will add multiple markers/squares to the map after getting the suggestions from our W3WAutosuggestEditText.
     *
     * @param listSuggestions list of [Suggestion]s returned by our text/voice autosuggest component.
     * @param markerColor is the [W3WMarkerColor] for the suggestion added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtSuggestion(
        listSuggestions: List<Suggestion>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
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
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtSuggestion(
        suggestion: Suggestion,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add [Coordinates] to the map. This method will add a marker/square to the map based on each of the [Coordinates] provided latitude and longitude.
     *
     * @param coordinates [Coordinates] to be added.
     * @param markerColor is the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtCoordinates(
        coordinates: Coordinates,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add a list of [Coordinates] to the map. This method will add multiple markers/squares to the map based on the latitude and longitude of each [Coordinates] on the list.
     *
     * @param listCoordinates list of [Coordinates]s to be added.
     * @param markerColor is the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtCoordinates(
        listCoordinates: List<Coordinates>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Set [Coordinates] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param coordinates [Coordinates] to be added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtCoordinates(
        coordinates: Coordinates,
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

    /** Add a three word address to the map. This method will add a marker/square to the map if [words] are a valid three word address, e.g., filled.count.soap. If it's not a valid three word address, [onError] will be called returning [APIResponse.What3WordsError.BAD_WORDS].
     *
     * @param words three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add a list of three word addresses to the map. This method will add a marker/square to the map if all [listWords] are a valid three word addresses, e.g., filled.count.soap. If any valid three word address is not valid, [onError] will be called returning [APIResponse.What3WordsError.BAD_WORDS].
     *
     * @param listWords list of three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [listWords] added.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Set [words] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param words three word address to be added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtWords(
        words: String,
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

    /** Remove all markers from the map. */
    fun removeAllMarkers()

    /** Get all added [SuggestionWithCoordinates] from the map.
     *
     * @return list of [SuggestionWithCoordinates] with all items added to the map.
     */
    fun getAllMarkers(): List<SuggestionWithCoordinates>

    /** Remove selected marker from the map. */
    fun unselect()

    /** This method should be called on [GoogleMap.setOnCameraIdleListener] or [MapboxMap.addOnMapIdleListener].
     * This will allow to refresh the grid bounds on camera idle.
     */
    fun updateMap()

    /** This method should be called on [GoogleMap.setOnCameraMoveListener] or [MapboxMap.addOnCameraChangeListener].
     * This will allow to swap from markers to squares and show/hide grid when zoom goes higher or lower than the [W3WGoogleMapsWrapper.ZOOM_SWITCH_LEVEL] or [W3WMapBoxWrapper.ZOOM_SWITCH_LEVEL] threshold.
     */
    fun updateMove()

}
