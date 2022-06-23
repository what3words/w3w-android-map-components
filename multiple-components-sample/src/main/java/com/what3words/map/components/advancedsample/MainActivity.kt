package com.what3words.map.components.advancedsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.what3words.components.maps.views.W3WGoogleMapFragment
import com.what3words.components.maps.views.W3WMap
import com.what3words.components.text.W3WAutoSuggestEditText

class MainActivity : AppCompatActivity(), W3WGoogleMapFragment.OnFragmentReadyCallback {
    private lateinit var fragment: W3WMap
    lateinit var search: W3WAutoSuggestEditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        search = findViewById(R.id.search)
        val map = supportFragmentManager.findFragmentById(R.id.map) as W3WGoogleMapFragment
        search.apiKey(BuildConfig.W3W_API_KEY)
            .returnCoordinates(true)
            .onSuggestionSelected {
                if (it != null) fragment.selectAtSuggestionWithCoordinates(
                    it
                ) else {
                    fragment.unselect()
                }
            }
        map.apiKey(BuildConfig.W3W_API_KEY, this)
    }

    override fun onFragmentReady(fragment: W3WMap) {
        this.fragment = fragment
        fragment.addMarkerAtWords("filled.count.soap")
        fragment.onSquareSelected { square, selectedByTouch, isMarked ->
            Log.i(
                "MainActivity",
                "square selected with words ${square.words}, was it touch? $selectedByTouch, is the square marked? $isMarked"
            )
            search.setSuggestionWithCoordinates(square)
        }
    }
}