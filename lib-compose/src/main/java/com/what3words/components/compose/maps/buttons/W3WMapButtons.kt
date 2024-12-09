package com.what3words.components.compose.maps.buttons

import android.graphics.PointF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.buttons.findmylocation.W3WFindMyLocationButton
import com.what3words.components.compose.maps.buttons.mapswitch.W3WMapSwitchButton
import com.what3words.components.compose.maps.buttons.recall.W3WRecallButton
import com.what3words.components.compose.maps.models.W3WMapType

@Composable
fun W3WMapButtons(
    modifier: Modifier = Modifier,
    mapConfig: W3WMapDefaults.MapConfig,
    isLocationEnabled: Boolean,
    isLocationActive: Boolean,
    isRecallButtonVisible: Boolean,
    accuracyDistance: Float,
    rotation: Float,
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
        if (mapConfig.buttonConfig.isRecallButtonEnabled && isRecallButtonVisible) {
            W3WRecallButton(
                onRecallClicked = onRecallClicked,
                onRecallPositionProvided = onRecallButtonPositionProvided,
                rotation = rotation,
            )
        }
        if (mapConfig.buttonConfig.isMyLocationButtonEnabled) {
            W3WFindMyLocationButton(
                accuracyDistance = accuracyDistance.toInt(),
                isLocationEnabled = isLocationEnabled,
                isLocationActive = isLocationActive,
                onMyLocationClicked = onMyLocationClicked
            )
        }
        if (mapConfig.buttonConfig.isMapSwitchButtonEnabled) {
            W3WMapSwitchButton {
                onMapTypeClicked(it)
            }
        }
    }
}