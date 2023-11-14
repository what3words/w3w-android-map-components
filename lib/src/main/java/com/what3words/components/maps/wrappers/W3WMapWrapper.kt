package com.what3words.components.maps.wrappers

import androidx.core.util.Consumer
import com.google.android.gms.maps.GoogleMap
import com.mapbox.maps.MapboxMap
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.components.maps.models.SuggestionWithCoordinatesAndStyle
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.views.W3WMap
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates

enum class GridColor {
    AUTO,
    DARK,
    LIGHT
}

/** What3words abstract map wrapper interface, every new map provider wrapper added to the component should implement this interface. */
interface W3WMapWrapper {

    /** Set the language of [SuggestionWithCoordinates.words] that onSuccess callbacks should return.
     *
     * @param language a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).
     */
    fun setLanguage(language: String): W3WMapWrapper

    /** Set zoom switch level. If the map zoom level is lower than [zoom], it will not show the grid; if the map zoom is higher or equal to [zoom], it will show the grid.
     *
     * @param zoom the zoom level to turn the grid visibility on and off.
     */
    fun setZoomSwitchLevel(zoom: Float)

    /** Get zoom switch level.
     *
     * @return the zoom level that defines the grid visibility.
     */
    fun getZoomSwitchLevel() : Float

    /** Due to different map providers setting Dark/Light modes differently, i.e., GoogleMaps sets dark mode using JSON styles, but Mapbox sets the dark mode as a MapType, this will allow you to control the colour of the what3words 3x3m grid.
     *
     * [GridColor.AUTO] - Will leave it up to the library to decide which Grid colour and selected square colour to match some specific map types, i.e., use [GridColor.DARK] on standard map types, [GridColor.LIGHT] on Satellite and Traffic map types.
     * [GridColor.LIGHT] - Will force the grid and the selected square colour to be light.
     * [GridColor.DARK] - Will force the grid and the selected square colour to be dark.
     *
     * @param gridColor set grid the colour. Per default, it will be [GridColor.AUTO].
     */
    fun setGridColor(gridColor: GridColor)

    /** Enable grid overlay over the map with all 3mx3m squares on the visible map bounds.
     *
     * @param isEnabled turn grid on or off, enabled by default.
     */
    fun gridEnabled(isEnabled: Boolean): W3WMapWrapper

    /** A callback for when an existing marker on the map is clicked.
     *
     * @param callback will be invoked when the user clicks an existing marker on the map.
     */
    fun onMarkerClicked(callback: Consumer<SuggestionWithCoordinates>): W3WMapWrapper

