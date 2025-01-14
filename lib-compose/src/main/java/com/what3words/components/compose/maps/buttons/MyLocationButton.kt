package com.what3words.components.compose.maps.buttons

import android.content.res.Configuration
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.state.LocationStatus
import com.what3words.design.library.ui.theme.W3WTheme
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
 * @param locationStatus The status of the location service.
 * @param unitMetrics The unit of accuracy distance, default is "m".
 * @param buttonConfig The configuration for the button, default is [W3WMapButtonsDefault.defaultLocationButtonConfig].
 * @param resourceString The resource string for the button, default is [W3WMapButtonsDefault.defaultResourceString].
 * @param contentDescription The content description for the button, default is [W3WMapButtonsDefault.defaultContentDescription].
 * @param onMyLocationClicked The callback when the button is clicked.
 */
@Composable
fun MyLocationButton(
    modifier: Modifier = Modifier,
    accuracyDistance: Int,
    isButtonEnabled: Boolean,
    locationStatus: LocationStatus,
    unitMetrics: String = METER,
    buttonConfig: W3WMapButtonsDefault.LocationButtonConfig = W3WMapButtonsDefault.defaultLocationButtonConfig(),
    resourceString: W3WMapButtonsDefault.ResourceString = W3WMapButtonsDefault.defaultResourceString(),
    contentDescription: W3WMapButtonsDefault.ContentDescription = W3WMapButtonsDefault.defaultContentDescription(),
    onMyLocationClicked: () -> Unit
) {

    var isShowingAccuracy by remember { mutableStateOf(false) }

    val locationIconVector = when (locationStatus) {
        LocationStatus.ACTIVE -> rememberVectorPainter(Icons.Filled.MyLocation)
        LocationStatus.INACTIVE -> painterResource(id = R.drawable.ic_my_location_outlined)
        LocationStatus.SEARCHING -> rememberVectorPainter(Icons.Filled.LocationSearching)
    }

    val locationIconColor = when (locationStatus) {
        LocationStatus.ACTIVE -> buttonConfig.locationButtonColor.locationIconColorActive
        else -> buttonConfig.locationButtonColor.locationIconColorInactive
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
                enter = buttonConfig.enterAnimation,
                exit = buttonConfig.exitAnimation
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End, // Align Row content to end to control animation direction
                    modifier = Modifier
                        .height(buttonConfig.locationButtonSize)
                        .clip(
                            RoundedCornerShape(
                                topStartPercent = 50,
                                bottomStartPercent = 50,
                            )
                        ) // Rounded on the start side
                        // TODO: The alpha for light mode is too much transparent, original is 0.16
                        .background(buttonConfig.locationButtonColor.accuracyBackgroundColor)
                        .padding(start = 12.dp, end = 4.dp)
                ) {
                    Text(
                        text = resourceString.accuracyMessage.format(accuracyDistance, unitMetrics),
                        style = buttonConfig.accuracyTextStyle,
                        color = buttonConfig.locationButtonColor.accuracyTextColor,
                        maxLines = 1 // Prevent text overflow
                    )
                    Spacer(Modifier.size(buttonConfig.locationButtonSize / 2))
                }
            }
            Spacer(Modifier.size(buttonConfig.locationButtonSize / 2))
        }

        Box {
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(buttonConfig.locationButtonColor.locationBackgroundColor)
                    .size(buttonConfig.locationButtonSize),
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
                    contentDescription = contentDescription.locationButtonDescription,
                    tint = locationIconColor,
                    modifier = Modifier.size(buttonConfig.locationIconSize)
                )
            }
            if (accuracyDistance >= SAFE_ACCURACY_DISTANCE) {
                WarningIndicator(
                    modifier = Modifier.align(Alignment.TopEnd),
                    accuracyDistance = accuracyDistance,
                    buttonConfig = buttonConfig,
                    contentDescription = contentDescription
                )
            }
        }
    }
}

@Composable
private fun WarningIndicator(
    modifier: Modifier,
    accuracyDistance: Int,
    buttonConfig: W3WMapButtonsDefault.LocationButtonConfig,
    contentDescription: W3WMapButtonsDefault.ContentDescription,
) {

    val indicatorBackgroundColor = when {
        accuracyDistance in SAFE_ACCURACY_DISTANCE until WARNING_ACCURACY_DISTANCE -> {
            buttonConfig.locationButtonColor.warningLowBackgroundColor
        }

        accuracyDistance >= WARNING_ACCURACY_DISTANCE -> {
            buttonConfig.locationButtonColor.warningHighBackgroundColor
        }

        else -> return
    }

    val indicatorIconColor = when {
        accuracyDistance in SAFE_ACCURACY_DISTANCE until WARNING_ACCURACY_DISTANCE -> {
            buttonConfig.locationButtonColor.warningLowIconColor
        }

        accuracyDistance >= WARNING_ACCURACY_DISTANCE -> {
            buttonConfig.locationButtonColor.warningHighIconColor
        }

        else -> return
    }

    Box(
        modifier = modifier
            .offset(x = 4.dp, y = (-4).dp)
            .size(buttonConfig.accuracyIndicatorSize)
            .clip(CircleShape)
            .background(color = indicatorBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = contentDescription.warningIconDescription,
            modifier = Modifier.padding(4.dp),
            tint = indicatorIconColor
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun A1() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 0,
            isButtonEnabled = true,
            unitMetrics = METER,
            locationStatus = LocationStatus.SEARCHING
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun A2() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 0,
            isButtonEnabled = true,
            unitMetrics = METER,
            locationStatus = LocationStatus.INACTIVE
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun A3() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 70,
            isButtonEnabled = true,
            unitMetrics = METER,
            locationStatus = LocationStatus.ACTIVE
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun A4() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 120,
            isButtonEnabled = true,
            unitMetrics = METER,
            locationStatus = LocationStatus.ACTIVE
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun A5() {
    W3WTheme {
        MyLocationButton(
            modifier = Modifier,
            onMyLocationClicked = {},
            accuracyDistance = 220,
            isButtonEnabled = true,
            unitMetrics = METER,
            locationStatus = LocationStatus.ACTIVE
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun A6() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        W3WTheme {
            MyLocationButton(
                modifier = Modifier,
                onMyLocationClicked = {},
                accuracyDistance = 220,
                isButtonEnabled = true,
                unitMetrics = METER,
                locationStatus = LocationStatus.ACTIVE
            )
        }
    }
}