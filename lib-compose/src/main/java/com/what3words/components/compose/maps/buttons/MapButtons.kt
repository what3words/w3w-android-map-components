package com.what3words.components.compose.maps.buttons

import android.graphics.PointF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.W3WMapDefaults
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault.defaultButtonLayoutConfig
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault.defaultLocationButtonColor
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.state.W3WButtonsState
import kotlin.math.ceil

/**
 * W3WMapButtons is a composable that organizes and displays a set of customizable buttons on a map.
 * These buttons provide various functionalities such as showing the user's current location, switching map types,
 * and recalling specific map positions.
 *
 * @param modifier [Modifier] to be applied to the layout of the buttons container.
 * @param buttonState [W3WButtonsState] representing the current state of the buttons including visibility and status.
 * @param mapType [W3WMapType] indicating the current type of map being displayed.
 * @param buttonConfig [W3WMapDefaults.ButtonConfig] that provides configuration settings determining the availability of buttons.
 * @param layoutConfig [W3WMapButtonsDefault.ButtonLayoutConfig] describes the layout configuration for the buttons,
 *                      with a default configuration if not specified.
 * @param resourceString [W3WMapButtonsDefault.ResourceString] for providing text resources used in button content descriptions.
 * @param contentDescription [W3WMapButtonsDefault.ContentDescription] for accessibility, detailing the actions of each button.
 * @param locationButtonColor [W3WMapButtonsDefault.LocationButtonColor] defines the color styling for the location button.
 * @param recallButtonColor [W3WMapButtonsDefault.RecallButtonColor] defines the color styling for the recall button.
 * @param onMyLocationClicked Callback executed when the user clicks the "My Location" button.
 * @param onMapTypeClicked Callback executed when the user clicks the "Map Type" switch button, including the new [W3WMapType].
 * @param onRecallClicked Callback executed when the user clicks the "Recall" button to recall a specific map location.
 * @param onRecallButtonPositionProvided Callback that provides the position of the recall button in [PointF] coordinates.
 */
@Composable
internal fun MapButtons(
    modifier: Modifier = Modifier,
    buttonState: W3WButtonsState,
    mapType: W3WMapType,
    buttonConfig: W3WMapDefaults.ButtonConfig,
    layoutConfig: W3WMapButtonsDefault.ButtonLayoutConfig = defaultButtonLayoutConfig(),
    resourceString: W3WMapButtonsDefault.ResourceString = W3WMapButtonsDefault.defaultResourceString(),
    contentDescription: W3WMapButtonsDefault.ContentDescription = W3WMapButtonsDefault.defaultContentDescription(),
    locationButtonColor: W3WMapButtonsDefault.LocationButtonColor = defaultLocationButtonColor(),
    recallButtonColor: W3WMapButtonsDefault.RecallButtonColor,
    onMyLocationClicked: (() -> Unit),
    onMapTypeClicked: ((W3WMapType) -> Unit),
    onRecallClicked: (() -> Unit),
    onRecallButtonPositionProvided: ((PointF) -> Unit),
) {
    // Calculate total height based on button configurations
    val totalButtonsHeight = remember(layoutConfig) {
        val recallHeight = if (buttonConfig.isRecallFeatureEnabled) {
            val config = layoutConfig.recallButtonLayoutConfig
            config.buttonSize + config.buttonPadding.calculateTopPadding() + config.buttonPadding.calculateBottomPadding()
        } else 0.dp

        val locationHeight = if (buttonConfig.isMyLocationFeatureEnabled) {
            val config = layoutConfig.locationButtonLayoutConfig
            config.buttonSize + config.buttonPadding.calculateTopPadding() + config.buttonPadding.calculateBottomPadding()
        } else 0.dp

        val mapSwitchHeight = if (buttonConfig.isMapSwitchFeatureEnabled) {
            val config = layoutConfig.mapSwitchButtonLayoutConfig
            config.buttonSize + config.buttonPadding.calculateTopPadding() + config.buttonPadding.calculateBottomPadding()
        } else 0.dp

        // Calculate spacing: (n-1) * spacing where n is the number of visible buttons
        val visibleButtonCount = listOf(
            buttonConfig.isRecallFeatureEnabled,
            buttonConfig.isMyLocationFeatureEnabled,
            buttonConfig.isMapSwitchFeatureEnabled
        ).count { it }

        val totalSpacing = if (visibleButtonCount > 1)
            ((visibleButtonCount - 1) * layoutConfig.buttonSpacing.value).dp else 0.dp

        recallHeight + locationHeight + mapSwitchHeight + totalSpacing
    }

    Column(
        modifier = modifier.height(totalButtonsHeight),
        verticalArrangement = Arrangement.spacedBy(
            space = layoutConfig.buttonSpacing,
            alignment = Alignment.Bottom
        ),
        horizontalAlignment = Alignment.End
    ) {
        if (buttonConfig.isRecallFeatureEnabled) {
            RecallButton(
                layoutConfig = layoutConfig.recallButtonLayoutConfig,
                onRecallClicked = onRecallClicked,
                onRecallButtonPositionProvided = onRecallButtonPositionProvided,
                rotation = ceil(buttonState.recallRotationDegree),
                recallButtonColor = recallButtonColor,
                contentDescription = contentDescription,
                isVisible = buttonState.isRecallButtonVisible,
                isButtonEnabled = buttonState.isRecallButtonEnabled,
                isCameraMoving = buttonState.isCameraMoving,
                selectedPosition = buttonState.selectedScreenLocation ?: PointF(),
            )
        }
        if (buttonConfig.isMyLocationFeatureEnabled && buttonState.isMyLocationButtonVisible) {
            MyLocationButton(
                layoutConfig = layoutConfig.locationButtonLayoutConfig,
                accuracyDistance = buttonState.accuracyDistance.toInt(),
                isButtonEnabled = buttonState.isMyLocationButtonEnabled,
                locationStatus = buttonState.locationStatus,
                colors = locationButtonColor,
                onMyLocationClicked = onMyLocationClicked,
                resourceString = resourceString,
                contentDescription = contentDescription,
            )
        }
        if (buttonConfig.isMapSwitchFeatureEnabled && buttonState.isMapSwitchButtonVisible) {
            MapSwitchButton(
                layoutConfig = layoutConfig.mapSwitchButtonLayoutConfig,
                w3wMapType = mapType,
                onMapTypeChange = onMapTypeClicked,
                contentDescription = contentDescription,
                isButtonEnabled = buttonState.isMapSwitchButtonEnabled
            )
        }
    }
}