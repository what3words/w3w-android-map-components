package com.what3words.components.compose.maps

import androidx.compose.runtime.MutableState
import androidx.core.util.Consumer
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.language.W3WRFC5646Language

class W3WMapStateManager(
    val state: MutableState<W3WMapState>,
    private val textDataSource: W3WTextDataSource
) {
    internal var language: W3WRFC5646Language = W3WRFC5646Language.EN_GB

    fun addCoordinates(
        coordinates: W3WCoordinates,
        markerColor: W3WMarkerColor,
        onSuccess: Consumer<W3WAddress>?,
        onError: Consumer<W3WError>?
    ) {
        textDataSource.convertTo3wa(
            coordinates, language
        )

        // Update to state
//        state.value
    }
}