package com.what3words.components.maps.views

import androidx.fragment.app.Fragment
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.core.datasource.text.W3WTextDataSource

/** [W3WMapFragment] A generic fragment interface to apply to all different map providers Fragments */
interface W3WMapFragment {
    interface OnMapReadyCallback {
        /**
         * This callback will be invoked when [W3WMap] is rendered and ready to use. Since different maps have different initialization methods, we have created a generic ready map callback.
         *
         * @return [W3WMap] that developer can manipulate, add/select/remove markers and squares.
         */
        fun onMapReady(map: W3WMap)
    }

    interface MapEventsCallback {
        /**
         * This callback will be invoked when the map moves.
         * We handle some internal logic inside the fragment when the map moves,
         * but the developer might want to do some extra work on top of this.
         */
        fun onMove()
        /**
         * This callback will be invoked when the map stops moving.
         * We handle some internal logic inside the fragment when the map stops moving,
         * but the developer might want to do some extra work on top of this.
         */
        fun onIdle()
    }

    /**
     * A [W3WMapFragment] initializer that will use our API or SDK as a data source.
     *
     * @param dataSource the [W3WTextDataSource] implementation to do all the internal API/SDK calls needed to get what3words addresses information.
     * @param callback this [OnMapReadyCallback] will be invoked when the [W3WMap] inside [W3WMapFragment] is rendered and ready to be used.
     * @param mapEventsCallback [MapEventsCallback] multiple [W3WMap] map events that allow the developer to add some extra needed logic on their apps.
     */
    fun initialize(
        dataSource: W3WTextDataSource,
        callback: OnMapReadyCallback,
        mapEventsCallback: MapEventsCallback? = null
    )

    /**
     * The base [Fragment] class agnostic to what map provider it contains.
     */
    val fragment: Fragment

    /**
     * Will move the position of the compass. Compass is managed differently for multiple map providers.
     * This generic function will move the compass in all map providers supported by what3words.
     *
     * @param leftMargin the left margin of the compass to the map view.
     * @param topMargin the top margin of the compass to the map view.
     * @param rightMargin the right margin of the compass to the map view.
     * @param bottomMargin the bottom margin of the compass to the map view.
     */
    fun moveCompassBy(leftMargin: Int, topMargin: Int, rightMargin: Int, bottomMargin: Int)
}