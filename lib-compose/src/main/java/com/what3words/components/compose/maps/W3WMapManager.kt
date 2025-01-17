package com.what3words.components.compose.maps

import android.annotation.SuppressLint
import android.graphics.PointF
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraPositionState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.components.compose.maps.W3WMapDefaults.LOCATION_DEFAULT
import com.what3words.components.compose.maps.W3WMapDefaults.MARKER_COLOR_DEFAULT
import com.what3words.components.compose.maps.extensions.addMarker
import com.what3words.components.compose.maps.extensions.computeHorizontalLines
import com.what3words.components.compose.maps.extensions.computeVerticalLines
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.extensions.toMarkers
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.mapper.toW3WMarker
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WGridScreenCell
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.models.W3WMarkerWithList
import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.components.compose.maps.state.LocationStatus
import com.what3words.components.compose.maps.state.W3WButtonsState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WGoogleCameraState
import com.what3words.components.compose.maps.state.camera.W3WMapboxCameraState
import com.what3words.components.compose.maps.utils.angleOfPoints
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WGridSection
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WRFC5646Language
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A central component for managing the state and interactions of a What3Words (W3W) map.
 *
 * This class encapsulates data sources, handles asynchronous operations, and exposes the map's state
 * through a [StateFlow]. It acts as a bridge between your application logic and the W3W map,
 * providing a convenient way to control the map's behavior and access its data.
 *
 * In most cases, this will be created via [rememberW3WMapManager].
 *
 * @param textDataSource An instance of [W3WTextDataSource], used for fetching what3words address information.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param initialMapState An optional [W3WMapState] object representing the initial state of the map. If not
 *   provided, a default [W3WMapState] is used.
 *
 * @property mapState A read-only [StateFlow] of [W3WMapState] exposing the current state of the map.
 */
