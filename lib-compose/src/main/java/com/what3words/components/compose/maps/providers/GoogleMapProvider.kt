package com.what3words.components.compose.maps.providers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.what3words.components.compose.maps.W3WMapProvider
import com.what3words.components.compose.maps.W3WMapState
import com.what3words.core.types.geometry.W3WCoordinates

class GoogleMapProvider : W3WMapProvider {
    @Composable
    override fun Map(
        modifier: Modifier,
        state: W3WMapState,
        onMapClicked: ((W3WCoordinates) -> Unit)?
    ) {

        GoogleMap(modifier = modifier,
            onMapClick = { latLng -> onMapClicked?.invoke(W3WCoordinates(latLng.latitude, latLng.longitude)) }
        ) {
            Marker()
        }
    }
}
