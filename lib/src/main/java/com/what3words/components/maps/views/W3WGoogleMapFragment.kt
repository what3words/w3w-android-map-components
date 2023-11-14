package com.what3words.components.maps.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.RequiresPermission
import androidx.core.util.Consumer
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.wrappers.GridColor
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.wrappers.W3WGoogleMapsWrapper
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.map.components.R
import com.what3words.map.components.databinding.W3wGoogleMapViewBinding

class W3WGoogleMapFragment() : W3WMapFragment, Fragment(), OnMapReadyCallback {

    private var mapFragment: SupportMapFragment? = null
    private lateinit var onReadyCallback: W3WMapFragment.OnMapReadyCallback
    private var mapEventsCallback: W3WMapFragment.MapEventsCallback? = null
    private var _binding: W3wGoogleMapViewBinding? = null
    private val binding get() = _binding!!
    private var apiKey: String? = null
    private var wrapper: What3WordsAndroidWrapper? = null
    private lateinit var map: W3WMap


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = W3wGoogleMapViewBinding.inflate(inflater, container, false)
        mapFragment = this.childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(this)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getMapCompass(): View? {
        val view = mapFragment?.view?.findViewWithTag<View>("GoogleMapMyLocationButton")
        val parent = view?.parent as? ViewGroup
        return parent?.getChildAt(4)
    }

    override fun moveCompassBy(
        leftMargin: Int,
        topMargin: Int,
        rightMargin: Int,
        bottomMargin: Int
    ) {
        val mapCompass = getMapCompass()
        mapCompass?.doOnLayout {
            // create layoutParams, giving it our wanted width and height(important, by default the width is "match parent")
            val rlp = RelativeLayout.LayoutParams(mapCompass.height, mapCompass.height)
            // position on top end
            rlp.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
            rlp.addRule(RelativeLayout.ALIGN_PARENT_END)
            rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            //give compass margin

            rlp.setMargins(leftMargin, topMargin, rightMargin, bottomMargin)
            mapCompass.layoutParams = rlp
        }
    }

    override fun apiKey(
        key: String,
        callback: W3WMapFragment.OnMapReadyCallback,
        mapEventsCallback: W3WMapFragment.MapEventsCallback?
    ) {
        apiKey = key
        onReadyCallback = callback
        this.mapEventsCallback = mapEventsCallback
    }

    override fun sdk(
        source: What3WordsAndroidWrapper,
        callback: W3WMapFragment.OnMapReadyCallback,
        mapEventsCallback: W3WMapFragment.MapEventsCallback?
    ) {
        wrapper = source
        onReadyCallback = callback
        this.mapEventsCallback = mapEventsCallback
    }

    override val fragment: Fragment
        get() = this

    override fun onMapReady(p0: GoogleMap) {
        val w3wMapsWrapper = when {
            apiKey != null -> {
                val wrapper = What3WordsV3(apiKey!!, requireContext())
                W3WGoogleMapsWrapper(
                    requireContext(),
                    p0,
                    wrapper
                )
            }

            wrapper != null -> {
                W3WGoogleMapsWrapper(
                    requireContext(),
                    p0,
                    wrapper!!
                )
            }

            else -> {
                throw Exception("MISSING SETUP")
            }
        }
        map = Map(w3wMapsWrapper, p0, mapEventsCallback)
        onReadyCallback.onMapReady(map)
    }

    class Map(
        private val w3wMapsWrapper: W3WGoogleMapsWrapper,
        private val map: GoogleMap,
        mapEventsCallback: W3WMapFragment.MapEventsCallback?
    ) :
        W3WMap {
        private var squareSelectedError: Consumer<APIResponse.What3WordsError>? = null
        private var squareSelectedSuccess: SelectedSquareConsumer<SuggestionWithCoordinates, Boolean, Boolean>? =
            null

        init {
            map.setMinZoomPreference(2.0f)
            map.setMaxZoomPreference(22.0f)

            map.setOnMapClickListener { latLng ->
                //OTHER FUNCTIONS
                this.w3wMapsWrapper.selectAtCoordinates(latLng.latitude, latLng.longitude, {
                    squareSelectedSuccess?.accept(
                        it,
                        true,
                        this.w3wMapsWrapper.findMarkerByCoordinates(
                            latLng.latitude,
                            latLng.longitude
                        ) != null
                    )
                }, {
                    squareSelectedError?.accept(it)
                })
            }

            map.setOnCameraIdleListener {
                mapEventsCallback?.onIdle()
                this.w3wMapsWrapper.updateMap()
            }

            map.setOnCameraMoveListener {
                mapEventsCallback?.onMove()
                this.w3wMapsWrapper.updateMove()
            }
        }

        fun googleMap(): GoogleMap {
            return map
        }

        override fun setLanguage(language: String) {
            w3wMapsWrapper.setLanguage(language)
        }

        override fun setGridColor(gridColor: GridColor) {
            w3wMapsWrapper.setGridColor(gridColor)
        }

        override fun onSquareSelected(
            onSuccess: SelectedSquareConsumer<SuggestionWithCoordinates, Boolean, Boolean>,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            this.squareSelectedSuccess = onSuccess
            this.squareSelectedError = onError
            this.w3wMapsWrapper.onMarkerClicked {
                onSuccess.accept(it, selectedByTouch = true, isMarked = true)
            }
        }

        override fun addMarkerAtSuggestion(
            suggestion: Suggestion,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            zoomLevel: Float?,
            onSuccess: Consumer<SuggestionWithCoordinates>?,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            w3wMapsWrapper.addMarkerAtSuggestion(suggestion, markerColor, {
                handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption, zoomLevel)
                onSuccess?.accept(it)
            }, {
                onError?.accept(it)
            })
        }

