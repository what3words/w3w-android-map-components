package com.what3words.components.maps.wrappers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.util.Consumer
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Image
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.ImageSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.imageSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.updateImage
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.toCameraOptions
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.components.maps.extensions.contains
import com.what3words.components.maps.extensions.generateUniqueId
import com.what3words.components.maps.extensions.main
import com.what3words.components.maps.models.W3WAddressWithStyle
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.toCircle
import com.what3words.components.maps.models.toGridFill
import com.what3words.components.maps.models.toPin
import com.what3words.components.maps.views.W3WMap
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.geometry.toGeoJSON
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.map.components.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * [W3WMapBoxWrapper] a wrapper to add what3words support to [MapboxMap].
 **
 * @param context app context.
 * @param textDataSource source of what3words data can be API or SDK.
 * @param mapView the [MapboxMap] view that [W3WMapBoxWrapper] should apply changes to.
 * @param dispatchers for custom dispatcher provider using [DefaultDispatcherProvider] by default.
 */
class W3WMapBoxWrapper(
    private val context: Context,
    private val mapView: MapboxMap,
    private val textDataSource: W3WTextDataSource,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : W3WMapWrapper {
    private var gridColor: GridColor = GridColor.AUTO
    private var isDirty: Boolean = false
    private var w3WMapManager: W3WMapManager = W3WMapManager(textDataSource, this, dispatchers)
    private var isDarkMode: Boolean = false
    private var zoomSwitchLevel: Float = DEFAULT_ZOOM_SWITCH_LEVEL
    private var selectedZoomedLayerId: String? = null
    private var selectedPinLayerId: String? = null


    companion object {
        const val GRID_LAYER = "GRID_LAYER"
        const val GRID_SOURCE = "GRID_SOURCE"
        const val SELECTED_SOURCE = "SELECTED_SOURCE"
        const val SELECTED_PIN_LAYER_PREFIX = "SELECTED_PIN_LAYER_%s"
        const val SELECTED_ZOOMED_SOURCE = "SELECTED_ZOOMED_SOURCE"
        const val SELECTED_ZOOMED_LAYER_PREFIX = "SELECTED_ZOOMED_LAYER_%s"
        const val SELECTED_ZOOMED_LAYER_SEARCH = "SELECTED_ZOOMED_LAYER_"
        const val DEFAULT_ZOOM_SWITCH_LEVEL = 19f
        const val MAX_ZOOM_LEVEL = 22f
        const val DEFAULT_BOUNDS_SCALE = 8f
        const val CIRCLE_ID_PREFIX = "CIRCLE_%s"
        const val PIN_ID_PREFIX = "PIN_%s"
        const val SQUARE_IMAGE_ID_PREFIX = "SQUARE_IMAGE_%s"
    }

    private var onMarkerClickedCallback: Consumer<W3WAddress>? = null
    private var lastScaledBounds: W3WRectangle? = null
    internal var isGridVisible: Boolean = false
    private var searchJob: Job? = null
    private var shouldDrawGrid: Boolean = true


    // TODO: Rethink this logic, MapBox v10 really complicated this, and the suggestion from someone at mapbox was this: https://github.com/mapbox/mapbox-maps-android/issues/1245#issuecomment-1082884148
    init {
        var callbackResult: Boolean
        mapView.addOnMapClickListener { point ->
            callbackResult = false
            mapView.queryRenderedFeatures(
                RenderedQueryGeometry(mapView.pixelForCoordinate(point)),
                RenderedQueryOptions(
                    w3WMapManager.listOfVisibleAddresses.map { it.address.words },
                    null
                )
            ) {
                if (it.isValue && it.value!!.isNotEmpty()) {
                    it.value!![0].source.let { source ->
                        w3WMapManager.listOfVisibleAddresses.firstOrNull { it.address.words == source }
                            ?.let {
                                isDirty = true
                                w3WMapManager.selectExistingMarker(it.address)
                                onMarkerClickedCallback?.accept(it.address)
                                callbackResult = true
                            }
                    }
                }
            }
            return@addOnMapClickListener callbackResult
        }
    }

    private fun shouldShowDarkGrid(): Boolean {
        if (gridColor == GridColor.LIGHT) return false
        if (gridColor == GridColor.DARK) return true
        return (mapView.getStyle()?.styleURI == Style.MAPBOX_STREETS
                || mapView.getStyle()?.styleURI == Style.TRAFFIC_DAY
                || mapView.getStyle()?.styleURI == Style.LIGHT
                || mapView.getStyle()?.styleURI == Style.OUTDOORS)
    }

    override fun setLanguage(language: W3WRFC5646Language): W3WMapBoxWrapper {
        w3WMapManager.language = language
        return this
    }

    override fun setZoomSwitchLevel(zoom: Float) {
        zoomSwitchLevel = zoom
    }

    override fun getZoomSwitchLevel(): Float {
        return zoomSwitchLevel
    }

    override fun gridEnabled(isEnabled: Boolean): W3WMapBoxWrapper {
        shouldDrawGrid = isEnabled
        return this
    }

    override fun setGridColor(gridColor: GridColor) {
        this.gridColor = gridColor
    }

    override fun onMarkerClicked(callback: Consumer<W3WAddress>): W3WMapBoxWrapper {
        onMarkerClickedCallback = callback
        return this
    }

    //region add/remove/select by W3WSuggestions

    override fun addMarkerAtSuggestion(
        suggestion: W3WSuggestion,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addWords(
            suggestion.w3wAddress.words,
            markerColor,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun addMarkerAtSuggestion(
        listSuggestions: List<W3WSuggestion>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addSuggestion(
            listSuggestions,
            markerColor,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun removeMarkerAtSuggestion(suggestion: W3WSuggestion) {
        isDirty = true
        w3WMapManager.removeSuggestion(suggestion)
    }

    override fun removeMarkerAtSuggestion(listSuggestions: List<W3WSuggestion>) {
        isDirty = true
        w3WMapManager.removeSuggestion(listSuggestions)
    }

    override fun selectAtSuggestion(
        suggestion: W3WSuggestion,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.selectSuggestion(
            suggestion,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    //endregion

    //region add/remove/select by W3WAddress
    override fun addMarkerAtAddress(
        address: W3WAddress,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addAddress(
            address,
            markerColor,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun addMarkerAtAddress(
        listAddresses: List<W3WAddress>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addAddress(listAddresses, markerColor, {
            isDirty = true
            onSuccess?.accept(it)
        }, onError)
    }

    override fun selectAtAddress(
        address: W3WAddress,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.selectedAddress(address, {
            isDirty = true
            onSuccess?.accept(it)
        }, onError)
    }

    override fun removeMarkerAtAddress(address: W3WAddress) {
        isDirty = true
        w3WMapManager.removeAddress(address)
    }

    override fun removeMarkerAtAddress(listAddresses: List<W3WAddress>) {
        isDirty = true
        w3WMapManager.removeAddress(listAddresses)
    }
    //endregion

    //region add/remove/select by coordinates

    override fun addMarkerAtCoordinates(
        lat: Double,
        lng: Double,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addCoordinates(
            lat,
            lng,
            markerColor,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun addMarkerAtCoordinates(
        listCoordinates: List<W3WCoordinates>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addCoordinates(
            listCoordinates,
            markerColor,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun selectAtCoordinates(
        lat: Double,
        lng: Double,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.selectCoordinates(
            lat,
            lng,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun findMarkerByCoordinates(lat: Double, lng: Double): W3WAddress? {
        return w3WMapManager.squareContains(lat, lng)?.address
    }

    override fun removeMarkerAtCoordinates(lat: Double, lng: Double) {
        isDirty = true
        w3WMapManager.removeCoordinates(lat, lng)
    }

    override fun removeMarkerAtCoordinates(listCoordinates: List<W3WCoordinates>) {
        isDirty = true
        w3WMapManager.removeCoordinates(listCoordinates)
    }
//endregion

    //region add/remove/select by words
    override fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addWords(
            words,
            markerColor,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addWords(
            listWords,
            markerColor,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun selectAtWords(
        words: String,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.selectWords(
            words,
            {
                isDirty = true
                onSuccess?.accept(it)
            },
            onError
        )
    }

    override fun removeMarkerAtWords(words: String) {
        isDirty = true
        w3WMapManager.removeWords(words)
    }

    override fun removeMarkerAtWords(listWords: List<String>) {
        isDirty = true
        w3WMapManager.removeWords(listWords)
    }

    override fun removeAllMarkers() {
        isDirty = true
        w3WMapManager.clearList()
    }

    override fun getAllMarkers(): List<W3WAddress> {
        return w3WMapManager.getList()
    }

    override fun getSelectedMarker(): W3WAddress? {
        return w3WMapManager.selectedAddress
    }

    override fun unselect() {
        runBlocking(dispatchers.io()) {
            isDirty = true
            w3WMapManager.unselect()
        }
    }

    /** This method should be called on [MapboxMap.addOnMapIdleListener].
     * This will allow to refresh the grid bounds on camera idle.
     */
    override fun updateMap() {
        deleteMarkers()
        if (isDirty) {
            redrawMapLayers()
        }
    }

    /** This method should be called on [MapboxMap.addOnCameraChangeListener].
     * This will allow to swap from markers to squares and show/hide grid when zoom goes higher or lower than the [W3WMapBoxWrapper.ZOOM_SWITCH_LEVEL] threshold.
     */
    override fun updateMove() {
        lastScaledBounds =
            scaleBounds(DEFAULT_BOUNDS_SCALE)
        if (mapView.cameraState.zoom < zoomSwitchLevel && isGridVisible) {
            redrawMapLayers(true)
            isGridVisible = false
            return
        }
        if (mapView.cameraState.zoom >= zoomSwitchLevel && !isGridVisible) {
            redrawMapLayers(true)
            return
        }
    }

    override fun getMapType(): W3WMap.MapType {
        return when (mapView.getStyle()?.styleURI) {
            Style.SATELLITE_STREETS -> W3WMap.MapType.HYBRID
            else -> W3WMap.MapType.NORMAL
        }
    }

    override fun setMapType(mapType: W3WMap.MapType) {
        when (mapType) {
            W3WMap.MapType.NORMAL -> mapView.loadStyleUri(if (isDarkMode) Style.DARK else Style.MAPBOX_STREETS)
            W3WMap.MapType.HYBRID -> mapView.loadStyleUri(Style.SATELLITE)
            W3WMap.MapType.TERRAIN -> mapView.loadStyleUri(Style.OUTDOORS)
            W3WMap.MapType.SATELLITE -> mapView.loadStyleUri(Style.SATELLITE)
        }
    }

    override fun isDarkMode(): Boolean {
        return isDarkMode
    }

    override fun setDarkMode(darkMode: Boolean, customJsonStyle: String?) {
        mapView.loadStyleUri(
            when (getMapType() == W3WMap.MapType.NORMAL) {
                true -> if (darkMode) Style.DARK else Style.MAPBOX_STREETS
                false -> Style.SATELLITE_STREETS
            }
        )
    }

    /** Scale bounds to [scale] times to get the grid larger than the visible [MapboxMap.coordinateBoundsForCamera] with [MapboxMap.cameraState]. This will increase performance and keep the grid visible for longer when moving camera.
     *
     * @param scale the factor scale to be applied to [MapboxMap.coordinateBoundsForCamera], e.g: 8f (8 times larger) or 0.5f (to cut by half)
     *
     */
    private fun scaleBounds(
        scale: Float = DEFAULT_BOUNDS_SCALE
    ): W3WRectangle {
        val bounds = mapView
            .coordinateBoundsForCamera(mapView.cameraState.toCameraOptions())
        val center = bounds.center()
        val finalNELat =
            ((scale * (bounds.northeast.latitude() - center.latitude()) + center.latitude()))
        val finalNELng =
            ((scale * (bounds.northeast.longitude() - center.longitude()) + center.longitude()))
        val finalSWLat =
            ((scale * (bounds.southwest.latitude() - center.latitude()) + center.latitude()))
        val finalSWLng =
            ((scale * (bounds.southwest.longitude() - center.longitude()) + center.longitude()))

        return W3WRectangle(
            W3WCoordinates(
                finalSWLat,
                finalSWLng
            ),
            W3WCoordinates(finalNELat, finalNELng)
        )
    }

    private fun redrawMapLayers(
        shouldCancelPreviousJob: Boolean = true,
        scale: Float = W3WGoogleMapsWrapper.DEFAULT_BOUNDS_SCALE
    ) {
        if (mapView.cameraState.zoom < zoomSwitchLevel && !isGridVisible) {
            clearMarkers()
            drawMarkersOnMap()
            isDirty = false
            return
        }
        if (mapView.cameraState.zoom < zoomSwitchLevel && isGridVisible) {
            isGridVisible = false
            clearGridAndZoomedInMarkers()
            drawMarkersOnMap()
            isDirty = false
            return
        }
        if (!shouldDrawGrid) return
        clearMarkers()
        isGridVisible = true
        lastScaledBounds =
            scaleBounds(scale)
        if (shouldCancelPreviousJob) searchJob?.cancel()
        searchJob = CoroutineScope(dispatchers.main()).launch {
            val grid = withContext(dispatchers.io()) {
                textDataSource.gridSection(lastScaledBounds!!)
            }
            isDirty = when (grid) {
                is W3WResult.Failure -> false
                is W3WResult.Success -> {
                    drawLinesOnMap(grid.value.toGeoJSON())
                    drawZoomedMarkers()
                    false
                }
            }
        }
    }

    private fun drawZoomedMarkers() {
        w3WMapManager.listOfVisibleAddresses.forEach {
            drawFilledZoomedMarker(it)
        }
        if (w3WMapManager.selectedAddress != null) {
            drawOutlineZoomedMarker(w3WMapManager.selectedAddress!!)
        }
    }

    /** [drawOutlineZoomedMarker] will be responsible for the highlighting the selected square by adding four lines to [MapboxMap.style] using [LineLayer].*/
    private fun drawOutlineZoomedMarker(address: W3WAddress) {
        if (address.square != null && address.center != null) {
            val listLines = LineString.fromLngLats(
                listOf(
                    Point.fromLngLat(
                        address.square!!.southwest.lng,
                        address.square!!.northeast.lat
                    ),
                    Point.fromLngLat(
                        address.square!!.northeast.lng,
                        address.square!!.northeast.lat
                    ),
                    Point.fromLngLat(
                        address.square!!.northeast.lng,
                        address.square!!.southwest.lat
                    ),
                    Point.fromLngLat(
                        address.square!!.southwest.lng,
                        address.square!!.southwest.lat
                    ),
                    Point.fromLngLat(
                        address.square!!.southwest.lng,
                        address.square!!.northeast.lat
                    ),
                    Point.fromLngLat(
                        address.square!!.northeast.lng,
                        address.square!!.northeast.lat
                    )
                )
            )
            mapView.getStyle { style ->
                val test = listLines.toJson()
                selectedZoomedLayerId = String.format(
                    SELECTED_ZOOMED_LAYER_PREFIX,
                    address.center!!.generateUniqueId()
                )
                if (style.styleSourceExists(SELECTED_ZOOMED_SOURCE)) {
                    style.getSourceAs<GeoJsonSource>(SELECTED_ZOOMED_SOURCE)?.data(test)
                } else {
                    style.addSource(
                        geoJsonSource(SELECTED_ZOOMED_SOURCE) {
                            data(test)
                        }
                    )
                }
                if (!style.styleLayerExists(selectedZoomedLayerId!!)) {
                    style.addLayer(
                        lineLayer(selectedZoomedLayerId!!, SELECTED_ZOOMED_SOURCE) {
                            lineColor(
                                if (shouldShowDarkGrid()) {
                                    context.getColor(R.color.grid_selected_normal)
                                } else {
                                    context.getColor(R.color.grid_selected_sat)
                                }
                            )
                            lineWidth(getGridSelectedBorderSizeBasedOnZoomLevel())
                        }
                    )
                }
            }
        }
    }

    /** [drawZoomedMarkers] will be responsible for the drawing all square images to four coordinates (top right, then clockwise) by adding them [MapboxMap.style] using [RasterLayer], still looking for a better option for this, this is the way I found it online.*/
    private fun drawFilledZoomedMarker(address: W3WAddressWithStyle) {
        if (address.address.square != null && address.address.center != null) {
            main(dispatchers) {
                val id = String.format(
                    SQUARE_IMAGE_ID_PREFIX,
                    address.address.center!!.generateUniqueId()
                )
                bitmapFromDrawableRes(context, address.markerColor.toGridFill())?.let { image ->
                    mapView.getStyle { style ->
                        if (!style.styleSourceExists(id)) {
                            style.addSource(
                                imageSource(id) {
                                    this.coordinates(
                                        listOf(
                                            listOf(
                                                address.address.square!!.southwest.lng,
                                                address.address.square!!.northeast.lat
                                            ),
                                            listOf(
                                                address.address.square!!.northeast.lng,
                                                address.address.square!!.northeast.lat

                                            ),
                                            listOf(
                                                address.address.square!!.northeast.lng,
                                                address.address.square!!.southwest.lat
                                            ),
                                            listOf(
                                                address.address.square!!.southwest.lng,
                                                address.address.square!!.southwest.lat
                                            )
                                        )
                                    )
                                }
                            )
                            style.addLayerBelow(
                                RasterLayer(
                                    id,
                                    id
                                ), GRID_LAYER
                            )
                            val imageSource: ImageSource =
                                style.getSourceAs(id)!!
                            val byteBuffer = ByteBuffer.allocate(image.byteCount)
                            image.copyPixelsToBuffer(byteBuffer)
                            imageSource.updateImage(
                                Image(
                                    image.width,
                                    image.height,
                                    byteBuffer.array()
                                )
                            )
                        } else {
                            val layer = style.getLayerAs<RasterLayer>(id)
                            layer?.visibility(Visibility.VISIBLE)
                        }
                    }
                }
            }
        }
    }

    /** [drawLinesOnMap] will be responsible for the drawing of the grid with data from the GeoJSON return from our API/SDK then adding the lines to [MapboxMap.style] using an [LineLayer].*/
    private fun drawLinesOnMap(geoJson: String) {
        mapView.getStyle {
            if (it.styleSourceExists(GRID_SOURCE)) {
                val source = it.getSourceAs<GeoJsonSource>(GRID_SOURCE)
                source?.data(geoJson)
                if (it.styleLayerExists(GRID_LAYER)) {
                    val layer = it.getLayerAs<LineLayer>(GRID_LAYER)
                    layer?.visibility(Visibility.VISIBLE)
                }
            } else {
                it.addSource(
                    geoJsonSource(GRID_SOURCE) {
                        data(geoJson)
                    }
                )
                it.addLayer(
                    lineLayer(GRID_LAYER, GRID_SOURCE) {
                        lineColor(getGridColorBasedOnZoomLevel())
                        lineWidth(getGridBorderSizeBasedOnZoomLevel())
                    }
                )
            }
        }
    }

    /** [clearGridAndZoomedInMarkers] will clear the grid [LineLayer] and all square images [RasterLayer] from [MapboxMap.style].*/
    private fun clearGridAndZoomedInMarkers() {
        mapView.getStyle { style ->
            if (style.styleLayerExists(GRID_LAYER)) {
                val layer = style.getLayerAs<LineLayer>(GRID_LAYER)
                layer?.visibility(Visibility.NONE)
            }
            style.styleLayers.filter { it.id.startsWith(SELECTED_ZOOMED_LAYER_SEARCH) }
                .forEach { t ->
                    style.removeStyleLayer(t.id)
                    style.removeStyleSource(SELECTED_ZOOMED_SOURCE)
                }
            w3WMapManager.listOfVisibleAddresses.forEach {
                val id =
                    String.format(SQUARE_IMAGE_ID_PREFIX, it.address.center?.generateUniqueId())
                if (style.styleLayerExists(id)) {
                    style.removeStyleLayer(id)
                    style.removeStyleSource(id)
                }
            }
        }
    }

    private fun deleteMarkers() {
        try {
            runBlocking {
                w3WMapManager.listOfAddressesToRemove.forEach { suggestion ->
                    mapView.getStyle { style ->
                        val id = String.format(
                            SQUARE_IMAGE_ID_PREFIX,
                            suggestion.address.center?.generateUniqueId()
                        )
                        if (style.styleLayerExists(id)) {
                            style.removeStyleLayer(id)
                            style.removeStyleSource(id)
                        }
                        if (style.styleLayerExists(suggestion.address.words)) {
                            style.removeStyleLayer(suggestion.address.words)
                            style.removeStyleSource(suggestion.address.words)
                        }
                    }
                }
                w3WMapManager.listOfAddressesToRemove.clear()
            }
        } catch (e: Exception) {
        }
    }


    /** [drawMarkersOnMap] holds the logic to decide which kind of pin should be added to [MapboxMap.style] based on selection and added points.*/
    private fun drawMarkersOnMap() {
        w3WMapManager.listOfVisibleAddresses.forEach {
            if (w3WMapManager.selectedAddress?.square?.contains(
                    it.address.center?.lat,
                    it.address.center?.lng
                ) == true
            ) {
                drawPin(it)
            } else {
                drawCircle(it)
            }
        }
        if (w3WMapManager.selectedAddress != null && w3WMapManager.listOfVisibleAddresses.all { it.address.words != w3WMapManager.selectedAddress!!.words }) {
            drawSelectedPin(w3WMapManager.selectedAddress!!)
        }
    }

    /** [drawSelectedPin] will be responsible for the drawing of the selected but not added w3w pins (i.e [R.drawable.ic_marker_pin_white] for [Style.SATELLITE]) by adding them to [MapboxMap.style] using a [SymbolLayer].*/
    private fun drawSelectedPin(data: W3WAddress) {
        data.center?.let { center ->
            val image = if (shouldShowDarkGrid()) {
                R.drawable.ic_marker_pin_dark_blue
            } else {
                R.drawable.ic_marker_pin_white
            }
            bitmapFromDrawableRes(
                context,
                image
            )?.let { bitmap ->
                mapView.getStyle { style ->
                    style.addImage(image.toString(), bitmap)
                    if (style.styleSourceExists(SELECTED_SOURCE)) {
                        style.getSourceAs<GeoJsonSource>(SELECTED_SOURCE)
                            ?.geometry(Point.fromLngLat(center.lng, center.lat))
                    } else {
                        style.addSource(
                            geoJsonSource(SELECTED_SOURCE) {
                                geometry(
                                    Point.fromLngLat(
                                        center.lng,
                                        center.lat
                                    )
                                )
                            }
                        )
                    }
                    selectedPinLayerId =
                        String.format(
                            SELECTED_PIN_LAYER_PREFIX,
                            center.generateUniqueId()
                        )
                    if (!style.styleLayerExists(selectedPinLayerId!!)) {
                        style.addLayer(
                            symbolLayer(selectedPinLayerId!!, SELECTED_SOURCE) {
                                iconImage(image.toString())
                                iconAnchor(IconAnchor.BOTTOM)
                                iconAllowOverlap(true)
                            }
                        )
                    }
                }
            }
        }
    }

    /** [drawPin] will be responsible for the drawing of the selected and added w3w pins (i.e [R.drawable.ic_marker_pin_red]) by adding them to [MapboxMap.style] using a [SymbolLayer].*/
    private fun drawPin(data: W3WAddressWithStyle) {
        data.address.center?.let { center ->
            data.markerColor.toPin().let { markerFillColor ->
                bitmapFromDrawableRes(context, markerFillColor)?.let { bitmap ->
                    mapView.getStyle { style ->
                        style.addImage(
                            String.format(PIN_ID_PREFIX, data.markerColor.toString()),
                            bitmap
                        )
                        if (!style.styleSourceExists(data.address.words)) {
                            style.addSource(
                                geoJsonSource(data.address.words) {
                                    geometry(
                                        Point.fromLngLat(
                                            center.lng,
                                            center.lat
                                        )
                                    )
                                }
                            )
                        }
                        if (!style.styleLayerExists(
                                center.generateUniqueId().toString()
                            )
                        ) {
                            style.addLayer(
                                symbolLayer(
                                    center.generateUniqueId().toString(),
                                    data.address.words
                                ) {
                                    iconImage(
                                        String.format(
                                            PIN_ID_PREFIX,
                                            data.markerColor.toString()
                                        )
                                    )
                                    iconAnchor(IconAnchor.BOTTOM)
                                    iconAllowOverlap(true)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /** [drawCircle] will be responsible for the drawing of the w3w circle pins (i.e [R.drawable.ic_marker_circle_red]) by adding them to [MapboxMap.style] using a [SymbolLayer].*/
    private fun drawCircle(data: W3WAddressWithStyle) {
        data.address.center?.let { center ->
            data.markerColor.toCircle().let { markerFillColor ->
                bitmapFromDrawableRes(context, markerFillColor)?.let { bitmap ->
                    mapView.getStyle { style ->
                        style.addImage(
                            String.format(CIRCLE_ID_PREFIX, data.markerColor.toString()),
                            bitmap
                        )
                        if (!style.styleSourceExists(data.address.words)) {
                            style.addSource(
                                geoJsonSource(data.address.words) {
                                    geometry(
                                        Point.fromLngLat(
                                            center.lng,
                                            center.lat
                                        )
                                    )
                                }
                            )
                        }
                        if (!style.styleLayerExists(
                                center.generateUniqueId().toString()
                            )
                        ) {
                            style.addLayer(
                                symbolLayer(
                                    center.generateUniqueId().toString(),
                                    data.address.words
                                ) {
                                    iconImage(
                                        String.format(
                                            CIRCLE_ID_PREFIX,
                                            data.markerColor.toString()
                                        )
                                    )
                                    iconAnchor(IconAnchor.CENTER)
                                    iconAllowOverlap(true)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [MapboxMap.cameraState] zoom. */
    private fun getGridColorBasedOnZoomLevel(): Int {
        return if (shouldShowDarkGrid()) {
            context.getColor(R.color.grid_dark)
        } else {
            context.getColor(R.color.grid_light)
        }
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [MapboxMap.cameraState] zoom. */
    private fun getGridBorderSizeBasedOnZoomLevel(zoom: Double = mapView.cameraState.zoom): Double {
        return when {
            zoom < zoomSwitchLevel -> context.resources.getDimensionPixelSize(R.dimen.grid_width_mapbox_gone)
                .toDouble()

            else -> context.resources.getDimension(R.dimen.grid_width_mapbox_far).toDouble()
        }
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [MapboxMap.cameraState] zoom. */
    private fun getGridSelectedBorderSizeBasedOnZoomLevel(zoom: Double = mapView.cameraState.zoom): Double {
        return when {
            zoom < 19 -> context.resources.getDimension(R.dimen.grid_width_mapbox_gone)
                .toDouble()

            zoom >= 19 && zoom < 20 -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_far)
                .toDouble()

            zoom >= 20 && zoom < 21 -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_middle)
                .toDouble()

            else -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_close)
                .toDouble()
        }
    }

    /** [clearMarkers] will clear the [selectedPinLayerId] and all [W3WMapManager.listOfVisibleAddresses] added [SymbolLayer]'s from [MapboxMap.style].*/
    private fun clearMarkers() {
        mapView.getStyle { style ->
            selectedPinLayerId?.let {
                if (style.styleLayerExists(it)) {
                    style.removeStyleLayer(it)
                    style.removeStyleSource(SELECTED_SOURCE)
                }
            }

            w3WMapManager.listOfVisibleAddresses.forEach {
                if (style.styleLayerExists(it.address.center?.generateUniqueId().toString())) {
                    style.removeStyleLayer(it.address.center?.generateUniqueId().toString())
                    style.removeStyleSource(it.address.words)
                }
            }
        }
    }
}
