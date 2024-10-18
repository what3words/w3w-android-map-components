package com.what3words.components.compose.maps.providers.googlemap

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import com.what3words.components.compose.maps.mapper.toMapType
import com.what3words.components.compose.maps.models.W3WMapState
import com.what3words.components.compose.maps.providers.W3WMapProvider

class W3WGoogleMapProvider : W3WMapProvider {
    companion object {
        const val DEFAULT_ZOOM_SWITCH_LEVEL = 19f
    }


    @OptIn(MapsComposeExperimentalApi::class)
    @Composable
    override fun Map(
        modifier: Modifier,
        contentPadding: PaddingValues,
        state: W3WMapState,
        onMapUpdate: () -> Unit,
        onMapMove: () -> Unit,
    ) {
        val cameraPositionState = rememberCameraPositionState {
            state.zoom?.let {
                position = CameraPosition.fromLatLngZoom(LatLng(0.0,0.0), it)
            }
        }

        val uiSettings by remember {
            mutableStateOf(
                MapUiSettings(
                    zoomControlsEnabled = false
                )
            )
        }

        val mapProperties by remember {
            mutableStateOf(
                MapProperties(
                    mapType = state.mapType.toMapType(),
                )
            )
        }

        //TODO:
        // cameraPositionState: animate camera
        // uiSetting: turn off some buttons control
        // mapProperties: switch mapType


        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            contentPadding = contentPadding,
            uiSettings = uiSettings,
            properties = mapProperties
        ) {

            MapEffect { map ->
                map.setOnCameraIdleListener {
                    onMapUpdate.invoke()
                }

                map.setOnCameraMoveListener {
                    onMapMove.invoke()
                }
            }

            W3WGoogleMapDrawer(state = state)
        }
    }
}

