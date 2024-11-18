package com.what3words.components.compose.maps

import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import androidx.compose.ui.graphics.Color
import androidx.core.util.Consumer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.what3words.androidwrapper.datasource.text.api.error.BadBoundingBoxTooBigError
import com.what3words.components.compose.maps.extensions.computeHorizontalLines
import com.what3words.components.compose.maps.extensions.computeVerticalLines
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.components.compose.maps.state.W3WCameraState
import com.what3words.components.compose.maps.state.W3WGoogleCameraState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.W3WMapboxCameraState
import com.what3words.components.compose.maps.state.addOrUpdateMarker
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A class that manages the state and interactions of a what3words map.
 *
 * This class uses a [W3WTextDataSource] to retrieve what3words address data and
 * a [CoroutineDispatcher] to perform operations asynchronously. It exposes a
 * [StateFlow] representing the current state of the map, which can be collected
 * by composable functions to render the map UI.
 *
 * @param textDataSource The data source used to retrieve what3words address data.
 * @param dispatcher The coroutine dispatcher used for asynchronous operations.
 * Defaults to [Dispatchers.IO].
 */
class W3WMapManager(
    private val textDataSource: W3WTextDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val mapProvider: MapProvider
) {
    private var language: W3WRFC5646Language = W3WRFC5646Language.EN_GB

    private val _state: MutableStateFlow<W3WMapState> = MutableStateFlow(W3WMapState())
    val state: StateFlow<W3WMapState> = _state.asStateFlow()

    init {
        if (mapProvider == MapProvider.MAPBOX) {
            _state.update {
                it.copy(cameraState = W3WMapboxCameraState(MapViewportState()))
            }
        } else if (mapProvider == MapProvider.GOOGLE_MAP) {
            _state.update {
                it.copy(
                    cameraState = W3WGoogleCameraState(
                        CameraPositionState(
                            position = CAMERA_POSITION_DEFAULT
                        )
                    )
                )
            }
        }
    }

    fun updateCameraState(newCameraState: W3WCameraState<*>) {
        CoroutineScope(Dispatchers.IO).launch {
            _state.update {
                it.copy(
                    cameraState = newCameraState,
                    gridPolyline = calculateGridPolylines(newCameraState)
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
        return state.value.isDarkMode
    }

    fun setDarkMode(darkMode: Boolean) {
        _state.update {
            it.copy(
                isDarkMode = darkMode
            )
        }
    }
    //endregion

    //region Map Ui Settings
    fun getMapType(): W3WMapType {
        return state.value.mapType
    }

    fun setMapType(mapType: W3WMapType) {
        _state.update {
            it.copy(
                mapType = mapType
            )
        }
    }

    fun setMapGesturesEnabled(enabled: Boolean) {
        _state.update {
            it.copy(
                isMapGestureEnable = enabled
            )
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
    fun setMyLocationEnabled(enabled: Boolean) {
        _state.update {
            it.copy(
                isMyLocationEnabled = enabled
            )
        }
    }

    //TODO: Need to confirm on/off button in button layout
    fun setMyLocationButton(enabled: Boolean) {
        _state.update {
            it.copy(
                isMyLocationButtonEnabled = enabled
            )
        }
    }
    //endregion

    // region Camera control
    fun orientCamera() {
        // Not implemented
    }

    fun moveToPosition(coordinates: W3WCoordinates) {
        // Not implemented
    }
    //endregion

    //region Square
    fun getSelectedMarker(): W3WAddress? {
        return state.value.selectedAddress
    }

    fun unselect() {
        _state.update {
            it.copy(
                selectedAddress = null
            )
        }
    }

    //endregion

    //region Marker
    fun addMarkerAtWords(
        words: String,
        markerColor: Color = Color.Red,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    ) {
        CoroutineScope(dispatcher).launch {
            when (val c23wa = textDataSource.convertToCoordinates(words)) {
                is W3WResult.Success -> {
                    _state.value = state.value.addOrUpdateMarker(
                        marker = W3WMarker(address = c23wa.value, color = markerColor)
                    )
                    onSuccess?.accept(c23wa.value)
                }

                is W3WResult.Failure -> {
                    onError?.accept(c23wa.error)
                }
            }
        }
    }

    fun addMarkerAtListWords(
        listWords: List<String>,
        markerColor: Color = Color.Red,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    ) {

    }

    fun addMarkerAtCoordinates(
        coordinates: W3WCoordinates,
        markerColor: Color = Color.Red,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    ) {
        CoroutineScope(dispatcher).launch {
            when (val c23wa = textDataSource.convertTo3wa(coordinates, language)) {
                is W3WResult.Success -> {
                    _state.value = state.value.addOrUpdateMarker(
                        marker = W3WMarker(address = c23wa.value, color = markerColor)
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
                    _state.value = state.value.copy(
                        selectedAddress = c23wa.value
                    )
                    onSuccess?.accept(c23wa.value)
                }

                is W3WResult.Failure -> {
                    onError?.accept(c23wa.error)
                }
            }
        }
    }

    private suspend fun calculateGridPolylines(cameraState: W3WCameraState<*>): Pair<List<W3WCoordinates>, List<W3WCoordinates>> {
        return withContext(Dispatchers.IO) {
            cameraState.gridBound?.let { safeBox ->
                when (val grid = textDataSource.gridSection(safeBox)) {
                    is W3WResult.Failure -> {
                        if (grid.error is BadBoundingBoxTooBigError) {
                            return@withContext Pair(emptyList(), emptyList())
                        } else {
                            throw grid.error
                        }
                    }

                    is W3WResult.Success -> {
                        val verticalLines = grid.value.lines.computeVerticalLines()
                        val horizontalLines = grid.value.lines.computeHorizontalLines()
                        Pair(verticalLines, horizontalLines)
                    }
                }
            } ?: Pair(emptyList(), emptyList())
        }
    }

    companion object {
        @JvmField
        val CAMERA_POSITION_DEFAULT = CameraPosition(LatLng(51.521251, -0.203586), 19f, 0f, 0f)
    }
}