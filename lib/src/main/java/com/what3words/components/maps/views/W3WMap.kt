package com.what3words.components.maps.views

import androidx.core.util.Consumer
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.wrappers.GridColor
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates

@FunctionalInterface
fun interface SelectedSquareConsumer<T, U, V> {
    fun accept(square: T, selectedByTouch: U, isMarked: V)
}

/** [W3WMap] A basic set of functions common to all W3W map objects */
interface W3WMap {
    enum class MapType {
        NORMAL,
        HYBRID,
        TERRAIN,
        SATELLITE
    }

    /** Set the language of [SuggestionWithCoordinates.words] that onSuccess callbacks should return.
     *
     * @param language a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).
     */
    fun setLanguage(language: String)

    /** Due to different map providers setting Dark/Light modes differently i.e: GoogleMaps sets dark mode using JSON styles but Mapbox has dark mode as a MapType.
     *
     * [GridColor.AUTO] - Will leave up to the library to decide which Grid color and selected square color to use to match some specific map types, i.e: use [GridColor.DARK] on normal map types, [GridColor.LIGHT] on Satellite and Traffic map types.
     * [GridColor.LIGHT] - Will force grid and selected square color to be light.
     * [GridColor.DARK] - Will force grid and selected square color to be dark.
     *
     * @param gridColor set grid color, per default will be [GridColor.AUTO].
     */
    fun setGridColor(gridColor: GridColor)

    /** A callback for when a square is selected either by clicking on the map or programmatically using any of [selectAtWords], [selectAtCoordinates], [selectAtSuggestion].
     *
     * @param onSuccess it will be invoked when an user clicks on the map. [SuggestionWithCoordinates] with all what3words info of the clicked square and [Boolean] will be true if the clicked square is added as a marker, false if not.
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun onSquareSelected(
        onSuccess: SelectedSquareConsumer<SuggestionWithCoordinates, Boolean, Boolean>,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Add [Suggestion] to the map. This method will add a marker/square to the map after getting the [Suggestion] from our autosuggest wrapper.
     *
     * @param suggestion the [Suggestion] returned by our autosuggest wrapper.
     * @param markerColor is the [W3WMarkerColor] for the [Suggestion] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus coordinates.
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtSuggestion(
        suggestion: Suggestion,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null,
        zoomLevel: Float? = null
    )

    /** Add [SuggestionWithCoordinates] to the map. This method will add a marker/square to the map after getting the [Suggestion] from our W3WAutosuggestEditText.
     *
     * @param suggestion the [SuggestionWithCoordinates] returned by our text/voice autosuggest component.
     * @param markerColor is the [W3WMarkerColor] for the [Suggestion] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus coordinates.
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtSquare(
        suggestion: SuggestionWithCoordinates,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null,
        zoomLevel: Float? = null
    )

    /** Set [SuggestionWithCoordinates] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param square the [SuggestionWithCoordinates] returned by our text/voice autosuggest component.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus coordinates.
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtSquare(
        square: SuggestionWithCoordinates,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null,
        zoomLevel: Float? = null
    )

    /** Remove a marker from square [SuggestionWithCoordinates]
     *
     * @param square [SuggestionWithCoordinates] to remove marker from.
     */
    fun removeMarkerAtSquare(square: SuggestionWithCoordinates)


    /** Add a list of [Suggestion] to the map. This method will add multiple markers/squares to the map after getting the suggestions from our W3WAutosuggestEditText.
     *
     * @param listSuggestions list of [Suggestion]s returned by our text/voice autosuggest component.
     * @param markerColor the [W3WMarkerColor] for the [listSuggestions] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus coordinates.
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
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus coordinates.
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtSuggestion(
        suggestion: Suggestion,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null,
        zoomLevel: Float? = null
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
        onError: Consumer<APIResponse.What3WordsError>? = null,
        zoomLevel: Float? = null
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
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus coordinates.
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtWords(
        words: String,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null,
        zoomLevel: Float? = null
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

    /** Add marker at [lat], [lng] coordinates to the map. This method will add a marker/square to the map based on each of the cordinates provided latitude and longitude.
     *
     * @param lat latitude coordinates to be added.
     * @param lng longitude coordinates to be added.
     * @param markerColor the [W3WMarkerColor] for the marker at [lat], [lng] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those coordinates.
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addMarkerAtCoordinates(
        lat: Double,
        lng: Double,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null,
        zoomLevel: Float? = null
    )

    /** Add list of Coordinates [Pair.first] latitude, [Pair.second] longitude to the map. This method will add multiple markers/squares to the map based on the latitude and longitude of each [Coordinates] on the list.
     *
     * @param listCoordinates list of [Pair.first] latitude, [Pair.second] longitude coordinates to be added.
     * @param markerColor the [W3WMarkerColor] for the [listCoordinates] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess an success callback will return a list of [SuggestionWithCoordinates] with all the what3words and coordinate info for all added [listCoordinates].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtCoordinates(
        listCoordinates: List<Pair<Double, Double>>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null
    )

    /** Set [lat], [lng] coordinates as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param lat latitude coordinates to be selected.
     * @param lng longitude coordinates to be selected.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus coordinates.
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtCoordinates(
        lat: Double,
        lng: Double,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<SuggestionWithCoordinates>? = null,
        onError: Consumer<APIResponse.What3WordsError>? = null,
        zoomLevel: Float? = null
    )

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

    /** Remove all markers from the map. */
    fun removeAllMarkers()

