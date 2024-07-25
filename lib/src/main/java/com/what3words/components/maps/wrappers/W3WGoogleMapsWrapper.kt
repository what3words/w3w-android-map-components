package com.what3words.components.maps.wrappers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.collections.GroundOverlayManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolylineManager
import com.what3words.androidwrapper.datasource.text.api.error.BadBoundingBoxTooBigError
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.components.maps.extensions.computeHorizontalLines
import com.what3words.components.maps.extensions.computeVerticalLines
import com.what3words.components.maps.extensions.contains
import com.what3words.components.maps.extensions.generateUniqueId
import com.what3words.components.maps.extensions.main
import com.what3words.components.maps.models.DarkModeStyle
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
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.map.components.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

/**
 * [W3WGoogleMapsWrapper] a wrapper to add what3words support to [GoogleMap].
 **
 * @param context app context.
 * @param mapView the [GoogleMap] view that [W3WGoogleMapsWrapper] should apply changes to.
 * @param textDataSource source of what3words data can be API or SDK.
 * @param dispatchers for custom dispatcher provider using [DefaultDispatcherProvider] by default.
 */
class W3WGoogleMapsWrapper(
    private val context: Context,
    private val mapView: GoogleMap,
    private val textDataSource: W3WTextDataSource,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : W3WMapWrapper {
    private var zoomSwitchLevel: Float = DEFAULT_ZOOM_SWITCH_LEVEL
    private var isDarkMode: Boolean = false

    companion object {
        const val VERTICAL_LINES_COLLECTION = "VERTICAL_LINES_COLLECTION"
        const val HORIZONTAL_LINES_COLLECTION = "HORIZONTAL_LINES_COLLECTION"
        const val SELECTED = "SELECTED"
        const val DEFAULT_ZOOM_SWITCH_LEVEL = 19f
        const val MAX_ZOOM_LEVEL = 22f
        const val DEFAULT_BOUNDS_SCALE = 6f
        const val SCALE_NORMALIZATION = 1f
    }

    private var gridColor: GridColor = GridColor.AUTO
    private var onMarkerClickedCallback: Consumer<W3WAddress>? = null
    private var lastScaledBounds: LatLngBounds? = null
    internal var isGridVisible: Boolean = false
    private var searchJob: Job? = null
    private var shouldDrawGrid: Boolean = true
    private var w3WMapManager: W3WMapManager = W3WMapManager(textDataSource, this, dispatchers)

    private val polylineManager: PolylineManager by lazy {
        val pol = PolylineManager(mapView)
        pol.newCollection(HORIZONTAL_LINES_COLLECTION)
        pol.newCollection(VERTICAL_LINES_COLLECTION)
        pol.newCollection(SELECTED)
        pol
    }

    private val markerManager: MarkerManager by lazy {
        MarkerManager(mapView)
    }

    private val groundOverlayManager: GroundOverlayManager by lazy {
        GroundOverlayManager(mapView)
    }

    private fun shouldShowDarkGrid(): Boolean {
        if (gridColor == GridColor.LIGHT) return false
        if (gridColor == GridColor.DARK) return true
        if (isDarkMode) return false
        return (mapView.mapType == GoogleMap.MAP_TYPE_NORMAL || mapView.mapType == GoogleMap.MAP_TYPE_TERRAIN)
    }

    override fun setLanguage(language: W3WRFC5646Language): W3WGoogleMapsWrapper {
        w3WMapManager.language = language
        return this
    }

    override fun setZoomSwitchLevel(zoom: Float) {
        zoomSwitchLevel = zoom
    }

    override fun getZoomSwitchLevel(): Float {
        return zoomSwitchLevel
    }

    override fun gridEnabled(isEnabled: Boolean): W3WGoogleMapsWrapper {
        shouldDrawGrid = isEnabled
        return this
    }

    override fun setGridColor(gridColor: GridColor) {
        this.gridColor = gridColor
    }

    override fun onMarkerClicked(callback: Consumer<W3WAddress>): W3WGoogleMapsWrapper {
        onMarkerClickedCallback = callback
        return this
    }

    //region add/remove/select by W3WSuggestion

    override fun addMarkerAtSuggestion(
        suggestion: W3WSuggestion,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addSuggestion(
            suggestion,
            markerColor,
            onSuccess,
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
            onSuccess,
            onError)
    }

    override fun removeMarkerAtSuggestion(suggestion: W3WSuggestion) {
        w3WMapManager.removeSuggestion(suggestion)
    }

    override fun removeMarkerAtSuggestion(listSuggestions: List<W3WSuggestion>) {
        w3WMapManager.removeSuggestion(listSuggestions)
    }

    override fun selectAtSuggestion(
        suggestion: W3WSuggestion,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.selectSuggestion(
            suggestion,
            onSuccess,
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
        w3WMapManager.addAddress(address, markerColor, onSuccess, onError)
    }

    override fun addMarkerAtAddress(
        listAddresses: List<W3WAddress>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<W3WAddress>>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addAddress(listAddresses, markerColor, onSuccess, onError)
    }

    override fun selectAtAddress(
        address: W3WAddress,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.selectedAddress(address, onSuccess, onError)
    }

    override fun removeMarkerAtAddress(address: W3WAddress) {
        w3WMapManager.removeAddress(address)
    }

    override fun removeMarkerAtAddress(listAddresses: List<W3WAddress>) {
        w3WMapManager.removeAddress(listAddresses)
    }

    //endregion

    //region add/remove by coordinates

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
            onSuccess,
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
            onSuccess,
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
            onSuccess,
            onError
        )
    }

    override fun findMarkerByCoordinates(lat: Double, lng: Double): W3WAddress? {
        return w3WMapManager.squareContains(lat, lng)?.address
    }

    override fun removeMarkerAtCoordinates(lat: Double, lng: Double) {
        w3WMapManager.removeCoordinates(lat, lng)
    }

    override fun removeMarkerAtCoordinates(listCoordinates: List<W3WCoordinates>) {
        w3WMapManager.removeCoordinates(listCoordinates)
    }
//endregion

    //region add/remove by words
    override fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        w3WMapManager.addWords(
            words,
            markerColor,
            onSuccess,
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
            onSuccess,
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
            onSuccess,
            onError
        )
    }

    override fun removeMarkerAtWords(words: String) {
        w3WMapManager.removeWords(words)
    }

    override fun removeMarkerAtWords(listWords: List<String>) {
        w3WMapManager.removeWords(listWords)
    }

    //endregion

    //region general public methods
    override fun removeAllMarkers() {
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
            w3WMapManager.unselect()
        }
    }

    /** This method should be called on [GoogleMap.setOnCameraIdleListener].
     * This will allow to refresh the grid bounds on camera idle.
     */
    override fun updateMap() {
        onMapMoved()
    }

    /** This method should be called on [GoogleMap.setOnCameraMoveListener].
     * This will allow to swap from markers to squares and show/hide grid when zoom goes higher or lower than the [ZOOM_SWITCH_LEVEL] threshold.
     */
    override fun updateMove() {
        lastScaledBounds =
            scaleBounds(mapView.projection.visibleRegion.latLngBounds)
        if (mapView.cameraPosition.zoom < zoomSwitchLevel && isGridVisible) {
            onMapMoved(true)
            isGridVisible = false
            return
        }
        if (mapView.cameraPosition.zoom >= zoomSwitchLevel && !isGridVisible) {
            onMapMoved(true)
            return
        }
    }

    override fun setMapType(mapType: W3WMap.MapType) {
        val newMapType = when (mapType) {
            W3WMap.MapType.NORMAL -> GoogleMap.MAP_TYPE_NORMAL
            W3WMap.MapType.HYBRID -> GoogleMap.MAP_TYPE_HYBRID
            W3WMap.MapType.TERRAIN -> GoogleMap.MAP_TYPE_TERRAIN
            W3WMap.MapType.SATELLITE -> GoogleMap.MAP_TYPE_SATELLITE
        }
        if (newMapType != mapView.mapType) {
            mapView.mapType = newMapType
            updateMap()
        }
    }

    override fun getMapType(): W3WMap.MapType {
        return when (mapView.mapType) {
            GoogleMap.MAP_TYPE_HYBRID -> W3WMap.MapType.HYBRID
            GoogleMap.MAP_TYPE_TERRAIN -> W3WMap.MapType.TERRAIN
            GoogleMap.MAP_TYPE_SATELLITE -> W3WMap.MapType.SATELLITE
            else -> W3WMap.MapType.NORMAL
        }
    }

    override fun setDarkMode(darkMode: Boolean, customJsonStyle: String?) {
        if (darkMode != isDarkMode) {
            isDarkMode = darkMode
            mapView.setMapStyle(
                if (darkMode) MapStyleOptions(
                    customJsonStyle ?: DarkModeStyle.darkMode
                )
                else null
            )
            updateMap()
        }
    }

    override fun isDarkMode(): Boolean {
        return isDarkMode
    }

    //endregion

    //region managers/collections on click events
    private val zoomedOutMarkerListener = GoogleMap.OnMarkerClickListener { p0 ->
        w3WMapManager.selectExistingMarker(p0.position.latitude, p0.position.longitude)
        onMarkerClickedCallback?.accept(w3WMapManager.selectedAddress)
        return@OnMarkerClickListener true
    }

    private val zoomedInMarkerListener =
        GoogleMap.OnGroundOverlayClickListener { p0 ->
            w3WMapManager.selectExistingMarker(p0.position.latitude, p0.position.longitude)
            onMarkerClickedCallback?.accept(w3WMapManager.selectedAddress)
        }
    //endregion

    //region private methods

    /** Scale bounds to [scale] times to get the grid larger than the visible [GoogleMap.getProjection] bounds. This will increase performance and keep the grid visible for longer when moving camera.
     *
     * @param bounds the [GoogleMap.getCameraPosition] bounds.
     * @param scale the factor scale to be applied to [bounds], e.g: 8f (8 times larger) or 0.5f (to cut by half)
     *
     */
    private fun scaleBounds(
        bounds: LatLngBounds,
        scale: Float = DEFAULT_BOUNDS_SCALE
    ): LatLngBounds {
        try {
            val center = bounds.center
            val centerPoint: Point = mapView.projection.toScreenLocation(center)
            val screenPositionNortheast: Point = mapView.projection.toScreenLocation(bounds.northeast)
            screenPositionNortheast.x =
                ((scale * (screenPositionNortheast.x - centerPoint.x) + centerPoint.x).roundToInt())
            screenPositionNortheast.y =
                ((scale * (screenPositionNortheast.y - centerPoint.y) + centerPoint.y).roundToInt())
            val scaledNortheast = mapView.projection.fromScreenLocation(screenPositionNortheast)
            val screenPositionSouthwest: Point = mapView.projection.toScreenLocation(bounds.southwest)
            screenPositionSouthwest.x =
                ((scale * (screenPositionSouthwest.x - centerPoint.x) + centerPoint.x).roundToInt())
            screenPositionSouthwest.y =
                ((scale * (screenPositionSouthwest.y - centerPoint.y) + centerPoint.y).roundToInt())
            val scaledSouthwest = mapView.projection.fromScreenLocation(screenPositionSouthwest)
            return LatLngBounds(scaledSouthwest, scaledNortheast)
        } catch (e: Exception) {
            //fallback to original bounds if something goes wrong with scaling
            e.printStackTrace()
            return bounds
        }
    }

    /** [onMapMoved] will be responsible for the drawing of grid, markers and squares depending on the zoom levels.
     * when:
     * - mapView.cameraPosition.zoom < ZOOM_SWITCH_LEVEL && !isGridVisible -> should draw all zoomed out markers .
     * - mapView.cameraPosition.zoom < ZOOM_SWITCH_LEVEL && isGridVisible -> should clear the grid and all zoomed in markers and draw zoomed out markers.
     * - else -> should clear all zoomed out markers and add get the grid from [wrapper], draw the grid and all zoomed in markers.
     *
     * @param shouldCancelPreviousJob if it should cancel previous [wrapper] call to get the grid, to improve performance and API requests.
     */
    private fun onMapMoved(
        shouldCancelPreviousJob: Boolean = true,
        scale: Float = DEFAULT_BOUNDS_SCALE
    ) {
        deleteMarkers()
        if (mapView.cameraPosition.zoom < zoomSwitchLevel && !isGridVisible) {
            clearMarkers()
            drawMarkersOnMap()
            return
        }
        if (mapView.cameraPosition.zoom < zoomSwitchLevel && isGridVisible) {
            isGridVisible = false
            clearGridAndZoomedInMarkers()
            drawMarkersOnMap()
            return
        }
        if (!shouldDrawGrid) return
        clearMarkers()
        isGridVisible = true
        lastScaledBounds =
            scaleBounds(mapView.projection.visibleRegion.latLngBounds, scale)
        if (shouldCancelPreviousJob) searchJob?.cancel()
        searchJob = CoroutineScope(dispatchers.io()).launch {
            val box = W3WRectangle(
                W3WCoordinates(
                    lastScaledBounds!!.southwest.latitude,
                    lastScaledBounds!!.southwest.longitude
                ),
                W3WCoordinates(
                    lastScaledBounds!!.northeast.latitude,
                    lastScaledBounds!!.northeast.longitude
                )
            )

            val grid = textDataSource.gridSection(box)
            when (grid) {
                is W3WResult.Failure -> {
                    if (grid.error is BadBoundingBoxTooBigError) {
                        main(dispatchers) {
                            onMapMoved(true, scale - SCALE_NORMALIZATION)
                        }
                    }
                }
                is W3WResult.Success -> {
                    val verticalLines = grid.value.lines.computeVerticalLines()
                    val horizontalLines = grid.value.lines.computeHorizontalLines()
                    drawLinesOnMap(
                        computedHorizontalLines = horizontalLines,
                        computedVerticalLines = verticalLines
                    )
                    drawZoomedMarkers()
                }
            }
        }
    }

    private fun drawMarkersOnMap() {
        w3WMapManager.listOfVisibleAddresses.filter { addressWithStyle ->
            if (lastScaledBounds != null && addressWithStyle.address.center != null) {
                lastScaledBounds!!.contains(
                    LatLng(addressWithStyle.address.center!!.lat, addressWithStyle.address.center!!.lng)
                )
            } else true
        }.forEach {
            if (w3WMapManager.selectedAddress?.square?.contains(
                    it.address.center!!.lat,
                    it.address.center!!.lng
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

    /** [drawPin] will be responsible for the drawing of the zoomed out marker if it's cached [W3WMapManager.listOfVisibleAddresses] AND selected [W3WMapManager.selectedAddress] using [GoogleMap] [markerManager]. (only one at the time)*/
    private fun drawPin(data: W3WAddressWithStyle) {
        data.address.center?.let { center ->
            data.markerColor.toPin().let { markerFillColor ->
                val markerOptions = MarkerOptions()
                    .contentDescription(center.generateUniqueId().toString())
                    .position(LatLng(center.lat, center.lng))
                    .icon(getBitmapDescriptorFromVector(context, markerFillColor))
                markerManager.getCollection(
                    data.address.words
                )?.addMarker(markerOptions) ?: kotlin.run {
                    val collection = markerManager.newCollection(
                        data.address.words
                    )
                    collection.addMarker(markerOptions)
                    collection.setOnMarkerClickListener(zoomedOutMarkerListener)
                }
            }
        }
    }

    /** [drawSelectedPin] will be responsible for the drawing of the zoomed out marker if it's selected [W3WMapManager.selectedAddress] AND NOT cached [W3WMapManager.listOfVisibleAddresses] using [GoogleMap] [markerManager]. (only one at the time)*/
    private fun drawSelectedPin(data: W3WAddress) {
        data.center?.let { center ->
            val markerOptions = MarkerOptions()
                .contentDescription(center.generateUniqueId().toString())
                .position(LatLng(center.lat, center.lng))
                .icon(
                    getBitmapDescriptorFromVector(
                        context,
                        if (shouldShowDarkGrid()) {
                            R.drawable.ic_marker_pin_dark_blue
                        } else {
                            R.drawable.ic_marker_pin_white
                        }
                    )
                )
            markerManager.getCollection(
                SELECTED
            )?.addMarker(markerOptions) ?: kotlin.run {
                val collection = markerManager.newCollection(
                    SELECTED
                )
                collection.addMarker(markerOptions)
                collection.setOnMarkerClickListener {
                    true
                }
            }
        }
    }

    /** [drawCircle] will be responsible for the drawing of the zoomed out marker if it's cached [W3WMapManager.listOfVisibleAddresses] AND NOT selected [W3WMapManager.selectedAddress] using [GoogleMap] [markerManager].*/
    private fun drawCircle(data: W3WAddressWithStyle) {
        data.address.center?.let { center ->
            val markerOptions = MarkerOptions()
                .contentDescription(center.generateUniqueId().toString())
                .position(LatLng(center.lat,center.lng))
                .icon(getBitmapDescriptorFromVector(context, data.markerColor.toCircle()))
            markerManager.getCollection(
                data.address.words
            )?.addMarker(markerOptions) ?: kotlin.run {
                val collection = markerManager.newCollection(
                    data.address.words
                )
                collection.addMarker(markerOptions)
                collection.setOnMarkerClickListener(zoomedOutMarkerListener)
            }
        }
    }

    /** [getBitmapDescriptorFromVector] will convert vector drawable to [BitmapDescriptor] that's used on [MarkerOptions], found on the internet should be reviewed. */
    private fun getBitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorDrawableResourceId: Int
    ): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [GoogleMap.getCameraPosition] zoom. */
    private fun getGridColorBasedOnZoomLevel(): Int {
        return if (shouldShowDarkGrid()) context.getColor(R.color.grid_dark)
        else context.getColor(R.color.grid_light)
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [GoogleMap.getCameraPosition] zoom. */
    private fun getGridBorderSizeBasedOnZoomLevel(zoom: Float = mapView.cameraPosition.zoom): Float {
        return when {
            zoom < zoomSwitchLevel -> context.resources.getDimension(R.dimen.grid_width_gone)
            else -> context.resources.getDimension(R.dimen.grid_width_close)
        }
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [GoogleMap.getCameraPosition] zoom. */
    private fun getGridSelectedBorderSizeBasedOnZoomLevel(zoom: Float = mapView.cameraPosition.zoom): Float {
        return when {
            zoom < 19 -> context.resources.getDimension(R.dimen.grid_width_gone)
            zoom >= 19 && zoom < 20 -> context.resources.getDimension(R.dimen.grid_selected_width_far)
            zoom >= 20 && zoom < 21 -> context.resources.getDimension(R.dimen.grid_selected_width_middle)
            else -> context.resources.getDimension(R.dimen.grid_selected_width_close)
        }
    }

    /** [drawLinesOnMap] will be responsible for the drawing of the grid using [GoogleMap] [polylineManager].*/
    private fun drawLinesOnMap(
        computedHorizontalLines: List<W3WCoordinates>,
        computedVerticalLines: List<W3WCoordinates>
    ) {
        main(dispatchers) {
            clearGridAndZoomedInMarkers()
            val optionsHorizontal = PolylineOptions().clickable(false)
                .width(getGridBorderSizeBasedOnZoomLevel())
                .color(getGridColorBasedOnZoomLevel())

            computedHorizontalLines.forEach {
                optionsHorizontal.add(LatLng(it.lat, it.lng))
            }

            val optionsVertical = PolylineOptions().clickable(false)
                .width(getGridBorderSizeBasedOnZoomLevel())
                .color(getGridColorBasedOnZoomLevel())

            computedVerticalLines.forEach {
                optionsVertical.add(LatLng(it.lat, it.lng))
            }

            polylineManager.getCollection(HORIZONTAL_LINES_COLLECTION)
                .addPolyline(optionsHorizontal)
            polylineManager.getCollection(VERTICAL_LINES_COLLECTION)
                .addPolyline(optionsVertical)
        }
    }

    private fun drawZoomedMarkers() {
        w3WMapManager.listOfVisibleAddresses.filter {
            if (lastScaledBounds != null && it.address.square != null) {
                lastScaledBounds!!.contains(
                    LatLng(it.address.center!!.lat, it.address.center!!.lng)
                )
            } else true
        }.forEach {
            drawFilledZoomedMarker(it.address, it.markerColor.toGridFill())
        }
        if (w3WMapManager.selectedAddress != null) {
            drawOutlineZoomedMarker(w3WMapManager.selectedAddress!!)
        }
    }

    /** [drawFilledZoomedMarker] will be responsible for the drawing of the zoomed in marker if it's cached [W3WMapManager.listOfVisibleAddresses] using [GoogleMap] [groundOverlayManager].*/
    private fun drawFilledZoomedMarker(address: W3WAddress, image: Int) {
        address.square?.let { square ->
            main(dispatchers) {
                val optionsVisible3wa = GroundOverlayOptions().clickable(true)
                    .image(getBitmapDescriptorFromVector(context, image))
                    .positionFromBounds(
                        LatLngBounds(
                            LatLng(
                                square.southwest.lat,
                                square.southwest.lng
                            ),
                            LatLng(square.northeast.lat, square.northeast.lng)
                        )
                    )

                groundOverlayManager.getCollection(
                    address.words
                )?.addGroundOverlay(optionsVisible3wa) ?: kotlin.run {
                    val collection = groundOverlayManager.newCollection(
                        address.words
                    )
                    collection.addGroundOverlay(optionsVisible3wa)
                    collection.setOnGroundOverlayClickListener(zoomedInMarkerListener)
                }
            }
        }
    }

    /** [drawOutlineZoomedMarker] will be responsible for the drawing of the zoomed in marker if it's selected [W3WMapManager.selectedAddress] using [GoogleMap] [polylineManager]. (only one at the time)*/
    private fun drawOutlineZoomedMarker(
        address: W3WAddress
    ) {
        address.square?.let { square ->
            main(dispatchers) {
                val optionsVisible3wa = PolylineOptions().clickable(false)
                    .width(getGridSelectedBorderSizeBasedOnZoomLevel())
                    .color(
                        if (shouldShowDarkGrid()) {
                            context.getColor(R.color.grid_selected_normal)
                        } else {
                            context.getColor(R.color.grid_selected_sat)
                        }
                    )
                    .zIndex(5f)
                optionsVisible3wa.addAll(
                    listOf(
                        LatLng(
                            square.northeast.lat,
                            square.southwest.lng
                        ),
                        LatLng(
                            square.northeast.lat,
                            square.northeast.lng
                        ),
                        LatLng(
                            square.southwest.lat,
                            square.northeast.lng
                        ),
                        LatLng(
                            square.southwest.lat,
                            square.southwest.lng
                        ),
                        LatLng(
                            square.northeast.lat,
                            square.southwest.lng
                        )
                    )
                )
                polylineManager.getCollection(
                    SELECTED
                )?.addPolyline(optionsVisible3wa) ?: kotlin.run {
                    polylineManager.newCollection(
                        SELECTED
                    ).addPolyline(optionsVisible3wa)
                }
            }
        }
    }

    /** [clearGridAndZoomedInMarkers] will clear the grid and all zoomed in markers from [polylineManager] and [groundOverlayManager].*/
    private fun clearGridAndZoomedInMarkers() {
        try {
            runBlocking {
                w3WMapManager.listOfVisibleAddresses.forEach { suggestion ->
                    polylineManager.getCollection(suggestion.address.words)?.polylines?.forEach {
                        main(dispatchers) {
                            it.remove()
                        }
                    }
                    groundOverlayManager.getCollection(suggestion.address.words)?.groundOverlays?.forEach {
                        main(dispatchers) {
                            it.remove()
                        }
                    }
                }
            }
            polylineManager.getCollection(SELECTED).polylines.forEach {
                it.remove()
            }
            polylineManager.getCollection(HORIZONTAL_LINES_COLLECTION).polylines.forEach {
                it.remove()
            }
            polylineManager.getCollection(VERTICAL_LINES_COLLECTION).polylines.forEach {
                it.remove()
            }
        } catch (e: Exception) {
        }
    }

    private fun deleteMarkers() {
        try {
            runBlocking {
                w3WMapManager.listOfAddressesToRemove.forEach { suggestion ->
                    markerManager.getCollection(suggestion.address.words)?.markers?.forEach {
                        main(dispatchers) {
                            it.remove()
                        }
                    }
                    polylineManager.getCollection(suggestion.address.words)?.polylines?.forEach {
                        main(dispatchers) {
                            it.remove()
                        }
                    }
                    groundOverlayManager.getCollection(suggestion.address.words)?.groundOverlays?.forEach {
                        main(dispatchers) {
                            it.remove()
                        }
                    }
                }
                w3WMapManager.listOfAddressesToRemove.clear()
            }
        } catch (e: Exception) {
        }
    }

    /** [clearMarkers] will clear all zoomed out markers from [markerManager].*/
    private fun clearMarkers() {
        try {
            runBlocking {
                w3WMapManager.listOfVisibleAddresses.forEach { suggestion ->
                    markerManager.getCollection(suggestion.address.words)?.markers?.forEach {
                        main(dispatchers) {
                            it.remove()
                        }
                    }
                }
                markerManager.getCollection(SELECTED)?.markers?.forEach {
                    main(dispatchers) {
                        it.remove()
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    //endregion
}