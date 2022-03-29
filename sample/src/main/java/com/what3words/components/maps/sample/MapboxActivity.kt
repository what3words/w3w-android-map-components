package com.what3words.components.maps.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.models.W3WApiDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.sample.databinding.ActivityMapboxBinding
import com.what3words.components.maps.sample.databinding.ActivityMapboxBinding.inflate
import com.what3words.components.maps.wrappers.W3WGoogleMapsWrapper
import com.what3words.components.maps.wrappers.W3WMapBoxWrapper
import com.what3words.javawrapper.request.Coordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MapboxActivity : AppCompatActivity() {
    private lateinit var w3wMapsWrapper: W3WMapBoxWrapper
    private lateinit var binding: ActivityMapboxBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflate(layoutInflater)
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
        setContentView(binding.root)

        val wrapper = What3WordsV3(BuildConfig.W3W_API_KEY, this@MapboxActivity)
        this.w3wMapsWrapper = W3WMapBoxWrapper(
            this,
            binding.mapView,
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

        binding.mapView.getMapboxMap().addOnMapIdleListener {
            Log.i("TEST", "addOnMapIdleListener")
            this.w3wMapsWrapper.updateMap()
        }

        binding.mapView.getMapboxMap().addOnCameraChangeListener {
            Log.i("TEST", "addOnCameraChangeListener")
            this.w3wMapsWrapper.updateMove()
        }


        binding.mapView.getMapboxMap().addOnMapClickListener { latLng ->
            //OTHER FUNCTIONS
            Log.i("TEST", "CLICK DETECTED ${latLng.latitude()}, ${latLng.longitude()}")
            val location = Coordinates(
                latLng.latitude(),
                latLng.longitude()
            )
            this.w3wMapsWrapper.selectAtCoordinates(location)
            true
        }
    }

//    @SuppressLint("PotentialBehaviorOverride")
//    override fun onMapReady(p0: GoogleMap) {
//        val wrapper = What3WordsV3(BuildConfig.W3W_API_KEY, this@MapboxActivity)
//        this.w3wMapsWrapper = W3WGoogleMapsWrapper(
//            this,
//            p0,
//            W3WApiDataSource(wrapper),
//        ).setLanguage("en")
//
//
//    }
}