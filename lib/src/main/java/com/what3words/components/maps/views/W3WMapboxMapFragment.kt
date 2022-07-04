package com.what3words.components.maps.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.models.W3WApiDataSource
import com.what3words.components.maps.models.W3WDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.wrappers.GridColor
import com.what3words.components.maps.wrappers.W3WMapBoxWrapper
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.map.components.databinding.W3wMapboxMapViewBinding

class W3WMapboxMapFragment() : Fragment(), W3WMap {
    private var squareSelectedError: Consumer<APIResponse.What3WordsError>? = null
    private var squareSelectedSuccess: SelectedSquareConsumer<SuggestionWithCoordinates, Boolean, Boolean>? =
        null
    private lateinit var onReadyCallback: OnFragmentReadyCallback
    private var _binding: W3wMapboxMapViewBinding? = null
    private val binding get() = _binding!!
    private var apiKey: String? = null
    private var sdkSource: W3WDataSource? = null
    private lateinit var w3wMapsWrapper: W3WMapBoxWrapper

    interface OnFragmentReadyCallback {
        fun onFragmentReady(fragment: W3WMap)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = W3wMapboxMapViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun apiKey(key: String, callback: OnFragmentReadyCallback) {
        apiKey = key
        onReadyCallback = callback
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            setup()
        }
    }

    fun sdk(source: W3WDataSource, callback: OnFragmentReadyCallback) {
        sdkSource = source
        onReadyCallback = callback
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            setup()
        }
    }

    fun getMap(): MapboxMap {
        return binding.mapView.getMapboxMap()
    }

    private fun setup() {
        when {
            apiKey != null -> {
                val wrapper = What3WordsV3(apiKey!!, requireContext())
                w3wMapsWrapper = W3WMapBoxWrapper(
                    requireContext(),
                    getMap(),
                    W3WApiDataSource(wrapper, requireContext())
                )
            }
            sdkSource != null -> {
                w3wMapsWrapper = W3WMapBoxWrapper(
                    requireContext(),
                    getMap(),
                    sdkSource!!
                )
            }
            else -> {
                throw Exception("MISSING SETUP")
            }
        }

        binding.mapView.getMapboxMap().addOnMapClickListener { latLng ->
            //OTHER FUNCTIONS
            this.w3wMapsWrapper.selectAtCoordinates(latLng.latitude(), latLng.longitude(), {
                squareSelectedSuccess?.accept(
                    it,
                    true,
                    this.w3wMapsWrapper.findMarkerByCoordinates(
                        latLng.latitude(),
                        latLng.longitude()
                    ) != null
                )
            }, {
                squareSelectedError?.accept(it)
            })
            true
        }

        getMap().addOnMapIdleListener {
            this.w3wMapsWrapper.updateMap()
        }

        getMap().addOnCameraChangeListener {
            this.w3wMapsWrapper.updateMove()
        }

        onReadyCallback.onFragmentReady(this)
    }

    override fun setLanguage(language: String) {
        w3wMapsWrapper.setLanguage(language)
    }

    override fun onSquareSelected(
        onSuccess: SelectedSquareConsumer<SuggestionWithCoordinates, Boolean, Boolean>,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        this.squareSelectedSuccess = onSuccess
        this.squareSelectedError = onError
    }

    override fun addMarkerAtSuggestion(
        suggestion: Suggestion,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtSuggestion(suggestion, markerColor, {
            handleZoomOption(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat), zoomOption)
            onSuccess?.accept(it)
        }, {
            onError?.accept(it)
        })
    }

    private fun handleZoomOption(latLng: Point, zoomOption: W3WZoomOption) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER -> {
                val cameraOptions = CameraOptions.Builder()
                    .center(latLng)
                    .build()
                getMap().setCamera(cameraOptions)
            }
            W3WZoomOption.CENTER_AND_ZOOM -> {
                val cameraOptions = CameraOptions.Builder()
                    .center(latLng)
                    .zoom(19.0)
                    .build()
                getMap().setCamera(cameraOptions)
            }
        }
    }

    private fun handleZoomOption(latLng: List<Point>, zoomOption: W3WZoomOption) {
        when (zoomOption) {
            W3WZoomOption.NONE -> {}
            W3WZoomOption.CENTER, W3WZoomOption.CENTER_AND_ZOOM -> {
                val options = getMap().cameraForCoordinates(latLng)
                getMap().setCamera(options)
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
            val listPoints = mutableListOf<Point>()
            it.forEach { marker ->
                listPoints.add(
                    Point.fromLngLat(
                        marker.coordinates.lng,
                        marker.coordinates.lat
                    )
                )
            }
            handleZoomOption(listPoints, zoomOption)
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
            handleZoomOption(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat), zoomOption)
            onSuccess?.accept(it)
            squareSelectedSuccess?.accept(
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

    override fun addMarkerAtSuggestionWithCoordinates(
        suggestion: SuggestionWithCoordinates,
        markerColor: W3WMarkerColor,
        zoomOption: W3WZoomOption,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapsWrapper.addMarkerAtSuggestionWithCoordinates(suggestion, markerColor, {
            handleZoomOption(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat), zoomOption)
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
            handleZoomOption(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat), zoomOption)
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
            handleZoomOption(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat), zoomOption)
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
            val listPoints = mutableListOf<Point>()
            it.forEach { marker ->
                listPoints.add(
                    Point.fromLngLat(
                        marker.coordinates.lng,
                        marker.coordinates.lat
                    )
                )
            }
            handleZoomOption(listPoints, zoomOption)
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
            handleZoomOption(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat), zoomOption)
            onSuccess?.accept(it)
            squareSelectedSuccess?.accept(
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
            handleZoomOption(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat), zoomOption)
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
            val listPoints = mutableListOf<Point>()
            it.forEach { marker ->
                listPoints.add(
                    Point.fromLngLat(
                        marker.coordinates.lng,
                        marker.coordinates.lat
                    )
                )
            }
            handleZoomOption(listPoints, zoomOption)
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
            handleZoomOption(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat), zoomOption)
            onSuccess?.accept(it)
            squareSelectedSuccess?.accept(
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

    override fun setGridColor(gridColor: GridColor) {
        w3wMapsWrapper.setGridColor(gridColor)
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