package com.what3words.components.compose.maps.buttons

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
 * @param selectedPosition The position used for calculating the offset for animation, defaults to PointF().
 * @param isVisible Determines whether the button should be visible.
 * @param isButtonEnabled Controls whether the button is enabled and can be interacted with.
 * @param isCameraMoving Indicates if the camera is currently moving, affecting button visibility.
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
    selectedPosition: PointF = PointF(),
    isVisible: Boolean = false,
    isButtonEnabled: Boolean,
    isCameraMoving: Boolean = false
) {

    // Track previous isVisible state to detect changes
    var prevIsVisible by remember { mutableStateOf(isVisible) }
    var shouldBeVisible by remember { mutableStateOf(isVisible) }

    val positionCallback = rememberUpdatedState(onRecallButtonPositionProvided)
    var buttonPosition: Offset by remember { mutableStateOf(Offset.Unspecified) }

    LaunchedEffect(isVisible, isCameraMoving) {
        // Show the button when isVisible is true,
        // OR  prevIsVisible was true AND we're currently moving the camera (isCameraMoving is true)
        shouldBeVisible = isVisible || (prevIsVisible && isCameraMoving)
        prevIsVisible = isVisible
    }

    AnimatedVisibility(
        visible = shouldBeVisible,
        enter = fadeIn(
            initialAlpha = 0f,
            animationSpec = tween(400)
        ) + slideIn(
            animationSpec = tween(400),
            initialOffset = { size ->
                IntOffset(
                    x = (selectedPosition.x - buttonPosition.x).toInt(),
                    y = (selectedPosition.y - buttonPosition.y).toInt()
                )
            }
        ),
        exit = fadeOut(
            targetAlpha = 0f,
            animationSpec = tween(durationMillis = 400, delayMillis = 200)
        ) + slideOut(
            animationSpec = tween(durationMillis = 400, delayMillis = 200),
            targetOffset = { size ->
                IntOffset(
                    x = (selectedPosition.x - buttonPosition.x).toInt(),
                    y = (selectedPosition.y - buttonPosition.y).toInt()
                )
            }
        )
    ) {
        IconButton(
            onClick = { onRecallClicked() },
            modifier = modifier
                .onGloballyPositioned { coordinate ->
                    // Get the center position of the recall button
                    // Only trigger one time when the button is initialized
                    // NOTE: onGloballyPositioned is called AFTER a composition,
                    // so the first appear time the button can't perform the animation fully because lack of position info.
                    if (!buttonPosition.isValid()) {
                        val size = coordinate.size
                        val position = coordinate.positionInRoot()
                        val centerX = position.x + size.width / 2
                        val centerY = position.y + size.height / 2
                        val centerPoint = PointF(centerX, centerY)
                        buttonPosition = Offset(centerX, centerY)
                        positionCallback.value(centerPoint)
                    }
                }
                .padding(layoutConfig.buttonPadding)
                .shadow(
                    elevation = if (isButtonEnabled) layoutConfig.elevation else 0.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .size(layoutConfig.buttonSize)
                .alpha(if (isButtonEnabled) 1f else layoutConfig.disabledIconOpacity)
                .background(recallButtonColor.recallBackgroundColor),
            enabled = isButtonEnabled,
        ) {
            Icon(
                modifier = Modifier
                    .size(layoutConfig.imageSize)
                    .padding(layoutConfig.imagePadding)
                    .graphicsLayer {
                        rotationZ = rotation
                    },
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = contentDescription.recallButtonDescription,
                tint = recallButtonColor.recallArrowColor
            )
        }
    }
}

@Preview
@Composable
private fun A1() {
    RecallButton(
        modifier = Modifier,
        rotation = 0f,
        isVisible = true,
        isButtonEnabled = true,
        isCameraMoving = false,
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
        isVisible = true,
        isButtonEnabled = true,
        isCameraMoving = false,
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
        isVisible = true,
        isButtonEnabled = true,
        isCameraMoving = false,
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
        isVisible = true,
        isButtonEnabled = true,
        isCameraMoving = false,
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
        isVisible = true,
        isButtonEnabled = true,
        isCameraMoving = false,
        onRecallClicked = {},
        onRecallButtonPositionProvided = {}
    )
}

@Preview
@Composable
private fun A6() {
    RecallButton(
        modifier = Modifier,
        rotation = 180f,
        isVisible = true,
        isButtonEnabled = false,
        isCameraMoving = false,
        onRecallClicked = {},
        onRecallButtonPositionProvided = {}
    )
}