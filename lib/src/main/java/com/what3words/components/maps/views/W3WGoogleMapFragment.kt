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
import com.what3words.components.maps.wrappers.GridColor
import com.what3words.components.maps.wrappers.W3WGoogleMapsWrapper
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.map.components.R
import com.what3words.map.components.databinding.W3wGoogleMapViewBinding

class W3WGoogleMapFragment() : Fragment(), OnMapReadyCallback, W3WMap {
    private var squareSelectedConsumer: SelectedSquareConsumer<SuggestionWithCoordinates, Boolean, Boolean>? =
        null
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

    fun getMap(): GoogleMap {
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
            this.w3wMapsWrapper.selectAtCoordinates(latLng.latitude, latLng.longitude, {
                squareSelectedConsumer?.accept(
                    it,
                    true,
                    this.w3wMapsWrapper.findMarkerByCoordinates(
                        latLng.latitude,
                        latLng.longitude
                    ) != null
                )
            })
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

    override fun setGridColor(gridColor: GridColor) {
        w3wMapsWrapper.setGridColor(gridColor)
    }

    override fun onSquareSelected(ssc: SelectedSquareConsumer<SuggestionWithCoordinates, Boolean, Boolean>) {
        squareSelectedConsumer = ssc
        w3wMapsWrapper.onMarkerClicked {
            squareSelectedConsumer?.accept(
                it,
                selectedByTouch = true,
                isMarked = true
            )
        }
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

    override fun addMarkerAtSuggestionWithCoordinates(
        suggestion: SuggestionWithCoordinates,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtSuggestionWithCoordinates(suggestion, markerColor, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun selectAtSuggestionWithCoordinates(
        suggestion: SuggestionWithCoordinates,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.selectAtSuggestionWithCoordinates(suggestion, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
            squareSelectedConsumer?.accept(
                it,
                selectedByTouch = false,
                isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(
                    it.coordinates.lat,
                    it.coordinates.lng
                ) != null
            )
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
            squareSelectedConsumer?.accept(
                it,
                selectedByTouch = false,
                isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(
                    it.coordinates.lat,
                    it.coordinates.lng
                ) != null
            )
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
            squareSelectedConsumer?.accept(
                it,
                selectedByTouch = false,
                isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(
                    it.coordinates.lat,
                    it.coordinates.lng
                ) != null
            )
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
        lat: Double,
        lng: Double,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtCoordinates(lat, lng, markerColor, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    override fun addMarkerAtCoordinates(
        listCoordinates: List<Pair<Double, Double>>,
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
        lat: Double,
        lng: Double,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.selectAtCoordinates(lat, lng, {
            handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption)
            onSuccess?.accept(it)
            squareSelectedConsumer?.accept(
                it,
                selectedByTouch = false,
                isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(
                    it.coordinates.lat,
                    it.coordinates.lng
                ) != null
            )
        }, {
            onError?.accept(it)
        })
    }

    override fun findMarkerByCoordinates(lat: Double, lng: Double): SuggestionWithCoordinates? {
        return w3wMapsWrapper.findMarkerByCoordinates(lat, lng)
    }

    override fun removeMarkerAtCoordinates(lat: Double, lng: Double) {
        w3wMapsWrapper.removeMarkerAtCoordinates(lat, lng)
    }

    override fun removeMarkerAtCoordinates(listCoordinates: List<Pair<Double, Double>>) {
        w3wMapsWrapper.removeMarkerAtCoordinates(listCoordinates)
    }

    override fun removeAllMarkers() {
        w3wMapsWrapper.removeAllMarkers()
    }

    override fun getAllMarkers(): List<SuggestionWithCoordinates> {
        return w3wMapsWrapper.getAllMarkers()
    }

    override fun getSelectedMarker(): SuggestionWithCoordinates? {
        return w3wMapsWrapper.getSelectedMarker()
    }

    override fun unselect() {
        w3wMapsWrapper.unselect()
    }
}