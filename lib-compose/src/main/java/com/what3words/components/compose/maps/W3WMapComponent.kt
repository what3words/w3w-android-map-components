package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

// Close
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    mapManager: W3WMapStateManager,
    mapProvider: W3WMapProvider
) {
    val mapState by mapManager.state

    W3WMapComponent(
        modifier = modifier,
        state = mapState,
        mapProvider = mapProvider
    )
}

// Open
@Composable
fun W3WMapComponent(
    modifier: Modifier = Modifier,
    state: W3WMapState,
    mapProvider: W3WMapProvider
) {

    Box(modifier = modifier) {
        mapProvider.Map(
            modifier = modifier,
            state = state,
            onMapClicked = {
                // Handle map click event
            },
        )

        W3WMapButtons()
    }


}

@Composable
fun W3WMapButtons() {

}
