package com.what3words.components.compose.maps.state

import android.graphics.PointF
import androidx.compose.runtime.Immutable
import com.what3words.components.compose.maps.models.W3WGridScreenCell
import com.what3words.components.compose.maps.models.W3WMapProjection

@Immutable
data class W3WButtonsState(

    // My Location button
    val accuracyDistance: Float = 0.0F,
    val locationStatus: LocationStatus = LocationStatus.INACTIVE,

    // Recall button
    val isRecallButtonVisible: Boolean = false,
    val isRecallButtonEnabled: Boolean = false,
    val recallRotationDegree: Float = 0F,
    val recallButtonPosition: PointF = PointF(0F, 0F),

    val isCameraMoving: Boolean = true,
    val mapProjection: W3WMapProjection? = null,
    val mapViewPort: W3WGridScreenCell? = null,
    val recallButtonViewPort: W3WGridScreenCell? = null,
    val selectedScreenLocation: PointF? = null,
)

enum class LocationStatus {
    ACTIVE,
    INACTIVE,
    SEARCHING
}