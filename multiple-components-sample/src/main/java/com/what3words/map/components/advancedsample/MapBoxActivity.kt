package com.what3words.map.components.advancedsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.mapbox.maps.Style
import com.what3words.components.maps.views.W3WMap
import com.what3words.components.maps.views.W3WMapboxMapFragment
import com.what3words.components.text.W3WAutoSuggestEditText

class MapBoxActivity : AppCompatActivity(), W3WMapboxMapFragment.OnMapReadyCallback {
    private lateinit var map: W3WMap
    lateinit var search: W3WAutoSuggestEditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapbox)
        search = findViewById(R.id.search)
        val fragment = supportFragmentManager.findFragmentById(R.id.map) as W3WMapboxMapFragment
        search.apiKey(BuildConfig.W3W_API_KEY)
            .returnCoordinates(true)
            .onSuggestionSelected {
                if (it != null) map.selectAtSuggestionWithCoordinates(
                    it
                ) else {
                    map.unselect()
                }
            }
        fragment.apiKey(BuildConfig.W3W_API_KEY, this)
    }

    override fun onMapReady(map: W3WMap) {
        this.map = map
        map.setLanguage("FR")
        map.addMarkerAtWords(
            "filled.count.soap",
            onSuccess = {
                Log.i(
                    "MainActivity",
                    "Marker added at ${it.words}, latitude: ${it.coordinates.lat}, longitude: ${it.coordinates.lng}"
                )
            }, onError = {
                Toast.makeText(
                    this,
                    "${it.key}, ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        map.onSquareSelected(
            onSuccess = { square, selectedByTouch, isMarked ->
                Log.i(
                    "MainActivity",
                    "square selected with words ${square.words}, was it touch? $selectedByTouch, is the square marked? $isMarked"
                )
                search.setSuggestionWithCoordinates(square)
            },
            onError = {
                Toast.makeText(
                    this,
                    "${it.key}, ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        (map as? W3WMapboxMapFragment.Map)?.mapBoxMap()?.let { mapBoxMap ->
            mapBoxMap.loadStyleUri(Style.MAPBOX_STREETS)
        }
    }
}