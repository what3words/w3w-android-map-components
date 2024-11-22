package com.what3words.components.compose.maps.models

import android.location.Location
import kotlinx.coroutines.flow.StateFlow

interface W3WLocationSource {
    // hasPermission && isLocationEnabled
    val isActive: StateFlow<Boolean>

    // Trigger fetch current location
    suspend fun fetchLocation(): Location
}