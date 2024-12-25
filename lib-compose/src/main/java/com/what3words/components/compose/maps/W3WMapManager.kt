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
import com.what3words.androidwrapper.datasource.text.api.error.BadBoundingBoxError
import com.what3words.androidwrapper.datasource.text.api.error.BadBoundingBoxTooBigError
import com.what3words.components.compose.maps.W3WMapDefaults.LOCATION_DEFAULT
import com.what3words.components.compose.maps.W3WMapDefaults.MARKER_COLOR_DEFAULT
import com.what3words.components.compose.maps.extensions.computeHorizontalLines
import com.what3words.components.compose.maps.extensions.computeVerticalLines
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.mapper.toW3WLatLong
import com.what3words.components.compose.maps.mapper.toW3WSquare
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WGridScreenCell
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.models.W3WSquare
import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.components.compose.maps.state.LIST_DEFAULT_ID
import com.what3words.components.compose.maps.state.W3WButtonsState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WGoogleCameraState
import com.what3words.components.compose.maps.state.camera.W3WMapboxCameraState
import com.what3words.components.compose.maps.utils.angleOfPoints
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WGridSection
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WRFC5646Language
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
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
import kotlinx.coroutines.flow.collectLatest
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
 * @param textDataSource An instance of [W3WTextDataSource], used for fetching W3W address information.
 * @param dispatcher A [CoroutineDispatcher] used for managing coroutines, defaulting to
 *   [Dispatchers.IO] for background operations.
 * @param mapProvider An instance of enum [MapProvider] to define map provide: GoogleMap, MapBox.
 * @param mapState An optional [W3WMapState] object representing the initial state of the map. If not
 *   provided, a default [W3WMapState] is used.
 *
 * @property mapState A read-only [StateFlow] of [W3WMapState] exposing the current state of the map.
 */
