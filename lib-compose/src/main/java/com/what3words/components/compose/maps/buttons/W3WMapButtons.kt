package com.what3words.components.compose.maps.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun W3WMapButtons(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        OutlinedButton(onClick = { /*TODO*/ },
            modifier= Modifier.size(50.dp),  //avoid the oval shape
            shape = CircleShape,
            border= BorderStroke(1.dp, Color.Blue),
            contentPadding = PaddingValues(0.dp),  //avoid the little icon
            colors = ButtonDefaults.outlinedButtonColors(contentColor =  Color.Blue)
        ) {
            Icon(Icons.Default.LocationSearching, contentDescription = "content description")
        }
    }
}