package com.what3words.components.compose.maps.buttons

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault.defaultRecallButtonColor
import com.what3words.components.compose.maps.buttons.W3WMapButtonsDefault.defaultRecallButtonLayoutConfig
import com.what3words.map.components.compose.R

/**
 * A composable function to display a recall button.
 *
 * @param modifier The modifier for the button.
 * @param rotation The rotation angle for the button in degrees.
 * @param layoutConfig The configuration for the button's layout, defining its size, positioning, and other properties. The default layout configuration is [defaultRecallButtonLayoutConfig].
 * @param recallButtonColor Defines the color scheme of the button, including background and text colors. The default color scheme is [defaultRecallButtonColor].
 * @param contentDescription The content description for the button.
 * @param onRecallClicked The callback when the button is clicked.
 * @param onRecallButtonPositionProvided The callback providing the button's position as a PointF.
 */
@Composable
internal fun RecallButton(
    modifier: Modifier = Modifier,
    rotation: Float,
    layoutConfig: W3WMapButtonsDefault.RecallButtonLayoutConfig = defaultRecallButtonLayoutConfig(),
    recallButtonColor: W3WMapButtonsDefault.RecallButtonColor = defaultRecallButtonColor(),
    contentDescription: W3WMapButtonsDefault.ContentDescription = W3WMapButtonsDefault.defaultContentDescription(),
    onRecallClicked: () -> Unit,
    onRecallButtonPositionProvided: (PointF) -> Unit,
) {

    var position: Offset by remember { mutableStateOf(Offset.Zero) }
    val positionCallback = rememberUpdatedState(onRecallButtonPositionProvided)

    IconButton(
        onClick = { onRecallClicked() },
        modifier = modifier
            .onGloballyPositioned { coordinate ->
                // Only trigger one time when the button is initialized
                // The coordinate is affected by the rotation
                if (position == Offset.Zero) {
                    position = coordinate.positionInWindow()
                    val point = PointF(position.x, position.y)
                    positionCallback.value(point)
                }
            }
            .padding(layoutConfig.buttonPadding)
            .graphicsLayer {
                rotationZ = rotation
            }
            .shadow(elevation = 3.dp, shape = CircleShape)
            .size(layoutConfig.buttonSize)
            .background(recallButtonColor.recallBackgroundColor)
    ) {
        Icon(
            modifier = Modifier
                .size(layoutConfig.imageSize)
                .padding(layoutConfig.imagePadding),
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = contentDescription.recallButtonDescription,
            tint = recallButtonColor.recallArrowColor
        )
    }
}

@Preview
@Composable
private fun A1() {
    RecallButton(
        modifier = Modifier,
        rotation = 0f,
        onRecallClicked = {},
        onRecallButtonPositionProvided = {}
    )
}

@Preview
@Composable
private fun A2() {
    RecallButton(
        modifier = Modifier,
        rotation = 45f,
        onRecallClicked = {},
        onRecallButtonPositionProvided = {}
    )
}

@Preview
@Composable
private fun A3() {
    RecallButton(
        modifier = Modifier,
        rotation = 90f,
        onRecallClicked = {},
        onRecallButtonPositionProvided = {}
    )
}

@Preview
@Composable
private fun A4() {
    RecallButton(
        modifier = Modifier,
        rotation = 135f,
        onRecallClicked = {},
        onRecallButtonPositionProvided = {}
    )
}

@Preview
@Composable
private fun A5() {
    RecallButton(
        modifier = Modifier,
        rotation = 180f,
        onRecallClicked = {},
        onRecallButtonPositionProvided = {}
    )
}