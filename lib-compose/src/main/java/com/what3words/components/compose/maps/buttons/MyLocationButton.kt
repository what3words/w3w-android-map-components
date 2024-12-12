package com.what3words.components.compose.maps.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.design.library.ui.theme.w3wColorScheme
import kotlinx.coroutines.delay

const val SAFE_ACCURACY_DISTANCE = 100
const val WARNING_ACCURACY_DISTANCE = 200
const val METER = "m"
const val FEET = "ft"
const val VISIBLE_TIME = 2000L

/**
 * A composable function to display a "Find My Location" button.
 *
 * @param modifier The modifier for the button.
 * @param accuracyDistance The accuracy of the current location in meters.
 * @param isLocationEnabled Whether the location permission is enabled or not.
 * @param isLocationActive Whether my location is active or not.
 * @param unitMetrics The unit of accuracy distance, default is "m".
 * @param accuracyMessage The message to display when the accuracy is not good enough, default is "GPS Accuracy (${accuracyDistance}$unitMetrics)".
 * @param locationButtonConfig The configuration for the button, default is [W3WMapButtonsDefault.defaultLocationButtonConfig].
 * @param onMyLocationClicked The callback when the button is clicked.
 */
@Composable
fun MyLocationButton(
    modifier: Modifier = Modifier,
    accuracyDistance: Int,
    isLocationEnabled: Boolean,
    isLocationActive: Boolean,
    unitMetrics: String = METER,
    accuracyMessage: String = "GPS Accuracy (${accuracyDistance}$unitMetrics)",
    locationButtonConfig: W3WMapButtonsDefault.LocationButtonConfig = W3WMapButtonsDefault.defaultLocationButtonConfig(),
    onMyLocationClicked: () -> Unit
) {

    var isShowingAccuracy by remember { mutableStateOf(false) }

    LaunchedEffect(isShowingAccuracy) {
        if (isShowingAccuracy) {
            delay(VISIBLE_TIME)
            isShowingAccuracy = false
        }
    }

    Box(
        modifier = modifier.padding(4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row {
            AnimatedVisibility(
                visible = isShowingAccuracy,
                enter = locationButtonConfig.enterAnimation,
                exit = locationButtonConfig.exitAnimation
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End, // Align Row content to end to control animation direction
                    modifier = Modifier
                        .height(locationButtonConfig.locationButtonSize)
                        .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)) // Rounded on the start side
                        .background(Color.White.copy(alpha = 0.7f)) // TODO: The alpha is too much transparent, original is 0.16
                        .padding(start = 12.dp, end = 4.dp)
                ) {
                    Text(
                        text = accuracyMessage,
                        style = locationButtonConfig.accuracyTextStyle,
                        maxLines = 1 // Prevent text overflow
                    )
                    Spacer(Modifier.size(locationButtonConfig.locationButtonSize / 2))
                }
            }
            Spacer(Modifier.size(locationButtonConfig.locationButtonSize / 2))
        }

        Box {
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .size(locationButtonConfig.locationButtonSize),
                onClick = {
                    onMyLocationClicked()
                    if (isLocationActive) {
                        isShowingAccuracy = true
                    }
                },
                enabled = isLocationEnabled
            ) {
                Icon(
                    imageVector = if (isLocationActive) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    contentDescription = "Location icon",
                    tint = if (isLocationActive) Color(0xFF14B5FF) else Color.Black, // TODO: Define 0xFF14B5FF color name
                    modifier = Modifier.size(locationButtonConfig.locationIconSize)
                )
            }
            if (accuracyDistance >= SAFE_ACCURACY_DISTANCE) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(locationButtonConfig.accuracyIndicatorSize)
                        .clip(CircleShape)
                        .background(
                            color = when {
                                accuracyDistance in SAFE_ACCURACY_DISTANCE until WARNING_ACCURACY_DISTANCE -> MaterialTheme.w3wColorScheme.warning
                                accuracyDistance >= WARNING_ACCURACY_DISTANCE -> MaterialTheme.colorScheme.error
                                else -> return
                            }
                        )
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun A1() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 0,
            isLocationEnabled = false,
            isLocationActive = false,
            unitMetrics = METER
        )
    }

}

@Preview
@Composable
private fun A2() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 0,
            unitMetrics = METER,
            isLocationEnabled = true,
            isLocationActive = true,
        )
    }
}

@Preview
@Composable
private fun A3() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 110,
            unitMetrics = METER,
            isLocationEnabled = true,
            isLocationActive = true,
        )
    }
}

@Preview
@Composable
private fun A4() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 220,
            unitMetrics = METER,
            isLocationEnabled = true,
            isLocationActive = true,
        )
    }
}

@Preview
@Composable
private fun A5() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 220,
            unitMetrics = FEET,
            isLocationEnabled = true,
            isLocationActive = true,
        )
    }
}