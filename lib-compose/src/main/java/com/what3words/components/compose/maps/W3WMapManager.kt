package com.what3words.components.compose.maps

import android.annotation.SuppressLint
import android.graphics.PointF
import androidx.annotation.RequiresPermission
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
import com.what3words.components.compose.maps.extensions.area
import com.what3words.components.compose.maps.extensions.calculateAreaOverlap
import com.what3words.components.compose.maps.extensions.computeHorizontalLines
import com.what3words.components.compose.maps.extensions.computeVerticalLines
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.extensions.toMarkers
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.mapper.toW3WMarker
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.models.W3WMarkerWithList
import com.what3words.components.compose.maps.models.W3WGridScreenCell
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.components.compose.maps.state.W3WButtonsState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WGoogleCameraState
import com.what3words.components.compose.maps.state.camera.W3WMapboxCameraState
import com.what3words.components.compose.maps.utils.angleOfPoints
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
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
 * @param textDataSource An instance of [W3WTextDataSource], used for fetching what3words address information.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param mapState An optional [W3WMapState] object representing the initial state of the map. If not
 *   provided, a default [W3WMapState] is used.
 *
 * @property mapState A read-only [StateFlow] of [W3WMapState] exposing the current state of the map.
 */
class W3WMapManager(
    private val textDataSource: W3WTextDataSource,
    val mapProvider: MapProvider,
    mapState: W3WMapState = W3WMapState(),
    buttonState: W3WButtonsState = W3WButtonsState(),
    private val dispatcher: CoroutineDispatcher = IO,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _mapState: MutableStateFlow<W3WMapState> = MutableStateFlow(mapState)
    val mapState: StateFlow<W3WMapState> = _mapState.asStateFlow()

    private val _buttonState: MutableStateFlow<W3WButtonsState> = MutableStateFlow(buttonState)
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
                    MapProvider.MAPBOX -> createMapboxCameraState()
                    MapProvider.GOOGLE_MAP -> createGoogleCameraState()
                }
            )
        }
        observeGridCalculation()
    }

    private fun createMapboxCameraState(): W3WMapboxCameraState {
        return W3WMapboxCameraState(
            MapViewportState(
                initialCameraState = CameraState(
                    Point.fromLngLat(LOCATION_DEFAULT.lng, LOCATION_DEFAULT.lat),
                    EdgeInsets(0.0, 0.0, 0.0, 0.0),
                    W3WMapboxCameraState.MY_LOCATION_ZOOM,
                    0.0,
                    0.0
                )
            )
        )
    }

    private fun createGoogleCameraState(): W3WGoogleCameraState {
        return W3WGoogleCameraState(
            CameraPositionState(
                position = CameraPosition(
                    LOCATION_DEFAULT.toGoogleLatLng(),
                    W3WGoogleCameraState.MY_LOCATION_ZOOM,
                    0f,
                    0f
                )
            )
        )
    }

    private fun observeGridCalculation() {
        scope.launch {
            gridCalculationFlow
                .filterNotNull()
                .collect { newGridBound ->
                    if (shouldCalculateGrid(newGridBound, lastProcessedGridBound)) {
                        calculateAndUpdateGrid(newGridBound)
                    }
                }
        }
    }

    private fun shouldCalculateGrid(
        newGridBound: W3WRectangle,
        oldGridBound: W3WRectangle?,
        areaOverlapThreshold: Double = 0.5,
        zoomOutThreshold: Double = 1.2
    ): Boolean {
        // Always calculate if there's no previous grid
        if (oldGridBound == null) return true

        // Check if the view has zoomed out significantly
        if (isSignificantZoomOut(newGridBound, oldGridBound, zoomOutThreshold)) {
            return true
        }

        // Check if we're below the minimum zoom level for grid calculation
        if (isBelowMinimumZoom()) {
            return false
        }

        // Check if the view has moved significantly
        return hasSignificantMovement(newGridBound, oldGridBound, areaOverlapThreshold)
    }

    private fun isSignificantZoomOut(
        newGridBound: W3WRectangle,
        oldGridBound: W3WRectangle,
        zoomOutThreshold: Double
    ): Boolean {
        return newGridBound.area / oldGridBound.area > zoomOutThreshold
    }

    private fun isBelowMinimumZoom(): Boolean {
        val zoomLevel = _mapState.value.cameraState?.getZoomLevel()
        val zoomSwitchLevel = mapConfig?.gridLineConfig?.zoomSwitchLevel
        return zoomLevel != null && zoomSwitchLevel != null && zoomLevel < zoomSwitchLevel
    }

    private fun hasSignificantMovement(
        newGridBound: W3WRectangle,
        oldGridBound: W3WRectangle,
        areaOverlapThreshold: Double
    ): Boolean {
        return newGridBound.calculateAreaOverlap(oldGridBound) < areaOverlapThreshold
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
     * @param address The [W3WAddress] to select.
     */
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
     * @param address The what3words address as a [String].
     */
    suspend fun setSelectedAddress(
        address: String,
    ) = withContext(dispatcher) {
        when (val result = textDataSource.convertToCoordinates(address)) {
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
     * @param address The What3Words address as a [String].
     * @param listName The name of the list from which markers should be removed. If null, markers will be removed from all lists.
     */
    suspend fun removeMarkerAt(
        address: String,
        listName: String? = null
    ): List<W3WMarker> = withContext(dispatcher) {
        val removedMarkers = mutableListOf<W3WMarker>()

        if (listName == null) {
            // Remove markers from all lists
            markersMap.forEach { (_, markers) ->
                val toRemove = markers.filter { it.words == address }
                removedMarkers.addAll(toRemove)
                markers.removeAll(toRemove)
            }

            markersMap.entries.removeIf { it.value.isEmpty() }
        } else {
            // Remove markers only from the specified list
            val markers = markersMap[listName]
            if (markers != null) {
                val toRemove = markers.filter { it.words == address }
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
     * @param address The What3Words address to search for markers.
     * @return A list of pairs, where each pair contains the name of the marker's list and the marker itself.
     *         The list is empty if no markers are found at the given address.
     */
    fun getMarkersAt(
        address: String
    ): List<W3WMarkerWithList> {
        return markersMap.flatMap { (listName, markers) ->
            markers.filter { it.words == address }
                .map { marker -> W3WMarkerWithList(listName, marker) }
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
     * @param address The what3words address where the marker should be placed.
     * @param markerColor The color of the marker. Defaults to MARKER_COLOR_DEFAULT.
     * @param listName The name of the list to which the marker should be added. Defaults to LIST_DEFAULT_ID.
     * @param zoomOption Specifies how the map should adjust its view after adding the marker. Defaults to CENTER_AND_ZOOM.
     * @param zoomLevel The zoom level to set if zooming is specified in zoomOption. If not provided, the default zoom level will be used.
     * @return A W3WResult containing either the created W3WMarker on success, or a W3WError on failure.
     */
    suspend fun addMarkerAt(
        address: String,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        when (val result = textDataSource.convertToCoordinates(address)) {
            is W3WResult.Failure -> {
                return@withContext W3WResult.Failure(result.error)
            }

            is W3WResult.Success -> {
                val marker = result.value.toW3WMarker(markerColor)
                addMakerInternal(marker, listName, zoomOption, zoomLevel)
                return@withContext W3WResult.Success(marker)
            }
        }
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
    suspend fun addMarkerAt(
        coordinates: W3WCoordinates,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        when (val result =
            textDataSource.convertTo3wa(
                W3WCoordinates(lat = coordinates.lat, lng = coordinates.lng),
                language
            )) {
            is W3WResult.Failure -> {
                return@withContext W3WResult.Failure(result.error)
            }

            is W3WResult.Success -> {
                val marker = result.value.toW3WMarker(markerColor)
                addMakerInternal(marker, listName, zoomOption, zoomLevel)
                return@withContext W3WResult.Success(marker)
            }
        }
    }

    private suspend fun addMakerInternal(
        marker: W3WMarker,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ) = withContext(dispatcher) {
        markersMap.addMarker(listName = listName, marker = marker)

        // Update the map state
        _mapState.update { currentState ->
            currentState.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        // Handle zoom options
        handleZoomOption(marker.center, zoomOption, zoomLevel)

//        logMarkersMap()
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

    fun updateAccuracyDistance(distance: Float) {
        _buttonState.update {
            it.copy(accuracyDistance = distance)
        }
    }

    fun updateIsLocationActive(isActive: Boolean) {
        _buttonState.update {
            it.copy(isLocationActive = isActive)
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
    }
}