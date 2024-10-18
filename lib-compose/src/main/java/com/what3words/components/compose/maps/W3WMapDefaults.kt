package com.what3words.components.compose.maps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object W3WMapDefaults {
    data class LayoutConfig(
        val contentPadding: PaddingValues
    )

    data class ButtonsLayoutConfig(
        val contentPadding: PaddingValues
    )

    @Composable
    fun defaultLayoutConfig(
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ): LayoutConfig {
        return LayoutConfig(
            contentPadding = contentPadding
        )
    }
}