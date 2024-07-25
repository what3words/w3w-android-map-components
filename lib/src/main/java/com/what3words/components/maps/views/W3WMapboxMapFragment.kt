package com.what3words.components.maps.views

import android.animation.Animator
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
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.what3words.androidwrapper.datasource.text.W3WApiTextDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.wrappers.GridColor
import com.what3words.components.maps.wrappers.W3WMapBoxWrapper
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.map.components.databinding.W3wMapboxMapViewBinding

class W3WMapboxMapFragment() : W3WMapFragment, Fragment() {
    private var onReadyCallback: W3WMapFragment.OnMapReadyCallback? = null
    private var mapEventsCallback: W3WMapFragment.MapEventsCallback? = null

    private var _binding: W3wMapboxMapViewBinding? = null
    private val binding get() = _binding!!
    private var apiKey: String? = null
    private var wrapper: W3WTextDataSource? = null
    private lateinit var map: W3WMap

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

    override fun apiKey(
        key: String,
        callback: W3WMapFragment.OnMapReadyCallback,
        mapEventsCallback: W3WMapFragment.MapEventsCallback?
    ) {
        apiKey = key
        onReadyCallback = callback
        this.mapEventsCallback = mapEventsCallback
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            setup()
        }
    }

    override fun sdk(
        source: W3WTextDataSource,
        callback: W3WMapFragment.OnMapReadyCallback,
        mapEventsCallback: W3WMapFragment.MapEventsCallback?
    ) {
        wrapper = source
        onReadyCallback = callback
        this.mapEventsCallback = mapEventsCallback
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            setup()
        }
    }

    override val fragment: Fragment
        get() = this

    override fun moveCompassBy(
        leftMargin: Int,
        topMargin: Int,
        rightMargin: Int,
        bottomMargin: Int
    ) {
        binding.mapView.compass.marginLeft = leftMargin.toFloat()
        binding.mapView.compass.marginTop = topMargin.toFloat()
        binding.mapView.compass.marginRight = rightMargin.toFloat()
        binding.mapView.compass.marginBottom = bottomMargin.toFloat()
    }

    private fun setup() {
        val w3wMapsWrapper = when {
            apiKey != null && _binding != null -> {
                W3WMapBoxWrapper(
                    requireContext(),
                    _binding!!.mapView.getMapboxMap(),
                    W3WApiTextDataSource.create(requireContext(), apiKey!!)
                )
            }

            wrapper != null && _binding != null -> {
                W3WMapBoxWrapper(
                    requireContext(),
                    _binding!!.mapView.getMapboxMap(),
                    wrapper!!
                )
            }

            else -> {
                throw Exception("MISSING SETUP")
            }
        }
        map = Map(w3wMapsWrapper, binding.mapView.getMapboxMap(), mapEventsCallback)
        onReadyCallback?.onMapReady(map)
    }

    class Map(
        private val w3wMapsWrapper: W3WMapBoxWrapper,
        private val map: MapboxMap,
        private val mapEventsCallback: W3WMapFragment.MapEventsCallback?
    ) :
        W3WMap {
        private var squareSelectedError: Consumer<W3WError>? = null
        private var squareSelectedSuccess: SelectedSquareConsumer<W3WAddress, Boolean, Boolean>? =
            null

        init {
            map.addOnMapClickListener { latLng ->
                //OTHER FUNCTIONS
                w3wMapsWrapper.selectAtCoordinates(latLng.latitude(), latLng.longitude(), {
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

            w3wMapsWrapper.onMarkerClicked {
                squareSelectedSuccess?.accept(
                    it,
                    true,
                    true
                )
            }

            map.addOnMapIdleListener {
                mapEventsCallback?.onIdle()
                w3wMapsWrapper.updateMap()
            }

            map.addOnCameraChangeListener {
                mapEventsCallback?.onMove()
                w3wMapsWrapper.updateMove()
            }
        }

        fun mapBoxMap(): MapboxMap {
            return map
        }

        override fun setLanguage(language: W3WRFC5646Language) {
            w3wMapsWrapper.setLanguage(language)
        }

        override fun onSquareSelected(
            onSuccess: SelectedSquareConsumer<W3WAddress, Boolean, Boolean>,
            onError: Consumer<W3WError>?
        ) {
            this.squareSelectedSuccess = onSuccess
            this.squareSelectedError = onError
        }


        private fun handleZoomOption(latLng: Point, zoomOption: W3WZoomOption, zoomLevel: Float?) {
            when (zoomOption) {
                W3WZoomOption.NONE -> {}
                W3WZoomOption.CENTER -> {
                    val cameraOptions = CameraOptions.Builder()
                        .center(latLng)
                        .build()
                    map.setCamera(cameraOptions)
                }

                W3WZoomOption.CENTER_AND_ZOOM -> {
                    val cameraOptions = CameraOptions.Builder()
                        .center(latLng)
                        .zoom(zoomLevel?.toDouble() ?: getZoomSwitchLevel().toDouble())
                        .build()
                    map.setCamera(cameraOptions)
                }
            }
        }

        private fun handleZoomOption(latLng: List<Point>, zoomOption: W3WZoomOption) {
            when (zoomOption) {
                W3WZoomOption.NONE -> {}
                W3WZoomOption.CENTER, W3WZoomOption.CENTER_AND_ZOOM -> {
                    val options = map.cameraForCoordinates(latLng)
                    map.setCamera(options)
                }
            }
        }

        //region add/remove/select by W3WSuggestion
        override fun addMarkerAtSuggestion(
            suggestion: W3WSuggestion,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.addMarkerAtSuggestion(suggestion, markerColor, { address ->
                address.center?.let {
                    handleZoomOption(
                        Point.fromLngLat(it.lng, it.lat),
                        zoomOption,
                        zoomLevel
                    )
                }
                onSuccess?.accept(address)
            }, {
                onError?.accept(it)
            })
        }

        override fun addMarkerAtSuggestion(
            listSuggestions: List<W3WSuggestion>,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<List<W3WAddress>>?,
            onError: Consumer<W3WError>?
        ) {
            w3wMapsWrapper.addMarkerAtSuggestion(listSuggestions, markerColor, {
                val listPoints = mutableListOf<Point>()
                it.forEach { marker ->
                    marker.center?.let { center ->
                        listPoints.add(
                            Point.fromLngLat(
                                center.lng,
                                center.lat
                            )
                        )
                    }
                }
                handleZoomOption(listPoints, zoomOption)
                onSuccess?.accept(it)
            }, {
                onError?.accept(it)
            })
        }

        override fun removeMarkerAtSuggestion(suggestion: W3WSuggestion) {
            w3wMapsWrapper.removeMarkerAtSuggestion(suggestion)
        }

        override fun removeMarkerAtSuggestion(listSuggestions: List<W3WSuggestion>) {
            w3wMapsWrapper.removeMarkerAtSuggestion(listSuggestions)
        }

        override fun selectAtSuggestion(
            suggestion: W3WSuggestion,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.selectAtSuggestion(suggestion, { address ->
                var isMarked = false
                address.center?.let {
                    handleZoomOption(
                        Point.fromLngLat(it.lng, it.lat),
                        zoomOption,
                        zoomLevel
                    )
                    isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(
                        it.lat,
                        it.lng
                    ) != null
                }
                onSuccess?.accept(address)
                squareSelectedSuccess?.accept(
                    address,
                    selectedByTouch = false,
                    isMarked = isMarked
                )
            }, {
                onError?.accept(it)
            })
        }
        //endregion

        //region add/remove/select by W3WAddress
        override fun addMarkerAtAddress(
            address: W3WAddress,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.addMarkerAtAddress(address, markerColor, {
                it.center?.let { center ->
                    handleZoomOption(
                        Point.fromLngLat(center.lng, center.lat),
                        zoomOption,
                        zoomLevel
                    )
                }
                onSuccess?.accept(it)
            }, {
                onError?.accept(it)
            })
        }

        override fun addMarkerAtAddress(
            listAddresses: List<W3WAddress>,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<List<W3WAddress>>?,
            onError: Consumer<W3WError>?
        ) {
            w3wMapsWrapper.addMarkerAtAddress(listAddresses, markerColor, {
                val listPoints = mutableListOf<Point>()
                it.forEach { marker ->
                    marker.center?.let { center ->
                        listPoints.add(
                            Point.fromLngLat(center.lng, center.lat)
                        )
                    }
                }
                handleZoomOption(listPoints, zoomOption)
                onSuccess?.accept(it)
            }, {
                onError?.accept(it)
            })
        }

        override fun selectAtAddress(
            address: W3WAddress,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.selectAtAddress(address, {
                it.center?.let { center ->
                    handleZoomOption(
                        Point.fromLngLat(center.lng, center.lat),
                        zoomOption,
                        zoomLevel
                    )
                }
                onSuccess?.accept(it)
            }, {
                onError?.accept(it)
            })
        }

        override fun removeMarkerAtAddress(address: W3WAddress) {
            w3wMapsWrapper.removeMarkerAtAddress(address)
        }

        override fun removeMarkerAtAddress(listAddresses: List<W3WAddress>) {
            w3wMapsWrapper.removeMarkerAtAddress(listAddresses)
        }
        //endregion

        //region add/remove/select by words
        override fun addMarkerAtWords(
            words: String,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.addMarkerAtWords(words, markerColor, { address ->
                address.center?.let {
                    handleZoomOption(
                        Point.fromLngLat(it.lng, it.lat),
                        zoomOption,
                        zoomLevel
                    )
                }
                onSuccess?.accept(address)
            }, {
                onError?.accept(it)
            })
        }

        override fun addMarkerAtWords(
            listWords: List<String>,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<List<W3WAddress>>?,
            onError: Consumer<W3WError>?
        ) {
            w3wMapsWrapper.addMarkerAtWords(listWords, markerColor, {
                val listPoints = mutableListOf<Point>()
                it.forEach { marker ->
                    marker.center?.let { center ->
                        listPoints.add(
                            Point.fromLngLat(
                                center.lng,
                                center.lat
                            )
                        )
                    }
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
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.selectAtWords(words, { address ->
                var isMarked = false
                address.center?.let {
                    handleZoomOption(
                        Point.fromLngLat(it.lng, it.lat),
                        zoomOption,
                        zoomLevel
                    )
                    isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(
                        it.lat,
                        it.lng
                    ) != null
                }
                onSuccess?.accept(address)
                squareSelectedSuccess?.accept(
                    address,
                    selectedByTouch = false,
                    isMarked = isMarked
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
        //endregion

        //region add/remove/select by coordinates
        override fun addMarkerAtCoordinates(
            lat: Double,
            lng: Double,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.addMarkerAtCoordinates(lat, lng, markerColor, { address ->
                address.center?.let {
                    handleZoomOption(
                        Point.fromLngLat(it.lng, it.lat),
                        zoomOption,
                        zoomLevel
                    )
                }
                onSuccess?.accept(address)
            }, {
                onError?.accept(it)
            })
        }

        override fun addMarkerAtCoordinates(
            listCoordinates: List<W3WCoordinates>,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<List<W3WAddress>>?,
            onError: Consumer<W3WError>?
        ) {
            w3wMapsWrapper.addMarkerAtCoordinates(listCoordinates, markerColor, {
                val listPoints = mutableListOf<Point>()
                it.forEach { marker ->
                    marker.center?.let { center ->
                        listPoints.add(
                            Point.fromLngLat(center.lng, center.lat)
                        )
                    }
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
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.selectAtCoordinates(lat, lng, { address ->
                var isMarked = false
                address.center?.let {
                    handleZoomOption(
                        Point.fromLngLat(it.lng, it.lat),
                        zoomOption,
                        zoomLevel
                    )
                    isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(
                        it.lat,
                        it.lng
                    ) != null
                }
                onSuccess?.accept(address)
                squareSelectedSuccess?.accept(
                    address,
                    selectedByTouch = false,
                    isMarked = isMarked
                )
            }, {
                onError?.accept(it)
            })
        }


        override fun removeMarkerAtCoordinates(lat: Double, lng: Double) {
            w3wMapsWrapper.removeMarkerAtCoordinates(lat, lng)
        }

        override fun removeMarkerAtCoordinates(listCoordinates: List<W3WCoordinates>) {
            w3wMapsWrapper.removeMarkerAtCoordinates(listCoordinates)
        }
        //endregion

        override fun findMarkerByCoordinates(lat: Double, lng: Double): W3WAddress? {
            return w3wMapsWrapper.findMarkerByCoordinates(lat, lng)
        }

        override fun setGridColor(gridColor: GridColor) {
            w3wMapsWrapper.setGridColor(gridColor)
        }

        override fun removeAllMarkers() {
            w3wMapsWrapper.removeAllMarkers()
        }

        override fun getAllMarkers(): List<W3WAddress> {
            return w3wMapsWrapper.getAllMarkers()
        }

        override fun getSelectedMarker(): W3WAddress? {
            return w3wMapsWrapper.getSelectedMarker()
        }

        override fun unselect() {
            w3wMapsWrapper.unselect()
        }

        override fun moveToPosition(latitude: Double, longitude: Double, zoom: Double) {
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(latitude, longitude))
                .zoom(zoom)
                .build()
            map.setCamera(cameraOptions)
        }

        override fun animateToPosition(
            latitude: Double,
            longitude: Double,
            zoom: Double,
            onFinished: () -> Unit
        ) {
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(latitude, longitude))
                .zoom(zoom)
                .build()
            map.flyTo(cameraOptions,
                MapAnimationOptions.mapAnimationOptions {
                    animatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {
                            // code to be invoked when animation starts
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            onFinished.invoke()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            onFinished.invoke()
                        }

                        override fun onAnimationRepeat(animation: Animator) {
                            // code to be invoked when animation repeats
                        }
                    })
                })
        }

        override fun setMapGesturesEnabled(enabled: Boolean) {
            map.gesturesPlugin {
                this.updateSettings {
                    this.pitchEnabled = enabled
                    this.rotateEnabled = enabled
                    this.scrollEnabled = enabled
                    this.quickZoomEnabled = enabled
                    this.pinchToZoomEnabled = enabled
                    this.doubleTapToZoomInEnabled = enabled
                    this.doubleTouchToZoomOutEnabled = enabled
                    //add more
                }
            }
        }

        override fun orientCamera() = with(map.cameraState) {
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(this.center.latitude(), this.center.longitude()))
                .zoom(this.zoom)
                .bearing(0.0)
                .pitch(this.pitch)
                .build()
            map.setCamera(cameraOptions)
        }

        override fun setMyLocationEnabled(enabled: Boolean) {
        }

        override fun setMyLocationButton(enabled: Boolean) {
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

        override fun getZoomSwitchLevel(): Float {
            return w3wMapsWrapper.getZoomSwitchLevel()
        }

        override val target: Pair<Double, Double>
            get() = Pair(map.cameraState.center.latitude(), map.cameraState.center.longitude())


        override val zoom: Float
            get() = map.cameraState.zoom.toFloat()
    }
}