package com.what3words.components.compose.maps

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.util.Consumer
import com.google.android.gms.maps.GoogleMap
import com.mapbox.maps.MapboxMap
import com.what3words.components.compose.maps.models.W3WMapMarker
import com.what3words.components.compose.maps.models.W3WMapState
import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.components.compose.maps.models.addMarker
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var defaultZoomLevel: Float = 0f
    private var language: W3WRFC5646Language = W3WRFC5646Language.EN_GB

    private val _state = MutableStateFlow(W3WMapState())
    val state: StateFlow<W3WMapState> = _state.asStateFlow()

    // Should be combine functions of W3WMap, W3WMapManager

    fun addMarkerAtWords(
        words: String,
        markerColor: Color = Color.Red,
        zoomOption: W3WZoomOption = W3WZoomOption.CENTER_AND_ZOOM,
        onSuccess: Consumer<W3WAddress>? = null,
        onError: Consumer<W3WError>? = null,
        zoomLevel: Float? = null
    ) {
        CoroutineScope(dispatcher).launch {
            when(val c23wa =  textDataSource.convertToCoordinates(words)) {
                is W3WResult.Success -> {
                    _state.value = state.value.addMarker(
                        marker = W3WMapMarker(address = c23wa.value, color = markerColor)
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
            when(val c23wa =  textDataSource.convertTo3wa(coordinates, language)) {
                is W3WResult.Success -> {
                    _state.value = state.value.addMarker(
                        marker = W3WMapMarker(address = c23wa.value, color = markerColor)
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
            when(val c23wa =  textDataSource.convertTo3wa(coordinates, language)) {
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

    /** This method should be called on [GoogleMap.setOnCameraIdleListener] or [MapboxMap.addOnMapIdleListener].
     * This will allow to refresh the grid bounds on camera idle.
     */
    fun updateMap() {
        Log.d("XXX","Manager updateMap")
    }

    /** This method should be called on [GoogleMap.setOnCameraMoveListener] or [MapboxMap.addOnCameraChangeListener].
     * This will allow to swap from markers to squares and show/hide grid when zoom goes higher or lower than the [W3WGoogleMapsWrapper.ZOOM_SWITCH_LEVEL] or [W3WMapBoxWrapper.ZOOM_SWITCH_LEVEL] threshold.
     */
    fun updateMove() {
        Log.d("XXX","Manager updateMove")
    }

}