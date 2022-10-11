package com.what3words.maps.mapbox.compose.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.wrappers.W3WMapBoxWrapper
import com.what3words.maps.mapbox.compose.sample.ui.theme.W3wandroidcomponentsmapsTheme

class UsingMapWrapperActivity : ComponentActivity() {
    private lateinit var w3wMapsWrapper: W3WMapBoxWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            W3wandroidcomponentsmapsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AndroidView(factory = {
                        val view = MapView(it)
                        doWithMapView(view)
                        view
                    })
                }
            }
        }
    }

    private fun doWithMapView(mapView: MapView) {
        val wrapper = What3WordsV3(BuildConfig.W3W_API_KEY, this)
        this.w3wMapsWrapper = W3WMapBoxWrapper(
            this,
            mapView.getMapboxMap(),
            wrapper,
        ).setLanguage("en")

        w3wMapsWrapper.addMarkerAtWords(
            "filled.count.soap",
            W3WMarkerColor.BLUE,
            {
                Log.i(
                    "UsingMapFragmentActivity",
                    "added ${it.words} at ${it.coordinates.lat}, ${it.coordinates.lng}"
                )
                val cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat))
                    .zoom(18.5)
                    .build()
                mapView.getMapboxMap().setCamera(cameraOptions)
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
//            val res = withContext(Dispatchers.IO) {
//                wrapper.autosuggest("filled.count.s").nResults(3).execute()
//            }
//            if (res.isSuccessful) {
//                //in case of autosuggest success add the 3 suggestions to the map.
//                w3wMapsWrapper.addMarkerAtSuggestion(
//                    res.suggestions,
//                    W3WMarkerColor.BLUE,
//                    onSuccess = { list ->
//                        //example adjusting camera to show the 3 results on the map.
//                        val points = mutableListOf<Point>()
//                        list.forEach {
//                            Log.i(
//                                "UsingMapWrapperActivity",
//                                "added ${it.words} at ${it.coordinates.lat}, ${it.coordinates.lng}\""
//                            )
//                            points.add(Point.fromLngLat(it.coordinates.lng, it.coordinates.lat))
//                        }
//                        val options = binding.mapView.getMapboxMap().cameraForCoordinates(
//                            points,
//                            EdgeInsets(100.0, 100.0, 100.0, 100.0)
//                        )
//                        binding.mapView.getMapboxMap().setCamera(options)
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
        mapView.getMapboxMap().addOnMapIdleListener {
            //...

            //needed to draw the 3x3m grid on the map
            this.w3wMapsWrapper.updateMap()
        }

        //REQUIRED
        mapView.getMapboxMap().addOnCameraChangeListener {
            //...

            //needed to draw the 3x3m grid on the map
            this.w3wMapsWrapper.updateMove()
        }

        mapView.getMapboxMap().addOnMapClickListener { latLng ->
            //..

            //example of how to select a 3x3m w3w square using lat/lng
            this.w3wMapsWrapper.selectAtCoordinates(
                latLng.latitude(),
                latLng.longitude()
            )
            true
        }
    }
}
