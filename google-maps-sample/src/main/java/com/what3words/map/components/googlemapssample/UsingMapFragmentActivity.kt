package com.what3words.map.components.googlemapssample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.models.W3WZoomOption
import com.what3words.components.maps.views.W3WGoogleMapFragment
import com.what3words.components.maps.views.W3WMap
import com.what3words.map.components.googlemapssample.databinding.ActivityUsingMapFragmentBinding

class UsingMapFragmentActivity : AppCompatActivity(), W3WGoogleMapFragment.OnFragmentReadyCallback {
    private lateinit var binding: ActivityUsingMapFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsingMapFragmentBinding.inflate(layoutInflater)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as W3WGoogleMapFragment

        //W3WGoogleMapFragment needs W3WGoogleMapFragment.OnFragmentReadyCallback to receive the callback when GoogleMap and W3W features are ready to be used
        mapFragment.apiKey(BuildConfig.W3W_API_KEY, this)
        setContentView(binding.root)
    }

    override fun onFragmentReady(fragment: W3WMap) {
        //set language to get all the 3wa in the desired language (default english)
        fragment.setLanguage("en")

        //example how to use W3WMap features (check interface for documentation).
        fragment.addMarkerAtWords(
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
                    this@UsingMapFragmentActivity,
                    "${it.key}, ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        //if you want to access the google map instance inside W3WGoogleMapFragment do the following
        (fragment as? W3WGoogleMapFragment)?.getMap()?.let {
            it.mapType = MAP_TYPE_NORMAL
        }
    }
}