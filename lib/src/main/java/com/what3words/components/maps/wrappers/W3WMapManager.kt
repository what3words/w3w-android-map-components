package com.what3words.components.maps.wrappers

import android.util.Log
import androidx.core.util.Consumer
import com.what3words.androidwrapper.datasource.text.W3WApiTextDataSource
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.components.maps.extensions.contains
import com.what3words.components.maps.extensions.io
import com.what3words.components.maps.extensions.main
import com.what3words.components.maps.models.W3WAddressWithStyle
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * [W3WMapManager] an abstract layer to retrieve data from [W3WTextDataSource] save it in memory [listOfVisibleAddresses] and be used in multiple map wrapper. i.e: [W3WGoogleMapsWrapper].
 **
 * @param textDataSource source of what3words data can be [W3WApiTextDataSource] or [W3WSdkTextDataSource].
 * @param w3WMapWrapper the [W3WMapWrapper] that this manager will add/remove/select squares from.
 * @param dispatchers for custom dispatcher handler using [DefaultDispatcherProvider] by default.
 */
internal class W3WMapManager(
    private val textDataSource: W3WTextDataSource,
    private val w3WMapWrapper: W3WMapWrapper,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    internal var selectedAddress: W3WAddress? = null
    internal val listOfVisibleAddresses: MutableList<W3WAddressWithStyle> by lazy {
        mutableListOf()
    }

    internal val listOfAddressesToRemove: MutableList<W3WAddressWithStyle> by lazy {
        mutableListOf()
    }

    internal var language: W3WRFC5646Language = W3WRFC5646Language.EN_GB

    //region add/remove/select by W3WSuggestion

    /** Add marker at [W3WSuggestion.w3wAddress.square][W3WAddress.square] and add it to [listOfVisibleAddresses].
     *
     * @param suggestion [W3WSuggestion] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the [listOfVisibleAddresses] list.
     */
    fun addSuggestion(
        suggestion: W3WSuggestion,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        addAddress(suggestion.w3wAddress, markerColor, onSuccess, onError)
    }

    /** Add markers at multiple [W3WSuggestion.w3wAddress.square]p's and add them to [listOfVisibleAddresses].
     *
     * @param suggestion list of [W3WSuggestion] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the [listOfVisibleAddresses] list.
     * @param onError is called if there was an [W3WError] with [W3WTextDataSource.convertToCoordinates].
     */
    fun addSuggestion(
        suggestion: List<W3WSuggestion>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        addAddress(suggestion.map { it.w3wAddress }, markerColor, onSuccess, onError)
    }

    /** Set a [W3WSuggestion.w3wAddress.square][W3WAddress.square] as a selected square. Only one selected square is allowed at a time. This method will save [suggestion].[W3WAddress] in [selectedAddress] and call [W3WMapWrapper.updateMap] to reflect the changes.
     *
     * @param suggestion [W3WSuggestion] to be selected.
     * @param onSuccess a success callback will return the same [W3WSuggestion].
     */
    fun selectSuggestion(
        suggestion: W3WSuggestion,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        selectedAddress(suggestion.w3wAddress, onSuccess, onError)
    }

    /**
     * remove a [W3WSuggestion.w3wAddress.square][W3WAddress.square] from [listOfVisibleAddresses].
     */
    fun removeSuggestion(suggestion: W3WSuggestion) {
        removeWords(suggestion.w3wAddress.words)
    }

    /**
     * remove multiple [W3WSuggestion.w3wAddress.square][W3WAddress.square] from [listOfVisibleAddresses].
     */
    fun removeSuggestion(suggestion: List<W3WSuggestion>) {
        removeWords(suggestion.map { it.w3wAddress.words })
    }

    //endregion

    //region add/remove/select by W3WAddress

    /** Add marker at [W3WAddress.square] and add it to [listOfVisibleAddresses].
     *
     * @param address [W3WAddress] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the [listOfVisibleAddresses] list.
     */
    fun addAddress(
        address: W3WAddress,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        io(dispatchers) {
            Log.d("W3WMapManager", "addAddress: $address")
            if (address.center == null) {
                Log.d("W3WMapManager", "addAddress.center is null, try addWords")
                addWords(
                    address.words,
                    markerColor,
                    onSuccess,
                    onError
                )
            } else {
                val locationMapped = findByExactLocation(
                    address.center!!.lat,
                    address.center!!.lng
                )
                if (locationMapped != null) listOfVisibleAddresses.remove(locationMapped)
                listOfVisibleAddresses.add(
                    W3WAddressWithStyle(
                        address,
                        markerColor
                    )
                )
                main(dispatchers) {
                    onSuccess?.accept(address)
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    fun addAddress(
        listAddresses: List<W3WAddress>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        if (listAddresses.any { it.center == null }) {
            onError?.accept(W3WError("One or more addresses do not have coordinates"))
            return
        }
        runBlocking(dispatchers.io()) {
            listAddresses.map { W3WAddressWithStyle(it, markerColor) }.forEach {
                val existing = findByExactLocation(
                    it.address.center!!.lat,
                    it.address.center!!.lng
                )
                if (existing != null) {
                    listOfVisibleAddresses.remove(existing)
                }
                listOfVisibleAddresses.add(it)
            }
            main(dispatchers) {
                onSuccess?.accept(listAddresses)
                w3WMapWrapper.updateMap()
            }
        }
    }

    /**
     * Remove a [W3WAddress.square] from [listOfVisibleAddresses].
     */
    fun removeAddress(address: W3WAddress) {
        removeWords(address.words)
    }

    /**
     * Remove multiple [W3WAddress.square] from [listOfVisibleAddresses].
     */
    fun removeAddress(listAddresses: List<W3WAddress>) {
        removeWords(listAddresses.map { it.words })
    }

    /** Set a [W3WAddress.square] as a selected square. Only one selected square is allowed at a time. This method will save [address] in [selectedAddress] and call [W3WMapWrapper.updateMap] to reflect the changes.
     *
     * @param address [W3WAddress] to be selected.
     * @param onSuccess a success callback will return the same [W3WAddress].
     */
    fun selectedAddress(
        address: W3WAddress,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        main(dispatchers) {
            if (address.center == null) {
                selectWords(address.words, onSuccess, onError)
            } else {
                selectedAddress = address
                onSuccess?.accept(address)
                w3WMapWrapper.updateMap()
            }
        }
    }
    //endregion

    //region add/remove/select by coordinates

    /** Add marker at [W3WAddress.square] and add it to [listOfVisibleAddresses].
     *
     * @param coordinates within [W3WAddress.square] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the [listOfVisibleAddresses] list.
     * @param onError is called if there was an [W3WError] with [W3WTextDataSource.convertTo3wa].
     */
    fun addCoordinates(
        coordinates: W3WCoordinates,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        main(dispatchers) {
            val c23wa = withContext(dispatchers.io()) {
                textDataSource.convertTo3wa(
                    coordinates, language
                )
            }
            when (c23wa) {
                is W3WResult.Failure -> onError?.accept(c23wa.error)
                is W3WResult.Success -> {
                    val locationMapped = findByExactLocation(coordinates.lat, coordinates.lng)
                    if (locationMapped != null) listOfVisibleAddresses.remove(locationMapped)
                    listOfVisibleAddresses.add(
                        W3WAddressWithStyle(
                            c23wa.value,
                            markerColor
                        )
                    )
                    onSuccess?.accept(c23wa.value)
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Select [W3WAddress.square] that contains coordinates [W3WCoordinates.lat] latitude and [W3WCoordinates.lng] longitude. Only one selected square is allowed at a time. This method will call [W3WTextDataSource.convertTo3wa] and, if successful, will save [W3WAddress] in [selectedAddress].
     *
     * @param coordinates to be selected.
     * @param onSuccess is called if [W3WAddress.square] was selected successfully.
     * @param onError is called if there was an [W3WError] with [W3WTextDataSource.convertTo3wa].
     */
    fun selectCoordinates(
        coordinates: W3WCoordinates,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        main(dispatchers) {
            val c23wa = withContext(dispatchers.io()) {
                textDataSource.convertTo3wa(coordinates, language)
            }
            when (c23wa) {
                is W3WResult.Failure -> onError?.accept(c23wa.error)
                is W3WResult.Success -> {
                    selectedAddress = c23wa.value
                    onSuccess?.accept(c23wa.value)
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Add markers at multiple [W3WAddress.square]'s that contain coordinates inside [listCoordinates].[W3WCoordinates].
     *
     * @param listCoordinates list of coordinates where [W3WCoordinates.lat] is latitude and [W3WCoordinates.lng] is longitude within the [W3WAddress.square] to add.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if ALL markers at [W3WAddress.square] are added successfully to the [listOfVisibleAddresses] list.
     * @param onError is called if there was an [W3WError] in any of the [W3WTextDataSource.convertTo3wa] calls.
     */
    fun addCoordinates(
        listCoordinates: List<W3WCoordinates>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        runBlocking(dispatchers.io()) {
            val toBeAdded = mutableListOf<W3WAddressWithStyle>()
            var error: W3WError? = null
            listCoordinates.forEach { location ->
                val c23wa =
                    textDataSource.convertTo3wa(
                        location, language
                    )
                when (c23wa) {
                    is W3WResult.Failure -> {
                        error = c23wa.error
                        return@forEach
                    }

                    is W3WResult.Success -> {
                        toBeAdded.add(
                            W3WAddressWithStyle(
                                c23wa.value,
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
                        it.address.center!!.lat,
                        it.address.center!!.lng
                    )
                    if (existing != null) {
                        listOfVisibleAddresses.remove(existing)
                    }
                    listOfVisibleAddresses.add(it)
                }
                main(dispatchers) {
                    onSuccess?.accept(toBeAdded.map { it.address })
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Remove marker at [W3WAddress.square] that contains coordinates [W3WCoordinates.lat] latitude and [W3WCoordinates.lng] longitude from [listOfVisibleAddresses].
     *
     * @param coordinates latitude and longitude coordinates within [W3WAddress.square] to be removed from the map.
     */
    fun removeCoordinates(coordinates: W3WCoordinates) {
        main(dispatchers) {
            squareContains(coordinates)?.let { toRemove ->
                listOfVisibleAddresses.remove(toRemove)
                listOfAddressesToRemove.add((toRemove))
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Remove multiple markers from the map. It will remove all [W3WAddress.square]'s that contain coordinates inside [listCoordinates].
     *
     * @param listCoordinates list of coordinates where [W3WCoordinates.lat] is latitude and [W3WCoordinates.lng] is longitude within the [W3WAddress.square] to remove.
     */
    fun removeCoordinates(listCoordinates: List<W3WCoordinates>) {
        runBlocking(dispatchers.io()) {
            listCoordinates.forEach {
                squareContains(it)?.let { toRemove ->
                    listOfAddressesToRemove.add(toRemove)
                }
            }
            if (listOfAddressesToRemove.isNotEmpty()) {
                listOfVisibleAddresses.removeAll(listOfAddressesToRemove)
                main(dispatchers) {
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    //endregion

    //region add/remove/select by words
    /** Add marker at [W3WAddress.square] that [W3WAddress.words] are equal to [words] and add it to [listOfVisibleAddresses].
     *
     * @param words the what3words address of [W3WAddress.square] to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the [listOfVisibleAddresses] list.
     * @param onError is called if there was an [W3WError] when calling [W3WTextDataSource.convertToCoordinates].
     */
    fun addWords(
        words: String,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        main(dispatchers) {
            val c23wa =
                withContext(dispatchers.io()) { textDataSource.convertToCoordinates(words) }
            when (c23wa) {
                is W3WResult.Success -> {
                    val locationMapped = listOfVisibleAddresses.firstOrNull {
                        it.address.words == words
                    }
                    if (locationMapped != null) listOfVisibleAddresses.remove(locationMapped)
                    listOfVisibleAddresses.add(
                        W3WAddressWithStyle(
                            c23wa.value,
                            markerColor
                        )
                    )
                    onSuccess?.accept(c23wa.value)
                    w3WMapWrapper.updateMap()
                }

                is W3WResult.Failure -> {
                    onError?.accept(c23wa.error)
                }
            }
        }
    }

    /** Add markers at multiple [W3WAddress.square] that [W3WAddress.words] are equal to each what3words address inside [listWords] and add them to [listOfVisibleAddresses].
     *
     * @param listWords the list of what3words addresses of multiple [W3WAddress.square]'s to be added to the map.
     * @param markerColor is the [W3WMarkerColor] for the [W3WAddress.square] added.
     * @param onSuccess is called if the marker at [W3WAddress.square] is added successfully to the [listOfVisibleAddresses] list.
     * @param onError is called if there was an [W3WError] with [W3WTextDataSource.convertToCoordinates].
     */
    fun addWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        main(dispatchers) {
            val toBeAdded = mutableListOf<W3WAddressWithStyle>()
            var error: W3WError? = null
            listWords.forEach { words ->
                val c23wa = withContext(dispatchers.io()) {
                    textDataSource.convertToCoordinates(words)
                }
                when (c23wa) {
                    is W3WResult.Failure -> {
                        error = c23wa.error
                        return@forEach
                    }

                    is W3WResult.Success -> {
                        toBeAdded.add(
                            W3WAddressWithStyle(
                                c23wa.value,
                                markerColor
                            )
                        )
                    }
                }
            }
            if (error != null) {
                onError?.accept(error!!)
            } else {
                toBeAdded.forEach {
                    val existing = findByExactLocation(
                        it.address.center!!.lat,
                        it.address.center!!.lng
                    )
                    if (existing != null) {
                        listOfVisibleAddresses.remove(existing)
                    }
                    listOfVisibleAddresses.add(it)
                }
                onSuccess?.accept(toBeAdded.map { it.address })
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Select [W3WAddress.square] that [SuggestionWithCoordinates.words] are equal to [words].
     * Only one selected square is allowed at a time.
     * This method will call [W3WTextDataSource.convertToCoordinates] and, if successful, will save [W3WAddress] in [selectedAddress].
     *
     * @param words what3words address to select.
     * @param onSuccess is called if [W3WAddress.square] was selected successfully.
     * @param onError is called if there was an [W3WError] with [W3WTextDataSource.convertToCoordinates].
     */
    fun selectWords(
        words: String,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        main(dispatchers) {
            val c23wa =
                withContext(dispatchers.io()) {
                    textDataSource.convertToCoordinates(words)
                }
            when (c23wa) {
                is W3WResult.Failure -> onError?.accept(c23wa.error)
                is W3WResult.Success -> {
                    selectedAddress = c23wa.value
                    onSuccess?.accept(c23wa.value)
                    w3WMapWrapper.updateMap()
                }
            }
        }
    }

    /** Remove [W3WAddress.square] from [listOfVisibleAddresses] where [W3WAddress.words] are equal to [words].
     *
     * @param words the what3words address to remove.
     */
    fun removeWords(words: String) {
        main(dispatchers) {
            listOfVisibleAddresses.firstOrNull { it.address.words == words }
                ?.let { toRemove ->
                    listOfVisibleAddresses.remove(toRemove)
                    listOfAddressesToRemove.add(toRemove)
                    w3WMapWrapper.updateMap()
                }
        }
    }

    /** Remove multiple [W3WAddress.square]'s from [listOfVisibleAddresses] where [W3WAddress.words] are equal to each what3words address inside [listWords]
     *
     * @param listWords the list of what3words addresses of multiple [W3WAddress.square]'s to remove from the map.
     */
    fun removeWords(listWords: List<String>) {
        main(dispatchers) {
            listWords.forEach { words ->
                listOfVisibleAddresses.firstOrNull { it.address.words == words }
                    ?.let { toRemove ->
                        listOfAddressesToRemove.add(toRemove)
                    }
            }
            if (listOfAddressesToRemove.isNotEmpty()) {
                listOfVisibleAddresses.removeAll(listOfAddressesToRemove)
                w3WMapWrapper.updateMap()
            }
        }
    }

    //endregion

    /** Clears [selectedAddress] and calls [W3WMapWrapper.updateMap] to reflect the changes */
    fun unselect() {
        selectedAddress = null
        main(dispatchers) {
            w3WMapWrapper.updateMap()
        }
    }

    /** Remove all markers from the map. It will clear [listOfVisibleAddresses]. */
    fun clearList() {
        main(dispatchers) {
            listOfVisibleAddresses.forEach { data ->
                listOfAddressesToRemove.add(data)
            }
            if (listOfAddressesToRemove.isNotEmpty()) {
                listOfVisibleAddresses.removeAll(listOfAddressesToRemove)
                w3WMapWrapper.updateMap()
            }
        }
    }

    /** Finds a [W3WAddressWithStyle] inside [listOfVisibleAddresses] strictly by [latitude] and [longitude].
     *
     * @param latitude the latitude search query.
     * @param longitude the longitude search query.
     * @return if the search query matches a [W3WAddressWithStyle] inside [listOfVisibleAddresses], will return it. If not, it will return null.
     */
    internal fun findByExactLocation(
        latitude: Double,
        longitude: Double
    ): W3WAddressWithStyle? {
        return listOfVisibleAddresses.firstOrNull { it.address.center?.lat == latitude && it.address.center?.lng == longitude }
    }

    /** Finds a [W3WAddressWithStyle] inside [listOfVisibleAddresses] by checking if [W3WCoordinates.lat] and [W3WCoordinates.lng] are inside of any [W3WAddress.square].
     *
     * @param coordinates is the latitude and longitude to search for.
     * @return if the search query matches a [W3WAddressWithStyle] inside [listOfVisibleAddresses], will return it. If not, it will return null.
     */
    internal fun squareContains(
       coordinates: W3WCoordinates
    ): W3WAddressWithStyle? {
        return listOfVisibleAddresses.firstOrNull {
            it.address.square?.contains(coordinates) == true
        }
    }


    /** Gets all markers from the map.
     *
     * @return all [W3WAddress] inside [listOfVisibleAddresses].
     */
    fun getList(): List<W3WAddress> {
        return listOfVisibleAddresses.map {
            it.address
        }
    }

    /**
     * Select an existing [W3WAddress.square] in [listOfVisibleAddresses] where the [W3WAddress.square] contains [W3WCoordinates.lat] and [W3WCoordinates.lng].
     */
    fun selectExistingMarker(coordinates: W3WCoordinates) {
        selectedAddress = squareContains(coordinates)?.address
        main(dispatchers) {
            w3WMapWrapper.updateMap()
        }
    }

    /**
     * Select an existing [W3WAddress.square] in [listOfVisibleAddresses].
     */
    fun selectExistingMarker(address: W3WAddress) {
        selectedAddress = address
        main(dispatchers) {
            w3WMapWrapper.updateMap()
        }
    }
}