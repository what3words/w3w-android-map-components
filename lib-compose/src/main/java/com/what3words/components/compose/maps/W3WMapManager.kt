package com.what3words.components.compose.maps

import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import androidx.compose.ui.graphics.Color
import androidx.core.util.Consumer
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
) {
    private var language: W3WRFC5646Language = W3WRFC5646Language.EN_GB

    private val _state = MutableStateFlow(W3WMapState())
    val state: StateFlow<W3WMapState> = _state.asStateFlow()

    // Should be combine functions of W3WMap, W3WMapManager

    init {
        _state.update {
            it.copy(
                language = language,
                cameraPosition = W3WMapState.CameraPosition(
                    zoom = 19f,
                    W3WCoordinates(10.782147, 106.671892),
                    50f,
                    false
                )
            )
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
    fun getMapType(): W3WMapState.MapType {
        return state.value.mapType
    }

    fun setMapType(mapType: W3WMapState.MapType) {
        _state.update {
            it.copy(
                mapType = mapType
            )
        }
    }

    fun setCameraPosition(cameraPosition: W3WMapState.CameraPosition) {
        _state.update {
            it.copy(
                cameraPosition = cameraPosition
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
        _state.update {
            it.copy(
                cameraPosition = it.cameraPosition?.copy(
                    bearing = 0f
                )
            )
        }

    }

    fun moveToPosition(cameraPosition: W3WMapState.CameraPosition) {
        _state.update {
            it.copy(
                cameraPosition = cameraPosition
            )
        }
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
        zoomOption: W3WMapState.ZoomOption = W3WMapState.ZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    ) {
        CoroutineScope(dispatcher).launch {
            when (val c23wa = textDataSource.convertToCoordinates(words)) {
                is W3WResult.Success -> {
                    _state.value = state.value.addOrUpdateMarker(
                        marker = W3WMapState.Marker(address = c23wa.value, color = markerColor)
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
        zoomOption: W3WMapState.ZoomOption = W3WMapState.ZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<List<W3WAddress>>? = null,
        onError: Consumer<W3WError>? = null
    ) {

    }

    fun addMarkerAtCoordinates(
        coordinates: W3WCoordinates,
        markerColor: Color = Color.Red,
        zoomOption: W3WMapState.ZoomOption = W3WMapState.ZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    ) {
        CoroutineScope(dispatcher).launch {
            when (val c23wa = textDataSource.convertTo3wa(coordinates, language)) {
                is W3WResult.Success -> {
                    _state.value = state.value.addOrUpdateMarker(
                        marker = W3WMapState.Marker(address = c23wa.value, color = markerColor)
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
        zoomOption: W3WMapState.ZoomOption = W3WMapState.ZoomOption.CENTER_AND_ZOOM,
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

    fun onCameraUpdated(cameraPosition: W3WMapState.CameraPosition) {
        if (cameraPosition != state.value.cameraPosition) {
            if (cameraPosition.isMoving) {
                //TODO implement same as  updateMove in Wrapper

            } else {
                //TODO implement same as  updateMap in Wrapper
                _state.update {
                    it.copy(
                        cameraPosition = cameraPosition
                    )
                }
            }
        }
    }
}