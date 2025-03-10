package com.what3words.components.compose.maps.state

import android.graphics.PointF
import androidx.compose.runtime.Immutable
import com.what3words.components.compose.maps.models.W3WGridScreenCell
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.state.LocationStatus.ACTIVE
import com.what3words.components.compose.maps.state.LocationStatus.INACTIVE
import com.what3words.components.compose.maps.state.LocationStatus.SEARCHING

/**
 * Data class representing the state of W3W map components such as My Location and Recall buttons.
 *
 * @property accuracyDistance The accuracy radius of the user's location in meters.
 * @property locationStatus Current status of the location tracking (ACTIVE, INACTIVE, SEARCHING).
 * @property isRecallButtonVisible Whether the recall button should be displayed.
 * @property isRecallButtonEnabled Whether the recall button is clickable.
 * @property recallRotationDegree Rotation angle of the recall button in degrees.
 * @property recallButtonPosition Position of the recall button on screen as a PointF.
 * @property isCameraMoving Indicates whether the map camera is currently moving.
 * @property mapProjection Projection information for the map.
 * @property mapViewPort Information about the visible area of the map.
 * @property recallButtonViewPort Information about the visible area around the recall button.
 * @property selectedScreenLocation The currently selected location on screen as a PointF.
 */
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

/**
 * Enum representing the current status of location tracking.
 *
 * @property ACTIVE Location tracking is active and the user's position is being tracked.
 * @property INACTIVE Location tracking is turned off or not available.
 * @property SEARCHING The system is currently attempting to establish the user's location.
 */
enum class LocationStatus {
    ACTIVE,
    INACTIVE,
    SEARCHING
}