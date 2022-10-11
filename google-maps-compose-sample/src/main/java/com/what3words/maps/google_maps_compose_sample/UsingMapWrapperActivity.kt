package com.what3words.maps.google_maps_compose_sample

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.models.W3WApiDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.views.W3WGoogleMapFragment
import com.what3words.components.maps.wrappers.W3WGoogleMapsWrapper
import com.what3words.maps.google_maps_compose_sample.databinding.ActivityUsingMapFragmentBinding
import com.what3words.maps.google_maps_compose_sample.databinding.ActivityUsingMapWrapperBinding
import com.what3words.maps.google_maps_compose_sample.ui.theme.W3wandroidcomponentsmapsTheme

class UsingMapWrapperActivity : FragmentActivity(), OnMapReadyCallback {
    private lateinit var w3wMapsWrapper: W3WGoogleMapsWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            W3wandroidcomponentsmapsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AndroidViewBinding(factory = { inflater, parent, attachToParent ->
                        val view =
                            ActivityUsingMapWrapperBinding.inflate(inflater, parent, attachToParent)
                        val supportMapFragment =
                            view.fragmentContainerView.getFragment<SupportMapFragment>()
                        supportMapFragment.getMapAsync(
                            this@UsingMapWrapperActivity
                        )
                        view
                    })
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        val wrapper = What3WordsV3(BuildConfig.W3W_API_KEY, this)
        this.w3wMapsWrapper = W3WGoogleMapsWrapper(
            this,
            map,
            W3WApiDataSource(wrapper, this),
        ).setLanguage("en")
//        example grid working with night style json for google maps generate here: https://mapstyle.withgoogle.com/
//        p0.setMapStyle(
//            MapStyleOptions.loadRawResourceStyle(
//                this, R.raw.night_style
//            )
//        )
//
//        w3wMapsWrapper.setGridColor(GridColor.LIGHT)

        w3wMapsWrapper.addMarkerAtWords(
            "index.home.raft",
            W3WMarkerColor.BLUE,
            {
                Log.i(
                    "UsingMapFragmentActivity",
                    "added ${it.words} at ${it.coordinates.lat}, ${it.coordinates.lng}"
                )
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(it.coordinates.lat, it.coordinates.lng))
                    .zoom(16f)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }, {
                Toast.makeText(
                    this@UsingMapWrapperActivity,
                    "${it.key}, ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

//        example how to add a autosuggest results from our w3w wrapper to the map
//        CoroutineScope(Dispatchers.Main).launch {
//            val res =
//                withContext(Dispatchers.IO) {
//                    wrapper.autosuggest("filled.count.s").nResults(3).execute()
//                }
//            if (res.isSuccessful) {
//                //in case of autosuggest success add the 3 suggestions to the map.
//                w3wMapsWrapper.addMarkerAtSuggestion(
//                    res.suggestions,
//                    W3WMarkerColor.BLUE,
//                    onSuccess = { list ->
//                        val latLngBounds = LatLngBounds.Builder()
//                        list.forEach {
//                            Log.i(
//                                "UsingMapWrapperActivity",
//                                "added ${it.words} at ${it.coordinates.lat}, ${it.coordinates.lng}\""
//                            )
//                            latLngBounds.include(LatLng(it.coordinates.lat, it.coordinates.lng))
//                        }
//                        p0.animateCamera(
//                            CameraUpdateFactory.newLatLngBounds(
//                                latLngBounds.build(),
//                                100
//                            )
//                        )
//                    },
//                    onError = {
//                        Toast.makeText(
//                            this@UsingMapWrapperActivity,
//                            "${it.key}, ${it.message}",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                )
//            } else {
//                Toast.makeText(
//                    this@UsingMapWrapperActivity,
//                    "${res.error.key}, ${res.error.message}",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }

        //click even on existing w3w added markers on the map.
        w3wMapsWrapper.onMarkerClicked {
            Log.i("UsingMapWrapperActivity", "clicked: ${it.words}")
        }

        //REQUIRED
        map.setOnCameraIdleListener {
            //...
            Log.i("BUG", "setOnCameraIdleListener")
            //needed to draw the 3x3m grid on the map
            this.w3wMapsWrapper.updateMap()
        }

        //REQUIRED
        map.setOnCameraMoveListener {
            //...
            Log.i("BUG", "setOnCameraMoveListener")
            //needed to draw the 3x3m grid on the map
            this.w3wMapsWrapper.updateMove()
        }

        map.setOnMapClickListener { latLng ->
            //..

            //example of how to select a 3x3m w3w square using lat/lng
            this.w3wMapsWrapper.selectAtCoordinates(
                latLng.latitude,
                latLng.longitude
            )
        }

    }
}