package com.what3words.components.compose.maps.buttons.mapswitch

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.map.components.compose.R

/**
 * A button component for switching between different map types.
 * Only supports [W3WMapState.MapType.NORMAL] and [W3WMapState.MapType.SATELLITE].
 *
 * @param modifier The modifier for the button.
 * @param w3wMapType The current map type.
 * @param onMapTypeChange The callback function to be invoked when the map type is changed.
 */
@Composable
fun W3WMapSwitchButton(
    modifier: Modifier = Modifier,
    w3wMapType: W3WMapType = W3WMapType.NORMAL,
    onMapTypeChange: (W3WMapType) -> Unit
) {
    var mapType by remember { mutableStateOf(w3wMapType) }
    IconButton(
        modifier = modifier
            .shadow(elevation = 3.dp, shape = CircleShape)
            .size(50.dp),
        onClick = {
            mapType = when (mapType) {
                W3WMapType.NORMAL -> W3WMapType.SATELLITE
                W3WMapType.SATELLITE -> W3WMapType.NORMAL
                else -> W3WMapType.NORMAL // Default to NORMAL
            }
            onMapTypeChange(mapType)
        }
    ) {
        Image(
            painter = painterResource(
                id = when (mapType) {
                    W3WMapType.NORMAL -> R.drawable.ic_map_satellite
                    W3WMapType.SATELLITE -> R.drawable.ic_map_normal
                    else -> R.drawable.ic_map_satellite
                }
            ),
            contentDescription = null, // TODO: Add content description later
        )
    }
}

@Preview
@Composable
fun MapNormalPreview() {
    W3WMapSwitchButton(w3wMapType = W3WMapType.NORMAL) {}
}

@Preview
@Composable
fun MapTypeSwitcherPreview() {
    W3WMapSwitchButton(w3wMapType = W3WMapType.SATELLITE) {}
}