class W3WMapManager(
    private val textDataSource: W3WTextDataSource,
    internal var mapProvider: MapProvider,
    val initialMapState: W3WMapState = W3WMapState(),
    private val initialButtonState: W3WButtonsState = W3WButtonsState(),
    private val dispatcher: CoroutineDispatcher = IO,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _mapState: MutableStateFlow<W3WMapState> = MutableStateFlow(initialMapState)
    val mapState: StateFlow<W3WMapState> = _mapState.asStateFlow()

    private val _buttonState: MutableStateFlow<W3WButtonsState> =
        MutableStateFlow(initialButtonState)
    val buttonState: StateFlow<W3WButtonsState> = _buttonState.asStateFlow()

    // This flow controls when the grid calculation should be performed.
    private val gridCalculationFlow = MutableStateFlow<W3WRectangle?>(null)
    private var lastProcessedGridBound: W3WRectangle? = null

    // Stores markers by list name
    private val markersMap: MutableMap<String, MutableList<W3WMarker>> = mutableMapOf()

    private var mapConfig: W3WMapDefaults.MapConfig? = null

    init {
        _mapState.update {
            it.copy(
                cameraState = when (mapProvider) {
                    MapProvider.MAPBOX -> createMapboxCameraState(initialMapState.cameraState)
                    MapProvider.GOOGLE_MAP -> createGoogleCameraState(initialMapState.cameraState)
                }
            )
        }
        observeGridCalculation()
    }

    private fun createMapboxCameraState(
        currentCameraState: W3WCameraState<*>? = null,
    ): W3WMapboxCameraState {
        val center = currentCameraState?.getCenter()
        val zoom = currentCameraState?.getZoomLevel()
        val bearing = currentCameraState?.getBearing()
        val tilt = currentCameraState?.getTilt()

        return W3WMapboxCameraState(
            MapViewportState(
                initialCameraState = CameraState(
                    center?.let { Point.fromLngLat(it.lng, it.lat) }
                        ?: Point.fromLngLat(LOCATION_DEFAULT.lng, LOCATION_DEFAULT.lat),
                    EdgeInsets(0.0, 0.0, 0.0, 0.0),
                    zoom?.toDouble() ?: W3WMapboxCameraState.MY_LOCATION_ZOOM,
                    bearing?.toDouble() ?: 0.0,
                    tilt?.toDouble() ?: 0.0
                )
            )
        )
    }

    private fun createGoogleCameraState(
        currentCameraState: W3WCameraState<*>? = null,
    ): W3WGoogleCameraState {
        val center = currentCameraState?.getCenter()
        val zoom = currentCameraState?.getZoomLevel()
        val bearing = currentCameraState?.getBearing()
        val tilt = currentCameraState?.getTilt()

        return W3WGoogleCameraState(
            CameraPositionState(
                position = CameraPosition(
                    center?.toGoogleLatLng() ?: LOCATION_DEFAULT.toGoogleLatLng(),
                    zoom ?: W3WGoogleCameraState.MY_LOCATION_ZOOM,
                    tilt ?: 0f,
                    bearing ?: 0f
                )
            )
        )
    }

    private fun observeGridCalculation() {
        scope.launch {
            gridCalculationFlow
                .filterNotNull()
                .collect { newGridBound ->
                    val visibleBound = _mapState.value.cameraState?.visibleBound
                    if (shouldCalculateGrid(lastProcessedGridBound, visibleBound)) {
                        calculateAndUpdateGrid(newGridBound)
                    }
                }
        }
    }

    private fun shouldCalculateGrid(
        oldGridBound: W3WRectangle?,
        visibleBound: W3WRectangle?,
    ): Boolean {
        // Always calculate if there's no previous grid
        if (oldGridBound == null) {
            return true
        }

        // No visible bound, no need to calculate
        if (visibleBound == null) {
            return false
        }

        // Check if we're below the minimum zoom level for grid calculation
        if (isBelowMinimumZoom()) {
            return false
        }

        return !isVisibleBoundFullyInsideGridBound(visibleBound, oldGridBound)
    }

    /**
     * Determines if the visible bound is fully inside the grid bound and not near its borders.
     *
     * This function checks if the visible bound (typically representing the camera's current view)
     * is completely contained within the grid bound, with an additional threshold to ensure it's
     * not too close to the edges. This helps in deciding whether to recalculate the grid or not.
     *
     */
    private fun isVisibleBoundFullyInsideGridBound(
        visibleBound: W3WRectangle,
        gridBound: W3WRectangle,
        threshold: Double = 0.1
    ): Boolean {
        val gridWidth = gridBound.northeast.lng - gridBound.southwest.lng
        val gridHeight = gridBound.northeast.lat - gridBound.southwest.lat

        val thresholdLng = gridWidth * threshold
        val thresholdLat = gridHeight * threshold

        return visibleBound.southwest.lng >= gridBound.southwest.lng + thresholdLng &&
                visibleBound.southwest.lat >= gridBound.southwest.lat + thresholdLat &&
                visibleBound.northeast.lng <= gridBound.northeast.lng - thresholdLng &&
                visibleBound.northeast.lat <= gridBound.northeast.lat - thresholdLat
    }

    private fun isBelowMinimumZoom(): Boolean {
        val zoomLevel = _mapState.value.cameraState?.getZoomLevel()
        val zoomSwitchLevel = mapConfig?.gridLineConfig?.zoomSwitchLevel
        return zoomLevel != null && zoomSwitchLevel != null && zoomLevel < zoomSwitchLevel
    }

    private suspend fun calculateAndUpdateGrid(gridBound: W3WRectangle) {
        val newGridLines = calculateGridPolylines(gridBound)
        if (newGridLines != null) {
            lastProcessedGridBound = gridBound
        }

        _mapState.update {
            it.copy(gridLines = newGridLines ?: W3WGridLines())
        }
    }

    private suspend fun calculateGridPolylines(gridBound: W3WRectangle): W3WGridLines? =
        withContext(dispatcher) {
            gridBound.let { safeBox ->
                when (val grid = textDataSource.gridSection(safeBox)) {
                    is W3WResult.Failure -> {
                        null
                    }

                    is W3WResult.Success -> grid.toW3WGridLines()
                }
            }
        }

    private fun W3WResult.Success<W3WGridSection>.toW3WGridLines(): W3WGridLines {
        return W3WGridLines(
            verticalLines = value.lines.computeVerticalLines().toImmutableList(),
            horizontalLines = value.lines.computeHorizontalLines().toImmutableList()
        )
    }

    /**
     * Sets the map provider while preserving the current camera state.
     *
     * This function updates the map provider and adjusts the camera state accordingly.
     * If the new provider is different from the current one, it creates a new camera state
     * based on the new provider type while maintaining the current camera position, zoom level,
     * bearing, and tilt.
     *
     * @param mapProvider The new [MapProvider] to be set (either [MapProvider.MAPBOX] or [MapProvider.GOOGLE_MAP]).
     */
    fun setMapProvider(mapProvider: MapProvider) {
        if (this.mapProvider != mapProvider) {
            this.mapProvider = mapProvider

            _mapState.update { currentState ->
                currentState.copy(
                    cameraState = when (mapProvider) {
                        MapProvider.MAPBOX -> createMapboxCameraState(
                            _mapState.value.cameraState
                        )

                        MapProvider.GOOGLE_MAP -> createGoogleCameraState(
                            _mapState.value.cameraState
                        )
                    }
                )
            }
        }
    }

    /**
     * The language for what3words address on the map. Default to English.
     */
    private var language: W3WRFC5646Language = W3WRFC5646Language.EN_GB

    /**
     * Retrieves a list of all markers on the map.
     */
    val markers: List<W3WMarker>
        get() {
            return markersMap.values.flatten()
        }

    internal suspend fun updateCameraState(newCameraState: W3WCameraState<*>) = withContext(IO) {
        _mapState.update {
            it.copy(
                cameraState = newCameraState,
            )
        }
        gridCalculationFlow.value = newCameraState.gridBound

        if (_buttonState.value.isRecallButtonEnabled) {
            handleRecallButton()
        }
    }

    /**
     * Sets the language for what3words addresses.
     *
     * @param language The language code as per [W3WRFC5646Language].
     */
    fun setLanguage(language: W3WRFC5646Language) {
        this.language = language
    }

    /**
     * Checks if dark mode is enabled for the map.
     *
     * @return Boolean indicating if dark mode is enabled.
     */
    fun isDarkModeEnabled(): Boolean {
        return mapState.value.isDarkMode
    }

    /**
     * Enables or disables dark mode for the map.
     *
     * @param darkMode Boolean indicating whether to enable dark mode.
     */
    fun enableDarkMode(darkMode: Boolean) {
        _mapState.update {
            it.copy(
                isDarkMode = darkMode
            )
        }
    }

    /**
     * Retrieves the current map type.
     *
     * @return The current map type as [W3WMapType].
     */
    fun getMapType(): W3WMapType {
        return mapState.value.mapType
    }

    /**
     * Sets the map type.
     *
     * @param mapType The desired map type as [W3WMapType].
     */
    fun setMapType(mapType: W3WMapType) {
        _mapState.update {
            it.copy(
                mapType = mapType
            )
        }
    }

    /**
     * Enables or disables map gestures.
     *
     * @param enabled Boolean indicating whether map gestures should be enabled.
     */
    fun setMapGesturesEnabled(enabled: Boolean) {
        _mapState.update {
            it.copy(
                isMapGestureEnable = enabled
            )
        }
    }

    /**
     * Enables or disables the "My Location" feature on the map.
     *
     * @param enabled Boolean indicating if the feature should be enabled.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    fun setMyLocationEnabled(enabled: Boolean) {
        _mapState.update {
            it.copy(
                isMyLocationEnabled = enabled
            )
        }
    }

    /**
     * Adjust camera bearing to 0.
     */
    suspend fun orientCamera() {
        mapState.value.cameraState?.orientCamera()
    }

    /**
     * Asynchronously moves the camera to a new position on the map. This method allows for optional animation of the camera movement and adjustment of zoom, bearing, and tilt for a more customized view.
     *
     * @param coordinates The target latitude and longitude ([W3WCoordinates]) where the camera should move to.
     * @param zoom Optional zoom level for the camera. If null, the current zoom level is maintained.
     * @param bearing Optional bearing for the camera in degrees. The bearing is the compass direction that the camera is pointing.
     * @param tilt Optional tilt angle for the camera, in degrees from the nadir (directly facing the Earth's surface).
     * @param animate Boolean flag indicating whether the camera movement should be animated. If true, the camera smoothly transitions to the new position.
     */
    suspend fun moveToPosition(
        coordinates: W3WCoordinates,
        zoom: Float? = null,
        bearing: Float? = null,
        tilt: Float? = null,
        animate: Boolean = true,
    ) = withContext(Dispatchers.Main) {
        mapState.value.cameraState?.moveToPosition(
            coordinates,
            zoom,
            bearing,
            tilt,
            animate
        )
    }

    /**
     * Retrieves the currently selected address on the map.
     *
     * @return The selected marker or square as [W3WMarker] or null if none is selected.
     */
    fun getSelectedAddress(): W3WAddress? {
        return mapState.value.selectedAddress
    }

    /**
     * Clears the currently selected address on the map.
     */
    fun clearSelectedAddress() {
        _mapState.update {
            it.copy(
                selectedAddress = null
            )
        }
    }

    /**
     * Selects an what3words address on the map.
     *
     * @param suggestion The [W3WSuggestion] to select.
     */
    @JvmName("setSelectedAddressAtSuggestion")
    suspend fun setSelectedAddress(
        suggestion: W3WSuggestion
    ) = withContext(dispatcher) {
        _mapState.update {
            it.copy(
                selectedAddress = suggestion.w3wAddress
            )
        }

        if (_buttonState.value.isRecallButtonEnabled) {
            handleRecallButton()
        }
    }

    /**
     * Selects an what3words address on the map.
     *
     * @param address The [W3WAddress] to select.
     */
    @JvmName("setSelectedAddressAtAddress")
    suspend fun setSelectedAddress(
        address: W3WAddress
    ) = withContext(dispatcher) {
        _mapState.update {
            it.copy(
                selectedAddress = address
            )
        }

        if (_buttonState.value.isRecallButtonEnabled) {
            handleRecallButton()
        }
    }

    /**
     * Selects an what3words address on the map at a specific what3words address.
     *
     * @param words The what3words address as a [String].
     */
    @JvmName("setSelectedAddressAtWords")
    suspend fun setSelectedAddress(
        words: String
    ) = withContext(dispatcher) {
        when (val result = textDataSource.convertToCoordinates(words)) {
            is W3WResult.Failure -> {
                throw result.error
            }

            is W3WResult.Success -> {
                setSelectedAddress(result.value)
            }
        }
    }

    /**
     * Selects an what3words address on the map at a specific [W3WCoordinates].
     *
     * @param coordinates The [W3WCoordinates] to be selected.
     */
    @JvmName("setSelectedAddressAtCoordinates")
    suspend fun setSelectedAddress(
        coordinates: W3WCoordinates
    ) = withContext(dispatcher) {
        when (val result = textDataSource.convertTo3wa(
            W3WCoordinates(lat = coordinates.lat, lng = coordinates.lng),
            language
        )) {
            is W3WResult.Failure -> {
                throw result.error
            }

            is W3WResult.Success -> {
                setSelectedAddress(result.value)
            }
        }
    }

    /**
     * Removes all markers from the map.
     */
    fun removeAllMarkers() {
        markersMap.clear()
        _mapState.update {
            it.copy(
                markers = persistentListOf()
            )
        }
    }

    /**
     * Removes a specific marker from the map.
     *
     * @param removeMarker The W3WMarker to be removed from the map.
     */
    suspend fun removeMarker(
        removeMarker: W3WMarker
    ) = withContext(dispatcher) {
        markersMap.forEach { (_, markers) ->
            markers.removeIf { it == removeMarker }
        }

        markersMap.entries.removeIf { it.value.isEmpty() }

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }
    }

    /**
     * Removes markers from the map at a specific what3words address.
     *
     * @param words The What3Words address as a [String].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkerAt(
        words: String,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        val removedMarkers = mutableListOf<W3WMarker>()

        if (listName == null) {
            // Remove markers from all lists
            markersMap.forEach { (_, markers) ->
                val toRemove = markers.filter { it.words == words }
                removedMarkers.addAll(toRemove)
                markers.removeAll(toRemove)
            }

            markersMap.entries.removeIf { it.value.isEmpty() }
        } else {
            // Remove markers only from the specified list
            val markers = markersMap[listName]
            if (markers != null) {
                val toRemove = markers.filter { it.words == words }
                removedMarkers.addAll(toRemove)
                markers.removeAll(toRemove)

                if (markers.isEmpty()) {
                    markersMap.remove(listName)
                }
            }
        }

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        removedMarkers
    }

    /**
     * Removes markers from the map at a specific [W3WCoordinates].
     *
     * @param coordinates The [W3WCoordinates] to remove markers from.
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkerAt(
        coordinates: W3WCoordinates,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        val removedMarkers = mutableListOf<W3WMarker>()

        if (listName == null) {
            // Remove markers from all lists
            markersMap.forEach { (_, markers) ->
                val toRemove = markers.filter { it.square.contains(coordinates) }
                removedMarkers.addAll(toRemove)
                markers.removeAll(toRemove)
            }

            markersMap.entries.removeIf { it.value.isEmpty() }
        } else {
            // Remove markers only from the specified list
            val markers = markersMap[listName]
            if (markers != null) {
                val toRemove = markers.filter { it.square.contains(coordinates) }
                removedMarkers.addAll(toRemove)
                markers.removeAll(toRemove)

                if (markers.isEmpty()) {
                    markersMap.remove(listName)
                }
            }
        }

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        removedMarkers
    }

    /**
     * Removes markers from the map at a specific what3words address.
     *
     * @param address The What3Words address as a [W3WAddress].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkerAt(
        address: W3WAddress,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        removeMarkerAt(address.words, listName)
    }

    /**
     * Removes markers from the map at a specific what3words address.
     *
     * @param suggestion The What3Words address as a [W3WSuggestion].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkerAt(
        suggestion: W3WSuggestion,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        removeMarkerAt(suggestion.w3wAddress.words, listName)
    }

    /**
     * Removes markers from the map at a specific what3words address.
     *
     * @param listWords list of The What3Words address as a [String].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkersAt(
        listWords: List<String>,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        val removedMarkers = mutableListOf<W3WMarker>()
        listWords.forEach {
            val markers = removeMarker(it, listName)
            removedMarkers.addAll(markers)
        }

        return@withContext removedMarkers
    }

    /**
     * Removes markers from the map at a specific what3words address.
     *
     * @param listCoordinates list of The [W3WCoordinates] to remove markers from.
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkersAt(
        listCoordinates: List<W3WCoordinates>,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        val removedMarkers = mutableListOf<W3WMarker>()
        listCoordinates.forEach {
            val markers = removeMarker(it, listName)
            removedMarkers.addAll(markers)
        }

        return@withContext removedMarkers
    }

    /**
     * Removes markers from the map at a specific what3words address.
     *
     * @param addresses list of The What3Words address as a [W3WAddress].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkersAt(
        addresses: List<W3WAddress>,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        removeMarkersAt(addresses.map { it.words }, listName)
    }

    /**
     * Removes markers from the map at a specific what3words address.
     *
     * @param suggestions list of The What3Words address as a [W3WSuggestion].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkersAt(
        suggestions: List<W3WSuggestion>,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        removeMarkersAt(suggestions.map { it.w3wAddress.words }, listName)
    }

    /**
     * Removes all markers from a specific list.
     *
     * @param listName The name of the list from which markers should be removed.
     */
    suspend fun removeListMarker(listName: String) = withContext(dispatcher) {
        markersMap.remove(listName)

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }
    }

    /**
     * Retrieves all markers at a specific [W3WCoordinates].
     *
     * This method searches through all marker collections in the map and returns
     * a list of markers that contain the given coordinates within their square.
     * Each marker is paired with the name of the list it belongs to.
     *
     * @param coordinates The coordinates [W3WCoordinates] to search for markers.
     * @return A list of pairs, where each pair contains the name of the marker's list and the marker itself.
     *         The list is empty if no markers are found at the given location.
     *
     * @see W3WMarker
     */
    @JvmName("getMarkersAtCoordinates")
    fun getMarkersAt(
        coordinates: W3WCoordinates
    ): List<W3WMarkerWithList> {
        return markersMap.flatMap { (listName, markers) ->
            markers.filter { it.square.contains(coordinates) }
                .map { marker -> W3WMarkerWithList(listName, marker) }
        }
    }

    /**
     * Retrieves all markers at a specific what3Words address.
     *
     * This method searches through all marker collections in the map and returns
     * a list of markers that have the given what3Words address.
     * Each marker is paired with the name of the list it belongs to.
     *
     * @param words The What3Words address to search for markers.
     * @return A list of pairs, where each pair contains the name of the marker's list and the marker itself.
     *         The list is empty if no markers are found at the given address.
     */
    @JvmName("getMarkersAtWords")
    fun getMarkersAt(
        words: String
    ): List<W3WMarkerWithList> {
        return markersMap.flatMap { (listName, markers) ->
            markers.filter { it.words == words }
                .map { marker -> W3WMarkerWithList(listName, marker) }
        }
    }

    /**
     * Retrieves all markers at a specific [W3WAddress].
     *
     * This method searches through all marker collections in the map and returns
     * a list of markers that contain the given coordinates within their square.
     * Each marker is paired with the name of the list it belongs to.
     *
     * @param address The address [W3WAddress] to search for markers.
     * @return A list of pairs, where each pair contains the name of the marker's list and the marker itself.
     *         The list is empty if no markers are found at the given location.
     *
     * @see W3WMarker
     */
    @JvmName("getMarkersAddress")
    fun getMarkersAt(
        address: W3WAddress
    ): List<W3WMarkerWithList> {
        return address.center?.let {
            getMarkersAt(it)
        }?:run{
            getMarkersAt(address.words)
        }
    }

    /**
     * Retrieves all markers at a specific [W3WSuggestion].
     *
     * This method searches through all marker collections in the map and returns
     * a list of markers that contain the given coordinates within their square.
     * Each marker is paired with the name of the list it belongs to.
     *
     * @param suggestion The address [W3WSuggestion] to search for markers.
     * @return A list of pairs, where each pair contains the name of the marker's list and the marker itself.
     *         The list is empty if no markers are found at the given location.
     *
     * @see W3WMarker
     */
    @JvmName("getMarkersAtSuggestion")
    fun getMarkersAt(
        suggestion: W3WSuggestion
    ): List<W3WMarkerWithList> {
        return suggestion.w3wAddress.center?.let {
            getMarkersAt(it)
        }?:run{
            getMarkersAt(suggestion.w3wAddress.words)
        }
    }

    /**
     * Retrieves all markers in a specific list.
     *
     * @param listName The name of the list to retrieve markers from.
     * @return A list of markers in the specified list.
     */
    fun getMarkersInList(listName: String): List<W3WMarker> {
        return markersMap[listName] ?: emptyList()
    }

    /**
     * Adds a marker at the specified what3words address on the map. It also updates the map view based on the specified zoom options.
     *
     * @param words The what3words address where the marker should be placed.
     * @param markerColor The color of the marker. Defaults to MARKER_COLOR_DEFAULT.
     * @param listName The name of the list to which the marker should be added. Defaults to LIST_DEFAULT_ID.
     * @param zoomOption Specifies how the map should adjust its view after adding the marker. Defaults to CENTER_AND_ZOOM.
     * @param zoomLevel The zoom level to set if zooming is specified in zoomOption. If not provided, the default zoom level will be used.
     * @return A W3WResult containing either the created W3WMarker on success, or a W3WError on failure.
     */
    @JvmName("addMarkerAtWords")
    suspend fun addMarkerAt(
        words: String,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext addMarkerInternal(
            listName = listName,
            input = words,
            markerColor = markerColor,
            convertFunction = { textDataSource.convertToCoordinates(words) },
            zoomOption = zoomOption,
            zoomLevel = zoomLevel
        )
    }

    /**
     * Adds a marker at the specified coordinates [W3WCoordinates]. It also updates the map view based on the specified zoom options.
     *
     * @param coordinates The coordinates [W3WCoordinates] where the marker should be placed.
     * @param listName The name of the list to which the marker should be added. Defaults to LIST_DEFAULT_ID.
     * @param markerColor The color of the marker. Defaults to MARKER_COLOR_DEFAULT.
     * @param zoomOption Specifies how the map should adjust its view after adding the marker. Defaults to CENTER_AND_ZOOM.
     * @param zoomLevel The zoom level to set if zooming is specified in zoomOption. Can be null.
     * @return A W3WResult containing either the created W3WMarker on success, or a W3WError on failure.
     *
     * @see W3WCoordinates
     * @see W3WMarkerColor
     * @see W3WZoomOption
     * @see W3WMarker
     * @see W3WResult
     */
    @JvmName("addMarkerAtCoordinates")
    suspend fun addMarkerAt(
        coordinates: W3WCoordinates,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext addMarkerInternal(
            listName = listName,
            input = coordinates,
            markerColor = markerColor,
            convertFunction = { textDataSource.convertTo3wa(coordinates, language) },
            zoomOption = zoomOption,
            zoomLevel = zoomLevel
        )
    }

    /**
     * Adds a marker at the specified address [W3WAddress]. It also updates the map view based on the specified zoom options.
     *
     * @param address The address [W3WAddress] where the marker should be placed.
     * @param listName The name of the list to which the marker should be added. Defaults to LIST_DEFAULT_ID.
     * @param markerColor The color of the marker. Defaults to MARKER_COLOR_DEFAULT.
     * @param zoomOption Specifies how the map should adjust its view after adding the marker. Defaults to CENTER_AND_ZOOM.
     * @param zoomLevel The zoom level to set if zooming is specified in zoomOption. Can be null.
     * @return A W3WResult containing either the created W3WMarker on success, or a W3WError on failure.
     *
     * @see W3WAddress
     * @see W3WMarkerColor
     * @see W3WZoomOption
     * @see W3WMarker
     * @see W3WResult
     */
    @JvmName("addMarkerAtAddress")
    suspend fun addMarkerAt(
        address: W3WAddress,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext addMarkerInternal(
            listName = listName,
            input = address,
            markerColor = markerColor,
            convertFunction = {
                if(address.center == null) {
                    textDataSource.convertToCoordinates(address.words)
                } else
                {
                    W3WResult.Success(address)
                }
            },
            zoomOption = zoomOption,
            zoomLevel = zoomLevel
        )
    }

    /**
     * Adds a marker at the specified address [W3WSuggestion]. It also updates the map view based on the specified zoom options.
     *
     * @param suggestion The address [W3WSuggestion] where the marker should be placed.
     * @param listName The name of the list to which the marker should be added. Defaults to LIST_DEFAULT_ID.
     * @param markerColor The color of the marker. Defaults to MARKER_COLOR_DEFAULT.
     * @param zoomOption Specifies how the map should adjust its view after adding the marker. Defaults to CENTER_AND_ZOOM.
     * @param zoomLevel The zoom level to set if zooming is specified in zoomOption. Can be null.
     * @return A W3WResult containing either the created W3WMarker on success, or a W3WError on failure.
     *
     * @see W3WSuggestion
     * @see W3WMarkerColor
     * @see W3WZoomOption
     * @see W3WMarker
     * @see W3WResult
     */
    @JvmName("addMarkerAtSuggestion")
    suspend fun addMarkerAt(
        suggestion: W3WSuggestion,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext addMarkerInternal(
            listName = listName,
            input = suggestion.w3wAddress,
            markerColor = markerColor,
            convertFunction = {
                if(suggestion.w3wAddress.center == null) {
                    textDataSource.convertToCoordinates(suggestion.w3wAddress.words)
                } else
                {
                    W3WResult.Success(suggestion.w3wAddress)
                }
            },
            zoomOption = zoomOption,
            zoomLevel = zoomLevel
        )
    }

    private suspend fun <T> addMarkerInternal(
        input: T,
        convertFunction: suspend (T) -> W3WResult<W3WAddress>,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext when (val result = convertFunction(input)) {
            is W3WResult.Success -> {
                val marker = result.value.toW3WMarker(markerColor)
                markersMap.addMarker(listName = listName, marker = marker)

                // Update the map state
                _mapState.update { currentState ->
                    currentState.copy(
                        markers = markersMap.toMarkers().toImmutableList()
                    )
                }

                handleZoomOption(marker.center, zoomOption, zoomLevel)
                W3WResult.Success(marker)
            }

            is W3WResult.Failure -> W3WResult.Failure(result.error, result.message)
        }

    }

    /**
     * Adds a batch of markers to the map at the specified what3words addresses.
     *
     * This suspend function asynchronously converts each provided what3words address to coordinates,
     * creates markers, and adds them to the map.
     *
     * @param listWords A list of what3words addresses (as strings) where markers should be placed.
     * @param listName The name of the list to which these markers should be added.
     * @param markerColor The color to be used for these markers.
     *
     * @see W3WMarker
     * @see W3WMarkerColor
     * @see W3WZoomOption
     *
     * Note: This function filters out any listWords that fail to convert to coordinates.
     *       Only successfully converted addresses will result in markers being added to the map.
     *
     * Usage:
     * ```
     * val listWords = listOf("index.home.raft", "filled.count.soap", "daring.lion.race")
     * mapManager.addMarkersAt(listWords)
     * ```
     */
    @JvmName("addMarkersAtListWords")
    suspend fun addMarkersAt(
        listWords: List<String>,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        return@withContext addMarkersInternal(
            listName = listName,
            inputs = listWords,
            markerColor = markerColor,
            convertFunction = {
                textDataSource.convertToCoordinates(it)
            },
            zoomOption = zoomOption,
        )
    }

    /**
     * Adds a batch of markers to the map at the specified coordinates.
     *
     * This suspend function asynchronously converts each provided coordinate to a what3words address,
     * creates markers, and adds them to the map. It uses coroutines for efficient parallel processing.
     *
     * @param listCoordinates A list of [W3WCoordinates] where markers should be placed.
     * @param listName The name of the list to which these markers should be added.
     * @param markerColor The color to be used for these markers.
     *
     * @see W3WCoordinates
     * @see W3WMarker
     * @see W3WMarkerColor
     * @see W3WZoomOption
     *
     * Note: This function filters out any coordinates that fail to convert to what3words addresses.
     *       Only successfully converted coordinates will result in markers being added to the map.
     *
     * Usage:
     * ```
     * val coordinates = listOf(
     *     W3WCoordinates(11.521251, -0.203586),
     *     W3WCoordinates(25.521252, -0.203587)
     * )
     * mapManager.addMarkersAt(coordinates)
     * ```
     */
    @JvmName("addMarkersAtListCoordinates")
    suspend fun addMarkersAt(
        listCoordinates: List<W3WCoordinates>,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        return@withContext addMarkersInternal(
            listName = listName,
            inputs = listCoordinates,
            markerColor = markerColor,
            convertFunction = {
                textDataSource.convertTo3wa(it,language)
            },
            zoomOption = zoomOption,
        )
    }

    /**
     * Adds a batch of markers to the map at the specified coordinates.
     *
     * This suspend function asynchronously converts each provided coordinate to a what3words address,
     * creates markers, and adds them to the map. It uses coroutines for efficient parallel processing.
     *
     * @param suggestions A list of [W3WSuggestion] where markers should be placed.
     * @param listName The name of the list to which these markers should be added.
     * @param markerColor The color to be used for these markers.
     *
     * @see W3WSuggestion
     * @see W3WMarker
     * @see W3WMarkerColor
     * @see W3WZoomOption
     *
     * Note: This function filters out any coordinates that fail to convert to what3words addresses.
     *       Only successfully converted coordinates will result in markers being added to the map.
     *
     * Usage:
     * ```
     * val coordinates = listOf(
     *     W3WCoordinates(11.521251, -0.203586),
     *     W3WCoordinates(25.521252, -0.203587)
     * )
     * mapManager.addMarkersAt(coordinates)
     * ```
     */
    @JvmName("addMarkersAtAddresses")
    suspend fun addMarkersAt(
        suggestions: List<W3WSuggestion>,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        return@withContext addMarkersInternal(
            listName = listName,
            inputs = suggestions,
            markerColor = markerColor,
            convertFunction = {
                if(it.w3wAddress.center == null) {
                    textDataSource.convertToCoordinates(it.w3wAddress.words)
                } else
                {
                    W3WResult.Success(it.w3wAddress)
                }
            },
            zoomOption = zoomOption,
        )
    }

    /**
     * Adds a batch of markers to the map at the specified coordinates.
     *
     * This suspend function asynchronously converts each provided coordinate to a what3words address,
     * creates markers, and adds them to the map. It uses coroutines for efficient parallel processing.
     *
     * @param addresses A list of [W3WAddress] where markers should be placed.
     * @param listName The name of the list to which these markers should be added.
     * @param markerColor The color to be used for these markers.
     *
     * @see W3WAddress
     * @see W3WMarker
     * @see W3WMarkerColor
     * @see W3WZoomOption
     *
     * Note: This function filters out any coordinates that fail to convert to what3words addresses.
     *       Only successfully converted coordinates will result in markers being added to the map.
     *
     * Usage:
     * ```
     * val coordinates = listOf(
     *     W3WCoordinates(11.521251, -0.203586),
     *     W3WCoordinates(25.521252, -0.203587)
     * )
     * mapManager.addMarkersAt(coordinates)
     * ```
     */
    @JvmName("addMarkersAtSuggestions")
    suspend fun addMarkersAt(
        addresses: List<W3WAddress>,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        return@withContext addMarkersInternal(
            listName = listName,
            inputs = addresses,
            markerColor = markerColor,
            convertFunction = {
                if(it.center == null) {
                    textDataSource.convertToCoordinates(it.words)
                } else
                {
                    W3WResult.Success(it)
                }
            },
            zoomOption = zoomOption,
        )
    }

    private suspend fun <T> addMarkersInternal(
        inputs: List<T>,
        convertFunction: suspend (T) -> W3WResult<W3WAddress>,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        val results = inputs.map { input -> async { convertFunction(input) } }.awaitAll().map { result ->
            when (result) {
                is W3WResult.Success -> {
                    W3WResult.Success(result.value.toW3WMarker(markerColor))
                }
                is W3WResult.Failure -> {
                    W3WResult.Failure(result.error, result.message)
                }
            }
        }

        val markers = results.filterIsInstance<W3WResult.Success<W3WMarker>>()
        markers.forEach {
            markersMap.addMarker(listName = listName, marker = it.value)
        }

        // Update the map state
        _mapState.update { currentState ->
            currentState.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        handleZoomOption(markers.map { it.value.center }, zoomOption)

        return@withContext results
    }

    /**
     * Handle zoom option for a [W3WCoordinates] with multiple zoom options which will use the zoom level
     * if it's provided or the default zoom level.
     */
    private suspend fun handleZoomOption(
        coordinates: W3WCoordinates,
        zoomOption: W3WZoomOption,
        zoom: Float?
    ) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER -> {
                mapState.value.cameraState?.moveToPosition(coordinates, animate = true)
            }

            W3WZoomOption.CENTER_AND_ZOOM -> {
                mapState.value.cameraState?.moveToPosition(
                    coordinates,
                    zoom,
                    animate = true
                )
            }
        }
    }

    /**
     * Handle zoom option for a list of [W3WCoordinates] with multiple zoom options which will use the zoom level
     * if it's provided or the default zoom level.
     */
    private suspend fun handleZoomOption(listCoordinates: List<W3WCoordinates>, zoomOption: W3WZoomOption) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER, W3WZoomOption.CENTER_AND_ZOOM -> {
                mapState.value.cameraState?.moveToPosition(listCoordinates)
            }
        }
    }

    fun updateAccuracyDistance(distance: Float) {
        _buttonState.update {
            it.copy(accuracyDistance = distance)
        }
    }

    fun updateLocationStatus(locationStatus: LocationStatus) {
        _buttonState.update {
            it.copy(locationStatus = locationStatus)
        }
    }

    fun setMapProjection(mapProjection: W3WMapProjection) {
        _buttonState.update {
            it.copy(mapProjection = mapProjection)
        }
    }

    fun setMapViewPort(mapViewPort: W3WGridScreenCell) {
        _buttonState.update {
            it.copy(
                mapViewPort = mapViewPort,
                recallButtonViewPort = W3WGridScreenCell(
                    PointF(mapViewPort.v1.x, 0f),
                    PointF(mapViewPort.v2.x, 0f),
                    mapViewPort.v3,
                    mapViewPort.v4
                )
            )
        }
    }

    fun setRecallButtonPosition(recallButtonPosition: PointF) {
        _buttonState.update {
            it.copy(recallButtonPosition = recallButtonPosition)
        }
    }

    fun setRecallButtonEnabled(isEnabled: Boolean) {
        _buttonState.update {
            it.copy(isRecallButtonEnabled = isEnabled)
        }
    }

    private suspend fun updateSelectedScreenLocation() {
        withContext(dispatcher) {
            val selectedAddress = mapState.value.selectedAddress?.center
            val mapProjection = buttonState.value.mapProjection
            withContext(Dispatchers.Main) {
                val selectedScreenLocation =
                    selectedAddress?.let { mapProjection?.toScreenLocation(it) }
                _buttonState.update {
                    it.copy(
                        selectedScreenLocation = selectedScreenLocation,
                    )
                }
            }
        }
    }

    private suspend fun updateRecallButtonColor() {
        withContext(dispatcher) {
            val selectedLatLng = getSelectedAddress()?.center ?: return@withContext

            val markersAtSelectedSquare =
                getMarkersAt(selectedLatLng)

            val markerSlashColor = if (markersAtSelectedSquare.size == 1) {
                markersAtSelectedSquare.first().marker.color.slash
            } else {
                Color.White
            }

            val markerBackgroundColor = if (markersAtSelectedSquare.size == 1) {
                markersAtSelectedSquare.first().marker.color.background
            } else {
                Color(0xFFE11F26) // TODO: Define name for this color
            }

            _buttonState.update {
                it.copy(
                    recallArrowColor = markerSlashColor,
                    recallBackgroundColor = markerBackgroundColor
                )
            }
        }
    }

    private suspend fun handleRecallButton() {
        updateSelectedScreenLocation()
        updateRecallButtonColor()

        val buttonState = buttonState.value
        val selectedScreenLocation = buttonState.selectedScreenLocation
        val recallButtonViewport = buttonState.recallButtonViewPort
        val mapProjection = buttonState.mapProjection
        val recallButtonPosition = buttonState.recallButtonPosition

        // Early return if necessary data is null
        if (mapProjection == null || selectedScreenLocation == null) return

        val shouldShowRecallButton =
            recallButtonViewport?.containsPoint(selectedScreenLocation) == false
        val rotationDegree =
            computeRecallButtonRotation(selectedScreenLocation, recallButtonPosition)

        _buttonState.update {
            it.copy(
                recallRotationDegree = rotationDegree,
                isRecallButtonVisible = shouldShowRecallButton,
            )
        }
    }

    private fun computeRecallButtonRotation(
        selectedScreenLocation: PointF,
        recallButtonPosition: PointF
    ) = angleOfPoints(recallButtonPosition, selectedScreenLocation).let { alpha ->
        // add 180 degrees to computed value to compensate arrow rotation
        (180 + alpha) * -1
    }

    fun setMapConfig(mapConfig: W3WMapDefaults.MapConfig) {
        this.mapConfig = mapConfig
    }

    companion object {
        const val LIST_DEFAULT_ID = "LIST_DEFAULT_ID"
        const val TAG = "W3WMapManager"

        /**
         * The default saver implementation for [W3WMapManager].
         */
        val Saver: Saver<W3WMapManager, *> = Saver(
            save = { manager: W3WMapManager ->
                val cameraState = manager.mapState.value.cameraState
                mapOf(
                    "textDataSource" to manager.textDataSource,
                    "mapProvider" to manager.mapProvider,
                    "language" to manager.language,
                    "mapState" to manager.mapState.value,
                    "buttonState" to manager.buttonState.value,
                    "mapConfig" to manager.mapConfig,
                    "markersMap" to manager.markersMap.mapValues { (_, markers) ->
                        markers.toList()
                    },
                    "cameraState" to when (cameraState) {
                        is W3WGoogleCameraState -> mapOf(
                            "type" to "google",
                            "position" to cameraState.cameraState.position
                        )

                        is W3WMapboxCameraState -> mapOf(
                            "type" to "mapbox",
                            "position" to cameraState.cameraState.cameraState
                        )

                        else -> null
                    }
                )
            },
            restore = { savedMap: Map<String, Any?> ->
                val mapProvider = savedMap["mapProvider"] as MapProvider
                val restoredCameraState = savedMap["cameraState"] as? Map<String, Any>
                val cameraState = when (restoredCameraState?.get("type") as? String) {
                    "google" -> W3WGoogleCameraState(CameraPositionState(restoredCameraState["position"] as CameraPosition))
                    "mapbox" -> W3WMapboxCameraState(MapViewportState(restoredCameraState["position"] as CameraState))
                    else -> when (mapProvider) {
                        MapProvider.GOOGLE_MAP -> W3WGoogleCameraState(CameraPositionState())
                        MapProvider.MAPBOX -> W3WMapboxCameraState(MapViewportState())
                    }
                }
                val restoredMapState =
                    (savedMap["mapState"] as W3WMapState).copy(cameraState = cameraState)
                val restoredButtonState = savedMap["buttonState"] as W3WButtonsState

                W3WMapManager(
                    textDataSource = savedMap["textDataSource"] as W3WTextDataSource,
                    mapProvider = mapProvider,
                    initialMapState = restoredMapState,
                    initialButtonState = restoredButtonState
                ).apply {
                    language = savedMap["language"] as W3WRFC5646Language
                    mapConfig = savedMap["mapConfig"] as W3WMapDefaults.MapConfig?
                    markersMap.clear()
                    markersMap.putAll(
                        (savedMap["markersMap"] as Map<String, List<W3WMarker>>).mapValues {
                            it.value.toMutableList()
                        }
                    )
                }
            }
        )
    }
}

/**
 * Create and [rememberSaveable] a [W3WMapManager] using [W3WMapManager.Saver].
 * [init] will be called when the [W3WMapManager] is first created to configure its
 * initial state.
 */
@Composable
inline fun rememberW3WMapManager(
    key: String? = null,
    textDataSource: W3WTextDataSource,
    mapProvider: MapProvider,
    crossinline init: W3WMapManager.() -> Unit = {}
): W3WMapManager = rememberSaveable(key = key, saver = W3WMapManager.Saver) {
    W3WMapManager(textDataSource = textDataSource, mapProvider = mapProvider).apply(init)
}