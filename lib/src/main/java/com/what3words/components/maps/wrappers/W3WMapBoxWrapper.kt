package com.what3words.components.maps.wrappers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Image
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
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
import com.what3words.javawrapper.response.Suggestion
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import com.what3words.map.components.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import androidx.core.util.Consumer
import com.google.android.gms.maps.GoogleMap
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.models.W3WApiDataSource

/**
 * [W3WMapBoxWrapper] a wrapper to add what3words support to [MapboxMap].
 **
 * @param context app context.
 * @param w3wDataSource source of what3words data can be API or SDK.
 * @param mapView the [MapboxMap] view that [W3WMapBoxWrapper] should apply changes to.
 * @param dispatchers for custom dispatcher provider using [DefaultDispatcherProvider] by default.
 */
class W3WMapBoxWrapper(
    private val context: Context,
    private val mapView: MapboxMap,
    private val w3wDataSource: W3WDataSource,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : W3WMapWrapper {
    private var isDirty: Boolean = false
    private var w3wMapManager: W3WMapManager = W3WMapManager(w3wDataSource, this, dispatchers)

    /**
     * [W3WMapBoxWrapper] a wrapper to add what3words support to [MapboxMap].
     **
     * @param context app context.
     * @param mapView the [MapboxMap] view that [W3WMapBoxWrapper] should apply changes to.
     * @param wrapper what3words android API wrapper to be used as source of data (i.e: c2c, c3wa, grid-section, autosuggest...).
     */
    constructor(
        context: Context,
        mapView: MapboxMap,
        wrapper: What3WordsV3
    ) : this(context, mapView, W3WApiDataSource(wrapper, context))

    companion object {
        const val GRID_LAYER = "GRID_LAYER"
        const val GRID_SOURCE = "GRID_SOURCE"
        const val SELECTED_SOURCE = "SELECTED_SOURCE"
        const val SELECTED_LAYER = "SELECTED_LAYER"
        const val SELECTED_ZOOMED_SOURCE = "SELECTED_ZOOMED_SOURCE"
        const val SELECTED_ZOOMED_LAYER = "SELECTED_ZOOMED_LAYER"
        const val ZOOM_SWITCH_LEVEL = 18f
        const val DEFAULT_BOUNDS_SCALE = 8f
        const val CIRCLE_ID_PREFIX = "CIRCLE_%s"
        const val PIN_ID_PREFIX = "PIN_%s"
        const val SQUARE_IMAGE_ID_PREFIX = "SQUARE_IMAGE_%s"
    }

    private var onMarkerClickedCallback: Consumer<SuggestionWithCoordinates>? = null
    private var lastScaledBounds: BoundingBox? = null
    private var isGridVisible: Boolean = false
    private var searchJob: Job? = null
    private var shouldDrawGrid: Boolean = true

    // TODO: Rethink this logic, MapBox v10 really complicated this, and the suggestion from someone at mapbox was this: https://github.com/mapbox/mapbox-maps-android/issues/1245#issuecomment-1082884148
    init {
        var latch: CountDownLatch
        var callbackResult: Boolean
        mapView.addOnMapClickListener { point ->
            callbackResult = false
            latch = CountDownLatch(1)
            mapView.executeOnRenderThread {
                mapView.queryRenderedFeatures(
                    RenderedQueryGeometry(mapView.pixelForCoordinate(point)),
                    RenderedQueryOptions(
                        w3wMapManager.suggestionsCached.map { it.suggestion.words },
                        null
                    )
                ) {
                    if (it.isValue && it.value!!.isNotEmpty()) {
                        it.value!![0].source.let { source ->
                            w3wMapManager.suggestionsCached.firstOrNull { it.suggestion.words == source }
                                ?.let {
                                    isDirty = true
                                    w3wMapManager.selectExistingMarker(it.suggestion)
                                    onMarkerClickedCallback?.accept(it.suggestion)
                                    callbackResult = true
                                }
                        }
                    }
                    latch.countDown()
                }
            }
            latch.await()
            return@addOnMapClickListener callbackResult
        }
    }


    /** Set the language of [SuggestionWithCoordinates.words] that onSuccess callbacks should return.
     *
     * @param language a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).
     */
    override fun setLanguage(language: String): W3WMapBoxWrapper {
        w3wMapManager.language = language
        return this
    }

    /** Enable grid overlay over map with all 3mx3m squares on the visible map bounds.
     *
     * @param isEnabled enable or disable grid, enabled by default.
     */
    override fun gridEnabled(isEnabled: Boolean): W3WMapBoxWrapper {
        shouldDrawGrid = isEnabled
        return this
    }

    /** A callback for when an existing marker on the map is clicked.
     *
     * @param callback it will be invoked when an existing marker on the map is clicked by the user.
     */
    override fun onMarkerClicked(callback: Consumer<SuggestionWithCoordinates>): W3WMapBoxWrapper {
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
        isDirty = true
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
        isDirty = true
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
        isDirty = true
        w3wMapManager.removeWords(suggestion.words)
    }

    /** Remove [Suggestion]s from the map.
     *
     * @param listSuggestions the list of [Suggestion]s to remove.
     */
    override fun removeMarkerAtSuggestion(listSuggestions: List<Suggestion>) {
        isDirty = true
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
        isDirty = true
        w3wMapManager.selectWords(
            suggestion.words,
            onSuccess,
            onError
        )
    }
    //endregion

    //region add/remove by coordinates

    /** Add [Coordinates] to the map. This method will add a marker/square to the map based on each of the [Coordinates] provided latitude and longitude.
     *
     * @param coordinates [Coordinates] to be added.
     * @param markerColor is the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun addMarkerAtCoordinates(
        coordinates: Coordinates,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        isDirty = true
        w3wMapManager.addCoordinates(
            coordinates,
            markerColor,
            onSuccess,
            onError
        )
    }

    /** Add a list of [Coordinates] to the map. This method will add multiple markers/squares to the map based on the latitude and longitude of each [Coordinates] on the list.
     *
     * @param listCoordinates list of [Coordinates]s to be added.
     * @param markerColor is the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun addMarkerAtCoordinates(
        listCoordinates: List<Coordinates>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        isDirty = true
        w3wMapManager.addCoordinates(
            listCoordinates,
            markerColor,
            onSuccess,
            onError
        )
    }

    /** Set [Coordinates] as selected marker on the map, it can only have one selected marker at the time.
     *
     * @param coordinates [Coordinates] to be added.
     * @param onSuccess the success callback will return a [SuggestionWithCoordinates] that will have all the [Suggestion] info plus [Coordinates].
     * @param onError the error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun selectAtCoordinates(
        coordinates: Coordinates,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        isDirty = true
        w3wMapManager.selectCoordinates(
            coordinates,
            onSuccess,
            onError
        )
    }

    override fun findMarkerByCoordinates(coordinates: Coordinates): SuggestionWithCoordinates? {
        return w3wMapManager.squareContains(coordinates.lat, coordinates.lng)?.suggestion
    }

    /** Remove [Coordinates] from the map.
     *
     * @param coordinates the [Coordinates] to be removed.
     */
    override fun removeMarkerAtCoordinates(coordinates: Coordinates) {
        isDirty = true
        w3wMapManager.removeCoordinates(coordinates)
    }

    /** Remove a list of [Coordinates] from the map.
     *
     * @param listCoordinates the list of [Coordinates] to remove.
     */
    override fun removeMarkerAtCoordinates(listCoordinates: List<Coordinates>) {
        isDirty = true
        w3wMapManager.removeCoordinates(listCoordinates)
    }
//endregion

    //region add/remove by words
    /** Add a three word address to the map. This method will add a marker/square to the map if [words] are a valid three word address, e.g., filled.count.soap. If it's not a valid three word address, [onError] will be called returning [APIResponse.What3WordsError.BAD_WORDS].
     *
     * @param words three word address to be added.
     * @param markerColor the [W3WMarkerColor] for the [Coordinates] added.
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message.
     */
    override fun addMarkerAtWords(
        words: String,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<SuggestionWithCoordinates>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        isDirty = true
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
     * @param onSuccess an success callback will return a [SuggestionWithCoordinates] with all the what3words info needed for those [Coordinates].
     * @param onError an error callback, will return a [APIResponse.What3WordsError] that will have the error type and message. If one item on the list fails to be added, this process will be fully reverted, only adds if all succeed.
     */
    override fun addMarkerAtWords(
        listWords: List<String>,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<List<SuggestionWithCoordinates>>?,
        onError: Consumer<APIResponse.What3WordsError>?
    ) {
        isDirty = true
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
        isDirty = true
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
        isDirty = true
        w3wMapManager.removeWords(words)
    }

    /** Remove a list of three word addresses from the map.
     *
     * @param listWords the list of three word addresses to remove.
     */
    override fun removeMarkerAtWords(listWords: List<String>) {
        isDirty = true
        w3wMapManager.removeWords(listWords)
    }

    /** Remove all markers from the map. */
    override fun removeAllMarkers() {
        isDirty = true
        w3wMapManager.clearList()
    }

    /** Get all added [SuggestionWithCoordinates] from the map.
     *
     * @return list of [SuggestionWithCoordinates] with all items added to the map.
     */
    override fun getAllMarkers(): List<SuggestionWithCoordinates> {
        return w3wMapManager.getList()
    }

    override fun unselect() {
        runBlocking(dispatchers.io()) {
            isDirty = true
            w3wMapManager.unselect()
        }
    }

    /** This method should be called on [MapboxMap.addOnMapIdleListener].
     * This will allow to refresh the grid bounds on camera idle.
     */
    override fun updateMap() {
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
        if (mapView.cameraState.zoom < ZOOM_SWITCH_LEVEL && isGridVisible) {
            redrawMapLayers(true)
            isGridVisible = false
            return
        }
        if (mapView.cameraState.zoom >= ZOOM_SWITCH_LEVEL && !isGridVisible) {
            redrawMapLayers(true)
            return
        }
    }

    /** Scale bounds to [scale] times to get the grid larger than the visible [MapboxMap.coordinateBoundsForCamera] with [MapboxMap.cameraState]. This will increase performance and keep the grid visible for longer when moving camera.
     *
     * @param scale the factor scale to be applied to [MapboxMap.coordinateBoundsForCamera], e.g: 8f (8 times larger) or 0.5f (to cut by half)
     *
     */
    private fun scaleBounds(
        scale: Float = DEFAULT_BOUNDS_SCALE
    ): BoundingBox {
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

        return BoundingBox(
            Coordinates(
                finalSWLat,
                finalSWLng
            ),
            Coordinates(finalNELat, finalNELng)
        )
    }

    private fun redrawMapLayers(
        shouldCancelPreviousJob: Boolean = true,
        scale: Float = W3WGoogleMapsWrapper.DEFAULT_BOUNDS_SCALE
    ) {
        isDirty = false
        if (mapView.cameraState.zoom < ZOOM_SWITCH_LEVEL && !isGridVisible) {
            clearMarkers()
            drawMarkersOnMap()
            return
        }
        if (mapView.cameraState.zoom < ZOOM_SWITCH_LEVEL && isGridVisible) {
            isGridVisible = false
            clearGridAndZoomedInMarkers()
            drawMarkersOnMap()
            return
        }
        if (!shouldDrawGrid) return
        clearMarkers()
        isGridVisible = true
        lastScaledBounds =
            scaleBounds(scale)
        if (shouldCancelPreviousJob) searchJob?.cancel()
        searchJob = CoroutineScope(dispatchers.io()).launch {
            when (val grid = w3wDataSource.getGeoJsonGrid(lastScaledBounds!!)) {
                is Either.Left -> {
                }
                is Either.Right -> {
                    drawLinesOnMap(grid.b)
                    drawZoomedMarkers()
                }
            }
        }
    }

    private fun drawZoomedMarkers() {
        w3wMapManager.suggestionsCached.forEach {
            drawFilledZoomedMarker(it)
        }
        if (w3wMapManager.selectedSuggestion != null) {
            drawOutlineZoomedMarker(w3wMapManager.selectedSuggestion!!)
        }
    }

    /** [drawOutlineZoomedMarker] will be responsible for the highlighting the selected square by adding four lines to [MapboxMap.style] using [LineLayer].*/
    private fun drawOutlineZoomedMarker(suggestion: SuggestionWithCoordinates) {
        val listLines = LineString.fromLngLats(
            listOf(
                Point.fromLngLat(
                    suggestion.square.southwest.lng,
                    suggestion.square.northeast.lat
                ),
                Point.fromLngLat(
                    suggestion.square.northeast.lng,
                    suggestion.square.northeast.lat
                ),
                Point.fromLngLat(
                    suggestion.square.northeast.lng,
                    suggestion.square.southwest.lat
                ),
                Point.fromLngLat(
                    suggestion.square.southwest.lng,
                    suggestion.square.southwest.lat
                ),
                Point.fromLngLat(
                    suggestion.square.southwest.lng,
                    suggestion.square.northeast.lat
                ),
                Point.fromLngLat(
                    suggestion.square.northeast.lng,
                    suggestion.square.northeast.lat
                )
            )
        )
        mapView.getStyle { style ->
            val test = listLines.toJson()
            if (style.styleSourceExists(SELECTED_ZOOMED_SOURCE)) {
                style.getSourceAs<GeoJsonSource>(SELECTED_ZOOMED_SOURCE)?.data(test)
            } else {
                style.addSource(
                    geoJsonSource(SELECTED_ZOOMED_SOURCE) {
                        data(test)
                    }
                )
            }
            if (!style.styleLayerExists(SELECTED_ZOOMED_LAYER)) {
                style.addLayer(
                    lineLayer(SELECTED_ZOOMED_LAYER, SELECTED_ZOOMED_SOURCE) {
                        lineColor(
                            if (mapView.getStyle()?.styleURI == Style.SATELLITE) {
                                context.getColor(R.color.grid_selected_sat)
                            } else {
                                context.getColor(R.color.grid_selected_normal)
                            }
                        )
                        lineWidth(getGridSelectedBorderSizeBasedOnZoomLevel())
                    }
                )
            }
        }
    }

    /** [drawZoomedMarkers] will be responsible for the drawing all square images to four coordinates (top right, then clockwise) by adding them [MapboxMap.style] using [RasterLayer], still looking for a better option for this, this is the way I found it online.*/
    private fun drawFilledZoomedMarker(suggestion: SuggestionWithCoordinatesAndStyle) {
        main(dispatchers) {
            val id = String.format(SQUARE_IMAGE_ID_PREFIX, suggestion.suggestion.words)
            bitmapFromDrawableRes(context, suggestion.markerColor.toGridFill())?.let { image ->
                mapView.getStyle { style ->
                    if (!style.styleSourceExists(id)) {
                        style.addSource(
                            imageSource(id) {
                                this.coordinates(
                                    listOf(
                                        listOf(
                                            suggestion.suggestion.square.southwest.lng,
                                            suggestion.suggestion.square.northeast.lat
                                        ),
                                        listOf(
                                            suggestion.suggestion.square.northeast.lng,
                                            suggestion.suggestion.square.northeast.lat

                                        ),
                                        listOf(
                                            suggestion.suggestion.square.northeast.lng,
                                            suggestion.suggestion.square.southwest.lat

                                        ),
                                        listOf(
                                            suggestion.suggestion.square.southwest.lng,
                                            suggestion.suggestion.square.southwest.lat
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
            if (style.styleLayerExists(SELECTED_ZOOMED_LAYER)) {
                style.removeStyleLayer(SELECTED_ZOOMED_LAYER)
                style.removeStyleSource(SELECTED_ZOOMED_SOURCE)
            }
            w3wMapManager.suggestionsCached.forEach {
                val id = String.format(SQUARE_IMAGE_ID_PREFIX, it.suggestion.words)
                if (style.styleLayerExists(id)) {
                    style.removeStyleLayer(id)
                    style.removeStyleSource(id)
                }
            }
        }
    }

    /** [drawMarkersOnMap] holds the logic to decide which kind of pin should be added to [MapboxMap.style] based on selection and added points.*/
    private fun drawMarkersOnMap() {
        w3wMapManager.suggestionsCached.forEach {
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

    /** [drawSelectedPin] will be responsible for the drawing of the selected but not added w3w pins (i.e [R.drawable.ic_marker_pin_white] for [Style.SATELLITE]) by adding them to [MapboxMap.style] using a [SymbolLayer].*/
    private fun drawSelectedPin(data: SuggestionWithCoordinates) {
        val image = if (mapView.getStyle()?.styleURI == Style.SATELLITE) {
            R.drawable.ic_marker_pin_white
        } else {
            R.drawable.ic_marker_pin_dark_blue
        }
        bitmapFromDrawableRes(
            context,
            image
        )?.let { bitmap ->
            mapView.getStyle { style ->
                style.addImage(image.toString(), bitmap)
                if (style.styleSourceExists(SELECTED_SOURCE)) {
                    style.getSourceAs<GeoJsonSource>(SELECTED_SOURCE)
                        ?.geometry(Point.fromLngLat(data.coordinates.lng, data.coordinates.lat))
                } else {
                    style.addSource(
                        geoJsonSource(SELECTED_SOURCE) {
                            geometry(Point.fromLngLat(data.coordinates.lng, data.coordinates.lat))
                        }
                    )
                }
                if (!style.styleLayerExists(SELECTED_LAYER)) {
                    style.addLayer(
                        symbolLayer(SELECTED_LAYER, SELECTED_SOURCE) {
                            iconImage(image.toString())
                            iconAnchor(IconAnchor.BOTTOM)
                            iconAllowOverlap(true)
                        }
                    )
                }
            }
        }
    }

    /** [drawPin] will be responsible for the drawing of the selected and added w3w pins (i.e [R.drawable.ic_marker_pin_red]) by adding them to [MapboxMap.style] using a [SymbolLayer].*/
    private fun drawPin(data: SuggestionWithCoordinatesAndStyle) {
        data.markerColor.toPin().let { markerFillColor ->
            bitmapFromDrawableRes(context, markerFillColor)?.let { bitmap ->
                mapView.getStyle { style ->
                    style.addImage(
                        String.format(PIN_ID_PREFIX, data.markerColor.toString()),
                        bitmap
                    )
                    if (!style.styleSourceExists(data.suggestion.words)) {
                        style.addSource(
                            geoJsonSource(data.suggestion.words) {
                                geometry(
                                    Point.fromLngLat(
                                        data.suggestion.coordinates.lng,
                                        data.suggestion.coordinates.lat
                                    )
                                )
                            }
                        )
                    }
                    if (!style.styleLayerExists(data.suggestion.words)) {
                        style.addLayer(
                            symbolLayer(data.suggestion.words, data.suggestion.words) {
                                iconImage(String.format(PIN_ID_PREFIX, data.markerColor.toString()))
                                iconAnchor(IconAnchor.BOTTOM)
                                iconAllowOverlap(true)
                            }
                        )
                    }
                }
            }
        }
    }

    /** [drawCircle] will be responsible for the drawing of the w3w circle pins (i.e [R.drawable.ic_marker_circle_red]) by adding them to [MapboxMap.style] using a [SymbolLayer].*/
    private fun drawCircle(data: SuggestionWithCoordinatesAndStyle) {
        data.markerColor.toCircle().let { markerFillColor ->
            bitmapFromDrawableRes(context, markerFillColor)?.let { bitmap ->
                mapView.getStyle { style ->
                    style.addImage(
                        String.format(CIRCLE_ID_PREFIX, data.markerColor.toString()),
                        bitmap
                    )
                    if (!style.styleSourceExists(data.suggestion.words)) {
                        style.addSource(
                            geoJsonSource(data.suggestion.words) {
                                geometry(
                                    Point.fromLngLat(
                                        data.suggestion.coordinates.lng,
                                        data.suggestion.coordinates.lat
                                    )
                                )
                            }
                        )
                    }
                    if (!style.styleLayerExists(data.suggestion.words)) {
                        style.addLayer(
                            symbolLayer(data.suggestion.words, data.suggestion.words) {
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
    private fun getGridColorBasedOnZoomLevel(zoom: Double = mapView.cameraState.zoom): Int {
        return when {
            zoom < ZOOM_SWITCH_LEVEL -> context.getColor(R.color.grid_mapbox)
            zoom >= ZOOM_SWITCH_LEVEL && zoom < 19 -> context.getColor(R.color.grid_mapbox)
            zoom >= 19 && zoom < 20 -> context.getColor(R.color.grid_mapbox)
            else -> context.getColor(R.color.grid_mapbox)
        }
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [MapboxMap.cameraState] zoom. */
    private fun getGridBorderSizeBasedOnZoomLevel(zoom: Double = mapView.cameraState.zoom): Double {
        return when {
            zoom < ZOOM_SWITCH_LEVEL -> context.resources.getDimension(R.dimen.grid_width_mapbox_gone)
                .toDouble()
            zoom >= ZOOM_SWITCH_LEVEL && zoom < 19 -> context.resources.getDimension(R.dimen.grid_width_mapbox_far)
                .toDouble()
            zoom >= 19 && zoom < 20 -> context.resources.getDimension(R.dimen.grid_width_mapbox_middle)
                .toDouble()
            else -> context.resources.getDimension(R.dimen.grid_width_close).toDouble()
        }
    }

    /** [getGridColorBasedOnZoomLevel] will get the grid color based on [MapboxMap.cameraState] zoom. */
    private fun getGridSelectedBorderSizeBasedOnZoomLevel(zoom: Double = mapView.cameraState.zoom): Double {
        return when {
            zoom < ZOOM_SWITCH_LEVEL -> context.resources.getDimension(R.dimen.grid_width_mapbox_gone)
                .toDouble()
            zoom >= ZOOM_SWITCH_LEVEL && zoom < 19 -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_far)
                .toDouble()
            zoom >= 19 && zoom < 20 -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_middle)
                .toDouble()
            else -> context.resources.getDimension(R.dimen.grid_selected_width_mapbox_close)
                .toDouble()
        }
    }

    /** [clearMarkers] will clear the [SELECTED_LAYER] and all [W3WMapManager.suggestionsCached] added [SymbolLayer]'s from [MapboxMap.style].*/
    private fun clearMarkers() {
        mapView.getStyle { style ->
            if (style.styleLayerExists(SELECTED_LAYER)) {
                style.removeStyleLayer(SELECTED_LAYER)
                style.removeStyleSource(SELECTED_SOURCE)
            }
            w3wMapManager.suggestionsCached.forEach {
                if (style.styleLayerExists(it.suggestion.words)) {
                    style.removeStyleLayer(it.suggestion.words)
                    style.removeStyleSource(it.suggestion.words)
                }
            }
        }
    }
}
