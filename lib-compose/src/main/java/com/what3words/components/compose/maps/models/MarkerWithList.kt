package com.what3words.components.compose.maps.models

import androidx.compose.runtime.Immutable

/**
 * A data class that represents a marker with its corresponding list name.
 */
@Immutable
data class MarkerWithList(val listName: String, val marker: W3WMarker)