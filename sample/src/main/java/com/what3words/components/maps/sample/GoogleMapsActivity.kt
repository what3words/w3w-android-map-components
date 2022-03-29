package com.what3words.components.maps.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.models.W3WApiDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.sample.databinding.ActivityGooglemapsBinding
import com.what3words.components.maps.sample.databinding.ActivityGooglemapsBinding.inflate
import com.what3words.components.maps.wrappers.W3WGoogleMapsWrapper
import com.what3words.javawrapper.request.Coordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class GoogleMapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var w3wMapsWrapper: W3WGoogleMapsWrapper
    private lateinit var binding: ActivityGooglemapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflate(layoutInflater)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setContentView(binding.root)
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(p0: GoogleMap) {
        val wrapper = What3WordsV3(BuildConfig.W3W_API_KEY, this@GoogleMapsActivity)
        this.w3wMapsWrapper = W3WGoogleMapsWrapper(
            this,
            p0,
            W3WApiDataSource(wrapper, this),
        ).setLanguage("en")

        runBlocking(Dispatchers.IO) {
            var res = wrapper.autosuggest("filled.count.s").nResults(3).execute()
            if (res.isSuccessful) {
                w3wMapsWrapper.addMarkerAtSuggestion(
                    res.suggestions,
                    W3WMarkerColor.BLUE,
                    onSuccess = { list ->
                        list.forEach {
                            Log.i("TEST", "added filled.count.s: ${it.words}")
                        }
                    },
                    onError = {

                    }
                )
            }
            res = wrapper.autosuggest("shadow.vocab.l").nResults(3).execute()
            if (res.isSuccessful) {
                w3wMapsWrapper.addMarkerAtSuggestion(
                    res.suggestions,
                    W3WMarkerColor.YELLOW,
                    onSuccess = { list ->
                        list.forEach {
                            Log.i("TEST", "added shadow.vocab.l: ${it.words}")
                        }
                    },
                    onError = {

                    }
                )
            }
            res = wrapper.autosuggest("index.home.r").nResults(3).execute()
            if (res.isSuccessful) {
                w3wMapsWrapper.addMarkerAtSuggestion(
                    res.suggestions,
                    W3WMarkerColor.RED,
                    onSuccess = { list ->
                        list.forEach {
                            Log.i("TEST", "added index.home.r: ${it.words}")
                        }
                    },
                    onError = {

                    }
                )
            }
        }

        w3wMapsWrapper.onMarkerClicked {
            Log.i("TEST", "clicked: ${it.words}")
        }

        p0.setOnCameraIdleListener {
            this.w3wMapsWrapper.updateMap()
        }

        p0.setOnCameraMoveListener {
            this.w3wMapsWrapper.updateMove()
        }


        p0.setOnMapClickListener { latLng ->
            //OTHER FUNCTIONS
            val location = Coordinates(
                latLng.latitude,
                latLng.longitude
            )
            this.w3wMapsWrapper.selectAtCoordinates(location)
        }
    }
}