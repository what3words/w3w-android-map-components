package com.what3words.components.maps.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.models.W3WApiDataSource
import com.what3words.components.maps.models.W3WDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.wrappers.W3WGoogleMapsWrapper
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.map.components.R
import com.what3words.map.components.databinding.W3wGoogleMapViewBinding

class W3WGoogleMapFragment() : Fragment(), OnMapReadyCallback, W3WMap {
    private lateinit var onReadyCallback: OnFragmentReadyCallback
    private var _binding: W3wGoogleMapViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap
    private var apiKey: String? = null
    private var sdkSource: W3WDataSource? = null
    private lateinit var w3wMapsWrapper: W3WGoogleMapsWrapper

    interface OnFragmentReadyCallback {
        fun onFragmentReady(fragment: W3WMap)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = W3wGoogleMapViewBinding.inflate(inflater, container, false)
        val mapFragment = this.childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun apiKey(key: String, callback: OnFragmentReadyCallback) {
        apiKey = key
        onReadyCallback = callback
    }

    fun sdk(source: W3WDataSource, callback: OnFragmentReadyCallback) {
        sdkSource = source
        onReadyCallback = callback
    }

    fun getMap() : GoogleMap {
        return map
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.setMinZoomPreference(2.0f)
        map.setMaxZoomPreference(20.0f)
        when {
            apiKey != null -> {
                val wrapper = What3WordsV3(apiKey!!, requireContext())
                w3wMapsWrapper = W3WGoogleMapsWrapper(
                    requireContext(),
                    map,
                    W3WApiDataSource(wrapper, requireContext())
                )
            }
            sdkSource != null -> {
                w3wMapsWrapper = W3WGoogleMapsWrapper(
                    requireContext(),
                    map,
                    sdkSource!!
                )
            }
            else -> {
                throw Exception("MISSING SETUP YOU IDIOT")
            }
        }
        onReadyCallback.onFragmentReady(this)
        p0.setOnMapClickListener { latLng ->
            //OTHER FUNCTIONS
            val location = Coordinates(
                latLng.latitude,
                latLng.longitude
            )
            this.w3wMapsWrapper.selectAtCoordinates(location)
        }

        p0.setOnCameraIdleListener {
            this.w3wMapsWrapper.updateMap()
        }

        p0.setOnCameraMoveListener {
            this.w3wMapsWrapper.updateMove()
        }
    }

    override fun setLanguage(language: String) {
        w3wMapsWrapper.setLanguage(language)
    }

    override fun onMarkerClicked(callback: Consumer<SuggestionWithCoordinates>) {
        w3wMapsWrapper.onMarkerClicked(callback)
    }

    override fun addMarkerAtSuggestion(
        suggestion: Suggestion,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtSuggestion(suggestion, markerColor, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    private fun handleZoomOption(latLng: LatLng, zoomOption: W3WZoomOption) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER -> {
                val cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
            W3WZoomOption.CENTER_AND_ZOOM -> {
                val cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(19f)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    private fun handleZoomOption(latLng: LatLngBounds, zoomOption: W3WZoomOption) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER, W3WZoomOption.CENTER_AND_ZOOM -> {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLng, 10))
            }
        }
    }

    override fun addMarkerAtSuggestion(
        listSuggestions: List<Suggestion>,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtSuggestion(listSuggestions, markerColor, {
            val latLngBoundsBuilder = LatLngBounds.builder()
            it.forEach { marker ->
                latLngBoundsBuilder.include(
                    LatLng(
                        marker.coordinates.lat,
                        marker.coordinates.lng
                    )
                )
            }
            handleZoomOption(latLngBoundsBuilder.build(), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun removeMarkerAtSuggestion(suggestion: Suggestion) {
        w3wMapsWrapper.removeMarkerAtSuggestion(suggestion)
    }

    override fun removeMarkerAtSuggestion(listSuggestions: List<Suggestion>) {
        w3wMapsWrapper.removeMarkerAtSuggestion(listSuggestions)
    }

    override fun selectAtSuggestion(
        suggestion: Suggestion,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.selectAtSuggestion(suggestion, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtWords(words, markerColor, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtWords(listWords, markerColor, {
            val latLngBoundsBuilder = LatLngBounds.builder()
            it.forEach { marker ->
                latLngBoundsBuilder.include(
                    LatLng(
                        marker.coordinates.lat,
                        marker.coordinates.lng
                    )
                )
            }
            handleZoomOption(latLngBoundsBuilder.build(), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun selectAtWords(
        words: String,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.selectAtWords(words, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun removeMarkerAtWords(listWords: List<String>) {
        w3wMapsWrapper.removeMarkerAtWords(listWords)
    }

    override fun removeMarkerAtWords(words: String) {
        w3wMapsWrapper.removeMarkerAtWords(words)
    }

    override fun addMarkerAtCoordinates(
        coordinates: Coordinates,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtCoordinates(coordinates, markerColor, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun addMarkerAtCoordinates(
        listCoordinates: List<Coordinates>,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtCoordinates(listCoordinates, markerColor, {
            val latLngBoundsBuilder = LatLngBounds.builder()
            it.forEach { marker ->
                latLngBoundsBuilder.include(
                    LatLng(
                        marker.coordinates.lat,
                        marker.coordinates.lng
                    )
                )
            }
            handleZoomOption(latLngBoundsBuilder.build(), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun selectAtCoordinates(
        coordinates: Coordinates,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.selectAtCoordinates(coordinates, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun findMarkerByCoordinates(coordinates: Coordinates): SuggestionWithCoordinates? {
        return w3wMapsWrapper.findMarkerByCoordinates(coordinates)
    }

    override fun removeMarkerAtCoordinates(coordinates: Coordinates) {
        w3wMapsWrapper.removeMarkerAtCoordinates(coordinates)
    }

    override fun removeMarkerAtCoordinates(listCoordinates: List<Coordinates>) {
        w3wMapsWrapper.removeMarkerAtCoordinates(listCoordinates)
    }

    override fun removeAllMarkers() {
        w3wMapsWrapper.removeAllMarkers()
    }

    override fun getAllMarkers(): List<SuggestionWithCoordinates> {
        return w3wMapsWrapper.getAllMarkers()
    }

    override fun unselect() {
        w3wMapsWrapper.unselect()
    }
}