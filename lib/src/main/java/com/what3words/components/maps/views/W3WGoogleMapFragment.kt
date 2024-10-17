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
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.wrappers.GridColor
import com.what3words.components.maps.wrappers.W3WGoogleMapsWrapper
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.map.components.R
import com.what3words.map.components.databinding.W3wGoogleMapViewBinding

class W3WGoogleMapFragment() : W3WMapFragment, Fragment(), OnMapReadyCallback,
    OnMapsSdkInitializedCallback {

    private var mapFragment: SupportMapFragment? = null
    private var onReadyCallback: W3WMapFragment.OnMapReadyCallback? = null
    private var mapEventsCallback: W3WMapFragment.MapEventsCallback? = null
    private var _binding: W3wGoogleMapViewBinding? = null
    private val binding get() = _binding!!
    private var dataSource: W3WTextDataSource? = null
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

    override fun onMapsSdkInitialized(p0: MapsInitializer.Renderer) {
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

    override fun initialize(
        dataSource: W3WTextDataSource,
        callback: W3WMapFragment.OnMapReadyCallback,
        mapEventsCallback: W3WMapFragment.MapEventsCallback?
    ) {
        this.dataSource = dataSource
        onReadyCallback = callback
        this.mapEventsCallback = mapEventsCallback
    }

    override val fragment: Fragment
        get() = this

    override fun onMapReady(p0: GoogleMap) {
        val w3wMapsWrapper = when {
            dataSource != null -> {
                W3WGoogleMapsWrapper(
                    requireContext(),
                    p0,
                    dataSource!!
                )
            }

            else -> {
                throw Exception("MISSING INITIALIZATION")
            }
        }
        map = Map(w3wMapsWrapper, p0, mapEventsCallback)
        onReadyCallback?.onMapReady(map)
    }

    class Map(
        private val w3wMapsWrapper: W3WGoogleMapsWrapper,
        private val map: GoogleMap,
        mapEventsCallback: W3WMapFragment.MapEventsCallback?
    ) :
        W3WMap {
        private var squareSelectedError: Consumer<W3WError>? = null
        private var squareSelectedSuccess: SelectedSquareConsumer<W3WAddress, Boolean, Boolean>? =
            null

        init {
            map.setMinZoomPreference(2.0f)
            map.setMaxZoomPreference(22.0f)

            map.setOnMapClickListener { latLng ->
                //OTHER FUNCTIONS
                val coordinates = W3WCoordinates(latLng.latitude, latLng.longitude)
                this.w3wMapsWrapper.selectAtCoordinates(coordinates, {
                    squareSelectedSuccess?.accept(
                        it,
                        true,
                        this.w3wMapsWrapper.findMarkerByCoordinates(coordinates) != null
                    )
                }, {
                    squareSelectedError?.accept(it)
                })
            }

            this.w3wMapsWrapper.onMarkerClicked { clickedAddress ->
                selectAtAddress(clickedAddress, onSuccess = { selectedAddress ->
                    squareSelectedSuccess?.accept(selectedAddress, selectedByTouch = true, isMarked = true)
                }, onError = { error ->
                    squareSelectedError?.accept(error)
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

        override fun setLanguage(language: W3WRFC5646Language) {
            w3wMapsWrapper.setLanguage(language)
        }

        override fun setGridColor(gridColor: GridColor) {
            w3wMapsWrapper.setGridColor(gridColor)
        }

        override fun onSquareSelected(
            onSuccess: SelectedSquareConsumer<W3WAddress, Boolean, Boolean>,
            onError: Consumer<W3WError>?
        ) {
            this.squareSelectedSuccess = onSuccess
            this.squareSelectedError = onError
        }

        /**
         * Handle zoom option for a [LatLng] with multiple zoom options which will use the zoom level
         * if it's provided or the default zoom level.
         */
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

        /**
         * Handle zoom option for a [LatLngBounds] rectangle with multiple zoom options which will use the zoom level
         * if it's provided or the default zoom level.
         */
        private fun handleZoomOption(latLng: LatLngBounds, zoomOption: W3WZoomOption) {
            when (zoomOption) {
                W3WZoomOption.NONE -> {}
                W3WZoomOption.CENTER, W3WZoomOption.CENTER_AND_ZOOM -> {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLng, 10))
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
            w3wMapsWrapper.addMarkerAtSuggestion(suggestion, markerColor, {
                it.center?.let { center ->
                    handleZoomOption(
                        LatLng(center.lat, center.lng),
                        zoomOption,
                        zoomLevel
                    )
                }
                onSuccess?.accept(it)
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
            w3wMapsWrapper.addMarkerAtSuggestion(listSuggestions, markerColor, { listAdded ->
                val latLngBoundsBuilder = LatLngBounds.builder()
                listAdded.forEach { address ->
                    address.center?.let {
                        latLngBoundsBuilder.include(LatLng(it.lat, it.lng))
                    }
                }
                handleZoomOption(latLngBoundsBuilder.build(), zoomOption)
                onSuccess?.accept(listAdded)
            }, {
                onError?.accept(it)
            })
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
                        LatLng(it.lat, it.lng),
                        zoomOption,
                        zoomLevel
                    )
                    isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(it) != null
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

        override fun removeMarkerAtSuggestion(suggestion: W3WSuggestion) {
            w3wMapsWrapper.removeMarkerAtSuggestion(suggestion)
        }

        override fun removeMarkerAtSuggestion(listSuggestions: List<W3WSuggestion>) {
            w3wMapsWrapper.removeMarkerAtSuggestion(listSuggestions)
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
                        LatLng(center.lat, center.lng),
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
            w3wMapsWrapper.addMarkerAtAddress(listAddresses, markerColor, { listAdded ->
                val latLngBoundsBuilder = LatLngBounds.builder()
                listAdded.forEach { address ->
                    address.center?.let {
                        latLngBoundsBuilder.include(LatLng(it.lat, it.lng))
                    }
                }
                handleZoomOption(latLngBoundsBuilder.build(), zoomOption)
                onSuccess?.accept(listAdded)
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
                var isMarked = false
                it.center?.let { center ->
                    handleZoomOption(
                        LatLng(center.lat, center.lng),
                        zoomOption,
                        zoomLevel
                    )
                    isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(center) != null
                }
                onSuccess?.accept(it)
                squareSelectedSuccess?.accept(
                    it,
                    selectedByTouch = false,
                    isMarked = isMarked
                )
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
                        LatLng(it.lat, it.lng),
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
                val latLngBoundsBuilder = LatLngBounds.builder()
                it.forEach { marker ->
                    marker.center?.let { center ->
                        latLngBoundsBuilder.include(LatLng(center.lat, center.lng))
                    }
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
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.selectAtWords(words, { address ->
                var isMarked = false
                address.center?.let {
                    handleZoomOption(
                        LatLng(it.lat, it.lng),
                        zoomOption,
                        zoomLevel
                    )
                    isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(it) != null
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
            coordinates: W3WCoordinates,
            markerColor: W3WMarkerColor,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.addMarkerAtCoordinates(coordinates, markerColor, { address ->
                address.center?.let {
                    handleZoomOption(
                        LatLng(it.lat, it.lng),
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
                val latLngBoundsBuilder = LatLngBounds.builder()
                it.forEach { marker ->
                    marker.center?.let { center ->
                        latLngBoundsBuilder.include(LatLng(center.lat, center.lng))
                    }
                }
                handleZoomOption(latLngBoundsBuilder.build(), zoomOption)
                onSuccess?.accept(it)
            }, {
                onError?.accept(it)
            })
        }

        override fun selectAtCoordinates(
            coordinates: W3WCoordinates,
            zoomOption: W3WZoomOption,
            onSuccess: Consumer<W3WAddress>?,
            onError: Consumer<W3WError>?,
            zoomLevel: Float?
        ) {
            w3wMapsWrapper.selectAtCoordinates(coordinates, { address ->
                var isMarked = false
                address.center?.let {
                    handleZoomOption(
                        LatLng(it.lat, it.lng),
                        zoomOption,
                        zoomLevel
                    )
                    isMarked = this.w3wMapsWrapper.findMarkerByCoordinates(it) != null
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

        override fun findMarkerByCoordinates(coordinates: W3WCoordinates): W3WAddress? {
            return w3wMapsWrapper.findMarkerByCoordinates(coordinates)
        }

        override fun removeMarkerAtCoordinates(coordinates: W3WCoordinates) {
            w3wMapsWrapper.removeMarkerAtCoordinates(coordinates)
        }

        override fun removeMarkerAtCoordinates(listCoordinates: List<W3WCoordinates>) {
            w3wMapsWrapper.removeMarkerAtCoordinates(listCoordinates)
        }

        //endregion

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

        override fun getZoomSwitchLevel(): Float {
            return w3wMapsWrapper.getZoomSwitchLevel()
        }

        override val target: Pair<Double, Double>
            get() = Pair(map.cameraPosition.target.latitude, map.cameraPosition.target.longitude)

        override val zoom: Float
            get() = map.cameraPosition.zoom
    }
}