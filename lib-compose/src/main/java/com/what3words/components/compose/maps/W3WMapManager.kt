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
     * Sets the selected marker at a specific what3Words address.
     *
     * Selects a marker using its geographical center (latitude and longitude) if available.
     * If the center is null, the selection is made using the three-word address (`words`).
     *
     * @param suggestion The what3Words suggestion as a [W3WSuggestion], which contains an address with optional center.
     */
    @JvmName("setSelectedAtSuggestion")
    suspend fun setSelectedAt(
        suggestion: W3WSuggestion
    ) = withContext(dispatcher) {
        suggestion.w3wAddress.center?.let {
            setSelectedInternal(suggestion.w3wAddress)
        }?:run {
            setSelectedAt(suggestion.w3wAddress.words)
        }
    }

    /**
     * Sets the selected marker at a specific what3Words address.
     *
     * Selects a marker based on its geographical center (latitude and longitude) or its three-word address.
     * If the `W3WAddress` has a valid center, it is used for selection; otherwise, the three-word address (`words`) is used.
     *
     * @param address The what3Words address as a [W3WAddress], which may or may not have a center (latitude and longitude).
     */
    @JvmName("setSelectedAtAtAddress")
    suspend fun setSelectedAt(
        address: W3WAddress
    ) = withContext(dispatcher) {
        address.center?.let {
            setSelectedInternal(address)
        }?:run {
            setSelectedAt(address.words)
        }
    }

    /**
     * Selects an what3words address on the map at a specific what3words address.
     *
     * @param words The what3words address as a [String].
     */
    @JvmName("setSelectedAtWords")
    suspend fun setSelectedAt(
        words: String
    ) = withContext(dispatcher) {
        when (val result = textDataSource.convertToCoordinates(words)) {
            is W3WResult.Failure -> {
                throw result.error
            }

            is W3WResult.Success -> {
                setSelectedInternal(result.value)
            }
        }
    }

    /**
     * Selects an what3words address on the map at a specific [W3WCoordinates].
     *
     * @param coordinates The [W3WCoordinates] to be selected.
     */
    @JvmName("setSelectedAtCoordinates")
    suspend fun setSelectedAt(
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
                setSelectedInternal(result.value)
            }
        }
    }

    private suspend fun setSelectedInternal(
        address: W3WAddress
    ) {
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
     * @param words The what3Words address as a [String].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     *
     * @return A list of [W3WMarker] objects that were removed. If no markers are found, an empty list is returned.
     *
     * @see [W3WMarker]
     *
     */
    @JvmName("removeMarkerAtWords")
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
     *
     * @return A list of [W3WMarker] objects that were removed. If no markers are found, an empty list is returned.
     *
     * @see [W3WCoordinates]
     * @see [W3WMarker]
     *
     */
    @JvmName("removeMarkerAtCoordinates")
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
     * The process is as follows:
     * - If the address has a valid center (latitude and longitude) [W3WAddress.center], the marker is removed using those coordinates3.
     * - If the center is null, the function uses the three-word address (e.g., "filled.count.soap") [W3WAddress.words]
     *   to identify and remove the marker instead.
     *
     * @param address The what3Words address as a [W3WAddress].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     *
     * @return A list of [W3WMarker] objects that were removed. If no markers are found, an empty list is returned.
     *
     * @see [W3WAddress]
     * @see [W3WMarker]
     *
     */
    @JvmName("removeMarkerAtAddress")
    suspend fun removeMarkerAt(
        address: W3WAddress,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        address.center?.let {
            removeMarkerAt(it, listName)
        }?:run {
            removeMarkerAt(address.words, listName)
        }
    }

    /**
     * Removes markers from the map at a specific what3Words address contained in a [W3WSuggestion].
     *
     * The process is as follows:
     * - If the address has a valid center (latitude and longitude) [W3WAddress.center], the marker is removed using those coordinates.
     * - If the center is null, the function uses the three-word address (e.g., "filled.count.soap") [W3WAddress.words]
     *   to identify and remove the marker instead.
     *
     * @param suggestion The what3Words address as a [W3WSuggestion], which contains a [W3WAddress] with optional center (latitude and longitude) and three-word address.
     * @param listName The name of the list from which markers should be removed. If `null`, markers will be removed from all lists. If a specific list name is provided, markers will only be removed from that list.
     *
     * @return A list of [W3WMarker] objects that were removed. If no markers are found, an empty list will be returned.
     *
     * @see [W3WSuggestion]
     * @see [W3WAddress]
     * @see [W3WMarker]
     */
    @JvmName("removeMarkerAtSuggestion")
    suspend fun removeMarkerAt(
        suggestion: W3WSuggestion,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        suggestion.w3wAddress.center?.let {
            removeMarkerAt(it, listName)
        }?:run {
            removeMarkerAt(suggestion.w3wAddress.words, listName)
        }
    }

    /**
     * Removes markers from the map at specific what3Words addresses based on a list of [String] addresses.
     *
     * The function will iterate over each address in the `listWords` and remove any corresponding markers.
     * - If `listName` is `null`, it will remove markers from all lists in the map.
     * - If `listName` is provided, only markers from that specific list will be removed.
     *
     * @param listWords A list of what3Words address strings (e.g., "filled.count.soap").
     * @param listName The name of the list from which markers should be removed. If `null`, markers will be removed from all lists. If a specific list name is provided, only markers from that list will be removed.
     *
     * @return A list of [W3WMarker] objects that were removed. If no markers are found for the provided words, an empty list will be returned.
     *
     * @see [W3WMarker]
     */
    @JvmName("removeMarkerAtListWords")
    suspend fun removeMarkersAt(
        listWords: List<String>,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        val removedMarkers = mutableListOf<W3WMarker>()
        listWords.forEach { words ->
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
        }

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        return@withContext removedMarkers
    }

    /**
     * Removes markers from the map at specific coordinates based on a list of [W3WCoordinates].
     *
     * The function will iterate over each set of coordinates in `listCoordinates` and remove any markers whose
     * location matches those coordinates. The removal behavior is as follows:
     * - If `listName` is `null`, markers will be removed from all lists.
     * - If a specific `listName` is provided, markers will only be removed from that list.
     *
     * @param listCoordinates A list of [W3WCoordinates] for which markers should be removed.
     * @param listName The name of the list from which markers should be removed. If `null`, markers will be removed from all lists. If a specific list name is provided, markers from that list will be removed.
     *
     * @return A list of [W3WMarker] objects that were removed. If no markers are found for the given coordinates, an empty list will be returned.
     *
     * @see [W3WCoordinates]
     * @see [W3WMarker]
     */
    @JvmName("removeMarkerAtListCoordinates")
    suspend fun removeMarkersAt(
        listCoordinates: List<W3WCoordinates>,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        val removedMarkers = mutableListOf<W3WMarker>()
        listCoordinates.forEach { coordinates ->
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
        }

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        return@withContext removedMarkers
    }

    /**
     * Removes markers from the map at specific what3Words addresses based on a list of [W3WAddress].
     *
     * The removal behavior is as follows:
     * - If `listName` is `null`, markers will be removed from all lists.
     * - If a specific `listName` is provided, markers will only be removed from that list.
     *
     * @param addresses A list of [W3WAddress] objects, each representing a specific what3Words address.
     *                  The address can be identified by its center (latitude and longitude) or words.
     * @param listName The name of the list from which markers should be removed. If `null`, markers will be removed from all lists.
     *
     * @return A list of [W3WMarker] objects that were removed. If no markers are found for the given addresses, an empty list will be returned.
     *
     * @see [W3WAddress]
     * @see [W3WMarker]
     */
    @JvmName("removeMarkerAtAddresses")
    suspend fun removeMarkersAt(
        addresses: List<W3WAddress>,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        val removedMarkers = mutableListOf<W3WMarker>()

        addresses.forEach { address ->
            val markersToRemove = address.center?.let {
                removeMarkerAt(it, listName)
            } ?: removeMarkerAt(address.words, listName)
            removedMarkers.addAll(markersToRemove)
        }

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        return@withContext removedMarkers
    }

    /**
     * Removes markers from the map at specific what3Words addresses contained in a list of [W3WSuggestion].
     *
     * The function will iterate over each [W3WSuggestion] in the `suggestions` list and remove the corresponding markers:
     * - If `listName` is `null`, markers will be removed from all lists in the map.
     * - If `listName` is provided, only markers from that specific list will be removed.
     *
     * @param suggestions A list of [W3WSuggestion] objects, where each suggestion contains a [W3WAddress] (either with a center or words).
     * @param listName The name of the list from which markers should be removed. If `null`, markers will be removed from all lists. If a specific list name is provided, only markers from that list will be removed.
     *
     * @return A list of [W3WMarker] objects that were removed. If no markers are found for the provided suggestions, an empty list will be returned.
     *
     * @see [W3WSuggestion]
     * @see [W3WAddress]
     * @see [W3WMarker]
     */
    @JvmName("removeMarkerAtSuggestions")
    suspend fun removeMarkersAt(
        suggestions: List<W3WSuggestion>,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        removeMarkersAt(suggestions.map { it.w3wAddress },listName)
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
     * @param words The what3Words address to search for markers.
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
     * Retrieves markers from the map at a specific location based on a [W3WAddress].
     *
     * The function checks if the [W3WAddress] contains a valid center (latitude and longitude):
     * - If the center is available, it retrieves markers using the coordinates.
     * - If the center is not available (i.e., `null`), it retrieves markers using the three-word address (e.g., "filled.count.soap").
     *
     * @param address The [W3WAddress] containing the [W3WAddress]. The address may either have a valid center (latitude and longitude)
     * or just the three-word address (words).
     *
     * @return A list of [W3WMarkerWithList] objects. This list consists of pairs, where each pair contains:
     * - The name of the marker's list.
     * - The marker itself ([W3WMarker]).
     *
     * If no markers are found for the provided address, an empty list is returned.
     *
     * @see [W3WAddress]
     * @see [W3WMarkerWithList]
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
     * Retrieves markers from the map at a specific location based on a [W3WSuggestion].
     *
     * The function checks if the [W3WSuggestion] contains a valid center (latitude and longitude):
     * - If the center is available, it retrieves markers using the coordinates.
     * - If the center is not available (i.e., `null`), it retrieves markers using the three-word address (e.g., "filled.count.soap").
     *
     * @param suggestion The [W3WSuggestion] containing the [W3WAddress]. The address may either have a valid center (latitude and longitude)
     * or just the three-word address (words).
     *
     * @return A list of [W3WMarkerWithList] objects. This list consists of pairs, where each pair contains:
     * - The name of the marker's list.
     * - The marker itself ([W3WMarker]).
     *
     * If no markers are found for the provided address, an empty list is returned.
     *
     * @see [W3WSuggestion]
     * @see [W3WMarkerWithList]
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
     * Adds a marker to the map at a specific what3Words address represented by a string (e.g., "filled.count.soap").
     *
     * The function attempts to convert the provided three-word address (`words`) into [W3WAddress] using [textDataSource].
     * If successful, it creates a marker at the corresponding location on the map.
     *
     * After adding the marker, the map state is updated, and the zoom behavior is applied based on the provided [zoomOption] and [zoomLevel].
     *
     * @param words The what3Words address as a string (e.g., "filled.count.soap") to locate and add a marker at.
     * @param markerColor The color of the marker to be added. Defaults to [MARKER_COLOR_DEFAULT].
     * @param listName The name of the list to which the marker will be added. Defaults to [LIST_DEFAULT_ID].
     * @param zoomOption The zoom behavior after the marker is added. Defaults to [W3WZoomOption.CENTER_AND_ZOOM].
     * @param zoomLevel The zoom level to apply if zooming is enabled. If `null`, the default zoom behavior is applied.
     *
     * @return A [W3WResult] containing either the added [W3WMarker] or a failure result with an error message if the conversion fails.
     *
     * @see [W3WMarker]
     * @see [W3WZoomOption]
     * @see [textDataSource]
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
     * Adds a marker to the map at a specific [W3WCoordinates] location.
     *
     * This function attempts to convert the provided [W3WCoordinates] into [W3WAddress] using [textDataSource].
     * If the conversion is successful, a marker is created at the corresponding coordinates.
     *
     * After adding the marker, the map state is updated, and the zoom behavior is applied based on the provided [zoomOption] and [zoomLevel].
     *
     * @param coordinates The [W3WCoordinates] to add as a marker.
     * @param listName The name of the list to which the marker will be added. Defaults to [LIST_DEFAULT_ID].
     * @param markerColor The color of the marker to be added. Defaults to [MARKER_COLOR_DEFAULT].
     * @param zoomOption The zoom behavior after the marker is added. Defaults to [W3WZoomOption.CENTER_AND_ZOOM].
     * @param zoomLevel The zoom level to apply if zooming is enabled. If `null`, the default zoom behavior is applied.
     *
     * @return A [W3WResult] containing either the added [W3WMarker] or a failure result with an error message.
     *
     * @see [W3WCoordinates]
     * @see [W3WMarker]
     * @see [W3WZoomOption]
     * @see [textDataSource]
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
     * Adds a marker to the map at a specific what3Words address.
     *
     * The function checks if the provided [W3WAddress] has a valid center (latitude and longitude):
     * - If the center is available, the marker is created using those [W3WAddress].
     * - If the center is `null`, the function attempts to convert the three-word address into coordinates using [textDataSource].
     *
     * After the marker is added, the map state is updated, and the zoom behavior is applied based on the provided [zoomOption] and [zoomLevel].
     *
     * @param address The [W3WAddress] to add as a marker. It may contain coordinates or a three-word address.
     * @param listName The name of the list to which the marker will be added. Defaults to [LIST_DEFAULT_ID].
     * @param markerColor The color of the marker to be added. Defaults to [MARKER_COLOR_DEFAULT].
     * @param zoomOption The zoom behavior after the marker is added. Defaults to [W3WZoomOption.CENTER_AND_ZOOM].
     * @param zoomLevel The zoom level to apply if zooming is enabled. If `null`, the default zoom behavior is applied.
     *
     * @return A [W3WResult] containing either the added [W3WMarker] or a failure result with an error message.
     *
     * @see [W3WAddress]
     * @see [W3WMarker]
     * @see [W3WZoomOption]
     * @see [textDataSource]
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
     * Adds a marker to the map at a specific [W3WSuggestion].
     *
     * The function will attempt to use the [W3WAddress] contained in the [W3WSuggestion].
     * - If the address has a valid center (latitude and longitude), it will add the marker using those [W3WAddress].
     * - If the center is `null`, the function will convert the three-word address to coordinates using [textDataSource].
     *
     * After the marker is added, the map state is updated, and the zoom behavior is applied based on the provided [zoomOption] and [zoomLevel].
     *
     * @param suggestion The [W3WSuggestion] that contains the what3Words address to be used.
     * @param listName The name of the list to which the marker will be added. If `null`, markers will be added to all lists.
     * @param markerColor The color of the marker to be added. Defaults to [MARKER_COLOR_DEFAULT].
     * @param zoomOption The zoom behavior after the marker is added. Defaults to [W3WZoomOption.CENTER_AND_ZOOM].
     * @param zoomLevel The zoom level to apply if zooming is enabled. If `null`, the default zoom behavior is applied.
     *
     * @return A [W3WResult] containing either the added [W3WMarker] or a failure result with an error message.
     *
     * @see [W3WSuggestion]
     * @see [W3WAddress]
     * @see [W3WMarker]
     * @see [W3WZoomOption]
     * @see [textDataSource]
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

    /**
     * Adds a marker to the map at a specific what3Words address.
     *
     * The process is as follows:
     * - The function converts the input into a [W3WAddress] using the provided [convertFunction].
     * - If the conversion is successful:
     *     - A marker is created from the address.
     *     - The marker is added to the map (associated with a specific list if given).
     *     - The map state is updated to reflect the newly added marker.
     *     - If a zoom option is specified, the map is zoomed to include the marker.
     * - If the conversion fails, a failure result is returned.
     *
     * @param input The input to be converted into a [W3WAddress] (e.g., a string or coordinates).
     * @param convertFunction A suspend function that converts the input into a [W3WResult] containing a [W3WAddress].
     *                        The function may return either a successful address or a failure.
     * @param markerColor The color of the marker to be added. Defaults to [MARKER_COLOR_DEFAULT].
     * @param listName The name of the list in which the marker will be added. Defaults to [LIST_DEFAULT_ID].
     * @param zoomOption Defines the zoom behavior after adding the marker. Defaults to [W3WZoomOption.CENTER_AND_ZOOM].
     * @param zoomLevel The zoom level to apply if zooming is enabled. If `null`, the default zoom behavior is applied.
     *
     * @return A [W3WResult] indicating whether the marker was successfully added or if there was an error.
     *         If successful, it contains the added [W3WMarker]. If failed, it contains an error message and error code.
     *
     * @see [W3WAddress]
     * @see [W3WMarker]
     * @see [W3WZoomOption]
     * @see [W3WMarkerColor]
     */
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
                val addMarkerResult = markersMap.addMarker(listName = listName, marker = marker)

                if(addMarkerResult is W3WResult.Success) {
                    // Update the map state
                    _mapState.update { currentState ->
                        currentState.copy(
                            markers = markersMap.toMarkers().toImmutableList()
                        )
                    }

                    // Handle zoom option
                    handleZoomOption(marker.center, zoomOption, zoomLevel)
                }

                addMarkerResult
            }

            is W3WResult.Failure -> W3WResult.Failure(result.error, result.message)
        }

    }

    /**
     * Adds markers to the map for a list of what3Words addresses (provided as strings).
     *
     * The function will convert each what3Words address (e.g., "filled.count.soap") to coordinates
     * and add a corresponding marker to the map.
     * - If `listName` is specified, markers will be added to that specific list.
     * - If no `listName` is provided, markers will be added to the default list.
     * - The function handles the conversion of each address to coordinates and adds markers at those locations.
     *
     * @param listWords A list of what3Words address strings (e.g., "filled.count.soap") to which markers should be added.
     * @param listName The name of the list to which the markers will be added. If not specified, the default list (`LIST_DEFAULT_ID`) will be used.
     * @param markerColor The color of the markers. If not specified, the default color (`MARKER_COLOR_DEFAULT`) will be used.
     * @param zoomOption The zoom option for the map. If not specified, the default option (`CENTER_AND_ZOOM`) will be used.
     *
     * @return A list of [W3WResult] objects representing the result of adding each marker.
     *         - [W3WResult.Success] will contain the successfully added [W3WMarker].
     *         - [W3WResult.Failure] will contain the error if any marker failed to be added.
     *
     * @see [W3WMarker]
     * @see [W3WResult]
     * @see [W3WZoomOption]
     * @see [W3WMarkerColor]
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
     * Adds markers to the map for a list of [W3WCoordinates].
     *
     * This function converts each [W3WCoordinates] object into a what3Words address using the `convertTo3wa` function
     * and then adds the corresponding marker to the map at the specified coordinates.
     * - If `listName` is specified, markers will be added to that specific list.
     * - If no `listName` is provided, markers will be added to the default list (`LIST_DEFAULT_ID`).
     * - The function handles the conversion of each coordinate to a what3Words address and adds markers at the corresponding locations.
     *
     * @param listCoordinates A list of [W3WCoordinates] objects that specify the coordinates where markers should be placed.
     * @param listName The name of the list to which the markers will be added. If not specified, the default list (`LIST_DEFAULT_ID`) will be used.
     * @param markerColor The color of the markers. If not specified, the default color (`MARKER_COLOR_DEFAULT`) will be used.
     * @param zoomOption The zoom option for the map. If not specified, the default option (`CENTER_AND_ZOOM`) will be used.
     *
     * @return A list of [W3WResult] objects representing the result of adding each marker.
     *         - [W3WResult.Success] will contain the successfully added [W3WMarker].
     *         - [W3WResult.Failure] will contain the error if any marker failed to be added.
     * @see [W3WCoordinates]
     * @see [W3WResult]
     * @see [W3WZoomOption]
     * @see [W3WMarkerColor]
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
     * Adds markers to the map for a list of [W3WSuggestion] objects.
     *
     * This function processes each suggestion in the list and:
     * - If the suggestion contains a valid center (latitude and longitude), it uses that address to add a marker.
     * - If the suggestion does not contain a valid center, it converts the three-word address (e.g., "filled.count.soap") to what3Words address and then adds the marker.
     *
     * The markers will be added to the specified list if `listName` is provided. If `listName` is `null`, the markers will be added to the default list (`LIST_DEFAULT_ID`).
     *
     * @param suggestions A list of [W3WSuggestion] objects, each containing a what3Words address with optional center (latitude and longitude).
     * @param listName The name of the list to which markers will be added. If not specified, the default list (`LIST_DEFAULT_ID`) is used.
     * @param markerColor The color of the markers. If not specified, the default color (`MARKER_COLOR_DEFAULT`) is used.
     * @param zoomOption The zoom option for the map after markers are added. If not specified, the default option (`CENTER_AND_ZOOM`) is used.
     *
     * @return A list of [W3WResult] objects representing the result of adding each marker.
     *         - [W3WResult.Success] contains the successfully added [W3WMarker].
     *         - [W3WResult.Failure] contains the error if any marker failed to be added.
     *
     * @see [W3WSuggestion]
     * @see [W3WResult]
     * @see [W3WMarker]
     * @see [W3WZoomOption]
     * @see [W3WMarkerColor]
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
     * Adds markers to the map at specific what3Words addresses.
     *
     * This function processes a list of [W3WAddress] objects, and:
     * - If the address contains a valid center (latitude and longitude), the marker is added using those coordinates.
     * - If the address does not contain a valid center, it will convert the three-word address (e.g., "filled.count.soap") to what3Words address and then add the marker.
     *
     * Markers are added to the specified list. If `listName` is not provided, the markers will be added to the default list (`LIST_DEFAULT_ID`).
     *
     * @param addresses A list of [W3WAddress] objects, each containing a what3Words address. The address may either have a center (coordinates) or a three-word address.
     * @param listName The name of the list to which markers will be added. If not specified, the default list (`LIST_DEFAULT_ID`) is used.
     * @param markerColor The color of the markers. If not specified, the default color (`MARKER_COLOR_DEFAULT`) is used.
     * @param zoomOption The zoom option for the map after markers are added. If not specified, the default option (`CENTER_AND_ZOOM`) is used.

     * @return A list of [W3WResult] objects representing the result of adding each marker.
     *         - [W3WResult.Success] contains the successfully added [W3WMarker].
     *         - [W3WResult.Failure] contains the error if any marker failed to be added.
     *
     * @see [W3WAddress]
     * @see [W3WMarker]
     * @see [W3WResult]
     * @see [W3WZoomOption]
     * @see [W3WMarkerColor]
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

    /**
     * Adds markers to the map for a list of inputs by converting each input to a [W3WMarker] using the provided [convertFunction].
     * The function processes each input asynchronously and adds the corresponding marker to the map.
     *
     * The function will:
     * - Convert each input into a [W3WAddress] using the [convertFunction].
     * - If the conversion is successful, it adds the corresponding [W3WMarker] to the map.
     * - If the conversion fails, it returns a failure result for that specific input.
     *
     * Once all markers are added, the map state is updated, and the zoom option is applied.
     *
     * @param inputs The list of inputs to be processed. Each input will be passed to the [convertFunction].
     * @param convertFunction A suspend function that converts each input into a [W3WResult] containing a [W3WAddress].
     *                        The conversion can either succeed (providing a valid address) or fail.
     * @param markerColor The color of the marker to be added to the map. Defaults to [MARKER_COLOR_DEFAULT].
     * @param listName The name of the list in which markers will be added. Defaults to [LIST_DEFAULT_ID].
     * @param zoomOption Defines the zoom behavior after the markers are added. Defaults to [W3WZoomOption.CENTER_AND_ZOOM].
     *
     * @return A list of [W3WResult] where each result represents the outcome of the marker addition.
     *         - [W3WResult.Success] contains the added marker.
     *         - [W3WResult.Failure] contains an error if the marker couldn't be added.
     *
     * @see [W3WAddress]
     * @see [W3WMarker]
     * @see [W3WResult]
     * @see [W3WZoomOption]
     * @see [W3WMarkerColor]
     */
    private suspend fun <T> addMarkersInternal(
        inputs: List<T>,
        convertFunction: suspend (T) -> W3WResult<W3WAddress>,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        val conversionResults = inputs.map { input ->
            async {
                convertFunction(input)
            }
        }.awaitAll()

        val results = conversionResults.map { result ->
            when (result) {
                is W3WResult.Success -> {
                    val marker = result.value.toW3WMarker(markerColor)
                    markersMap.addMarker(listName = listName, marker = marker)
                }
                is W3WResult.Failure -> W3WResult.Failure(result.error, result.message)
            }
        }

        // Update the map state
        _mapState.update { currentState ->
            currentState.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        val successResults = results.filterIsInstance<W3WResult.Success<W3WMarker>>()
        handleZoomOption(successResults.map { it.value.center }, zoomOption)

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
            //TODO: Find solution to handle color in composable side due to manager no keep the color config
//            val markerColor = when (markersAtSelectedSquare.size) {
//                0 -> mapConfig?.markerConfig?.selectedZoomOutColor
//                1 -> markersAtSelectedSquare.first().marker.color
//                else -> mapConfig?.markerConfig?.defaultMarkerColor
//            }

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