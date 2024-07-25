package com.what3words.components.maps.views

import androidx.core.util.Consumer
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.wrappers.GridColor
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.javawrapper.response.APIResponse
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
    fun setLanguage(language: W3WRFC5646Language)

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
     * @param onSuccess it will be invoked when an user clicks on the map. [W3WAddress] with all what3words info of the clicked square and [Boolean] will be true if the clicked square is added as a marker, false if not.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message.
     */
    fun onSquareSelected(
        onSuccess: SelectedSquareConsumer<W3WAddress, Boolean, Boolean>,
        onError: Consumer<W3WError>? = null
    )

    //region add/remove/select by W3WSuggestion

    /** Add [W3WSuggestion] to the map. This method will add a marker/square to the map after getting the [W3WSuggestion] from our autosuggest wrapper.
     *
     * @param suggestion the [W3WSuggestion] returned by our autosuggest wrapper.
     * @param markerColor is the [W3WMarkerColor] for the [W3WSuggestion] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess the success callback will return a [W3WAddress] that will have all the [W3WSuggestion] info plus coordinates.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message.
     */
    fun addMarkerAtSuggestion(
        suggestion: W3WSuggestion,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    )

    /** Add a list of [W3WSuggestion] to the map. This method will add multiple markers/squares to the map after getting the suggestions from our W3WAutosuggestEditText.
     *
     * @param listSuggestions list of [W3WSuggestion]s returned by our text/voice autosuggest component.
     * @param markerColor the [W3WMarkerColor] for the [listSuggestions] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [W3WAddress] that will have all the [W3WSuggestion] info plus coordinates.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtSuggestion(
        listSuggestions: List<W3WSuggestion>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Remove [W3WSuggestion] from the map.
     *
     * @param suggestion the [W3WSuggestion] to be removed.
     */
    fun removeMarkerAtSuggestion(suggestion: W3WSuggestion)

    /** Remove [W3WSuggestion]s from the map.
     *
     * @param listSuggestions the list of [W3WSuggestion]s to remove.
     */
    fun removeMarkerAtSuggestion(listSuggestions: List<W3WSuggestion>)

    /** Set [W3WSuggestion] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param suggestion the [W3WSuggestion] returned by our text/voice autosuggest component.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [W3WAddress] that will have all the [W3WSuggestion] info plus coordinates.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message.
     */
    fun selectAtSuggestion(
        suggestion: W3WSuggestion,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    )

    //endregion

    //region add/remove/select by W3WAddress
    /** Add [W3WAddress] to the map. This method will add a marker/square to the map after getting the [W3WAddress] from our W3WAutosuggestEditText.
     *
     * @param address the [W3WAddress] returned by our text/voice autosuggest component.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess the success callback will return a [W3WAddress] that will have all the [W3WAddress] info with coordinates.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message.
     */
    fun addMarkerAtAddress(
        address: W3WAddress,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    )

    /** Add a list of [W3WAddress] to the map. This method will add multiple markers/squares to the map after getting the addresses from our W3WAutosuggestEditText.
     *
     * @param listAddresses list of [W3WAddress] returned by our text/voice autosuggest component.
     * @param markerColor the [W3WMarkerColor] for the [listAddresses] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a list of [W3WAddress] that will have all the [W3WAddress] info plus coordinates.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtAddress(
        listAddresses: List<W3WAddress>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Set [W3WAddress] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param address the [W3WAddress] returned by our text/voice autosuggest component.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [W3WAddress] that will have all the [W3WAddress] info plus coordinates.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message.
     */
    fun selectAtAddress(
        address: W3WAddress,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    )

    /** Remove a marker from square [W3WAddress.square]
     *
     * @param address [W3WAddress] to remove marker from.
     */
    fun removeMarkerAtAddress(address: W3WAddress)

    /** Remove markers from squares [W3WAddress.square]
     *
     * @param listAddresses list of [W3WAddress] to remove markers from.
     */
    fun removeMarkerAtAddress(listAddresses: List<W3WAddress>)

    //endregion

    //region add/remove/select by words

    /** Add a three word address to the map. This method will add a marker/square to the map if [words] are a valid three word address, e.g., filled.count.soap. If it's not a valid three word address, [onError] will be called returning [BadWordsError].
     *
     * @param words three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [words] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess an success callback will return a [W3WAddress] with all the what3words and coordinates info needed for those [words].
     * @param onError an error callback, will return a [W3WError] that will have the error type and message.
     */
    fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    )

    /** Add a list of three word addresses to the map. This method will add a marker/square to the map if all [listWords] are a valid three word addresses, e.g., filled.count.soap. If any valid three word address is not valid, [onError] will be called returning [BadWordsError].
     *
     * @param listWords, list of three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [listWords] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess an success callback will return a list of [W3WAddress] with all the what3words and coordinate info for all added [listWords].
     * @param onError an error callback, will return a [W3WError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Set [words] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param words three word address to be added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [W3WAddress] that will have all the [W3WAddress] info plus coordinates.
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectAtWords(
        words: String,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
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

    //endregion

    //region add/remove/select by coordinates

    /** Add marker at [lat], [lng] coordinates to the map. This method will add a marker/square to the map based on each of the coordinates provided latitude and longitude.
     *
     * @param lat latitude coordinates to be added.
     * @param lng longitude coordinates to be added.
     * @param markerColor the [W3WMarkerColor] for the marker at [lat], [lng] added.
     * @param zoomOption the zoom option for this marker, by default will center and zoom, if not desired please use [W3WZoomOption.NONE] or [W3WZoomOption.CENTER]
     * @param onSuccess an success callback will return a [W3WAddress] with all the what3words info needed for those coordinates.
     * @param onError an error callback, will return a [W3WError] that will have the error type and message.
     */
    fun addMarkerAtCoordinates(
        lat: Double,
        lng: Double,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    )

    /** Add list of Coordinates with [W3WCoordinates.lat] as latitude, [W3WCoordinates.lng] as longitude to add to the map. This method will add multiple markers/squares to the map based on the latitude and longitude of each [W3WCoordinates] on the list.
     *
     * @param listCoordinates list of Coordinates which [W3WCoordinates.lat] is latitude and [W3WCoordinates.lng] is longitude of the [W3WAddress] to add to the map.
     * @param markerColor the [W3WMarkerColor] for the [listCoordinates] added.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess an success callback will return a list of [W3WCoordinates] with all the what3words and coordinate info for all added [listCoordinates].
     * @param onError an error callback, will return a [W3WError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtCoordinates(
        listCoordinates: List<W3WCoordinates>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Set [lat], [lng] coordinates as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param lat latitude coordinates to be selected.
     * @param lng longitude coordinates to be selected.
     * @param zoomOption the zoom option for these markers, by default will center and zoom to show all added, if not desired please use [W3WZoomOption.NONE]
     * @param onSuccess the success callback will return a [W3WAddress] that will have all the what3words address info plus coordinates.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message.
     */
    fun selectAtCoordinates(
        lat: Double,
        lng: Double,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    )

    fun findMarkerByCoordinates(
        lat: Double,
        lng: Double
    ): W3WAddress?

    /** Remove marker at [lat],[lng] from the map.
     *
     * @param lat latitude coordinates of the marker to be removed.
     * @param lng longitude coordinates of the marker to be removed.
     */
    fun removeMarkerAtCoordinates(lat: Double, lng: Double)

    /** Remove markers based on [listCoordinates] which [W3WCoordinates.lat] is latitude and [W3WCoordinates.lng] is longitude of the [W3WAddress] to remove from the map.
     *
     * @param listCoordinates list of Coordinates which [W3WCoordinates.lat] is latitude and [W3WCoordinates.lng] is longitude of the [W3WAddress] to remove from the map.
     */
    fun removeMarkerAtCoordinates(listCoordinates: List<W3WCoordinates>)

    /** Remove all markers from the map. */
    fun removeAllMarkers()

    /** Get all added [W3WAddress] from the map.
     *
     * @return list of [W3WAddress] with all items added to the map.
     */
    fun getAllMarkers(): List<W3WAddress>

    /** Get selected marker from the map.
     *
     * @return [W3WAddress] of the selected marker, if non currently selected it will return null.
     */
    fun getSelectedMarker(): W3WAddress?

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