package com.what3words.components.compose.maps.buttons.recall

import android.graphics.PointF
import android.util.Log
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun W3WRecallButton(
    modifier: Modifier = Modifier,
    rotation: Float = 0F,
    backgroundColor: Color = Color(0xFFE11F26),
    arrowColor: Color = Color.White,
    onRecallClicked: () -> Unit,
    onRecallPositionProvided: (PointF) -> Unit,
) {

    var position: Offset? by remember { mutableStateOf(null) }

    IconButton(
        onClick = { onRecallClicked() },
        modifier = modifier
            .rotate(rotation)
            .shadow(elevation = 3.dp, shape = CircleShape)
            .size(50.dp)
            .background(backgroundColor)
            .onGloballyPositioned { coordinate ->
                if (position == null) {
                    position = coordinate.localToWindow(Offset.Zero)
                    val point = PointF(position!!.x, position!!.y)
                    onRecallPositionProvided.invoke(point)
                }
            },
    ) {
        Icon(
            modifier = Modifier
                .size(30.dp)
                .padding(1.25.dp)
                .offset(x = (-2).dp),
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = null, // TODO: Add content description later
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
        onRecallClicked = {},
        onRecallPositionProvided = {}
    )
}

@Preview
@Composable
private fun A2() {
    W3WRecallButton(
        modifier = Modifier,
        rotation = 45f,
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        onRecallClicked = {},
        onRecallPositionProvided = {}
    )
}

@Preview
@Composable
private fun A3() {
    W3WRecallButton(
        modifier = Modifier,
        rotation = 90f,
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        onRecallClicked = {},
        onRecallPositionProvided = {}
    )
}

@Preview
@Composable
private fun A4() {
    W3WRecallButton(
        modifier = Modifier,
        rotation = 135f,
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        onRecallClicked = {},
        onRecallPositionProvided = {}
    )
}

@Preview
@Composable
private fun A5() {
    W3WRecallButton(
        modifier = Modifier,
        rotation = 180f,
        backgroundColor = Color(0xFFE11F26),
        arrowColor = Color.White,
        onRecallClicked = {},
        onRecallPositionProvided = {}
    )
}