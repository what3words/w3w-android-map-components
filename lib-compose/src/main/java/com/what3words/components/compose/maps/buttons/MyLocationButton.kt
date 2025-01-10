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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.state.LocationStatus
import com.what3words.map.components.compose.R
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
 * @param isButtonEnabled Whether the location button is enabled or not.
 * @param isDarkMode Whether the map is in dark mode or not.
 * @param locationStatus The status of the location service.
 * @param unitMetrics The unit of accuracy distance, default is "m".
 * @param accuracyMessage The message to display when the accuracy is not good enough, default is "GPS Accuracy (${accuracyDistance}$unitMetrics)".
 * @param locationButtonConfig The configuration for the button, default is [W3WMapButtonsDefault.defaultLocationButtonConfig].
 * @param onMyLocationClicked The callback when the button is clicked.
 */
@Composable
fun MyLocationButton(
    modifier: Modifier = Modifier,
    accuracyDistance: Int,
    isButtonEnabled: Boolean,
    isDarkMode: Boolean,
    locationStatus: LocationStatus,
    unitMetrics: String = METER,
    accuracyMessage: String = "GPS Accuracy (${accuracyDistance}$unitMetrics)",
    locationButtonConfig: W3WMapButtonsDefault.LocationButtonConfig = W3WMapButtonsDefault.defaultLocationButtonConfig(),
    onMyLocationClicked: () -> Unit
) {

    var isShowingAccuracy by remember { mutableStateOf(false) }

    val locationIconVector = when (locationStatus) {
        LocationStatus.ACTIVE -> rememberVectorPainter(Icons.Filled.MyLocation)
        LocationStatus.INACTIVE -> painterResource(id = R.drawable.ic_my_location_outlined)
        LocationStatus.SEARCHING -> rememberVectorPainter(Icons.Filled.LocationSearching)
    }
    // TODO: Define color name
    val locationIconColor = when (locationStatus) {
        LocationStatus.ACTIVE -> Color(0xFF14B5FF)
        else -> Color(0xFFAAABAE)
    }

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
                        .clip(
                            RoundedCornerShape(
                                topStartPercent = 50,
                                bottomStartPercent = 50
                            )
                        ) // Rounded on the start side
                        // TODO: The alpha for light mode is too much transparent, original is 0.16
                        .background(if (isDarkMode) Color.Black.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.7f))
                        .padding(start = 12.dp, end = 4.dp)
                ) {
                    Text(
                        text = accuracyMessage,
                        style = locationButtonConfig.accuracyTextStyle,
                        color = if (isDarkMode) Color.White else Color.Black,
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
                    .background(if (isDarkMode) Color(0xFF111416) else Color.White)
                    .size(locationButtonConfig.locationButtonSize),
                onClick = {
                    onMyLocationClicked()
                    if (locationStatus == LocationStatus.ACTIVE) {
                        isShowingAccuracy = true
                    }
                },
                enabled = isButtonEnabled
            ) {
                Icon(
                    painter = locationIconVector,
                    contentDescription = "Location icon",
                    tint = locationIconColor,
                    modifier = Modifier.size(locationButtonConfig.locationIconSize)
                )
            }
            if (accuracyDistance >= SAFE_ACCURACY_DISTANCE) {
                WarningIndicator(
                    modifier = Modifier.align(Alignment.TopEnd),
                    accuracyDistance = accuracyDistance,
                    isDarkMode = isDarkMode,
                    indicatorSize = locationButtonConfig.accuracyIndicatorSize,
                )
            }
        }
    }
}

@Composable
private fun WarningIndicator(
    modifier: Modifier,
    accuracyDistance: Int,
    isDarkMode: Boolean,
    indicatorSize: Dp,
) {

    val indicatorBackgroundColor = when {
        accuracyDistance in SAFE_ACCURACY_DISTANCE until WARNING_ACCURACY_DISTANCE -> {
            if (isDarkMode) Color(0xFFFFDA88) else Color(0xFFF8C03C)
        }

        accuracyDistance >= WARNING_ACCURACY_DISTANCE -> Color(0xFFF26C50)
        else -> return
    }

    val indicatorIconColor = if (isDarkMode) Color(0xFF382800) else Color(0xFFFFFBFF)

    Box(
        modifier = modifier
            .offset(x = 4.dp, y = (-4).dp)
            .size(indicatorSize)
            .clip(CircleShape)
            .background(color = indicatorBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            modifier = Modifier.padding(4.dp),
            tint = indicatorIconColor
        )
    }
}

@Preview
@Composable
private fun A1() {
    MyLocationButton(
        modifier = Modifier,
        onMyLocationClicked = {},
        accuracyDistance = 0,
        isButtonEnabled = false,
        unitMetrics = METER,
        isDarkMode = false,
        locationStatus = LocationStatus.INACTIVE
    )
}

@Preview
@Composable
private fun A2() {
    MyLocationButton(
        modifier = Modifier,
        onMyLocationClicked = {},
        accuracyDistance = 0,
        isButtonEnabled = false,
        unitMetrics = METER,
        isDarkMode = false,
        locationStatus = LocationStatus.ACTIVE
    )
}

@Preview
@Composable
private fun A3() {
    MyLocationButton(
        modifier = Modifier,
        onMyLocationClicked = {},
        accuracyDistance = 0,
        isButtonEnabled = false,
        unitMetrics = METER,
        isDarkMode = false,
        locationStatus = LocationStatus.SEARCHING
    )
}

@Preview
@Composable
private fun A4() {
    MyLocationButton(
        modifier = Modifier,
        onMyLocationClicked = {},
        accuracyDistance = 120,
        isButtonEnabled = true,
        unitMetrics = METER,
        isDarkMode = false,
        locationStatus = LocationStatus.ACTIVE
    )
}

@Preview
@Composable
private fun A5() {
    MyLocationButton(
        modifier = Modifier,
        onMyLocationClicked = {},
        accuracyDistance = 220,
        isButtonEnabled = true,
        unitMetrics = METER,
        isDarkMode = false,
        locationStatus = LocationStatus.ACTIVE
    )
}

@Preview
@Composable
private fun A6() {
    MyLocationButton(
        modifier = Modifier,
        onMyLocationClicked = {},
        accuracyDistance = 220,
        isButtonEnabled = true,
        unitMetrics = METER,
        isDarkMode = true,
        locationStatus = LocationStatus.ACTIVE
    )
}

@Preview
@Composable
private fun A7() {
    MyLocationButton(
        modifier = Modifier,
        onMyLocationClicked = {},
        accuracyDistance = 120,
        isButtonEnabled = true,
        unitMetrics = METER,
        isDarkMode = true,
        locationStatus = LocationStatus.ACTIVE
    )
}