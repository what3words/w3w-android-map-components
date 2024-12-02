package com.what3words.components.compose.maps

import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import androidx.core.util.Consumer
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraPositionState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.androidwrapper.datasource.text.api.error.BadBoundingBoxError
import com.what3words.androidwrapper.datasource.text.api.error.BadBoundingBoxTooBigError
import com.what3words.components.compose.maps.W3WMapDefaults.LOCATION_DEFAULT
import com.what3words.components.compose.maps.W3WMapDefaults.MAKER_COLOR_DEFAULT
import com.what3words.components.compose.maps.extensions.computeHorizontalLines
import com.what3words.components.compose.maps.extensions.computeVerticalLines
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.mapper.toW3WLatLong
import com.what3words.components.compose.maps.mapper.toW3WSquare
import com.what3words.components.compose.maps.models.W3WGridLines
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.components.compose.maps.state.W3WButtonsState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.addListMarker
import com.what3words.components.compose.maps.state.addMarker
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.components.compose.maps.state.camera.W3WGoogleCameraState
import com.what3words.components.compose.maps.state.camera.W3WMapboxCameraState
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val mapProvider: MapProvider,
    mapState: W3WMapState = W3WMapState(),
    buttonState: W3WButtonsState = W3WButtonsState(),
) {
    private var language: W3WRFC5646Language = W3WRFC5646Language.EN_GB

    private val _mapState: MutableStateFlow<W3WMapState> = MutableStateFlow(mapState)
    val mapState: StateFlow<W3WMapState> = _mapState.asStateFlow()

    private val _buttonState: MutableStateFlow<W3WButtonsState> = MutableStateFlow(buttonState)
    val buttonState: StateFlow<W3WButtonsState> = _buttonState.asStateFlow()

    init {
        if (mapProvider == MapProvider.MAPBOX) {
            _mapState.update {
                it.copy(
                    cameraState = W3WMapboxCameraState(
                        MapViewportState(
                            initialCameraState = CameraState(
                                Point.fromLngLat(
                                    LOCATION_DEFAULT.lng,
                                    LOCATION_DEFAULT.lat
                                ),
                                EdgeInsets(0.0, 0.0, 0.0, 0.0),
                                W3WMapboxCameraState.MY_LOCATION_ZOOM,
                                0.0,
                                0.0
                            )
                        )
                    )
                )
            }
        } else if (mapProvider == MapProvider.GOOGLE_MAP) {
            _mapState.update {
                it.copy(
                    cameraState = W3WGoogleCameraState(
                        CameraPositionState(
                            position = CameraPosition(
                                LOCATION_DEFAULT.toGoogleLatLng(),
                                W3WGoogleCameraState.MY_LOCATION_ZOOM,
                                0f,
                                0f
                            )
                        )
                    )
                )
            }
        }
    }

    //region W3WMap Config
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
    //endregion

    //region Map Ui Settings
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

    //TODO: Need to confirm on/off button in button layout
    fun setMyLocationButton(enabled: Boolean) {
        //TODO: Update in button state
    }
    //endregion

    // region Camera control
    fun orientCamera() {
        mapState.value.cameraState?.orientCamera()
    }

    fun moveToPosition(
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

    fun updateCameraState(newCameraState: W3WCameraState<*>) {
        CoroutineScope(Dispatchers.IO).launch {
            val newGridLine = calculateGridPolylines(newCameraState)
            _mapState.update {
                it.copy(
                    cameraState = newCameraState,
                    gridLines = newGridLine
                )
            }
        }
    }
    //endregion

    //region Selected Address
    fun setSelectedMarker(marker: W3WMarker) {
        _mapState.value = mapState.value.copy(
            selectedAddress = marker
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
    suspend fun addMarkerAtListWords(
        listId: String,
        listWords: List<String>,
        listColor: W3WMarkerColor,
        onSuccess: ((List<W3WAddress>) -> Unit)? = null,
        onError: ((List<W3WError>) -> Unit)? = null,
    ) = withContext(IO) {

        val successfulAddresses = mutableListOf<W3WAddress>()
        val errors = mutableListOf<W3WError>()
        val markers = mutableListOf<W3WMarker>()

        listWords.forEach { word ->
            when (val c23wa = textDataSource.convertToCoordinates(word)) {
                is W3WResult.Success -> {
                    val marker = W3WMarker(
                        words = c23wa.value.words,
                        square = c23wa.value.square!!.toW3WSquare(),
                        latLng = c23wa.value.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
                        color = listColor
                    )
                    markers.add(marker)
                    successfulAddresses.add(c23wa.value)
                }

                is W3WResult.Failure -> {
                    errors.add(c23wa.error)
                }
            }
        }

        _mapState.value = mapState.value.addListMarker(
            listId = listId,
            markers = markers.toImmutableList(),
            listColor = listColor
        )

        // Invoke success and error callbacks
        onSuccess?.invoke(successfulAddresses)
        onError?.invoke(errors)
    }

    suspend fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor = MAKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: ((W3WAddress) -> Unit)? = null,
        onError: ((W3WError) -> Unit)? = null,
        zoomLevel: Float? = null
    ) = withContext(IO) {
        when (val result = textDataSource.convertToCoordinates(words)) {
            is W3WResult.Success -> {
                val marker = W3WMarker(
                    words = result.value.words,
                    square = result.value.square!!.toW3WSquare(),
                    latLng = result.value.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
                    color = markerColor
                )

                _mapState.value = mapState.value.addMarker(
                    marker = marker)

                onSuccess?.invoke(result.value)

            }

            is W3WResult.Failure -> {
                onError?.invoke(result.error)
            }
        }
    }

    fun addMarkerAtCoordinates(
        coordinates: W3WCoordinates,
        markerColor: W3WMarkerColor = MAKER_COLOR_DEFAULT,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    ) {
        CoroutineScope(dispatcher).launch {
            when (val c23wa = textDataSource.convertTo3wa(coordinates, language)) {
                is W3WResult.Success -> {
                    _mapState.value = mapState.value.addMarker(
                        marker = W3WMarker(
                            words = c23wa.value.words,
                            square = c23wa.value.square!!.toW3WSquare(),
                            latLng = c23wa.value.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
                            color = markerColor
                        )
                    )
                    onSuccess?.accept(c23wa.value)
                }

                is W3WResult.Failure -> {
                    onError?.accept(c23wa.error)
                }
            }
        }
    }

    fun selectAtCoordinates(
        coordinates: W3WCoordinates,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    ) {
        CoroutineScope(dispatcher).launch {
            when (val c23wa = textDataSource.convertTo3wa(coordinates, language)) {
                is W3WResult.Success -> {
                    _mapState.value = mapState.value.copy(
                        selectedAddress = W3WMarker(
                            words = c23wa.value.words,
                            square = c23wa.value.square!!.toW3WSquare(),
                            latLng = c23wa.value.center?.toW3WLatLong() ?: LOCATION_DEFAULT,
                            color = MAKER_COLOR_DEFAULT
                        )
                    )
                    onSuccess?.accept(c23wa.value)
                }

                is W3WResult.Failure -> {
                    onError?.accept(c23wa.error)
                }
            }
        }
    }

    // Method used to test add/remove drawn markers on map. To be removed
    fun removeMarkerAtWords(words: String) {
        _mapState.update { currentState ->
            // Filter out markers with the specified words
            val updatedListMakers = currentState.listMakers.mapValues { (_, listMarker) ->
                listMarker.filter { it.words != words }.toImmutableList()
            }.filter { (_, listMarker) -> listMarker.isNotEmpty() }

            // Return the updated state with the cleaned list
            currentState.copy(listMakers = updatedListMakers.toImmutableMap())
        }
    }

    private suspend fun calculateGridPolylines(cameraState: W3WCameraState<*>): W3WGridLines {
        return withContext(Dispatchers.IO) {
            cameraState.gridBound?.let { safeBox ->
                when (val grid = textDataSource.gridSection(safeBox)) {
                    is W3WResult.Failure -> {
                        if (grid.error is BadBoundingBoxTooBigError || grid.error is BadBoundingBoxError) {
                            return@withContext W3WGridLines()
                        } else {
                            throw grid.error
                        }
                    }

                    is W3WResult.Success -> {
                        W3WGridLines(
                            verticalLines = grid.value.lines.computeVerticalLines(),
                            horizontalLines = grid.value.lines.computeHorizontalLines()
                        )
                    }
                }
            } ?: W3WGridLines()
        }
    }

    // Button state region

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
}