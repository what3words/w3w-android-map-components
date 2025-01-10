package com.what3words.components.compose.maps.models

import android.location.Location
import com.what3words.components.compose.maps.state.LocationStatus
import kotlinx.coroutines.flow.StateFlow

interface W3WLocationSource {
    // hasPermission && isLocationEnabled
    val locationStatus: StateFlow<LocationStatus>

    // Trigger fetch current location
    suspend fun fetchLocation(): Location
}