package com.what3words.components.maps.wrappers

import androidx.core.util.Consumer
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.components.maps.extensions.contains
import com.what3words.components.maps.extensions.io
import com.what3words.components.maps.extensions.main
import com.what3words.components.maps.extensions.toSuggestionWithCoordinates
import com.what3words.components.maps.models.SuggestionWithCoordinatesAndStyle
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * [W3WMapManager] an abstract layer to retrieve data from [What3WordsAndroidWrapper] save it in memory [suggestionsCached] and be used in multiple map wrapper. i.e: [W3WGoogleMapsWrapper].
 **
 * @param wrapper source of what3words data can be API or SDK.
 * @param w3WMapWrapper the [W3WMapWrapper] that this manager will add/remove/select squares from.
 * @param dispatchers for custom dispatcher handler using [DefaultDispatcherProvider] by default.
 */
internal class W3WMapManager(
    private val wrapper: What3WordsAndroidWrapper,
    private val w3WMapWrapper: W3WMapWrapper,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    internal var selectedSuggestion: SuggestionWithCoordinates? = null
    internal val suggestionsCached: MutableList<SuggestionWithCoordinatesAndStyle> by lazy {
        mutableListOf()
    }

    internal val suggestionsRemoved: MutableList<SuggestionWithCoordinatesAndStyle> by lazy {
        mutableListOf()
    }

    internal var language: String = "en"

    /** Add marker at [SuggestionWithCoordinates.square] that contains coordinates [lat] latitude and [lng] longitude and add it to [suggestionsCached].
     *
     * @param lat latitude coordinates within [SuggestionWithCoordinates.square] to be added to the map.
     * @param lng longitude coordinates within [SuggestionWithCoordinates.square] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if the marker at [SuggestionWithCoordinates.square] is added successfully to the [suggestionsCached] list.
     * @param onError is called if there was an [APIResponse.What3WordsError] with [What3WordsAndroidWrapper.convertTo3wa].
     */
    fun addCoordinates(
        lat: Double,
        lng: Double,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        main(dispatchers) {
            val c23wa = withContext(dispatchers.io()) {
                wrapper.convertTo3wa(
                    Coordinates(
                        lat,
                        lng
                    )
                ).language(language).execute()
            }
            if (c23wa.isSuccessful) {
                val withCoordinates = c23wa.toSuggestionWithCoordinates()
                val locationMapped = findByExactLocation(lat, lng)
                if (locationMapped != null) suggestionsCached.remove(locationMapped)
                suggestionsCached.add(
                    SuggestionWithCoordinatesAndStyle(
                        withCoordinates,
                        markerColor
                    )
                )
                onSuccess?.accept(withCoordinates)
                w3WMapWrapper.updateMap()
            } else {
                onError?.accept(c23wa.error)
            }
        }
    }

    /** Add marker at [SuggestionWithCoordinates.square] and add it to [suggestionsCached].
     *
     * @param suggestionWithCoordinates [SuggestionWithCoordinates] to be added.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if the marker at [SuggestionWithCoordinates.square] is added successfully to the [suggestionsCached] list.
     */
    fun addSuggestionWithCoordinates(
        suggestionWithCoordinates: SuggestionWithCoordinates,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?
    ) {
        io(dispatchers) {
            val locationMapped = findByExactLocation(
                suggestionWithCoordinates.coordinates.lat,
                suggestionWithCoordinates.coordinates.lng
            )
            if (locationMapped != null) suggestionsCached.remove(locationMapped)

            suggestionsCached.add(
                SuggestionWithCoordinatesAndStyle(
                    suggestionWithCoordinates,
                    markerColor
                )
            )
            main(dispatchers) {
                onSuccess?.accept(suggestionWithCoordinates)
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Set a [SuggestionWithCoordinates.square] as a selected square. Only one selected square is allowed at a time. This method will save [suggestion] in [selectedSuggestion] and call [W3WMapWrapper.updateMap] to reflect the changes.
     *
     * @param suggestion [SuggestionWithCoordinates] to be selected.
     * @param onSuccess a success callback will return the same [SuggestionWithCoordinates].
     */
    fun selectSuggestionWithCoordinates(
        suggestion: SuggestionWithCoordinates,
        onSuccess: Consumer<SuggestionWithCoordinates>?
    ) {
        main(dispatchers) {
            selectedSuggestion = suggestion
            onSuccess?.accept(suggestion)
            w3WMapWrapper.updateMap()
        }
    }

    /** Select [SuggestionWithCoordinates.square] that contains coordinates [lat] latitude and [lng] longitude. Only one selected square is allowed at a time. This method will call [What3WordsAndroidWrapper.convertTo3wa] and, if successful, will save [SuggestionWithCoordinates] in [selectedSuggestion].
     *
     * @param lat coordinates latitude to be selected.
     * @param lng coordinates longitude to be selected.
     * @param onSuccess is called if [SuggestionWithCoordinates.square] was selected successfully.
     * @param onError is called if there was an [APIResponse.What3WordsError] with [What3WordsAndroidWrapper.convertTo3wa].
     */
    fun selectCoordinates(
        lat: Double,
        lng: Double,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        main(dispatchers) {
            val c23wa = withContext(dispatchers.io()) {
                wrapper.convertTo3wa(
                    Coordinates(
                        lat,
                        lng
                    )
                ).language(language).execute()
            }
            if (c23wa.isSuccessful) {
                val withCoordinates = c23wa.toSuggestionWithCoordinates()
                selectedSuggestion = withCoordinates
                onSuccess?.accept(withCoordinates)
                w3WMapWrapper.updateMap()
            } else {
                onError?.accept(c23wa.error)
            }
        }
    }

    /** Add markers at multiple [SuggestionWithCoordinates.square]'s that contain coordinates inside [listCoordinates] where [Pair.first] is latitude and [Pair.second] is longitude, and add them to [suggestionsCached].
     *
     * @param listCoordinates list of coordinates where [Pair.first] is latitude and [Pair.second] is longitude within the [SuggestionWithCoordinates.square] to add.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if ALL markers at [SuggestionWithCoordinates.square] are added successfully to the [suggestionsCached] list.
     * @param onError is called if there was an [APIResponse.What3WordsError] in any of the [What3WordsAndroidWrapper.convertTo3wa] calls.
     */
    fun addCoordinates(
        listCoordinates: List<Pair<Double, Double>>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        runBlocking(dispatchers.io()) {
            val toBeAdded = mutableListOf<SuggestionWithCoordinatesAndStyle>()
            var error: APIResponse.What3WordsError? = null
            listCoordinates.forEach { location ->

                val c23wa =
                    wrapper.convertTo3wa(
                        Coordinates(
                            location.first,
                            location.second
                        )
                    ).language(language).execute()
                if (c23wa.isSuccessful) {
                    val withCoordinates = c23wa.toSuggestionWithCoordinates()
                    toBeAdded.add(
                        SuggestionWithCoordinatesAndStyle(
                            withCoordinates,
                            markerColor
                        )
                    )
                } else {
                    error = c23wa.error
                    return@forEach
                }
            }
            if (error != null) {
                main(dispatchers) {
                    onError?.accept(error!!)
                }
            } else {
                toBeAdded.forEach {
                    val existing = findByExactLocation(
                        it.suggestion.coordinates.lat,
                        it.suggestion.coordinates.lng
                    )
                    if (existing != null) {
                        suggestionsCached.remove(existing)
                    }
                    suggestionsCached.add(it)
                }
                main(dispatchers) {
                    onSuccess?.accept(toBeAdded.map { it.suggestion })
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Remove marker at [SuggestionWithCoordinates.square] from [suggestionsCached] that contains coordinates [lat] latitude and [lng] longitude.
     *
     * @param lat the latitude of the [SuggestionWithCoordinates.square] to be removed.
     * @param lng the longitude of the [SuggestionWithCoordinates.square] to be removed.
     */
    fun removeCoordinates(lat: Double, lng: Double) {
        main(dispatchers) {
            squareContains(lat, lng)?.let { toRemove ->
                suggestionsCached.remove(toRemove)
                suggestionsRemoved.add((toRemove))
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Remove markers at multiple [SuggestionWithCoordinates.square]'s from [suggestionsCached] that contain coordinates inside [listCoordinates] where [Pair.first] is latitude, and [Pair.second] is longitude.
     *
     * @param listCoordinates list of coordinates where [Pair.first] is latitude and [Pair.second] is longitude within the [SuggestionWithCoordinates.square] to remove.
     */
    fun removeCoordinates(listCoordinates: List<Pair<Double, Double>>) {
        runBlocking(dispatchers.io()) {
            listCoordinates.forEach {
                squareContains(it.first, it.second)?.let { toRemove ->
                    suggestionsRemoved.add(toRemove)
                }
            }
            if (suggestionsRemoved.isNotEmpty()) {
                suggestionsCached.removeAll(suggestionsRemoved)
                main(dispatchers) {
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Add marker at [SuggestionWithCoordinates.square] that [SuggestionWithCoordinates.words] are equal to [words] and add it to [suggestionsCached].
     *
     * @param words the what3words address of [SuggestionWithCoordinates.square] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if the marker at [SuggestionWithCoordinates.square] is added successfully to the [suggestionsCached] list.
     * @param onError is called if there was an [APIResponse.What3WordsError] with [What3WordsAndroidWrapper.convertToCoordinates].
     */
    fun addWords(
        words: String,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        main(dispatchers) {
            val c23wa =
                withContext(dispatchers.io()) { wrapper.convertToCoordinates(words).execute() }
            if (c23wa.isSuccessful) {
                val withCoordinates = c23wa.toSuggestionWithCoordinates()
                val locationMapped = suggestionsCached.firstOrNull {
                    it.suggestion.words == words
                }
                if (locationMapped != null) suggestionsCached.remove(locationMapped)
                suggestionsCached.add(
                    SuggestionWithCoordinatesAndStyle(
                        withCoordinates,
                        markerColor
                    )
                )
                onSuccess?.accept(withCoordinates)
                w3WMapWrapper.updateMap()
            } else {
                onError?.accept(c23wa.error)
            }
        }
    }

    /** Add markers at multiple [SuggestionWithCoordinates.square] that [SuggestionWithCoordinates.words] are equal to each what3words address inside [listWords] and add them to [suggestionsCached].
     *
     * @param listWords the list of what3words addresses of multiple [SuggestionWithCoordinates.square]'s to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [SuggestionWithCoordinates.square] added.
     * @param onSuccess is called if the marker at [SuggestionWithCoordinates.square] is added successfully to the [suggestionsCached] list.
     * @param onError is called if there was an [APIResponse.What3WordsError] with [What3WordsAndroidWrapper.convertToCoordinates].
     */
    fun addWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        main(dispatchers) {
            val toBeAdded = mutableListOf<SuggestionWithCoordinatesAndStyle>()
            var error: APIResponse.What3WordsError? = null
            listWords.forEach { words ->
                val c23wa = withContext(dispatchers.io()) {
                    wrapper.convertToCoordinates(words).execute()
                }
                if (c23wa.isSuccessful) {
                    val withCoordinates = c23wa.toSuggestionWithCoordinates()
                    toBeAdded.add(
                        SuggestionWithCoordinatesAndStyle(
                            withCoordinates,
                            markerColor
                        )
                    )
                } else {
                    error = c23wa.error
                    return@forEach
                }
            }
            if (error != null) {
                onError?.accept(error!!)
            } else {
                toBeAdded.forEach {
                    val existing = findByExactLocation(
                        it.suggestion.coordinates.lat,
                        it.suggestion.coordinates.lng
                    )
                    if (existing != null) {
                        suggestionsCached.remove(existing)
                    }
                    suggestionsCached.add(it)
                }
                onSuccess?.accept(toBeAdded.map { it.suggestion })
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Select [SuggestionWithCoordinates.square] that [SuggestionWithCoordinates.words] are equal to [words].
     * Only one selected square is allowed at a time.
     * This method will call [What3WordsAndroidWrapper.convertToCoordinates] and, if successful, will save [SuggestionWithCoordinates] in [selectedSuggestion].
     *
     * @param words what3words address to select.
     * @param onSuccess is called if [SuggestionWithCoordinates.square] was selected successfully.
     * @param onError is called if there was an [APIResponse.What3WordsError] with [What3WordsAndroidWrapper.convertToCoordinates].
     */
    fun selectWords(
        words: String,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        main(dispatchers) {
            val c23wa =
                withContext(dispatchers.io()) { wrapper.convertToCoordinates(words).execute() }
            if (c23wa.isSuccessful) {
                val withCoordinates = c23wa.toSuggestionWithCoordinates()
                selectedSuggestion = withCoordinates
                onSuccess?.accept(withCoordinates)
                w3WMapWrapper.updateMap()
            } else {
                onError?.accept(c23wa.error)
            }
        }
    }

    /** Clears [selectedSuggestion] and calls [W3WMapWrapper.updateMap] to reflect the changes */
    fun unselect() {
        selectedSuggestion = null
        main(dispatchers) {
            w3WMapWrapper.updateMap()
        }
    }

    /** Remove [SuggestionWithCoordinates.square] from [suggestionsCached] where [SuggestionWithCoordinates.words] are equal to [words].
     *
     * @param words the what3words address to remove.
     */
    fun removeWords(words: String) {
        main(dispatchers) {
            suggestionsCached.firstOrNull { it.suggestion.words == words }?.let { toRemove ->
                suggestionsCached.remove(toRemove)
                suggestionsRemoved.add(toRemove)
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Remove multiple [SuggestionWithCoordinates.square]'s from [suggestionsCached] where [SuggestionWithCoordinates.words] are equal to each what3words address inside [listWords]
     *
     * @param listWords the list of what3words addresses of multiple [SuggestionWithCoordinates.square]'s to remove from the map.
     */
    fun removeWords(listWords: List<String>) {
        main(dispatchers) {
            listWords.forEach { words ->
                suggestionsCached.firstOrNull { it.suggestion.words == words }?.let { toRemove ->
                    suggestionsRemoved.add(toRemove)
                }
            }
            if (suggestionsRemoved.isNotEmpty()) {
                suggestionsCached.removeAll(suggestionsRemoved)
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Remove all markers from the map. It will clear [suggestionsCached]. */
    fun clearList() {
        main(dispatchers) {
            suggestionsCached.forEach { data ->
                suggestionsRemoved.add(data)
            }
            if (suggestionsRemoved.isNotEmpty()) {
                suggestionsCached.removeAll(suggestionsRemoved)
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Finds a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached] strictly by [latitude] and [longitude].
     *
     * @param latitude the latitude search query.
     * @param longitude the longitude search query.
     * @return if the search query matches a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached], will return it. If not, it will return null.
     */
    internal fun findByExactLocation(
        latitude: Double,
        longitude: Double
    ): SuggestionWithCoordinatesAndStyle? {
        return suggestionsCached.firstOrNull { it.suggestion.coordinates.lat == latitude && it.suggestion.coordinates.lng == longitude }
    }

    /** Finds a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached] by checking if [latitude] and [longitude] are inside of any [SuggestionWithCoordinates.square].
     *
     * @param latitude is the latitude to search for.
     * @param longitude is the longitude to search for.
     * @return if the search query matches a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached], will return it. If not, it will return null.
     */
    internal fun squareContains(
        latitude: Double,
        longitude: Double
    ): SuggestionWithCoordinatesAndStyle? {
        return suggestionsCached.firstOrNull { it.suggestion.square.contains(latitude, longitude) }
    }


    /** Gets all markers from the map.
     *
     * @return all [SuggestionWithCoordinates] inside [suggestionsCached].
     */
    fun getList(): List<SuggestionWithCoordinates> {
        return suggestionsCached.map {
            it.suggestion
        }
    }

    /**
     * Select an existing [SuggestionWithCoordinates.square] in [suggestionsCached] where the [SuggestionWithCoordinates.square] contains [latitude] and [longitude].
     */
    fun selectExistingMarker(latitude: Double, longitude: Double) {
        selectedSuggestion = squareContains(latitude, longitude)?.suggestion
        main(dispatchers) {
            w3WMapWrapper.updateMap()
        }
    }

    /**
     * Select an existing [SuggestionWithCoordinates.square] in [suggestionsCached].
     */
    fun selectExistingMarker(suggestionWithCoordinates: SuggestionWithCoordinates) {
        selectedSuggestion = suggestionWithCoordinates
        main(dispatchers) {
            w3WMapWrapper.updateMap()
        }
    }
}
