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
import com.what3words.components.compose.maps.extensions.addListMarker
import com.what3words.components.compose.maps.extensions.addMarker
import com.what3words.components.compose.maps.extensions.computeHorizontalLines
import com.what3words.components.compose.maps.extensions.computeVerticalLines
import com.what3words.components.compose.maps.extensions.contains
import com.what3words.components.compose.maps.extensions.toMarkers
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.mapper.toW3WLatLong
import com.what3words.components.compose.maps.mapper.toW3WMarker
import com.what3words.components.compose.maps.mapper.toW3WRectangle
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WGridScreenCell
import com.what3words.components.compose.maps.models.W3WLatLng
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
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
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WGridSection
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
        latLng: W3WLatLng,
        zoom: Float? = null,
        bearing: Float? = null,
        tilt: Float? = null,
        animate: Boolean = false,
    ) {
        mapState.value.cameraState?.moveToPosition(
            latLng,
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

    //region Selected Marker
    fun getSelectedMarker(): W3WMarker? {
        return mapState.value.selectedMarker
    }

    fun unselect() {
        _mapState.update {
            it.copy(
                selectedMarker = null
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

    fun findMarkerByLatLng(
        latLng: W3WLatLng
    ): W3WMarker? {
        return mapState.value.markers.firstOrNull {
            it.square.contains(latLng)
        }
    }

    suspend fun addMaker(
        marker: W3WMarker,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ) = withContext(dispatcher) {
        // Add the marker to the markersMap
        markersMap.addMarker(listName = listName, marker = marker)

        // Update the map state
        _mapState.update { currentState ->
            currentState.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        // Handle zoom options
        handleZoomOption(marker.latLng, zoomOption, zoomLevel)
    }

    suspend fun addListMarker(
        markers: List<W3WMarker>,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM
    ) {
        markersMap.addListMarker(
            listName = listName,
            markers = markers,
        )

        _mapState.update {
            it.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        // Handle zoom for lists
        handleZoomOption(markers.map { it.latLng }, zoomOption)
    }

    suspend fun setSelectedMarker(
        selectedMarker: W3WMarker
    ) {
        _mapState.value = mapState.value.copy(
            selectedMarker = selectedMarker
        )

        if (_buttonState.value.isRecallButtonEnabled) {
            handleRecallButton()
        }
    }

    fun removeByMarker(
        removeMarker: W3WMarker,
        listName: String? = null
    ): List<W3WMarker> {
        return removeMarkerByLatLng(
            listName = listName,
            latLng = removeMarker.latLng
        )
    }

    fun removeByListMarker(
        markers: List<W3WMarker>,
        listName: String? = null
    ): List<W3WMarker> {
        val removedMarkers = mutableListOf<W3WMarker>()

        markers.forEach {
            removedMarkers.addAll(removeByMarker(it, listName))
        }

        return removedMarkers
    }
    //endregion

    //region Marker with Words
    suspend fun addMarkerByWords(
        words: String,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext addMarker(
            listName = listName,
            input = words,
            convertFunction = { convertFunctionWords(it, markerColor) },
            zoomOption = zoomOption,
            zoomLevel = zoomLevel
        )
    }

    suspend fun addMarkerByListWords(
        listWords: List<String>,
        listColor: W3WMarkerColor,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        return@withContext addListMarker(
            listName = listName,
            inputs = listWords,
            convertFunction = { convertFunctionWords(it, listColor) },
            zoomOption = zoomOption
        )
    }

    suspend fun selectByWords(
        words: String,
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        val result = convertFunctionWords(words)

        if (result is W3WResult.Success) {
            setSelectedMarker(selectedMarker = result.value)
        }

        return@withContext result
    }

    suspend fun removeMarkerByWords(
        words: String,
        listName: String? = null
    ): W3WResult<List<W3WMarker>> = withContext(dispatcher) {
        return@withContext when (val result = convertFunctionWords(words)) {
            is W3WResult.Failure -> {
                W3WResult.Failure(result.error, result.message)
            }

            is W3WResult.Success -> {
                W3WResult.Success(
                    removeMarkerByLatLng(
                        listName = listName,
                        latLng = result.value.latLng
                    )
                )
            }
        }
    }

    suspend fun removeMarkerByListWords(
        listWords: List<String>,
        listName: String? = null
    ): List<W3WResult<List<W3WMarker>>> = withContext(dispatcher) {
        // Start multiple asynchronous tasks to handle each word concurrently
        val results = listWords.map { word ->
            async {
                removeMarkerByWords(
                    listName = listName,
                    words = word
                )
            }
        }

        // Await all results and return as a list
        results.awaitAll()
    }
    //endregion

    //region Marker with W3WCoordinates
    suspend fun addMarkerByCoordinates(
        coordinates: W3WCoordinates,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext addMarker(
            listName = listName,
            input = coordinates,
            convertFunction = { convertFunctionCoordinate(it, markerColor) },
            zoomOption = zoomOption,
            zoomLevel = zoomLevel
        )
    }

    suspend fun addMarkerByListCoordinates(
        listCoordinates: List<W3WCoordinates>,
        listName: String = LIST_DEFAULT_ID,
        listColor: W3WMarkerColor,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        return@withContext addListMarker(
            listName = listName,
            zoomOption = zoomOption,
            inputs = listCoordinates,
            convertFunction = { convertFunctionCoordinate(it, listColor) }
        )
    }

    suspend fun selectByCoordinates(
        coordinates: W3WCoordinates,
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        val result = findMarkerByLatLng(coordinates.toW3WLatLong())?.let { marker ->
            W3WResult.Success(marker)
        } ?: run {
            convertFunctionCoordinate(coordinates)
        }

        if (result is W3WResult.Success) {
            setSelectedMarker(selectedMarker = result.value)
        }

        return@withContext result
    }

    fun removeMarkerByCoordinates(
        coordinates: W3WCoordinates,
        listName: String? = null
    ): List<W3WMarker> {
        return removeMarkerByLatLng(
            listName = listName,
            latLng = coordinates.toW3WLatLong()
        )
    }

    fun removeMarkerByListCoordinates(
        listCoordinates: List<W3WCoordinates>,
        listName: String? = null,
    ): List<W3WMarker> {
        val removedMarkers = mutableListOf<W3WMarker>()

        listCoordinates.forEach {
            removedMarkers.addAll(removeMarkerByLatLng(listName, it.toW3WLatLong()))
        }

        return removedMarkers
    }
    //endregion

    //region Marker with Suggestions
    suspend fun addMarkerBySuggestion(
        suggestion: W3WSuggestion,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext addMarker(
            listName = listName,
            input = suggestion.w3wAddress,
            convertFunction = { convertFunctionAddress(it, markerColor) },
            zoomOption = zoomOption,
            zoomLevel = zoomLevel
        )
    }

    suspend fun addMarkerByListSuggestion(
        suggestions: List<W3WSuggestion>,
        listColor: W3WMarkerColor,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        return@withContext addListMarker(
            listName = listName,
            inputs = suggestions.map { it.w3wAddress },
            zoomOption = zoomOption,
            convertFunction = { convertFunctionAddress(it, listColor) }
        )
    }

    suspend fun selectBySuggestion(
        suggestion: W3WSuggestion
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext suggestion.w3wAddress.center?.let {
            selectByCoordinates(it)
        } ?: run {
            selectByWords(suggestion.w3wAddress.words)
        }
    }

    suspend fun removeMarkerBySuggestion(
        suggestion: W3WSuggestion,
        listName: String? = null
    ): W3WResult<List<W3WMarker>> = withContext(dispatcher) {
        return@withContext suggestion.w3wAddress.center?.let {
            W3WResult.Success(removeMarkerByLatLng(
                listName = listName,
                latLng = it.toW3WLatLong()
            ))
        }?:run {
            removeMarkerByWords(
                listName = listName,
                words = suggestion.w3wAddress.words
            )
        }
    }

    suspend fun removeMarkerByListSuggestion(
        suggestions: List<W3WSuggestion>,
        listName: String? = null
    ): List<W3WResult<List<W3WMarker>>> = withContext(dispatcher) {
        // Start multiple asynchronous tasks to handle each word concurrently
        val results = suggestions.map { suggestion ->
            async {
                removeMarkerBySuggestion(
                    listName = listName,
                    suggestion = suggestion
                )
            }
        }

        // Await all results and return as a list
        results.awaitAll()

    }
    //endregion

    //region Marker with Address
    suspend fun addMarkerByAddress(
        address: W3WAddress,
        listName: String = LIST_DEFAULT_ID,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        zoomLevel: Float? = null
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext addMarker(
            listName = listName,
            input = address,
            convertFunction = { convertFunctionAddress(it, markerColor) },
            zoomOption = zoomOption,
            zoomLevel = zoomLevel
        )
    }

    suspend fun addMarkerByListAddress(
        listAddress: List<W3WAddress>,
        listColor: W3WMarkerColor,
        listName: String = LIST_DEFAULT_ID,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        return@withContext addListMarker(
            listName = listName,
            inputs = listAddress,
            zoomOption = zoomOption,
            convertFunction = { convertFunctionAddress(it, listColor) }
        )
    }

    suspend fun selectByAddress(
        address: W3WAddress
    ): W3WResult<W3WMarker> = withContext(dispatcher) {
        return@withContext address.center?.let {
            selectByCoordinates(it)
        } ?: run {
            selectByWords(address.words)
        }
    }

    suspend fun removeMarkerByAddress(
        address: W3WAddress,
        listName: String? = null
    ): W3WResult<List<W3WMarker>> = withContext(dispatcher) {
        return@withContext address.center?.let {
            W3WResult.Success(removeMarkerByLatLng(
                listName = listName,
                latLng = it.toW3WLatLong()
            ))
        }?:run {
            removeMarkerByWords(
                listName = listName,
                words = address.words
            )
        }
    }

    suspend fun removeMarkerByListAddress(
        listAddress: List<W3WAddress>,
        listName: String? = null
    ): List<W3WResult<List<W3WMarker>>>  = withContext(dispatcher)  {
        // Start multiple asynchronous tasks to handle each word concurrently
        val results = listAddress.map { address ->
            async {
                removeMarkerByAddress(
                    listName = listName,
                    address = address
                )
            }
        }

        // Await all results and return as a list
        results.awaitAll()
    }
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
                when (val grid = textDataSource.gridSection(safeBox.toW3WRectangle())) {
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
            val selectedAddress = mapState.value.selectedMarker?.latLng
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
            val markerSlashColor = mapState.value.selectedMarker?.color?.slash
            val markerBackgroundColor = mapState.value.selectedMarker?.color?.background
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

    private fun convertFunctionWords(
        words: String,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
    ): W3WResult<W3WMarker> {
         return when(val result = textDataSource.convertToCoordinates(words)) {
             is W3WResult.Success -> {
                 W3WResult.Success(result.value.toW3WMarker(markerColor))
             }
             is W3WResult.Failure -> {
                 W3WResult.Failure(result.error, result.message)
             }
         }
    }

    private fun convertFunctionCoordinate(
        coordinates: W3WCoordinates,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
    ): W3WResult<W3WMarker> {
        return when(val result = textDataSource.convertTo3wa(coordinates,language)) {
            is W3WResult.Success -> {
                W3WResult.Success(result.value.toW3WMarker(markerColor))
            }
            is W3WResult.Failure -> {
                W3WResult.Failure(result.error, result.message)
            }
        }
    }

    private fun convertFunctionAddress(
        address: W3WAddress,
        markerColor: W3WMarkerColor = MARKER_COLOR_DEFAULT,
    ): W3WResult<W3WMarker> {
        return if(address.center == null) {
            when(val result = textDataSource.convertToCoordinates(address.words)) {
                is W3WResult.Success -> {
                    W3WResult.Success(result.value.toW3WMarker(markerColor))
                }
                is W3WResult.Failure -> {
                    W3WResult.Failure(result.error, result.message)
                }
            }
        } else {
            W3WResult.Success(address.toW3WMarker())
        }
    }

    private suspend fun <T> addMarker(
        listName: String,
        zoomOption: W3WZoomOption,
        zoomLevel: Float?,
        input: T,
        convertFunction: suspend (T) -> W3WResult<W3WMarker>
    ): W3WResult<W3WMarker> {
        return when (val result = convertFunction(input)) {
            is W3WResult.Success -> {
                addMaker(
                    listName = listName,
                    marker = result.value,
                    zoomOption = zoomOption,
                    zoomLevel = zoomLevel
                )

                result
            }
            is W3WResult.Failure -> result
        }
    }

    private suspend fun <T> addListMarker(
        listName: String,
        inputs: List<T>,
        zoomOption: W3WZoomOption,
        convertFunction: suspend (T) -> W3WResult<W3WMarker>
    ): List<W3WResult<W3WMarker>> = withContext(dispatcher) {
        // Concurrently process all inputs
        val result = inputs.map { input -> async { convertFunction(input) } }.awaitAll()
        val markers = result.filterIsInstance<W3WResult.Success<W3WMarker>>().map { it.value }

        addListMarker(
            listName = listName,
            markers = markers,
            zoomOption = zoomOption
        )

        // Return the complete list of results
        return@withContext result
    }

    private fun removeMarkerByLatLng(
        listName: String? = null,
        latLng: W3WLatLng
    ): List<W3WMarker>  {
        val removedMarkers = mutableListOf<W3WMarker>()
        val listToRemove = mutableListOf<String>()

        // Loop through the markersMap and check for markers to remove
        markersMap.forEach { (key, markers) ->
            val iterator = markers.iterator()
            while (iterator.hasNext()) {
                val marker = iterator.next()
                val shouldRemove = if (listName == null) {
                    // If listName is null, remove all markers matching the latLng
                    marker.square.contains(latLng)
                } else {
                    // If listName is not null, remove only markers with matching latLng AND listName
                    marker.square.contains(latLng) && key == listName
                }

                // If the marker matches the condition, remove it
                if (shouldRemove) {
                    removedMarkers.add(marker)
                    iterator.remove()
                }
            }

            // If this list has no more markers, we should consider removing this list entirely
            if (markers.isEmpty()) {
                listToRemove.add(key)
            }
        }

        // Remove any empty lists from the map
        listToRemove.forEach { markersMap.remove(it) }

        // Update the map state with the new markers
        _mapState.update { currentState ->
            currentState.copy(
                markers = markersMap.toMarkers().toImmutableList()
            )
        }

        return removedMarkers

    }

    /**
     * Handle zoom option for a [W3WLatLng] with multiple zoom options which will use the zoom level
     * if it's provided or the default zoom level.
     */
    private suspend fun handleZoomOption(latLng: W3WLatLng, zoomOption: W3WZoomOption, zoom: Float?) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER -> {
                mapState.value.cameraState?.moveToPosition(latLng, animate = true)
            }

            W3WZoomOption.CENTER_AND_ZOOM -> {
                mapState.value.cameraState?.moveToPosition(
                    latLng,
                    zoom,
                    animate = true
                )
            }
        }
    }

    /**
     * Handle zoom option for a list of [W3WLatLng] with multiple zoom options which will use the zoom level
     * if it's provided or the default zoom level.
     */
    private suspend fun handleZoomOption(listLatLng: List<W3WLatLng>, zoomOption: W3WZoomOption) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER, W3WZoomOption.CENTER_AND_ZOOM -> {
                mapState.value.cameraState?.moveToPosition(listLatLng)
            }
        }
    }
    //endregion
}