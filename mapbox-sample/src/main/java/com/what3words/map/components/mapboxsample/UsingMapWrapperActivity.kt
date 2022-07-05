package com.what3words.map.components.mapboxsample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.androidwrapper.helpers.DefaultDispatcherProvider
import com.what3words.components.maps.models.W3WApiDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.wrappers.W3WMapBoxWrapper
import com.what3words.javawrapper.request.Coordinates
import com.what3words.map.components.mapboxsample.databinding.ActivityUsingMapWrapperBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsingMapWrapperActivity : AppCompatActivity() {
    private lateinit var w3wMapsWrapper: W3WMapBoxWrapper
    private lateinit var binding: ActivityUsingMapWrapperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsingMapWrapperBinding.inflate(layoutInflater)
        binding.mapView.getMapboxMap().loadStyleUri(Style.OUTDOORS)
        setContentView(binding.root)

        val wrapper = What3WordsV3(BuildConfig.W3W_API_KEY, this)
        this.w3wMapsWrapper = W3WMapBoxWrapper(
            this,
            binding.mapView.getMapboxMap(),
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
                binding.mapView.getMapboxMap().setCamera(cameraOptions)
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
        binding.mapView.getMapboxMap().addOnMapIdleListener {
            //...

            //needed to draw the 3x3m grid on the map
            this.w3wMapsWrapper.updateMap()
        }

        //REQUIRED
        binding.mapView.getMapboxMap().addOnCameraChangeListener {
            //...

            //needed to draw the 3x3m grid on the map
            this.w3wMapsWrapper.updateMove()
        }

        binding.mapView.getMapboxMap().addOnMapClickListener { latLng ->
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
