package com.what3words.components.maps.wrappers

import androidx.core.util.Consumer
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.components.maps.extensions.contains
import com.what3words.components.maps.extensions.main
import com.what3words.components.maps.models.Either
import com.what3words.components.maps.models.SuggestionWithCoordinatesAndStyle
import com.what3words.components.maps.models.W3WDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import kotlinx.coroutines.runBlocking

/**
 * [W3WMapManager] an abstract layer to retrieve data from [W3WDataSource] save it in memory [suggestionsCached] and be used in multiple map wrapper. i.e: [W3WGoogleMapsWrapper].
 **
 * @param w3wDataSource source of what3words data can be API or SDK.
 * @param dispatchers for custom dispatcher handler using [DefaultDispatcherProvider] by default.
 */
internal class W3WMapManager(
    private val w3wDataSource: W3WDataSource,
    private val w3WMapWrapper: W3WMapWrapper,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    internal var selectedSuggestion: SuggestionWithCoordinates? = null
    internal val suggestionsCached: MutableList<SuggestionWithCoordinatesAndStyle> by lazy {
        mutableListOf()
    }

    internal var language: String = "en"

    /** Add [Coordinates] to [suggestionsCached]. This method will call [W3WDataSource.getSuggestionByCoordinates] and if success will save [SuggestionWithCoordinatesAndStyle] in [suggestionsCached] to keep [SuggestionWithCoordinates] and all the styles applied to it.
     *
     * @param coordinates [Coordinates] to be added.
     * @param markerColor is the [W3WMarkerColor] for the [Coordinates] added.
     * @return [Either] where will be [Either.Left] if an error occurs or [Either.Right] with [SuggestionWithCoordinates] if [W3WDataSource.getSuggestionByCoordinates] it's successful.
     */
    fun addCoordinates(
        coordinates: Coordinates,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        runBlocking(dispatchers.io()) {
            when (val c23wa = w3wDataSource.getSuggestionByCoordinates(coordinates, language)) {
                is Either.Left -> {
                    main(dispatchers) {
                        onError?.accept(c23wa.a)
                    }
                }
                is Either.Right -> {
                    val locationMapped = findByExactLocation(coordinates.lat, coordinates.lng)
                    if (locationMapped != null) suggestionsCached.remove(locationMapped)
                    suggestionsCached.add(
                        SuggestionWithCoordinatesAndStyle(
                            c23wa.b,
                            markerColor
                        )
                    )
                    main(dispatchers) {
                        onSuccess?.accept(c23wa.b)
                        w3WMapWrapper.updateMap()
                    }
                }
            }
        }
    }

    /** Set [Coordinates] as selected, only one selected allowed at the time. This method will call [W3WDataSource.getSuggestionByCoordinates] and if success will save [SuggestionWithCoordinates] in [selectCoordinates].
     *
     * @param coordinates [Coordinates] to be added.
     * @return [Either] where will be [Either.Left] if an error occurs or [Either.Right] with [SuggestionWithCoordinates] if [W3WDataSource.getSuggestionByCoordinates] it's successful.
     */
    fun selectCoordinates(
        coordinates: Coordinates,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        runBlocking(dispatchers.io()) {
            when (val c23wa = w3wDataSource.getSuggestionByCoordinates(coordinates, language)) {
                is Either.Left -> {
                    main(dispatchers) {
                        onError?.accept(c23wa.a)
                    }
                }
                is Either.Right -> {
                    selectedSuggestion = c23wa.b
                    main(dispatchers) {
                        onSuccess?.accept(c23wa.b)
                        w3WMapWrapper.updateMap()
                    }
                }
            }
        }
    }

    /** Add a list of [Coordinates] to [suggestionsCached]. This method will call [W3WDataSource.getSuggestionByCoordinates] for each [Coordinates] and if ALL succeed will save [SuggestionWithCoordinatesAndStyle] in [suggestionsCached] to keep in memory all [SuggestionWithCoordinates] and all the styles applied to it.
     *
     * @param listCoordinates list of [Coordinates] to be added.
     * @param markerColor is the [W3WMarkerColor] for the [Coordinates] added,.
     * @return [Either] where will be [Either.Left] if any error occurs or [Either.Right] with a list of [SuggestionWithCoordinates] if ALL [W3WDataSource.getSuggestionByCoordinates] are successful.
     */
    fun addCoordinates(
        listCoordinates: List<Coordinates>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        runBlocking(dispatchers.io()) {
            val toBeAdded = mutableListOf<SuggestionWithCoordinatesAndStyle>()
            var error: APIResponse.What3WordsError? = null
            listCoordinates.forEach { location ->
                when (
                    val c23wa =
                        w3wDataSource.getSuggestionByCoordinates(location, language)
                ) {
                    is Either.Left -> {
                        error = c23wa.a
                        return@forEach
                    }
                    is Either.Right -> {
                        toBeAdded.add(
                            SuggestionWithCoordinatesAndStyle(
                                c23wa.b,
                                markerColor
                            )
                        )
                    }
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

    /** Remove [Coordinates] from [suggestionsCached].
     *
     * @param coordinates the [Coordinates] to be removed.
     */
    fun removeCoordinates(coordinates: Coordinates) {
        runBlocking(dispatchers.io()) {
            squareContains(coordinates.lat, coordinates.lng)?.let { toRemove ->
                suggestionsCached.remove(toRemove)
                main(dispatchers) {
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Remove a list of [Coordinates] from [suggestionsCached].
     *
     * @param listCoordinates the list of [Coordinates] to remove.
     */
    fun removeCoordinates(listCoordinates: List<Coordinates>) {
        runBlocking(dispatchers.io()) {
            val listToRemove = mutableListOf<SuggestionWithCoordinatesAndStyle>()
            listCoordinates.forEach {
                squareContains(it.lat, it.lng)?.let { toRemove ->
                    listToRemove.add(toRemove)
                }
            }
            if (listToRemove.isNotEmpty()) {
                suggestionsCached.removeAll(listToRemove)
                main(dispatchers) {
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Add three word address [words] to [suggestionsCached]. This method will call [W3WDataSource.getSuggestionByWords] and if success will save [SuggestionWithCoordinatesAndStyle] in [suggestionsCached] to keep [SuggestionWithCoordinates] and all the styles applied to it.
     *
     * @param words to be added.
     * @param markerColor is the [W3WMarkerColor] for the [words] added.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [words].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addWords(
        words: String,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        runBlocking(dispatchers.io()) {
            when (val c23wa = w3wDataSource.getSuggestionByWords(words)) {
                is Either.Left -> {
                    main(dispatchers) {
                        onError?.accept(c23wa.a)
                    }
                }
                is Either.Right -> {
                    val locationMapped = suggestionsCached.firstOrNull {
                        it.suggestion.words == words
                    }
                    if (locationMapped != null) suggestionsCached.remove(locationMapped)
                    suggestionsCached.add(
                        SuggestionWithCoordinatesAndStyle(
                            c23wa.b,
                            markerColor
                        )
                    )
                    main(dispatchers) {
                        onSuccess?.accept(c23wa.b)
                        w3WMapWrapper.updateMap()
                    }
                }
            }
        }
    }

    /** Add a list of three word addresses to [suggestionsCached]. This method will call [W3WDataSource.getSuggestionByWords] for each words in [listWords] and if ALL succeed will save [SuggestionWithCoordinatesAndStyle] in [suggestionsCached] to keep in memory all [SuggestionWithCoordinates] and all the styles applied to it.
     * finally calls [W3WMapWrapper.updateMap] to reflect the changes.
     *
     * @param listWords list of words to be added.
     * @param markerColor is the [W3WMarkerColor] for the [listWords] added.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [listWords].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun addWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        runBlocking(dispatchers.io()) {
            val toBeAdded = mutableListOf<SuggestionWithCoordinatesAndStyle>()
            var error: APIResponse.What3WordsError? = null
            listWords.forEach { words ->
                when (
                    val c23wa =
                        w3wDataSource.getSuggestionByWords(words)
                ) {
                    is Either.Left -> {
                        error = c23wa.a
                        return@forEach
                    }
                    is Either.Right -> {
                        toBeAdded.add(
                            SuggestionWithCoordinatesAndStyle(
                                c23wa.b,
                                markerColor
                            )
                        )
                    }
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

    /** Set a three word address [words] as selected, only one selected allowed at the time. This method will call [W3WDataSource.getSuggestionByCoordinates] and if success will save [SuggestionWithCoordinates] in [selectCoordinates] and calls [W3WMapWrapper.updateMap] to reflect the changes.
     *
     * @param words three word address to be selected.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [words].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    fun selectWords(
        words: String,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        runBlocking(dispatchers.io()) {
            when (val c23wa = w3wDataSource.getSuggestionByWords(words)) {
                is Either.Left -> {
                    main(dispatchers) {
                        onError?.accept(c23wa.a)
                    }
                }
                is Either.Right -> {
                    selectedSuggestion = c23wa.b
                    main(dispatchers) {
                        onSuccess?.accept(c23wa.b)
                        w3WMapWrapper.updateMap()
                    }
                }
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

    /** Remove three word address from [suggestionsCached].
     *
     * @param words the three word address to be removed.
     */
    fun removeWords(words: String) {
        runBlocking(dispatchers.io()) {
            suggestionsCached.firstOrNull { it.suggestion.words == words }?.let { toRemove ->
                suggestionsCached.remove(toRemove)
                main(dispatchers) {
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Remove a list of three word addresses from the map.
     *
     * @param listWords the list of three word addresses to remove.
     */
    fun removeWords(listWords: List<String>) {
        runBlocking(dispatchers.io()) {
            val listToRemove = mutableListOf<SuggestionWithCoordinatesAndStyle>()
            listWords.forEach { words ->
                suggestionsCached.firstOrNull { it.suggestion.words == words }?.let { toRemove ->
                    listToRemove.add(toRemove)
                }
            }
            if (listToRemove.isNotEmpty()) {
                suggestionsCached.removeAll(listToRemove)
                main(dispatchers) {
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Remove all from [suggestionsCached]. */
    fun clearList() {
        runBlocking(dispatchers.io()) {
            val listToRemove = mutableListOf<SuggestionWithCoordinatesAndStyle>()
            suggestionsCached.forEach { data ->
                listToRemove.add(data)
            }
            if (listToRemove.isNotEmpty()) {
                suggestionsCached.removeAll(listToRemove)
                main(dispatchers) {
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Finds a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached] exactly by [latitude] and [longitude].
     *
     * @param latitude the latitude search query.
     * @param longitude the longitude search query.
     * @return if the search query matches a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached] will return it if not will return null.
     */
    internal fun findByExactLocation(
        latitude: Double,
        longitude: Double
    ): SuggestionWithCoordinatesAndStyle? {
        return suggestionsCached.firstOrNull { it.suggestion.coordinates.lat == latitude && it.suggestion.coordinates.lng == longitude }
    }

    /** Finds a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached] by checking if [latitude] and [longitude] is inside of any [SuggestionWithCoordinates.square].
     *
     * @param latitude the latitude to search for.
     * @param longitude the longitude to search for.
     * @return if the search query matches a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached] will return it if not will return null.
     */
    internal fun squareContains(
        latitude: Double,
        longitude: Double
    ): SuggestionWithCoordinatesAndStyle? {
        return suggestionsCached.firstOrNull { it.suggestion.square.contains(latitude, longitude) }
    }

    /** Finds a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached] by checking if [latitude] and [longitude] is inside of any [SuggestionWithCoordinates.square].
     *
     * @param latitude the latitude to search for.
     * @param longitude the longitude to search for.
     * @return if the search query matches a [SuggestionWithCoordinatesAndStyle] inside [suggestionsCached] will return it if not will return null.
     */
    internal fun selectedSquareContains(
        latitude: Double,
        longitude: Double
    ): SuggestionWithCoordinates? {
        return if (selectedSuggestion?.square?.contains(
                latitude,
                longitude
            ) == true
        ) selectedSuggestion else null
    }

    /** Gets all [SuggestionWithCoordinates] inside [suggestionsCached].
     *
     * @return all [SuggestionWithCoordinates] inside [suggestionsCached].
     */
    fun getList(): List<SuggestionWithCoordinates> {
        return suggestionsCached.map {
            it.suggestion
        }
    }

    fun selectExistingMarker(latitude: Double, longitude: Double) {
        selectedSuggestion = squareContains(latitude, longitude)?.suggestion
        main(dispatchers) {
            w3WMapWrapper.updateMap()
        }
    }

    fun selectExistingMarker(suggestionWithCoordinates: SuggestionWithCoordinates) {
        selectedSuggestion = suggestionWithCoordinates
        main(dispatchers) {
            w3WMapWrapper.updateMap()
        }
    }
}
