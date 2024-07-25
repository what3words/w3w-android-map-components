package com.what3words.components.maps.wrappers

import androidx.core.util.Consumer
import com.google.android.gms.maps.GoogleMap
import com.mapbox.maps.MapboxMap
import com.what3words.androidwrapper.datasource.text.api.error.BadWordsError
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.views.W3WMap
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language

enum class GridColor {
    AUTO,
    DARK,
    LIGHT
}

/** What3words abstract map wrapper interface, every new map provider wrapper added to the component should implement this interface. */
interface W3WMapWrapper {

    /** Set the language of [W3WAddress.words] that onSuccess callbacks should return.
     *
     * @param language a supported [W3WRFC5646Language]. Defaults to en (English).
     */
    fun setLanguage(language: W3WRFC5646Language): W3WMapWrapper

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
    fun onMarkerClicked(callback: Consumer<W3WAddress>): W3WMapWrapper

    /** Add [W3WAddress] to the map.
     * This method will add a marker to the map after getting the [W3WAddress.square]
     * from our AutosuggestEditText or OCR.
     *
     * @param address the [W3WAddress] returned by our text/voice/OCR component.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the map.
     * @param onError is called if there was an error [W3WError] adding the [W3WAddress.square] to the map.
     */
    fun addMarkerAtAddress(
        address: W3WAddress,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Add a list of [W3WAddress]'s from our text/voice/ocr component to the map.
     *
     * @param listAddresses the list of [W3WAddress]'s returned by our text/voice/ocr component.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the map.
     * @param onError is called if there was an error [W3WError] adding any of the [W3WAddress.square]'s to the map.
     */
    fun addMarkerAtAddress(
        listAddresses: List<W3WAddress>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Set a [W3WAddress] as a selected square. Only one selected square is allowed at a time.
     *
     * @param address [W3WAddress] to be selected.
     * @param onSuccess a success callback will return the same [W3WAddress].
     * @param onError is called if there was an error [W3WError] selecting the [W3WAddress.square] on the map.
     */
    fun selectAtAddress(
        address: W3WAddress,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Remove marker at [W3WAddress.square] from the map.
     *
     * @param address the [W3WAddress] to remove.
     */
    fun removeMarkerAtAddress(address: W3WAddress)

    /** Remove markers at all [W3WAddress.square]'s from the map.
     *
     * @param listAddresses the list of [W3WAddress.square] to remove.
     */
    fun removeMarkerAtAddress(listAddresses: List<W3WAddress>)

    /** Add [W3WSuggestion] from our text/voice/ocr component to the map.
     * This method will add a marker/square to the map after converting [W3WSuggestion] to
     * [W3WAddress.square].
     *
     * @param suggestion the [W3WSuggestion] returned by our text/voice/ocr component.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the map.
     * @param onError is called if there was an error [W3WError] adding the [W3WAddress.square] to the map.
     */
    fun addMarkerAtSuggestion(
        suggestion: W3WSuggestion,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Add a list of [W3WSuggestion]'s from our text/voice/ocr component to the map.
     * This method will add ALL markers/squares to the map after converting [W3WSuggestion] to
     * [W3WAddress.square], if one failed none will be added.
     *
     * @param listSuggestions the list of [W3WSuggestion]'s returned by our text/voice/ocr component.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the map.
     * @param onError is called if there was an error [W3WError] adding any of the [W3WAddress.square]'s to the map.
     */
    fun addMarkerAtSuggestion(
        listSuggestions: List<W3WSuggestion>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Remove marker at [W3WSuggestion.w3wAddress.words][W3WAddress.words] from the map.
     *
     * @param suggestion the [W3WSuggestion] to remove.
     */
    fun removeMarkerAtSuggestion(suggestion: W3WSuggestion)

    /** Remove markers at all [W3WSuggestion.w3wAddress.words][W3WAddress.words]'s from the map.
     *
     * @param listSuggestions the list of [W3WSuggestion.w3wAddress.words][W3WAddress.words] to remove.
     */
    fun removeMarkerAtSuggestion(listSuggestions: List<W3WSuggestion>)

    /** Set a [W3WSuggestion] as a selected square. Only one selected square is allowed at a time.
     *
     * @param suggestion [W3WSuggestion] to be selected.
     * @param onSuccess a success callback will return [W3WAddress], including coordinates.
     * @param onError is called if there was an error [W3WError] selecting the [W3WAddress.square] on the map.
     */
    fun selectAtSuggestion(
        suggestion: W3WSuggestion,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Add marker at [W3WAddress.square] that contains coordinates [lat] latitude and [lng] longitude and add it to the map.
     *
     * @param lat latitude coordinates within [W3WAddress.square] to be added to the map.
     * @param lng longitude coordinates within [W3WAddress.square] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the map.
     * @param onError is called if there was an error [W3WError] adding the [W3WAddress.square]'s to the map.
     */
    fun addMarkerAtCoordinates(
        lat: Double,
        lng: Double,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Add markers at multiple [W3WAddress.square]'s that contain coordinates inside [listCoordinates] where [W3WCoordinates.lat] is latitude and [W3WCoordinates.lng] is longitude, and add them to map.
     *
     * @param listCoordinates list of coordinates  where [W3WCoordinates.lat] is latitude and [W3WCoordinates.lng] is longitude within the [W3WAddress.square] to add.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if ALL markers at [W3WAddress.square] are added successfully to the the map.
     * @param onError is called if there was an error [W3WError] adding any of the [W3WAddress.square]'s to the map.
     */
    fun addMarkerAtCoordinates(
        listCoordinates: List<W3WCoordinates>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Select [W3WAddress.square] that contains coordinates [lat] latitude and [lng] longitude. Only one selected square is allowed at a time.
     *
     * @param lat coordinates latitude to be selected.
     * @param lng coordinates longitude to be selected.
     * @param onSuccess is called if [W3WAddress.square] was selected successfully.
     * @param onError is called if there was an [W3WError] selecting [W3WAddress.square] in the map.
     */
    fun selectAtCoordinates(
        lat: Double,
        lng: Double,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Finds a marker on the map strictly by [lat] and [lng].
     *
     * @param lat the latitude search query.
     * @param lng the longitude search query.
     * @return if a marker on the map matches the search query [W3WAddress] will be returned. If not, it will return null.
     */
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

    /** Remove markers based on [listCoordinates] which [W3WCoordinates.lat] is latitude, [W3WCoordinates.lng] is longitude of the marker in the map.
     *
     * @param listCoordinates list of [W3WCoordinates.lat] is latitude, [W3WCoordinates.lng] is longitude coordinates of the markers to be removed.
     */
    fun removeMarkerAtCoordinates(listCoordinates: List<W3WCoordinates>)

    /** Add a what3words address to the map. This method will add a marker/square to the map if [words] are a valid what3words address, e.g., filled.count.soap. If it's not a valid what3words address, [onError] will be called returning [BadWordsError].
     *
     * @param words what3words address to be added.
     * @param markerColor the [W3WMarkerColor] for the [words] added.
     * @param onSuccess an success callback will return a [W3WAddress] with all the what3words info needed for those coordinates.
     * @param onError an error callback, will return a [W3WError] that will have the error type and message.
     */
    fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Add a list of what3words addresses to the map. This method will add a marker/square to the map if all [listWords] are a valid what3words addresses, e.g., filled.count.soap. If any valid what3words address is not valid, [onError] will be called returning [BadWordsError].
     *
     * @param listWords list of what3words addresses to be added.
     * @param markerColor the [W3WMarkerColor] for the [listWords] added.
     * @param onSuccess an success callback will return a [W3WAddress] with all the what3words info needed for those coordinates.
     * @param onError an error callback, will return a [W3WError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor = W3WMarkerColor.RED,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Set [words] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param words what3words address to be added.
     * @param onSuccess the success callback will return a [W3WAddress] that will have all the what3words address info plus coordinates.
     * @param onError the error callback, will return a [W3WError] that will have the error type and message.
     */
    fun selectAtWords(
        words: String,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null
    )

    /** Remove what3words address from the map.
     *
     * @param words the what3words address to be removed.
     */
    fun removeMarkerAtWords(words: String)

    /** Remove a list of what3words addresses from the map.
     *
     * @param listWords the list of what3words address to remove.
     */
    fun removeMarkerAtWords(listWords: List<String>)

    /** Remove all markers from the map. */
    fun removeAllMarkers()

    /** Get all added [W3WAddress] from the map.
     *
     * @return list of [W3WAddress] with all items added to the map.
     */
    fun getAllMarkers(): List<W3WAddress>

    /** Get the selected marker from the map.
     *
     * @return the selected [W3WAddress] from the map.
     */
    fun getSelectedMarker(): W3WAddress?

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