        private fun handleZoomOption(latLng: LatLng, zoomOption: W3WZoomOption, zoom: Float?) {
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
                        .zoom(zoom ?: getZoomSwitchLevel())
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

        override fun addMarkerAtSquare(
            suggestion: SuggestionWithCoordinates,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            zoomLevel: Float?,
            onSuccess: Consumer<SuggestionWithCoordinates>?,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            w3wMapsWrapper.addMarkerAtSuggestionWithCoordinates(suggestion, markerColor, {
                handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption, zoomLevel)
                onSuccess?.accept(it)
            }, {
                onError?.accept(it)
            })
        }

        override fun selectAtSquare(
            square: SuggestionWithCoordinates,
            zoomOption: W3WZoomOption,
            zoomLevel: Float?,
            onSuccess: Consumer<SuggestionWithCoordinates>?,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            w3wMapsWrapper.selectAtSuggestionWithCoordinates(square, {
                handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption, zoomLevel)
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

        override fun removeMarkerAtSquare(square: SuggestionWithCoordinates) {
            w3wMapsWrapper.removeMarkerAtWords(square.words)
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
            zoomLevel: Float?,
            onSuccess: Consumer<SuggestionWithCoordinates>?,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            w3wMapsWrapper.selectAtSuggestion(suggestion, {
                handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption, zoomLevel)
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

        override fun addMarkerAtWords(
            words: String,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            zoomLevel: Float?,
            onSuccess: Consumer<SuggestionWithCoordinates>?,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            w3wMapsWrapper.addMarkerAtWords(words, markerColor, {
                handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption, zoomLevel)
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
            zoomLevel: Float?,
            onSuccess: Consumer<SuggestionWithCoordinates>?,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            w3wMapsWrapper.selectAtWords(words, {
                handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption, zoomLevel)
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
            zoomLevel: Float?,
            onSuccess: Consumer<SuggestionWithCoordinates>?,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            w3wMapsWrapper.addMarkerAtCoordinates(lat, lng, markerColor, {
                handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption, zoomLevel)
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
            zoomLevel: Float?,
            onSuccess: Consumer<SuggestionWithCoordinates>?,
            onError: Consumer<APIResponse.What3WordsError>?
        ) {
            w3wMapsWrapper.selectAtCoordinates(lat, lng, {
                handleZoomOption(LatLng(it.coordinates.lat, it.coordinates.lng), zoomOption, zoomLevel)
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

        override fun moveToPosition(latitude: Double, longitude: Double, zoom: Double) {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(latitude, longitude), zoom.toFloat()
                )
            )
        }

        override fun animateToPosition(
            latitude: Double,
            longitude: Double,
            zoom: Double,
            onFinished: () -> Unit
        ) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(latitude, longitude), zoom.toFloat()
                ), object : GoogleMap.CancelableCallback {
                    override fun onCancel() {
                        onFinished.invoke()
                    }

                    override fun onFinish() {
                        onFinished.invoke()
                    }

                }
            )
        }

        override fun setMapGesturesEnabled(enabled: Boolean) {
            map.uiSettings.setAllGesturesEnabled(enabled)
        }

        override fun orientCamera() = with(map.cameraPosition) {
            val newCameraPosition = CameraPosition(target, zoom, tilt, 0f)
            map.moveCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition))
        }

        @SuppressLint("MissingPermission")
        @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
        override fun setMyLocationEnabled(enabled: Boolean) {
            map.isMyLocationEnabled = enabled
        }

        override fun setMyLocationButton(enabled: Boolean) {
            map.uiSettings.isMyLocationButtonEnabled = enabled
        }

        override fun getMapType(): W3WMap.MapType {
            return w3wMapsWrapper.getMapType()
        }

        override fun setMapType(mapType: W3WMap.MapType) {
            w3wMapsWrapper.setMapType(mapType)
        }

        override fun isDarkMode(): Boolean {
            return w3wMapsWrapper.isDarkMode()
        }

        override fun setDarkMode(darkMode: Boolean, customJsonStyle: String?) {
            w3wMapsWrapper.setDarkMode(darkMode, customJsonStyle)
        }

        override fun isMapAtGridLevel(): Boolean {
            return w3wMapsWrapper.isGridVisible
        }

        override fun setZoomSwitchLevel(zoom: Float) {
            w3wMapsWrapper.setZoomSwitchLevel(zoom)
        }

        override fun getZoomSwitchLevel() : Float {
            return w3wMapsWrapper.getZoomSwitchLevel()
        }

        override val target: Pair<Double, Double>
            get() = Pair(map.cameraPosition.target.latitude, map.cameraPosition.target.longitude)

        override val zoom: Float
            get() = map.cameraPosition.zoom
    }
}