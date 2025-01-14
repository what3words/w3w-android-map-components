package com.what3words.components.compose.maps.buttons

import android.graphics.PointF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.state.W3WButtonsState

/**
 * W3WMapButtons is a composable that displays the buttons on the map.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param buttonConfig Configuration for the buttons.
 * @param buttonState State of the buttons.
 * @param isLocationEnabled Flag to indicate if location is enabled.
 * @param onMyLocationClicked Callback to be invoked when the my location button is clicked.
 * @param onMapTypeClicked Callback to be invoked when the map type button is clicked.
 * @param onRecallClicked Callback to be invoked when the recall button is clicked.
 * @param onRecallButtonPositionProvided Callback to be invoked when the recall button position is provided.
 */
@Composable
fun W3WMapButtons(
    modifier: Modifier = Modifier,
    buttonConfig: W3WMapDefaults.ButtonConfig,
    buttonState: W3WButtonsState,
    isLocationEnabled: Boolean,
    resourceString: W3WMapButtonsDefault.ResourceString = W3WMapButtonsDefault.defaultResourceString(),
    contentDescription: W3WMapButtonsDefault.ContentDescription = W3WMapButtonsDefault.defaultContentDescription(),
    onMyLocationClicked: (() -> Unit),
    onMapTypeClicked: ((W3WMapType) -> Unit),
    onRecallClicked: (() -> Unit),
    onRecallButtonPositionProvided: ((PointF) -> Unit),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (buttonConfig.isRecallButtonAvailable) {
            RecallButton(
                onRecallClicked = onRecallClicked,
                onRecallButtonPositionProvided = onRecallButtonPositionProvided,
                isVisible = buttonState.isRecallButtonVisible,
                rotation = buttonState.recallRotationDegree,
                arrowColor = buttonState.recallArrowColor,
                backgroundColor = buttonState.recallBackgroundColor,
                contentDescription = contentDescription,
            )
        }
        if (buttonConfig.isMyLocationButtonAvailable) {
            MyLocationButton(
                accuracyDistance = buttonState.accuracyDistance.toInt(),
                isButtonEnabled = isLocationEnabled,
                locationStatus = buttonState.locationStatus,
                onMyLocationClicked = onMyLocationClicked,
                resourceString = resourceString,
                contentDescription = contentDescription,
            )
        }
        if (buttonConfig.isMapSwitchButtonAvailable) {
            MapSwitchButton(
                onMapTypeChange = onMapTypeClicked,
                contentDescription = contentDescription,
            )
        }
    }
}