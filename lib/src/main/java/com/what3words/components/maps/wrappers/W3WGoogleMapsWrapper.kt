package com.what3words.components.maps.wrappers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.collections.GroundOverlayManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolylineManager
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.androidwrapper.helpers.DispatcherProvider
import com.what3words.components.maps.extensions.contains
import com.what3words.components.maps.extensions.main
import com.what3words.components.maps.models.Either
import com.what3words.components.maps.models.SuggestionWithCoordinatesAndStyle
import com.what3words.components.maps.models.W3WDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.toCircle
import com.what3words.components.maps.models.toGridFill
import com.what3words.components.maps.models.toPin
import com.what3words.javawrapper.request.BoundingBox
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Line
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.map.components.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.core.util.Consumer
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.models.W3WApiDataSource
import kotlin.math.roundToInt

/**
 * [W3WGoogleMapsWrapper] a wrapper to add what3words support to [GoogleMap].
 **
 * @param context app context.
 * @param mapView the [GoogleMap] view that [W3WGoogleMapsWrapper] should apply changes to.
 * @param w3wDataSource source of what3words data can be API or SDK.
 * @param dispatchers for custom dispatcher provider using [DefaultDispatcherProvider] by default.
 */
class W3WGoogleMapsWrapper(
    private val context: Context,
    private val mapView: GoogleMap,
    private val w3wDataSource: W3WDataSource,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : W3WMapWrapper {

    /**
     * [W3WGoogleMapsWrapper] a wrapper to add what3words support to [GoogleMap].
     **
     * @param context app context.
     * @param mapView the [GoogleMap] view that [W3WGoogleMapsWrapper] should apply changes to.
     * @param wrapper what3words android API wrapper to be used as source of data (i.e: c2c, c3wa, grid-section, autosuggest...).
     */
    constructor(
        context: Context,
        mapView: GoogleMap,
        wrapper: What3WordsV3
    ) : this(context, mapView, W3WApiDataSource(wrapper, context))

    companion object {
        const val VERTICAL_LINES_COLLECTION = "VERTICAL_LINES_COLLECTION"
        const val HORIZONTAL_LINES_COLLECTION = "HORIZONTAL_LINES_COLLECTION"
        const val SELECTED = "SELECTED"
        const val ZOOM_SWITCH_LEVEL = 18f
        const val DEFAULT_BOUNDS_SCALE = 6f
        const val SCALE_NORMALIZATION = 1f
    }

    private var gridColor: GridColor = GridColor.AUTO
    private var onMarkerClickedCallback: Consumer<SuggestionWithCoordinates>? = null
    private var lastScaledBounds: LatLngBounds? = null
    private var isGridVisible: Boolean = false
    private var searchJob: Job? = null
    private var shouldDrawGrid: Boolean = true
    private var w3wMapManager: W3WMapManager = W3WMapManager(w3wDataSource, this, dispatchers)

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
        return (mapView.mapType == GoogleMap.MAP_TYPE_NORMAL || mapView.mapType == GoogleMap.MAP_TYPE_TERRAIN )
    }

    /** Set the language of [SuggestionWithCoordinates.words] that onSuccess callbacks should return.
     *
     * @param language a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).
     */
    override fun setLanguage(language: String): W3WGoogleMapsWrapper {
        w3wMapManager.language = language
        return this
    }

    /** Enable grid overlay over map with all 3mx3m squares on the visible map bounds.
     *
     * @param isEnabled enable or disable grid, enabled by default.
     */
    override fun gridEnabled(isEnabled: Boolean): W3WGoogleMapsWrapper {
        shouldDrawGrid = isEnabled
        return this
    }

    /** Due to different map providers setting Dark/Light modes differently i.e: GoogleMaps sets dark mode using JSON styles but Mapbox has dark mode as a MapType.
     *
     * [GridColor.AUTO] - Will leave up to the library to decide which Grid color and selected square color to use to match some specific map types, i.e: use [GridColor.DARK] on normal map types, [GridColor.LIGHT] on Satellite and Traffic map types.
     * [GridColor.LIGHT] - Will force grid and selected square color to be light.
     * [GridColor.DARK] - Will force grid and selected square color to be dark.
     *
     * @param gridColor set grid color, per default will be [GridColor.AUTO].
     */
    override fun setGridColor(gridColor: GridColor) {
        this.gridColor = gridColor
    }

    /** A callback for when an existing marker on the map is clicked.
     *
     * @param callback it will be invoked when an existing marker on the map is clicked by the user.
     */
    override fun onMarkerClicked(callback: Consumer<SuggestionWithCoordinates>): W3WGoogleMapsWrapper {
        onMarkerClickedCallback = callback
        return this
    }

    //region add/remove by suggestion

    /** Add [Suggestion] to the map. This method will add a marker/square to the map after getting the [Suggestion] from our W3WAutosuggestEditText.
     *
     * @param suggestion the [Suggestion] returned by our text/voice autosuggest component.
     * @param markerColor is the [W3WMarkerColor] for the [Suggestion] added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun addMarkerAtSuggestion(
        suggestion: Suggestion,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.addWords(
            suggestion.words,
            markerColor,
            onSuccess,
            onError
        )
    }

    /** Add a list of [Suggestion] to the map. This method will add multiple markers/squares to the map after getting the suggestions from our W3WAutosuggestEditText.
     *
     * @param listSuggestions list of [Suggestion]s returned by our text/voice autosuggest component.
     * @param markerColor is the [W3WMarkerColor] for the suggestion added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun addMarkerAtSuggestion(
        listSuggestions: List<Suggestion>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.addWords(
            listSuggestions.map { it.words },
            markerColor,
            onSuccess,
            onError
        )
    }

    /** Remove [Suggestion] from the map.
     *
     * @param suggestion the [Suggestion] to be removed.
     */
    override fun removeMarkerAtSuggestion(suggestion: Suggestion) {
        w3wMapManager.removeWords(suggestion.words)
    }

    /** Remove [Suggestion]s from the map.
     *
     * @param listSuggestions the list of [Suggestion]s to remove.
     */
    override fun removeMarkerAtSuggestion(listSuggestions: List<Suggestion>) {
        w3wMapManager.removeWords(listSuggestions.map { it.words })
    }

    /** Set [Suggestion] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param suggestion the [Suggestion] returned by our text/voice autosuggest component.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun selectAtSuggestion(
        suggestion: Suggestion,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.selectWords(
            suggestion.words,
            onSuccess,
            onError
        )
    }

    override fun addMarkerAtSuggestionWithCoordinates(
        suggestion: SuggestionWithCoordinates,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        if (suggestion.coordinates != null) {
            w3wMapManager.addSuggestionWithCoordinates(suggestion, markerColor, onSuccess)
        } else {
            w3wMapManager.addWords(suggestion.words, markerColor, onSuccess, onError)
        }
    }

    override fun selectAtSuggestionWithCoordinates(
        suggestion: SuggestionWithCoordinates,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        if (suggestion.coordinates != null) {
            w3wMapManager.selectSuggestionWithCoordinates(suggestion, onSuccess)
        } else {
            w3wMapManager.selectWords(suggestion.words, onSuccess, onError)
        }
    }

    //endregion

    //region add/remove by coordinates

    /** Add [Coordinates] to the map. This method will add a marker/square to the map based on each of the [Coordinates] provided latitude and longitude.
     *
     * @param lat latitude to be added.
     * @param lng longitude to be added.
     * @param markerColor is the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun addMarkerAtCoordinates(
        lat: Double,
        lng: Double,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.addCoordinates(
            lat,
            lng,
            markerColor,
            onSuccess,
            onError
        )
    }

    /** Add a list of [Coordinates] to the map. This method will add multiple markers/squares to the map based on the latitude and longitude of each [Coordinates] on the list.
     *
     * @param listCoordinates list of [Pair.first] = latitude, [Pair.second] = longitude to be added.
     * @param markerColor is the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun addMarkerAtCoordinates(
        listCoordinates: List<Pair<Double, Double>>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.addCoordinates(
            listCoordinates,
            markerColor,
            onSuccess,
            onError
        )
    }

    /** Set Coordinates [lat], [lng] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param lat latitude to be added.
     * @param lng longitude to be added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun selectAtCoordinates(
        lat: Double,
        lng: Double,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.selectCoordinates(
            lat,
            lng,
            onSuccess,
            onError
        )
    }

    override fun findMarkerByCoordinates(lat: Double, lng: Double): SuggestionWithCoordinates? {
        return w3wMapManager.squareContains(lat, lng)?.suggestion
    }

    /** Remove marker at [lat],[lng] from the map.
     *
     * @param lat latitude coordinates of the marker to be removed.
     * @param lng longitude coordinates of the marker to be removed.
     */
    override fun removeMarkerAtCoordinates(lat: Double, lng: Double) {
        w3wMapManager.removeCoordinates(lat, lng)
    }

    /** Remove markers based on [listCoordinates] which [Pair.first] is latitude, [Pair.second] is longitude of the marker in the map.
     *
     * @param listCoordinates list of [Pair.first] latitude, [Pair.second] longitude coordinates of the markers to be removed.
     */
    override fun removeMarkerAtCoordinates(listCoordinates: List<Pair<Double, Double>>) {
        w3wMapManager.removeCoordinates(listCoordinates)
    }
//endregion

    //region add/remove by words
    /** Add a three word address to the map. This method will add a marker/square to the map if [words] are a valid three word address, e.g., filled.count.soap. If it's not a valid three word address, [onError] will be called returning [APIResponse.What3WordsError.BAD_WORDS].
     *
     * @param words three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [words].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.addWords(
            words,
            markerColor,
            onSuccess,
            onError
        )
    }

    /** Add a list of three word addresses to the map. This method will add a marker/square to the map if all [listWords] are a valid three word addresses, e.g., filled.count.soap. If any valid three word address is not valid, [onError] will be called returning [APIResponse.What3WordsError.BAD_WORDS].
     *
     * @param listWords list of three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [listWords] added.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [listWords].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    override fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.addWords(
            listWords,
            markerColor,
            onSuccess,
            onError
        )
    }

    /** Set [words] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param words three word address to be added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun selectAtWords(
        words: String,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        w3wMapManager.selectWords(
            words,
            onSuccess,
            onError
        )
    }

    /** Remove three word address from the map.
     *
     * @param words the three word address to be removed.
     */
    override fun removeMarkerAtWords(words: String) {
        w3wMapManager.removeWords(words)
    }

    /** Remove a list of three word addresses from the map.
     *
     * @param listWords the list of three word addresses to remove.
     */
    override fun removeMarkerAtWords(listWords: List<String>) {
        w3wMapManager.removeWords(listWords)
    }

    //endregion

    //region general public methods
    /** Remove all markers from the map. */
    override fun removeAllMarkers() {
        w3wMapManager.clearList()
    }

    /** Get all added [SuggestionWithCoordinates] from the map.
     *
     * @return list of [SuggestionWithCoordinates] with all items added to the map.
     */
    override fun getAllMarkers(): List<SuggestionWithCoordinates> {
        return w3wMapManager.getList()
    }

    override fun getSelectedMarker(): SuggestionWithCoordinates? {
        return w3wMapManager.selectedSuggestion
    }

    override fun unselect() {
        runBlocking(dispatchers.io()) {
            w3wMapManager.unselect()
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
        if (mapView.cameraPosition.zoom < ZOOM_SWITCH_LEVEL && isGridVisible) {
            onMapMoved(true)
            isGridVisible = false
            return
        }
        if (mapView.cameraPosition.zoom >= ZOOM_SWITCH_LEVEL && !isGridVisible) {
            onMapMoved(true)
            return
        }
    }

    //endregion

    //region managers/collections on click events
    private val zoomedOutMarkerListener = GoogleMap.OnMarkerClickListener { p0 ->
        w3wMapManager.selectExistingMarker(p0.position.latitude, p0.position.longitude)
        w3wMapManager.selectedSuggestion?.let {
            onMarkerClickedCallback?.accept(it)
        }
        updateMap()
        return@OnMarkerClickListener true
    }

    private val zoomedInMarkerListener =
        GoogleMap.OnGroundOverlayClickListener { p0 ->
            w3wMapManager.selectExistingMarker(p0.position.latitude, p0.position.longitude)
            w3wMapManager.selectedSuggestion?.let {
                onMarkerClickedCallback?.accept(it)
            }
            updateMap()
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
    }

    /** [onMapMoved] will be responsible for the drawing of grid, markers and squares depending on the zoom levels.
     * when:
     * - mapView.cameraPosition.zoom < ZOOM_SWITCH_LEVEL && !isGridVisible -> should draw all zoomed out markers .
     * - mapView.cameraPosition.zoom < ZOOM_SWITCH_LEVEL && isGridVisible -> should clear the grid and all zoomed in markers and draw zoomed out markers.
     * - else -> should clear all zoomed out markers and add get the grid from [w3wDataSource], draw the grid and all zoomed in markers.
     *
     * @param shouldCancelPreviousJob if it should cancel previous [w3wDataSource] call to get the grid, to improve performance and API requests.
     */
    private fun onMapMoved(
        shouldCancelPreviousJob: Boolean = true,
        scale: Float = DEFAULT_BOUNDS_SCALE
    ) {
        if (mapView.cameraPosition.zoom < ZOOM_SWITCH_LEVEL && !isGridVisible) {
            clearMarkers()
            drawMarkersOnMap()
            return
        }
        if (mapView.cameraPosition.zoom < ZOOM_SWITCH_LEVEL && isGridVisible) {
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
            val box = BoundingBox(
                Coordinates(
                    lastScaledBounds!!.southwest.latitude,
                    lastScaledBounds!!.southwest.longitude
                ),
                Coordinates(
                    lastScaledBounds!!.northeast.latitude,
                    lastScaledBounds!!.northeast.longitude
                )
            )

            when (val grid = w3wDataSource.getGrid(box)) {
                is Either.Left -> {
                    if (grid.a == APIResponse.What3WordsError.BAD_BOUNDING_BOX_TOO_BIG) {
                        main(dispatchers) {
                            onMapMoved(true, scale - SCALE_NORMALIZATION)
                        }
                    }
                }
                is Either.Right -> {
                    val verticalLines = grid.b.computeVerticalLines()
                    val horizontalLines = grid.b.computeHorizontalLines()
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
        w3wMapManager.suggestionsCached.filter {
            if (lastScaledBounds != null) {
                lastScaledBounds!!.contains(
                    LatLng(it.suggestion.coordinates.lat, it.suggestion.coordinates.lng)
                )
            } else true
        }.forEach {
            if (w3wMapManager.selectedSuggestion?.square?.contains(
                    it.suggestion.coordinates.lat,
                    it.suggestion.coordinates.lng
                ) == true
            ) {
                drawPin(it)
            } else {
                drawCircle(it)
            }
        }
        if (w3wMapManager.selectedSuggestion != null && w3wMapManager.suggestionsCached.all { it.suggestion.words != w3wMapManager.selectedSuggestion!!.words }) {
            drawSelectedPin(w3wMapManager.selectedSuggestion!!)
        }
    }

    /** [drawPin] will be responsible for the drawing of the zoomed out marker if it's cached [W3WMapManager.suggestionsCached] AND selected [W3WMapManager.selectedSuggestion] using [GoogleMap] [markerManager]. (only one at the time)*/
    private fun drawPin(data: SuggestionWithCoordinatesAndStyle) {
        data.markerColor.toPin().let { markerFillColor ->
            val markerOptions = MarkerOptions()
                .position(LatLng(data.suggestion.coordinates.lat, data.suggestion.coordinates.lng))
                .icon(getBitmapDescriptorFromVector(context, markerFillColor))
            markerManager.getCollection(
                data.suggestion.words
            )?.addMarker(markerOptions) ?: kotlin.run {
                val collection = markerManager.newCollection(
                    data.suggestion.words
                )
                collection.addMarker(markerOptions)
                collection.setOnMarkerClickListener(zoomedOutMarkerListener)
            }
        }
    }

    /** [drawSelectedPin] will be responsible for the drawing of the zoomed out marker if it's selected [W3WMapManager.selectedSuggestion] AND NOT cached [W3WMapManager.suggestionsCached] using [GoogleMap] [markerManager]. (only one at the time)*/
    private fun drawSelectedPin(data: SuggestionWithCoordinates) {
        val markerOptions = MarkerOptions()
            .position(LatLng(data.coordinates.lat, data.coordinates.lng))
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

    /** [drawCircle] will be responsible for the drawing of the zoomed out marker if it's cached [W3WMapManager.suggestionsCached] AND NOT selected [W3WMapManager.selectedSuggestion] using [GoogleMap] [markerManager].*/
    private fun drawCircle(data: SuggestionWithCoordinatesAndStyle) {
        val markerOptions = MarkerOptions()
            .position(LatLng(data.suggestion.coordinates.lat, data.suggestion.coordinates.lng))
            .icon(getBitmapDescriptorFromVector(context, data.markerColor.toCircle()))
        markerManager.getCollection(
            data.suggestion.words
        )?.addMarker(markerOptions) ?: kotlin.run {
            val collection = markerManager.newCollection(
                data.suggestion.words
            )
            collection.addMarker(markerOptions)
            collection.setOnMarkerClickListener(zoomedOutMarkerListener)
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
            zoom < ZOOM_SWITCH_LEVEL -> context.resources.getDimension(R.dimen.grid_width_gone)
            else -> context.resources.getDimension(R.dimen.grid_width_close)
        }
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [GoogleMap.getCameraPosition] zoom. */
    private fun getGridSelectedBorderSizeBasedOnZoomLevel(zoom: Float = mapView.cameraPosition.zoom): Float {
        return when {
            zoom < ZOOM_SWITCH_LEVEL -> context.resources.getDimension(R.dimen.grid_width_gone)
            zoom >= ZOOM_SWITCH_LEVEL && zoom < 19 -> context.resources.getDimension(R.dimen.grid_selected_width_far)
            zoom >= 19 && zoom < 20 -> context.resources.getDimension(R.dimen.grid_selected_width_middle)
            else -> context.resources.getDimension(R.dimen.grid_selected_width_close)
        }
    }

    /** [drawLinesOnMap] will be responsible for the drawing of the grid using [GoogleMap] [polylineManager].*/
    private fun drawLinesOnMap(
        computedHorizontalLines: List<com.what3words.javawrapper.response.Coordinates>,
        computedVerticalLines: List<com.what3words.javawrapper.response.Coordinates>
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
        w3wMapManager.suggestionsCached.filter {
            if (lastScaledBounds != null) {
                lastScaledBounds!!.contains(
                    LatLng(it.suggestion.coordinates.lat, it.suggestion.coordinates.lng)
                )
            } else true
        }.forEach {
            drawFilledZoomedMarker(it.suggestion, it.markerColor.toGridFill())
        }
        if (w3wMapManager.selectedSuggestion != null) {
            drawOutlineZoomedMarker(w3wMapManager.selectedSuggestion!!)
        }
    }

    /** [drawFilledZoomedMarker] will be responsible for the drawing of the zoomed in marker if it's cached [W3WMapManager.suggestionsCached] using [GoogleMap] [groundOverlayManager].*/
    private fun drawFilledZoomedMarker(suggestion: SuggestionWithCoordinates, image: Int) {
        main(dispatchers) {
            val optionsVisible3wa = GroundOverlayOptions().clickable(true)
                .image(getBitmapDescriptorFromVector(context, image))
                .positionFromBounds(
                    LatLngBounds(
                        LatLng(
                            suggestion.square.southwest.lat,
                            suggestion.square.southwest.lng
                        ),
                        LatLng(suggestion.square.northeast.lat, suggestion.square.northeast.lng)
                    )
                )

            groundOverlayManager.getCollection(
                suggestion.words
            )?.addGroundOverlay(optionsVisible3wa) ?: kotlin.run {
                val collection = groundOverlayManager.newCollection(
                    suggestion.words
                )
                collection.addGroundOverlay(optionsVisible3wa)
                collection.setOnGroundOverlayClickListener(zoomedInMarkerListener)
            }
        }
    }

    /** [drawOutlineZoomedMarker] will be responsible for the drawing of the zoomed in marker if it's selected [W3WMapManager.selectedSuggestion] using [GoogleMap] [polylineManager]. (only one at the time)*/
    private fun drawOutlineZoomedMarker(
        suggestion: SuggestionWithCoordinates
    ) {
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
                        suggestion.square.northeast.lat,
                        suggestion.square.southwest.lng
                    ),
                    LatLng(
                        suggestion.square.northeast.lat,
                        suggestion.square.northeast.lng
                    ),
                    LatLng(
                        suggestion.square.southwest.lat,
                        suggestion.square.northeast.lng
                    ),
                    LatLng(
                        suggestion.square.southwest.lat,
                        suggestion.square.southwest.lng
                    ),
                    LatLng(
                        suggestion.square.northeast.lat,
                        suggestion.square.southwest.lng
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

    /** [clearGridAndZoomedInMarkers] will clear the grid and all zoomed in markers from [polylineManager] and [groundOverlayManager].*/
    private fun clearGridAndZoomedInMarkers() {
        try {
            runBlocking {
                w3wMapManager.suggestionsCached.forEach { suggestion ->
                    polylineManager.getCollection(suggestion.suggestion.words)?.polylines?.forEach {
                        main(dispatchers) {
                            polylineManager.getCollection(suggestion.suggestion.words).remove(it)
                        }
                    }
                    groundOverlayManager.getCollection(suggestion.suggestion.words)?.groundOverlays?.forEach {
                        main(dispatchers) {
                            groundOverlayManager.getCollection(suggestion.suggestion.words)
                                .remove(it)
                        }
                    }
                }
            }
            polylineManager.getCollection(SELECTED).polylines.forEach {
                polylineManager.getCollection(SELECTED).remove(it)
            }
            polylineManager.getCollection(HORIZONTAL_LINES_COLLECTION).polylines.forEach {
                polylineManager.getCollection(HORIZONTAL_LINES_COLLECTION).remove(it)
            }
            polylineManager.getCollection(VERTICAL_LINES_COLLECTION).polylines.forEach {
                polylineManager.getCollection(VERTICAL_LINES_COLLECTION).remove(it)
            }
        } catch (e: Exception) {
        }
    }

    /** [clearMarkers] will clear all zoomed out markers from [markerManager].*/
    private fun clearMarkers() {
        try {
            runBlocking {
                w3wMapManager.suggestionsCached.forEach { suggestion ->
                    markerManager.getCollection(suggestion.suggestion.words)?.markers?.forEach {
                        main(dispatchers) {
                            markerManager.getCollection(suggestion.suggestion.words).remove(it)
                        }
                    }
                }
                markerManager.getCollection(SELECTED)?.markers?.forEach {
                    main(dispatchers) {
                        markerManager.getCollection(SELECTED).remove(it)
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    //endregion
}

/** [computeVerticalLines] will compute vertical lines to work with [PolylineManager], it will invert every odd line to avoid diagonal connection.
 * List of [Line]'s will come from [W3WDataSource] with the following logic:
 * 1     3      5
 * |  /  |  /  |
 * 2     4     6 ..
 *
 * @return will return list of [com.what3words.javawrapper.response.Coordinates] with the following logic:
 * 1     4-----5
 * |     |     |
 * 2-----3     6 ..
 */
internal fun List<Line>.computeVerticalLines(): List<com.what3words.javawrapper.response.Coordinates> {
    val computedVerticalLines =
        mutableListOf<com.what3words.javawrapper.response.Coordinates>()

    // all vertical lines
    val verticalLines = mutableListOf<Line>()
    verticalLines.addAll(this.filter { it.start.lng == it.end.lng })

    var t = 0
    while (verticalLines.isNotEmpty()) {
        verticalLines.maxByOrNull { it.start.lat }?.let { topLeftGrid ->
            if (t % 2 == 0) {
                computedVerticalLines.add(topLeftGrid.start)
                computedVerticalLines.add(topLeftGrid.end)
            } else {
                computedVerticalLines.add(topLeftGrid.end)
                computedVerticalLines.add(topLeftGrid.start)
            }
            verticalLines.remove(topLeftGrid)
        }
        t += 1
    }
    return computedVerticalLines
}

/** [computeHorizontalLines] will compute horizontal lines to work with [PolylineManager], it will invert every odd line to avoid diagonal connection.
 * List of [Line]'s will come from [W3WDataSource] with the following logic:
 * A-----B
 *    /
 * C-----D
 *    /
 * E-----F
 *
 * @return will return list of [com.what3words.javawrapper.response.Coordinates] with the following logic:
 * A-----B
 *       |
 * D-----C
 * |
 * E-----F
 */
internal fun List<Line>.computeHorizontalLines(): List<com.what3words.javawrapper.response.Coordinates> {
    val computedHorizontalLines =
        mutableListOf<com.what3words.javawrapper.response.Coordinates>()

    // all horizontal lines
    val horizontalLines = mutableListOf<Line>()
    horizontalLines.addAll(this.filter { it.start.lat == it.end.lat })

    var t = 0
    while (horizontalLines.isNotEmpty()) {
        horizontalLines.minByOrNull { it.start.lng }?.let { topLeftGrid ->
            if (t % 2 == 0) {
                computedHorizontalLines.add(topLeftGrid.start)
                computedHorizontalLines.add(topLeftGrid.end)
            } else {
                computedHorizontalLines.add(topLeftGrid.end)
                computedHorizontalLines.add(topLeftGrid.start)
            }
            horizontalLines.remove(topLeftGrid)
            t += 1
        }
    }
    return computedHorizontalLines
}