    /** Add [SuggestionWithCoordinates] to the map.
     * This method will add a marker to the map after getting the [SuggestionWithCoordinates.square]
     * from our AutosuggestEditText or OCR.
     *
     * @param suggestion the [SuggestionWithCoordinates] returned by our text/voice/OCR component.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if the marker at [SuggestionWithCoordinates.square] is added successfully to the map.
     * @param onError is called if there was an error [APIResponse.What3WordsError] adding the [SuggestionWithCoordinates.square] to the map.
     */
    fun addMarkerAtSuggestionWithCoordinates(
        suggestion: SuggestionWithCoordinates,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Set a [SuggestionWithCoordinates] as a selected square. Only one selected square is allowed at a time.
     *
     * @param suggestion [SuggestionWithCoordinates] to be selected.
     * @param onSuccess a success callback will return the same [SuggestionWithCoordinates].
     * @param onError is called if there was an error [APIResponse.What3WordsError] selecting the [SuggestionWithCoordinates.square] on the map.
     */
    fun selectAtSuggestionWithCoordinates(
        suggestion: SuggestionWithCoordinates,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add [Suggestion] from our text/voice/ocr component to the map.
     * This method will add a marker/square to the map after converting [Suggestion] to
     * [SuggestionWithCoordinates.square].
     *
     * @param suggestion the [Suggestion] returned by our text/voice/ocr component.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if the marker at [SuggestionWithCoordinates.square] is added successfully to the map.
     * @param onError is called if there was an error [APIResponse.What3WordsError] adding the [SuggestionWithCoordinates.square] to the map.
     */
    fun addMarkerAtSuggestion(
        suggestion: Suggestion,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add a list of [Suggestion]'s from our text/voice/ocr component to the map.
     * This method will add ALL markers/squares to the map after converting [Suggestion] to
     * [SuggestionWithCoordinates.square], if one failed none will be added.
     *
     * @param listSuggestions the list of [Suggestion]'s returned by our text/voice/ocr component.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if the marker at [SuggestionWithCoordinates.square] is added successfully to the map.
     * @param onError is called if there was an error [APIResponse.What3WordsError] adding any of the [SuggestionWithCoordinates.square]'s to the map.
     */
    fun addMarkerAtSuggestion(
        listSuggestions: List<Suggestion>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Remove marker at [Suggestion.words] from the map.
     *
     * @param suggestion the [Suggestion] to remove.
     */
    fun removeMarkerAtSuggestion(suggestion: Suggestion)

    /** Remove markers at all [Suggestion.words]'s from the map.
     *
     * @param listSuggestions the list of [Suggestion.words] to remove.
     */
    fun removeMarkerAtSuggestion(listSuggestions: List<Suggestion>)

    /** Set a [Suggestion] as a selected square. Only one selected square is allowed at a time.
     *
     * @param suggestion [Suggestion] to be selected.
     * @param onSuccess a success callback will return [SuggestionWithCoordinates], including coordinates.
     * @param onError is called if there was an error [APIResponse.What3WordsError] selecting the [SuggestionWithCoordinates.square] on the map.
     */
    fun selectAtSuggestion(
        suggestion: Suggestion,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add marker at [SuggestionWithCoordinates.square] that contains coordinates [lat] latitude and [lng] longitude and add it to the map.
     *
     * @param lat latitude coordinates within [SuggestionWithCoordinates.square] to be added to the map.
     * @param lng longitude coordinates within [SuggestionWithCoordinates.square] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if the marker at [SuggestionWithCoordinates.square] is added successfully to the map.
     * @param onError is called if there was an error [APIResponse.What3WordsError] adding the [SuggestionWithCoordinates.square]'s to the map.
     */
    fun addMarkerAtCoordinates(
        lat: Double,
        lng: Double,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add markers at multiple [SuggestionWithCoordinates.square]'s that contain coordinates inside [listCoordinates] where [Pair.first] is latitude and [Pair.second] is longitude, and add them to map.
     *
     * @param listCoordinates list of coordinates where [Pair.first] is latitude and [Pair.second] is longitude within the [SuggestionWithCoordinates.square] to add.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if ALL markers at [SuggestionWithCoordinates.square] are added successfully to the the map.
     * @param onError is called if there was an error [APIResponse.What3WordsError] adding any of the [SuggestionWithCoordinates.square]'s to the map.
     */
    fun addMarkerAtCoordinates(
        listCoordinates: List<Pair<Double, Double>>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Select [SuggestionWithCoordinates.square] that contains coordinates [lat] latitude and [lng] longitude. Only one selected square is allowed at a time.
     *
     * @param lat coordinates latitude to be selected.
     * @param lng coordinates longitude to be selected.
     * @param onSuccess is called if [SuggestionWithCoordinates.square] was selected successfully.
     * @param onError is called if there was an [APIResponse.What3WordsError] selecting [SuggestionWithCoordinates.square] in the map.
     */
    fun selectAtCoordinates(
        lat: Double,
        lng: Double,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Finds a marker on the map strictly by [lat] and [lng].
     *
     * @param lat the latitude search query.
     * @param lng the longitude search query.
     * @return if a marker on the map matches the search query [SuggestionWithCoordinates] will be returned. If not, it will return null.
     */
    fun findMarkerByCoordinates(
        lat: Double,
        lng: Double
    ): SuggestionWithCoordinates?

    /** Remove marker at [lat],[lng] from the map.
     *
     * @param lat latitude coordinates of the marker to be removed.
     * @param lng longitude coordinates of the marker to be removed.
     */
    fun removeMarkerAtCoordinates(lat: Double, lng: Double)

    /** Remove markers based on [listCoordinates] which [Pair.first] is latitude, [Pair.second] is longitude of the marker in the map.
     *
     * @param listCoordinates list of [Pair.first] latitude, [Pair.second] longitude coordinates of the markers to be removed.
     */
    fun removeMarkerAtCoordinates(listCoordinates: List<Pair<Double, Double>>)

    /** Add a three word address to the map. This method will add a marker/square to the map if [words] are a valid three word address, e.g., filled.count.soap. If it's not a valid three word address, [onError] will be called returning [APIResponse.What3WordsError.BAD_WORDS].
     *
     * @param words three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [words] added.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those coordinates.
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
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those coordinates.
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
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus coordinates.
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

    /** Get all added [SuggestionWithCoordinates] from the map.
     *
     * @return list of [SuggestionWithCoordinates] with all items added to the map.
     */
    fun getSelectedMarker(): SuggestionWithCoordinates?

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

    /** Set [W3WMap.MapType] of the map, these are the available options due to multiple map provide compatibility:
     * [W3WMap.MapType.NORMAL], [W3WMap.MapType.TERRAIN], [W3WMap.MapType.HYBRID] and [W3WMap.MapType.SATELLITE].
     *
     * @param mapType the [W3WMap.MapType] to be applied to the map.
     */
    fun setMapType(mapType: W3WMap.MapType)

    /** Get current [W3WMap.MapType] from the map, these are the available options due to multiple map provide compatibility:
     * [W3WMap.MapType.NORMAL], [W3WMap.MapType.TERRAIN], [W3WMap.MapType.HYBRID] and [W3WMap.MapType.SATELLITE].
     *
     * @returns the [W3WMap.MapType] currently applied to the map.
     */
    fun getMapType() : W3WMap.MapType

    /** Set the map to Dark/Night or Light/Day mode, to maintain compatibility with different map providers, we either handle setting
     * as a MapType internally (i.e. Mapbox) or we apply a default JSON style or the one provided with [customJsonStyle] (i.e. GoogleMaps).
     *
     * @param darkMode true if should be using dark mode, false should be using day mode.
     */
    fun setDarkMode(darkMode: Boolean, customJsonStyle: String?)

    /** Check if the map is currently presenting in Dark/Night or Light/Day mode.
     *
     * @returns true if map is currently using dark mode, false if using day mode.
     */
    fun isDarkMode(): Boolean
}
