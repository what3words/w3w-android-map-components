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

@Composable
fun W3WMapButtons(
    modifier: Modifier = Modifier,
    buttonConfig: W3WMapDefaults.ButtonConfig,
    buttonState: W3WButtonsState,
    isLocationEnabled: Boolean,
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
        if (buttonConfig.isRecallButtonUsed) {
            RecallButton(
                onRecallClicked = onRecallClicked,
                onRecallButtonPositionProvided = onRecallButtonPositionProvided,
                isVisible = buttonState.isRecallButtonVisible,
                rotation = buttonState.recallRotationDegree,
                arrowColor = buttonState.recallArrowColor,
                backgroundColor = buttonState.recallBackgroundColor
            )
        }
        if (buttonConfig.isMyLocationButtonUsed) {
            MyLocationButton(
                accuracyDistance = buttonState.accuracyDistance.toInt(),
                isLocationEnabled = isLocationEnabled,
                isLocationActive = buttonState.isLocationActive,
                onMyLocationClicked = onMyLocationClicked
            )
        }
        if (buttonConfig.isMapSwitchButtonUsed) {
            MapSwitchButton(
                onMapTypeChange = onMapTypeClicked
            )
        }
    }
}