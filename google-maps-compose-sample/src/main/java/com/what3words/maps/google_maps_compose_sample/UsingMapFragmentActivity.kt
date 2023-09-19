package com.what3words.maps.google_maps_compose_sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.views.W3WGoogleMapFragment
import com.what3words.components.maps.views.W3WMap
import com.what3words.components.maps.views.W3WMapFragment
import com.what3words.maps.google_maps_compose_sample.databinding.ActivityUsingMapFragmentBinding
import com.what3words.maps.google_maps_compose_sample.ui.theme.W3wandroidcomponentsmapsTheme

class UsingMapFragmentActivity : FragmentActivity(), W3WMapFragment.OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            W3wandroidcomponentsmapsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AndroidViewBinding(
                        factory = { inflater, parent, attachToParent ->
                            val view = ActivityUsingMapFragmentBinding.inflate(
                                inflater,
                                parent,
                                attachToParent
                            )
                            val googleMapFragment =
                                view.fragmentContainerView.getFragment<W3WGoogleMapFragment>()
                            googleMapFragment.apiKey(
                                BuildConfig.W3W_API_KEY,
                                this@UsingMapFragmentActivity
                            )
                            view
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onMapReady(map: W3WMap) {
        //set language to get all the 3wa in the desired language (default english)
        map.setLanguage("en")

        //example how to use W3WMap features (check interface for documentation).
        map.addMarkerAtWords(
            "filled.count.soap",
            W3WMarkerColor.BLUE,
            W3WZoomOption.CENTER_AND_ZOOM,
            {
                Log.i(
                    "UsingMapFragmentActivity",
                    "added ${it.words} at ${it.coordinates.lat}, ${it.coordinates.lng}"
                )
            }, {
                Toast.makeText(
                    this,
                    "${it.key}, ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        //if you want to access the google map instance inside W3WGoogleMapFragment do the following
        (map as? W3WGoogleMapFragment.Map)?.googleMap()?.let {
            it.mapType = MAP_TYPE_NORMAL
        }
    }
}