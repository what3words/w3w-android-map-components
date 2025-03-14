package com.what3words.components.compose.maps.buttons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.map.components.compose.R

/**
 * A button component for switching between different map types.
 * Only supports [W3WMapType.NORMAL] and [W3WMapType.SATELLITE].
 *
 * @param modifier The modifier for the button.
 * @param layoutConfig The configuration for button layout including padding, size and elevation.
 * @param isButtonEnabled Whether the button is enabled or disabled.
 * @param w3wMapType The current map type.
 * @param contentDescription The content description for the button.
 * @param onMapTypeChange The callback function to be invoked when the map type is changed.
 */
@Composable
internal fun MapSwitchButton(
    modifier: Modifier = Modifier,
    layoutConfig: W3WMapButtonsDefault.MapSwitchButtonLayoutConfig = W3WMapButtonsDefault.defaultMapSwitchButtonLayoutConfig(),
    isButtonEnabled: Boolean,
    w3wMapType: W3WMapType = W3WMapType.NORMAL,
    contentDescription: W3WMapButtonsDefault.ContentDescription = W3WMapButtonsDefault.defaultContentDescription(),
    onMapTypeChange: (W3WMapType) -> Unit
) {
    var mapType by remember { mutableStateOf(w3wMapType) }
    IconButton(
        modifier = modifier
            .padding(layoutConfig.buttonPadding)
            .size(layoutConfig.buttonSize)
            .shadow(
                elevation = if (isButtonEnabled) layoutConfig.elevation else 0.dp,
                shape = CircleShape
            )
            .clip(CircleShape)
            .alpha(if (isButtonEnabled) 1f else layoutConfig.disabledIconOpacity),

        onClick = {
            mapType = when (mapType) {
                W3WMapType.NORMAL -> W3WMapType.SATELLITE
                W3WMapType.SATELLITE -> W3WMapType.NORMAL
                else -> W3WMapType.NORMAL // Default to NORMAL
            }
            onMapTypeChange(mapType)
        },
        enabled = isButtonEnabled
    ) {
        Image(
            painter = painterResource(
                id = when (mapType) {
                    W3WMapType.NORMAL -> R.drawable.ic_map_satellite
                    W3WMapType.SATELLITE -> R.drawable.ic_map_normal
                    else -> R.drawable.ic_map_satellite
                }
            ),
            contentDescription = contentDescription.mapSwitchButtonDescription,
        )
    }
}

@Preview(name = "Enabled")
@Composable
fun NormalMapA1() {
    MapSwitchButton(
        w3wMapType = W3WMapType.NORMAL,
        isButtonEnabled = true,
    ) {}
}

@Preview(name = "Disabled")
@Composable
fun NormalMapA2() {
    MapSwitchButton(
        w3wMapType = W3WMapType.NORMAL,
        isButtonEnabled = false,
    ) {}
}

@Preview(name = "Enabled")
@Composable
fun SatelliteMapA1() {
    MapSwitchButton(
        w3wMapType = W3WMapType.SATELLITE,
        isButtonEnabled = true,
    ) {}
}

@Preview(name = "Disabled")
@Composable
fun SatelliteMapA2() {
    MapSwitchButton(
        w3wMapType = W3WMapType.SATELLITE,
        isButtonEnabled = false,
    ) {}
}