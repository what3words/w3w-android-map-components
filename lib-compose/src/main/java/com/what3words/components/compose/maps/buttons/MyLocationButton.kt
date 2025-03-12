package com.what3words.components.compose.maps.buttons

import android.R.attr.onClick
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.state.LocationStatus
import com.what3words.components.compose.maps.utils.ImmediateAnimatedVisibility
import com.what3words.design.library.ui.theme.W3WTheme
import com.what3words.map.components.compose.R
import kotlinx.coroutines.delay

const val SAFE_ACCURACY_DISTANCE = 100
const val WARNING_ACCURACY_DISTANCE = 200
const val METER = "m"
const val FEET = "ft"
const val ACCURACY_VISIBLE_TIME = 3000L

private enum class SearchIconState {
    ICON_ONE,
    ICON_TWO,
}

/**
 * A composable function to display a "Find My Location" button.
 *
 * @param modifier The modifier for the button.
 * @param accuracyDistance The accuracy of the current location in meters.
 * @param isButtonEnabled Whether the location button is enabled or not.
 * @param locationStatus The status of the location service.
 * @param unitMetrics The unit of accuracy distance, default is "m".
 * @param layoutConfig Configuration for the button's layout, including positioning, size, and other layout properties. Defaults to [W3WMapButtonsDefault.defaultLocationButtonConfig].
 * @param colors Defines the color scheme of the location button, such as background and icon colors. Defaults to [W3WMapButtonsDefault.defaultLocationButtonColor].
 * @param resourceString The resource string for the button, default is [W3WMapButtonsDefault.defaultResourceString].
 * @param contentDescription The content description for the button, default is [W3WMapButtonsDefault.defaultContentDescription].
 * @param onMyLocationClicked The callback when the button is clicked.
 */
@Composable
internal fun MyLocationButton(
    modifier: Modifier = Modifier,
    accuracyDistance: Int,
    isButtonEnabled: Boolean,
    locationStatus: LocationStatus,
    unitMetrics: String = METER,
    layoutConfig: W3WMapButtonsDefault.LocationButtonLayoutConfig = W3WMapButtonsDefault.defaultLocationButtonConfig(),
    colors: W3WMapButtonsDefault.LocationButtonColor = W3WMapButtonsDefault.defaultLocationButtonColor(),
    resourceString: W3WMapButtonsDefault.ResourceString = W3WMapButtonsDefault.defaultResourceString(),
    contentDescription: W3WMapButtonsDefault.ContentDescription = W3WMapButtonsDefault.defaultContentDescription(),
    onMyLocationClicked: () -> Unit
) {

    var isShowingAccuracy by remember { mutableStateOf(false) }
    var searchingIconState by remember { mutableStateOf(SearchIconState.ICON_ONE) }

    val locationSearchingIcon = rememberVectorPainter(Icons.Filled.LocationSearching)
    val myLocationIcon = rememberVectorPainter(Icons.Filled.MyLocation)
    val locationOutlinedIcon = painterResource(id = R.drawable.ic_my_location_outlined)

    val locationIconVector by remember(locationStatus, searchingIconState) {
        derivedStateOf {
            when (locationStatus) {
                LocationStatus.SEARCHING -> {
                    when (searchingIconState) {
                        SearchIconState.ICON_ONE -> locationSearchingIcon
                        SearchIconState.ICON_TWO -> myLocationIcon
                    }
                }

                LocationStatus.ACTIVE -> myLocationIcon
                LocationStatus.INACTIVE -> locationOutlinedIcon
            }
        }
    }

    val locationIconColor by remember(locationStatus) {
        derivedStateOf {
            when (locationStatus) {
                LocationStatus.ACTIVE -> colors.locationIconColorActive
                else -> colors.locationIconColorInactive
            }
        }
    }

    LaunchedEffect(locationStatus) {
        if (locationStatus == LocationStatus.SEARCHING) {
            while (true) {
                // Switch searching icon
                searchingIconState = SearchIconState.ICON_ONE
                delay(400L)
                searchingIconState = SearchIconState.ICON_TWO
                delay(800L)
            }
        }
    }

    LaunchedEffect(isShowingAccuracy) {
        if (isShowingAccuracy) {
            delay(ACCURACY_VISIBLE_TIME)
            isShowingAccuracy = false
        }
    }

    Box(
        modifier = modifier.padding(layoutConfig.padding),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row {
            AnimatedVisibility(
                visible = isShowingAccuracy,
                enter = layoutConfig.accuracyEnterAnimation,
                exit = layoutConfig.accuracyExitAnimation
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End, // Align Row content to end to control animation direction
                    modifier = Modifier
                        .height(layoutConfig.locationButtonSize)
                        .clip(
                            RoundedCornerShape(
                                topStartPercent = 50,
                                bottomStartPercent = 50,
                            )
                        ) // Rounded on the start side
                        // TODO: The alpha for light mode is too much transparent, original is 0.16
                        .background(colors.accuracyBackgroundColor)
                        .padding(start = 12.dp, end = 4.dp)
                ) {
                    Text(
                        text = resourceString.accuracyMessage.format(accuracyDistance, unitMetrics),
                        style = layoutConfig.accuracyTextStyle,
                        color = colors.accuracyTextColor,
                        maxLines = 1 // Prevent text overflow
                    )
                    Spacer(Modifier.size(layoutConfig.locationButtonSize / 2))
                }
            }
            Spacer(Modifier.size(layoutConfig.locationButtonSize / 2))
        }

        ImmediateAnimatedVisibility(
            enter = layoutConfig.buttonVisibleAnimation
        ) {
            Box {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .shadow(
                            elevation = if (isButtonEnabled) layoutConfig.elevation else 0.dp,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .alpha(if (isButtonEnabled) 1f else layoutConfig.disabledIconOpacity)
                        .background(colors.locationBackgroundColor)
                        .size(layoutConfig.locationButtonSize),
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
                        modifier = Modifier.size(layoutConfig.locationIconSize)
                    )
                }
                if (accuracyDistance >= SAFE_ACCURACY_DISTANCE) {
                    WarningIndicator(
                        modifier = Modifier.align(Alignment.TopEnd),
                        accuracyDistance = accuracyDistance,
                        buttonConfig = layoutConfig,
                        colors = colors,
                        contentDescription = contentDescription
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningIndicator(
    modifier: Modifier,
    accuracyDistance: Int,
    buttonConfig: W3WMapButtonsDefault.LocationButtonLayoutConfig,
    colors: W3WMapButtonsDefault.LocationButtonColor,
    contentDescription: W3WMapButtonsDefault.ContentDescription,
) {

    val (indicatorBackgroundColor, indicatorIconColor) = when {
        accuracyDistance >= WARNING_ACCURACY_DISTANCE -> colors.warningHighBackgroundColor to colors.warningHighIconColor
        accuracyDistance >= SAFE_ACCURACY_DISTANCE -> colors.warningLowBackgroundColor to colors.warningLowIconColor
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