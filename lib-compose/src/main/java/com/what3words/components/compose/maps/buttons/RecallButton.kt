package com.what3words.components.compose.maps.buttons

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A composable function to display a recall button.
 *
 * @param modifier The modifier for the button.
 * @param rotation The rotation angle for the button in degrees.
 * @param backgroundColor The background color of the button.
 * @param arrowColor The color of the arrow icon.
 * @param isVisible Whether the button is visible or not.
 * @param contentDescription The content description for the button.
 * @param onRecallClicked The callback when the button is clicked.
 * @param onRecallButtonPositionProvided The callback providing the button's position as a PointF.
 */
@Composable
fun RecallButton(
    modifier: Modifier = Modifier,
    rotation: Float = 0F,
    backgroundColor: Color,
    arrowColor: Color,
    isVisible: Boolean = false,
    contentDescription: W3WMapButtonsDefault.ContentDescription = W3WMapButtonsDefault.defaultContentDescription(),
    onRecallClicked: () -> Unit,
    onRecallButtonPositionProvided: (PointF) -> Unit,
) {

    var position: Offset by remember { mutableStateOf(Offset.Zero) }

    IconButton(
        onClick = { onRecallClicked() },
        modifier = modifier
            .padding(4.dp)
            .alpha(if (isVisible) 1f else 0f)
            .rotate(rotation)
            .shadow(elevation = 3.dp, shape = CircleShape)
            .size(50.dp)
            .background(backgroundColor)
            .onGloballyPositioned { coordinate ->
                // Only trigger one time when the button is initialized
                // The coordinate is affected by the rotation
                if (position == Offset.Zero) {
                    position = coordinate.positionInWindow()
                    val point = PointF(position.x, position.y)
                    onRecallButtonPositionProvided.invoke(point)
                }
            },
        enabled = isVisible
    ) {
        Icon(
            modifier = Modifier
                .size(30.dp)
                .padding(1.25.dp)
                .offset(x = (-2).dp),
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = contentDescription.recallButtonDescription,
            tint = arrowColor
        )
    }
}

@Preview
@Composable
private fun A1() {
    RecallButton(
        modifier = Modifier,
        rotation = 0f,
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        isVisible = true,
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
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        isVisible = true,
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
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        isVisible = true,
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
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        isVisible = true,
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
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        isVisible = true,
        onRecallClicked = {},
        onRecallButtonPositionProvided = {}
    )
}