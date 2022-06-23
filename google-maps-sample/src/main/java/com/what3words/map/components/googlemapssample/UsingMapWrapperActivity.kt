package com.what3words.map.components.googlemapssample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.components.maps.models.W3WApiDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.wrappers.W3WGoogleMapsWrapper
import com.what3words.javawrapper.request.Coordinates
import com.what3words.map.components.googlemapssample.databinding.ActivityUsingMapWrapperBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsingMapWrapperActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var w3wMapsWrapper: W3WGoogleMapsWrapper
    private lateinit var binding: ActivityUsingMapWrapperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsingMapWrapperBinding.inflate(layoutInflater)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setContentView(binding.root)
    }

    override fun onMapReady(p0: GoogleMap) {
        val wrapper = What3WordsV3(BuildConfig.W3W_API_KEY, this)
        this.w3wMapsWrapper = W3WGoogleMapsWrapper(
            this,
            p0,
            W3WApiDataSource(wrapper, this),
        ).setLanguage("en")

        w3wMapsWrapper.addMarkerAtWords(
            "filled.count.soap",
            W3WMarkerColor.BLUE,
            {
                Log.i(
                    "UsingMapFragmentActivity",
                    "added ${it.words} at ${it.coordinates.lat}, ${it.coordinates.lng}"
                )
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(it.coordinates.lat, it.coordinates.lng))
                    .zoom(19f)
                    .build()
                p0.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }, {
                Toast.makeText(
                    this@UsingMapWrapperActivity,
                    "${it.key}, ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
        //example how to add a autosuggest results from our w3w wrapper to the map
        CoroutineScope(Dispatchers.Main).launch {
            val res =
                withContext(Dispatchers.IO) {
                    wrapper.autosuggest("filled.count.s").nResults(3).execute()
                }
            if (res.isSuccessful) {
                //in case of autosuggest success add the 3 suggestions to the map.
                w3wMapsWrapper.addMarkerAtSuggestion(
                    res.suggestions,
                    W3WMarkerColor.BLUE,
                    onSuccess = { list ->
                        val latLngBounds = LatLngBounds.Builder()
                        list.forEach {
                            Log.i(
                                "UsingMapWrapperActivity",
                                "added ${it.words} at ${it.coordinates.lat}, ${it.coordinates.lng}\""
                            )
                            latLngBounds.include(LatLng(it.coordinates.lat, it.coordinates.lng))
                        }
                        p0.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                latLngBounds.build(),
                                100
                            )
                        )
                    },
                    onError = {
                        Toast.makeText(
                            this@UsingMapWrapperActivity,
                            "${it.key}, ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } else {
                Toast.makeText(
                    this@UsingMapWrapperActivity,
                    "${res.error.key}, ${res.error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        //click even on existing w3w added markers on the map.
        w3wMapsWrapper.onMarkerClicked {
            Log.i("UsingMapWrapperActivity", "clicked: ${it.words}")
        }

        //REQUIRED
        p0.setOnCameraIdleListener {
            //...

            //needed to draw the 3x3m grid on the map
            this.w3wMapsWrapper.updateMap()
        }

        //REQUIRED
        p0.setOnCameraMoveListener {
            //...

            //needed to draw the 3x3m grid on the map
            this.w3wMapsWrapper.updateMove()
        }

        p0.setOnMapClickListener { latLng ->
            //..

            //example of how to select a 3x3m w3w square using lat/lng
            this.w3wMapsWrapper.selectAtCoordinates(
                latLng.latitude,
                latLng.longitude
            )
        }
    }
}