class W3WMapManager(
    private val textDataSource: W3WTextDataSource,
    private val dispatcher: CoroutineDispatcher = IO,
    val mapProvider: MapProvider,
    mapState: W3WMapState = W3WMapState(),
    buttonState: W3WButtonsState = W3WButtonsState(),
) {
    private var language: W3WRFC5646Language = W3WRFC5646Language.EN_GB

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _mapState: MutableStateFlow<W3WMapState> = MutableStateFlow(mapState)
    val mapState: StateFlow<W3WMapState> = _mapState.asStateFlow()

    private val _buttonState: MutableStateFlow<W3WButtonsState> = MutableStateFlow(buttonState)
    val buttonState: StateFlow<W3WButtonsState> = _buttonState.asStateFlow()

    private val gridCalculationFlow = MutableStateFlow<W3WCameraState<*>?>(null)

    // A mutable map that stores lists of markers, keyed by a list name identifier
    private val markersMap: MutableMap<String, MutableList<W3WMarker>> = mutableMapOf()

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

// ====== API Functions ======
    //region Map Settings
    /** Set the language of [W3WAddress.words] that onSuccess callbacks should return.
     *
     * @param language a supported [W3WRFC5646Language]. Defaults to en (English).
     */
    fun setLanguage(language: W3WRFC5646Language) {
        this.language = language

    }

    fun isDarkMode(): Boolean {
        return mapState.value.isDarkMode
    }

    fun setDarkMode(darkMode: Boolean) {
        _mapState.update {
            it.copy(
                isDarkMode = darkMode
            )
        }
    }

    fun getMapType(): W3WMapType {
        return mapState.value.mapType
    }

    fun setMapType(mapType: W3WMapType) {
        _mapState.update {
            it.copy(
                mapType = mapType
            )
        }
    }

    fun setMapGesturesEnabled(enabled: Boolean) {
        _mapState.update {
            it.copy(
                isMapGestureEnable = enabled
            )
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    fun setMyLocationEnabled(enabled: Boolean) {
        _mapState.update {
            it.copy(
                isMyLocationEnabled = enabled
            )
        }
    }
    //endregion

    // region Camera control
    suspend fun orientCamera() {
        mapState.value.cameraState?.orientCamera()
    }

    suspend fun moveToPosition(
        coordinates: W3WCoordinates,
        zoom: Float? = null,
        bearing: Float? = null,
        tilt: Float? = null,
        animate: Boolean = false,
    ) {
        mapState.value.cameraState?.moveToPosition(
            coordinates = coordinates,
            zoom,
            bearing,
            tilt,
            animate
        )
    }

    suspend fun updateCameraState(newCameraState: W3WCameraState<*>) = withContext(IO) {
        _mapState.update {
            it.copy(
                cameraState = newCameraState,
            )
        }
        gridCalculationFlow.value = newCameraState

        if (_buttonState.value.isRecallButtonEnabled) {
            handleRecallButton()
        }
    }
    //endregion

    //region Selected Address
    fun setSelectedMarker(marker: W3WMarker) {
        _mapState.value = mapState.value.copy(
            selectedAddress = marker.copy(
                color = marker.color,
                isInMultipleList = hasMultipleLists(marker, markersMap)
            )
        )
    }

    fun getSelectedMarker(): W3WMarker? {
        return mapState.value.selectedAddress
    }

    fun unselect() {
        _mapState.update {
            it.copy(
                selectedAddress = null
            )
        }
    }


    //endregion

    //region Markers
    fun removeAllMarkers() {
        _mapState.update {
            it.copy(
                markers = persistentListOf()
            )
        }
    }

    fun getAllMarkers(): List<W3WMarker> {
        return markersMap.values.flatten()
    }

    fun findMarkerByCoordinates(
        coordinates: W3WCoordinates
    ): W3WMarker? {
       return mapState.value.markers.firstOrNull {
           it.square.contains(coordinates)
       }
    }
    //endregion

    //region Marker with Words
    suspend fun addMarkerAtListWords(
        listName: String,
        listWords: List<String>,
        listColor: W3WMarkerColor,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
    ): List<W3WResult<W3WAddress>> = withContext(dispatcher) {
        // Create a list of deferred results to process the words concurrently
        val deferredResults = listWords.map { word ->
            async {
                textDataSource.convertToCoordinates(word)
            }
        }

        // Wait for all results to be processed
        val results = deferredResults.awaitAll()

        // Create markers for all successful results
        val markers = results.filterIsInstance<W3WResult.Success<W3WAddress>>().map { result ->
            val address = result.value
            W3WMarker(
                words = address.words,
                square = address.square!!.toW3WSquare(),
                latLng = address.center!!.toW3WLatLong(),
                color = listColor
            )
        }

        addListMarker(
            listName = listName,
            listColor = listColor,
            markers = markers
        )

        // Handle zoom for lists
        handleZoomOption(markers.map { W3WCoordinates(it.latLng.lat,it.latLng.lng) }, zoomOption)

        // Return the complete list of results
        return@withContext results
    }

    suspend fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WAddress> = withContext(dispatcher) {
        val result = textDataSource.convertToCoordinates(words)

        if (result is W3WResult.Success) {
            val marker = W3WMarker(
                words = result.value.words,
                square = result.value.square!!.toW3WSquare(),
                latLng = result.value.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
                color = markerColor
            )

            addMarker(
                marker = marker
            )

            handleZoomOption(result.value.center!!, zoomOption, zoomLevel)
        }

        return@withContext result
    }

    suspend fun selectAtWords(
        words: String,
    ): W3WResult<W3WAddress> = withContext(dispatcher) {
        val result = textDataSource.convertToCoordinates(words)

        if (result is W3WResult.Success) {
            val marker = W3WMarker(
                words = result.value.words,
                square = result.value.square!!.toW3WSquare(),
                latLng = result.value.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
                color = MARKER_COLOR_DEFAULT
            )

            setSelectedMarker(marker)
        }

        return@withContext result
    }

    suspend fun removeMarkerAtWords(words: String) = withContext(dispatcher) {
        val result = textDataSource.convertToCoordinates(words)

        if (result is W3WResult.Success) {
            // Create a new map where we filter out markers by coordinates and remove empty lists
            val updatedMarkersMap = markersMap
                .mapValues { (_, markers) -> markers.filter { it.latLng != result.value.center?.toW3WLatLong() }.toMutableList() }
                .filter { (_, markers) -> markers.isNotEmpty() }.toMutableMap()

            // Update the map state with the new markers
            _mapState.update { currentState ->
                currentState.copy(
                    markers = updatedMarkersMap.toMarkers().toImmutableList()
                )
            }
        }

        return@withContext result
    }

    suspend fun removeMarkerAtListWords(listWords: List<String>) = withContext(dispatcher)  {
        // Create a list of deferred results to process the words concurrently
        val deferredResults = listWords.map { word ->
            async {
                textDataSource.convertToCoordinates(word)
            }
        }

        // Wait for all results to be processed
        val results = deferredResults.awaitAll()

        // For each result (which is expected to be of type W3WResult), we handle success and failure
        results.forEach { result ->
            if (result is W3WResult.Success) {
                val coordinates = result.value.center?.toW3WLatLong()

                if (coordinates != null) {
                    // Create a new map where we filter out markers by latLng and remove empty lists
                    val updatedMarkersMap = markersMap
                        .mapValues { (_, markers) ->
                            markers.filter { it.latLng != coordinates }.toMutableList() // Filter markers not matching the coordinates
                        }
                        .filter { (_, markers) -> markers.isNotEmpty() } // Remove empty lists
                        .toMutableMap()

                    // Update the map state with the new markers
                    _mapState.update { currentState ->
                        currentState.copy(
                            markers = updatedMarkersMap.toMarkers().toImmutableList()
                        )
                    }
                }
            }
        }

        // Return the complete list of results
        return@withContext results
    }
    //endregion

    //region Marker with Coordinates
    suspend fun addMarkerAtCoordinates(
        coordinates: W3WCoordinates,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WAddress> = withContext(dispatcher) {
        val result = textDataSource.convertTo3wa(coordinates, language)

        if (result is W3WResult.Success) {
            addMarker(
                marker = W3WMarker(
                    words = result.value.words,
                    square = result.value.square!!.toW3WSquare(),
                    latLng = result.value.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
                    color = markerColor
                )
            )

            handleZoomOption(result.value.center!!, zoomOption, zoomLevel)
        }

        return@withContext result
    }

    suspend fun addMarkerAtListCoordinates(
        listName: String,
        listCoordinates: List<W3WCoordinates>,
        listColor: W3WMarkerColor,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
    ): List<W3WResult<W3WAddress>> = withContext(dispatcher) {
        // Create a list of deferred results to process the words concurrently
        val deferredResults = listCoordinates.map { coordinates ->
            async {
                textDataSource.convertTo3wa(coordinates, language)
            }
        }

        // Wait for all results to be processed
        val results = deferredResults.awaitAll()

        // Create markers for all successful results
        val markers = results.filterIsInstance<W3WResult.Success<W3WAddress>>().map { result ->
            val address = result.value
            W3WMarker(
                words = address.words,
                square = address.square!!.toW3WSquare(),
                latLng = address.center!!.toW3WLatLong(),
                color = listColor
            )
        }

        addListMarker(
            listName = listName,
            listColor = listColor,
            markers = markers
        )

        // Handle zoom for lists
        handleZoomOption(markers.map { W3WCoordinates(it.latLng.lat,it.latLng.lng) }, zoomOption)

        // Return the complete list of results
        return@withContext results
    }

    suspend fun selectAtCoordinates(
        coordinates: W3WCoordinates,
    ): W3WResult<W3WAddress> = withContext(dispatcher) {
        var marker = findMarkerByCoordinates(coordinates)
        if (marker != null) {
            setSelectedMarker(marker)
        }

        val result = textDataSource.convertTo3wa(coordinates, language)

        if (result is W3WResult.Success) {
            if (marker == null) {
                marker = findMarkerBy3wa(markersMap, result.value.words) ?: W3WMarker(
                    words = result.value.words,
                    square = result.value.square!!.toW3WSquare(),
                    latLng = result.value.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
                    color = MARKER_COLOR_DEFAULT
                )

                setSelectedMarker(marker)
            }

            if (_buttonState.value.isRecallButtonEnabled) {
                handleRecallButton()
            }
        }

        return@withContext result
    }

    suspend fun removeMarkerAtCoordinates(coordinates: W3WCoordinates) = withContext(dispatcher) {
        // Create a new map where we filter out markers by coordinates and remove empty lists
        val updatedMarkersMap = markersMap
            .mapValues { (_, markers) -> markers.filter { it.latLng != coordinates.toW3WLatLong() }.toMutableList() }
            .filter { (_, markers) -> markers.isNotEmpty() }.toMutableMap()

        // Update the map state with the new markers
        _mapState.update { currentState ->
            currentState.copy(
                markers = updatedMarkersMap.toMarkers().toImmutableList()
            )
        }
    }

    suspend fun removeMarkerAtListCoordinates(listCoordinates: List<W3WCoordinates>) = withContext(dispatcher)  {
        // For each result (which is expected to be of type W3WResult), we handle success and failure
        listCoordinates.forEach { coordinates ->
            // Create a new map where we filter out markers by latLng and remove empty lists
            val updatedMarkersMap = markersMap
                .mapValues { (_, markers) ->
                    markers.filter { it.latLng != coordinates.toW3WLatLong() }.toMutableList() }
                .filter { (_, markers) -> markers.isNotEmpty() } // Remove empty lists
                .toMutableMap()

            // Update the map state with the new markers
            _mapState.update { currentState ->
                currentState.copy(
                    markers = updatedMarkersMap.toMarkers().toImmutableList()
                )
            }
        }
    }
    //endregion

    //region Marker with Coordinates
    //endregion

    //region Buttons
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
    //endregion

    //region Private function
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
                .collectLatest { calculateAndUpdateGrid(it) }
        }
    }

    private suspend fun calculateAndUpdateGrid(cameraState: W3WCameraState<*>) {
        val newGridLine = calculateGridPolylines(cameraState)
        _mapState.update {
            it.copy(gridLines = newGridLine)
        }
    }

    private suspend fun calculateGridPolylines(cameraState: W3WCameraState<*>): W3WGridLines =
        withContext(dispatcher) {
            cameraState.gridBound?.let { safeBox ->
                when (val grid = textDataSource.gridSection(safeBox)) {
                    is W3WResult.Failure -> handleGridError(grid.error)
                    is W3WResult.Success -> grid.toW3WGridLines()
                }
            } ?: W3WGridLines()
        }

    private fun handleGridError(error: W3WError): W3WGridLines {
        return if (error is BadBoundingBoxTooBigError || error is BadBoundingBoxError) {
            W3WGridLines()
        } else {
            throw error
        }
    }

    private fun W3WResult.Success<W3WGridSection>.toW3WGridLines(): W3WGridLines {
        return W3WGridLines(
            verticalLines = value.lines.computeVerticalLines().toImmutableList(),
            horizontalLines = value.lines.computeHorizontalLines().toImmutableList()
        )
    }

    private suspend fun updateSelectedScreenLocation() {
        withContext(dispatcher) {
            val selectedAddress = mapState.value.selectedAddress?.latLng
            val mapProjection = buttonState.value.mapProjection
            val selectedScreenLocation =
                selectedAddress?.let { mapProjection?.toScreenLocation(it) }
            _buttonState.update {
                it.copy(
                    selectedScreenLocation = selectedScreenLocation,
                )
            }
        }
    }

    private suspend fun updateRecallButtonColor() {
        withContext(dispatcher) {
            val markerSlashColor = mapState.value.selectedAddress?.color?.slash
            val markerBackgroundColor = mapState.value.selectedAddress?.color?.background
            _buttonState.update {
                it.copy(
                    recallArrowColor = markerSlashColor ?: Color.White,
                    recallBackgroundColor = markerBackgroundColor ?: Color(0xFFE11F26), // TODO: Define name for this color
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

        val shouldShowRecallButton = recallButtonViewport?.containsPoint(selectedScreenLocation) == false
        val rotationDegree = computeRecallButtonRotation(selectedScreenLocation, recallButtonPosition)

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

    private fun addMarker(
        marker: W3WMarker
    ) {
        markersMap.addMarker(
            marker = marker
        )

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }
    }

    private fun addListMarker(
        listName: String,
        listColor: W3WMarkerColor,
        markers: List<W3WMarker>
    ) {
        markersMap.addListMarker(
            listName = listName,
            markers = markers,
            listColor = listColor
        )

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }
    }

    private fun removeMarker(
        marker: W3WMarker
    ) {
        // Filter out markers with the specified words
        markersMap.mapValues { (_, listMarker) ->
            listMarker.filter { it.id != marker.id }.toImmutableList()
        }.filter { (_, listMarker) -> listMarker.isNotEmpty() }

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }
    }

    /**
     * Handle zoom option for a [W3WCoordinates] with multiple zoom options which will use the zoom level
     * if it's provided or the default zoom level.
     */
    private suspend fun handleZoomOption(coordinates: W3WCoordinates, zoomOption: W3WZoomOption, zoom: Float?) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER -> {
                mapState.value.cameraState?.moveToPosition(
                    coordinates = coordinates,
                    animate = true
                )
            }

            W3WZoomOption.CENTER_AND_ZOOM -> {
                mapState.value.cameraState?.moveToPosition(
                    coordinates = coordinates,
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
    //endregion
}

//region Extension
private fun MutableMap<String, MutableList<W3WMarker>>.addListMarker(
    listName: String,
    listColor: W3WMarkerColor,
    markers: List<W3WMarker>,
) {
    if (markers.isEmpty()) return

    val currentList = this.getOrPut(listName) { mutableListOf() }

    markers.map { it.copy(color = listColor) }.forEach { marker ->
        // Check if the marker already exists (based on its `id`)
        val index = currentList.indexOfFirst { it.id == marker.id }

        if (index != -1) {
            // If the marker exists, update it with the new one
            currentList[index] = marker
        } else {
            // If the marker does not exist, add it to the list
            currentList.add(marker)
        }
    }

    this[listName] = currentList
}

private fun MutableMap<String, MutableList<W3WMarker>>.addMarker(
    listName: String? = null,  // Optional list identifier
    marker: W3WMarker,        // Marker to add or update
) {
    // Determine the listName: use provided listName or a default
    val key = listName ?: LIST_DEFAULT_ID

    // Get or create the current list of markers (using MutableList for in-place updates)
    val currentList = this[key] ?: mutableListOf()

    // Create a new list by either updating or adding the marker
    if (marker in currentList) {
        // Replace the existing marker if it exists (by id)
        val index = currentList.indexOfFirst { it.id == marker.id }
        if (index != -1) {
            currentList[index] = marker
        }
    } else {
        // Marker doesn't exist, add it to the list
        currentList.add(marker)
    }

    // Update the map with the modified list
    this[key] = currentList
}

/**
 * Convert Map maker to list marker with unique ID
 *
 * @return list of W3WMarker
 */
private fun MutableMap<String, MutableList<W3WMarker>>.toMarkers(): List<W3WMarker> {
    return this.values.flatten().map { item ->
        item.copy(
            isInMultipleList = hasMultipleLists(
                item,
                this
            )
        )
    }
}

private fun hasMultipleLists(
    marker: W3WMarker,
    listMarkers: Map<String, List<W3WMarker>>
): Boolean {
    val count = listMarkers.values.flatten().count { it.id == marker.id }
    return count > 1
}

private fun findMarkerBy3wa(markers: Map<String, List<W3WMarker>>, words: String): W3WMarker? {
    return markers.values.flatten().find { marker ->
        marker.words == words
    }
}

private fun W3WSquare.contains(coordinates: W3WCoordinates?): Boolean {
    if (coordinates == null) return false
    return if (coordinates.lat >= this.southwest.lat && coordinates.lat <= this.northeast.lat && coordinates.lng >= this.southwest.lng && coordinates.lng <= this.northeast.lng) return true
    else false
}
//endregion