    /** Get all added [SuggestionWithCoordinates] from the map.
     *
     * @return list of [SuggestionWithCoordinates] with all items added to the map.
     */
    fun getAllMarkers(): List<SuggestionWithCoordinates>

    /** Get selected marker from the map.
     *
     * @return [SuggestionWithCoordinates] of the selected marker, if non currently selected it will return null.
     */
    fun getSelectedMarker(): SuggestionWithCoordinates?

    /** Remove selected marker from the map. */
    fun unselect()

    /** Moves map camera to position without animations.
     *
     * @param latitude is the latitude of the map camera to move to.
     * @param longitude is the latitude of the map camera to move to.
     * @param zoom is the zoom level of the map camera to move to.
     */
    fun moveToPosition(latitude: Double, longitude: Double, zoom: Double)

    /** Moves map camera to position with animation.
     *
     * @param latitude is the latitude of the map camera to move to.
     * @param longitude is the latitude of the map camera to move to.
     * @param zoom is the zoom level of the map camera to move to.
     * @param onFinished a callback invoked when the camera animated move finishes.
     */
    fun animateToPosition(
        latitude: Double,
        longitude: Double,
        zoom: Double,
        onFinished: () -> Unit
    )

    /** Enable and disable map gestures.
     *
     * @param enabled turn map gestures on or off.
     */
    fun setMapGesturesEnabled(enabled: Boolean)

    /** Change map camera to 0 bearing. */
    fun orientCamera()

    /** Enable or disable my location feature for map providers that support it (i.e. blue dot in Google maps).
     *
     * @param enabled turn on or off my location feature (i.e. blue dot in Google maps)
     */
    fun setMyLocationEnabled(enabled: Boolean)

    /** Enable or disable the default my location button. The developer might want the current location icon on the map
     * but want to disable the default button to move to it.
     *
     * @param enabled turn my location button on or off.
     */
    fun setMyLocationButton(enabled: Boolean)

    /** Get current [W3WMap.MapType] from the map, these are the available options due to multiple map provide compatibility:
     * [W3WMap.MapType.NORMAL], [W3WMap.MapType.TERRAIN], [W3WMap.MapType.HYBRID] and [W3WMap.MapType.SATELLITE].
     *
     * @returns the [W3WMap.MapType] currently applied to the map.
     */
    fun getMapType(): MapType

    /** Set [W3WMap.MapType] of the map, these are the available options due to multiple map provide compatibility:
     * [W3WMap.MapType.NORMAL], [W3WMap.MapType.TERRAIN], [W3WMap.MapType.HYBRID] and [W3WMap.MapType.SATELLITE].
     *
     * @param mapType the [W3WMap.MapType] to be applied to the map.
     */
    fun setMapType(mapType: MapType)

    /** Check if the map is currently presenting in Dark/Night or Light/Day mode.
     *
     * @returns true if map is currently using dark mode, false if using day mode.
     */
    fun isDarkMode(): Boolean

    /** Set the map to Dark/Night or Light/Day mode, to maintain compatibility with different map providers, we either handle setting
     * as a MapType internally (i.e. Mapbox) or we apply a default JSON style or the one provided with [customJsonStyle] (i.e. GoogleMaps).
     *
     * @param darkMode true if should be using dark mode, false should be using day mode.
     */
    fun setDarkMode(darkMode: Boolean, customJsonStyle: String? = null)

    /** Checks if Grid is currently rendered on top of the map.
     *
     * @return true if grid is rendered on the map, false if not.
     */
    fun isMapAtGridLevel(): Boolean

    /** Set zoom switch level. If the map zoom level is lower than [zoom], it will not show the grid; if the map zoom is higher or equal to [zoom], it will show the grid.
     *
     * @param zoom the zoom level to turn the grid visibility on and off.
     */
    fun setZoomSwitchLevel(zoom: Float)

    /** Get zoom switch level.
     *
     * @return the zoom level that defines the grid visibility.
     */
    fun getZoomSwitchLevel(): Float

    /** Get Map current target, the center position (lat/lng) of the camera.
     *
     * @return the current target of them map camera where [Pair.first] is latitude and [Pair.second] is longitude.
     */
    val target: Pair<Double, Double>

    /** Get Map current zoom level.
     *
     * @return the current zoom level of them map camera.
     */
    val zoom: Float
}