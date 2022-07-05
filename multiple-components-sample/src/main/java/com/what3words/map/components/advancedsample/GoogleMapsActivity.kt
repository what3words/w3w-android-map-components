package com.what3words.map.components.advancedsample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.what3words.components.maps.views.W3WGoogleMapFragment
import com.what3words.components.maps.views.W3WMap
import com.what3words.components.text.W3WAutoSuggestEditText

class GoogleMapsActivity : AppCompatActivity(), W3WGoogleMapFragment.OnMapReadyCallback {
    private lateinit var map: W3WMap
    lateinit var search: W3WAutoSuggestEditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_googlemaps)
        search = findViewById(R.id.search)
        val fragment = supportFragmentManager.findFragmentById(R.id.map) as W3WGoogleMapFragment
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

        (map as? W3WGoogleMapFragment.Map)?.googleMap()?.let { googleMap ->
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        }
    }
}