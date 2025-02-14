package com.what3words.map.components.compose.button

import com.what3words.components.compose.maps.state.LocationStatus
import com.what3words.map.components.compose.BaseW3WMapManagerTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class W3WMapManagerButtonStateTest: BaseW3WMapManagerTest() {

    @Test
    fun setRecallButtonEnabled_enablesRecallButton() = runTest {
        // When: Enabling the recall button
        mapManager.setRecallButtonEnabled(true)

        // Then: Verify that the isRecallButtonEnabled is updated to true
        val buttonState = mapManager.buttonState.value
        assertEquals(true, buttonState.isRecallButtonEnabled)
    }

    @Test
    fun setRecallButtonEnabled_disablesRecallButton() = runTest {
        // When: Disabling the recall button
        mapManager.setRecallButtonEnabled(false)

        // Then: Verify that the isRecallButtonEnabled is updated to false
        val buttonState = mapManager.buttonState.value
        assertEquals(false, buttonState.isRecallButtonEnabled)
    }

    @Test
    fun testRecallButtonEnabled_defaultValue_returnFalse() = runTest {
        // Then: Verify that the default isRecallButtonEnabled is false
        val buttonState = mapManager.buttonState.value
        assertEquals(false, buttonState.isRecallButtonEnabled)
    }

    @Test
    fun updateLocationStatus_updatesStatusToActive() = runTest {
        // When: Updating the location status to ACTIVE
        mapManager.updateLocationStatus(LocationStatus.ACTIVE)

        // Then: Verify that the location status in _buttonState is updated to ACTIVE
        val status = mapManager.buttonState.value.locationStatus
        assertEquals(LocationStatus.ACTIVE, status)
    }

    @Test
    fun updateLocationStatus_updatesStatusToInactive() = runTest {
        // When: Updating the location status to INACTIVE
        mapManager.updateLocationStatus(LocationStatus.INACTIVE)

        // Then: Verify that the location status in _buttonState is updated to INACTIVE
        val status = mapManager.buttonState.value.locationStatus
        assertEquals(LocationStatus.INACTIVE, status)
    }

    @Test
    fun updateLocationStatus_updatesStatusToSearching() = runTest {
        // When: Updating the location status to SEARCHING
        mapManager.updateLocationStatus(LocationStatus.SEARCHING)

        // Then: Verify that the location status in _buttonState is updated to SEARCHING
        val status = mapManager.buttonState.value.locationStatus
        assertEquals(LocationStatus.SEARCHING, status)
    }

    @Test
    fun testLocationStatus_defaultValue_returnINACTIVE() = runTest {
        // Then: Verify that the default locationStatus is INACTIVE
        val status = mapManager.buttonState.value.locationStatus
        assertEquals(LocationStatus.INACTIVE, status)
    }

    @Test
    fun updateAccuracyDistance_updatesAccuracyDistanceCorrectly() = runTest {
        // Given: Initial accuracy distance is set to some default value
        val initialDistance = mapManager.buttonState.value.accuracyDistance
        assertEquals(0f, initialDistance, 0.0f) // Assuming initial is 0

        // When: Updating the accuracy distance to a new value
        val newAccuracyDistance = 15.5f
        mapManager.updateAccuracyDistance(newAccuracyDistance)

        // Then: The accuracy distance in buttonState should be updated to the new value
        val updatedDistance = mapManager.buttonState.value.accuracyDistance
        assertEquals(newAccuracyDistance, updatedDistance)
    }
}