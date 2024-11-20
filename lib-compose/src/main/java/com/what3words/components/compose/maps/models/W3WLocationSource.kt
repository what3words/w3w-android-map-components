package com.what3words.components.compose.maps.models

import android.location.Location

interface W3WLocationSource {
    fun fetchLocation(
        onLocationFetched: (Location) -> Unit,
        onError: (Exception) -> Unit,
    )
}