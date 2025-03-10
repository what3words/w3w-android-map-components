package com.what3words.components.compose.maps.models

import android.location.Location
import com.what3words.components.compose.maps.state.LocationStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for providing location data to What3Words components.
 *
 * This interface defines methods to access the current location status and fetch
 * the user's current location. Implementations of this interface should handle permission
 * checks, location service availability, and location updates.
 *
 * The [locationStatus] flow provides real-time updates about the current state of location permissions
 * and services. The [fetchLocation] method can be used to request a one-time location update.
 */
interface W3WLocationSource {
    // hasPermission && isLocationEnabled
    val locationStatus: StateFlow<LocationStatus>

    // Trigger fetch current location
    suspend fun fetchLocation(): Location
}