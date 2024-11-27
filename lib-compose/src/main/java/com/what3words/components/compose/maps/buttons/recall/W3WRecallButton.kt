package com.what3words.components.compose.maps.buttons.recall

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun W3WRecallButton(
    modifier: Modifier = Modifier,
    rotation: Float,
    backgroundColor: Color,
    arrowColor: Color,
    onRecallButtonClick: () -> Unit
) {

    var currentRotation by remember { mutableFloatStateOf(rotation) }
    val animatedRotation by animateFloatAsState(
        targetValue = currentRotation,
        animationSpec = tween(1000), // animate over 1 second
        label = "Rotation animation"
    )

    LaunchedEffect(rotation) {
        // animate the rotation every second
        while (true) {
            currentRotation = Random.nextFloat() * 360f // rotate by 10 degrees
            delay(1000) // wait for 1 second
        }
    }

    IconButton(
        onClick = { onRecallButtonClick() },
        modifier = modifier
            .rotate(animatedRotation)
            .shadow(elevation = 3.dp, shape = CircleShape)
            .size(50.dp)
            .background(backgroundColor)
    ) {
        Icon(
            modifier = Modifier
                .size(30.dp)
                .padding(1.25.dp)
                .offset(x = (-2).dp),
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = null,
            tint = arrowColor
        )
    }
}

@Preview
@Composable
private fun A1() {
    W3WRecallButton(
        modifier = Modifier,
        rotation = 0f,
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        onRecallButtonClick = {}
    )
}