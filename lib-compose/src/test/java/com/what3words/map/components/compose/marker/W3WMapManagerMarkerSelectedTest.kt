package com.what3words.map.components.compose.marker

import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WDistance
import com.what3words.core.types.language.W3WProprietaryLanguage
import com.what3words.map.components.compose.BaseW3WMapManagerTest
import com.what3words.map.components.compose.DummyUtils
import com.what3words.map.components.compose.DummyUtils.Companion.dummyCoordinates
import com.what3words.map.components.compose.DummyUtils.Companion.dummySquare
import com.what3words.map.components.compose.DummyUtils.Companion.dummyW3WAddress
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class W3WMapManagerMarkerSelectedTest: BaseW3WMapManagerTest() {
    @Test
    fun getSelectedAddress_noSelectedAddress_returnsNull() {
        // Given: no address is selected
        val expectedAddress = null

        // When: calling getSelectedAddress
        val actualAddress = mapManager.getSelectedAddress()

        // Then: expect that the returned address is null
        assertEquals(expectedAddress, actualAddress)
    }

    @Test
    fun getSelectedAddress_withSelectedAddress_returnsAddress() = runTest {
        // Given: a suggestion without a center
        val words = "///filled.count.soap"
        every {
            textDataSource.convertToCoordinates(words)
        }.answers {
            W3WResult.Success(dummyW3WAddress)
        }

        // When: calling setSelectedAt with the words
        mapManager.setSelectedAt(words)

        // Then: expect that the selected address is updated using words
        verify(exactly = 1) { textDataSource.convertToCoordinates(words) }
        assertEquals(dummyW3WAddress, mapManager.getSelectedAddress())
    }

    @Test
    fun setSelectedAt_withValidWords_setSelectedAddress() = runTest {
        // Given: a valid three-word address
        val words = "///filled.count.soap"
        every {
            textDataSource.convertToCoordinates(words)
        }.answers {
            W3WResult.Success(dummyW3WAddress)
        }

        // When: calling setSelectedAt with the words
        mapManager.setSelectedAt(words)

        // Then: expect that the selected address is updated using words
        verify(exactly = 1) { textDataSource.convertToCoordinates(words) }
        assertEquals(dummyW3WAddress, mapManager.getSelectedAddress())
    }

    @Test(expected = W3WError::class)
    fun setSelectedAt_withInvalidWords_throwsError() = runTest {
        // Given: an invalid three-word address
        val words = "///invalid.words.address"

        every {
            textDataSource.convertToCoordinates(words)
        }.answers {
            W3WResult.Failure(W3WError("Invalid address"))
        }

        // When: calling setSelectedAt with the invalid words
        mapManager.setSelectedAt(words)

        // Then: expect that an exception is thrown
        verify(exactly = 1) { textDataSource.convertToCoordinates(words) }
    }

    @Test
    fun setSelectedAt_withValidCoordinates_setSelectedAddress() = runTest {
        // Given: valid coordinates and a successful conversion to 3wa
        val coordinates = dummyCoordinates
        every {
            textDataSource.convertTo3wa(coordinates, any())
        }.answers {
            W3WResult.Success(dummyW3WAddress)
        }

        // When: calling setSelectedAt with the coordinates
        mapManager.setSelectedAt(coordinates)

        // Then: expect that the selected address is updated
        verify(exactly = 1) { textDataSource.convertTo3wa(coordinates, any()) }
        assertEquals(dummyW3WAddress, mapManager.getSelectedAddress())
    }

    @Test(expected = W3WError::class)
    fun setSelectedAt_withInvalidCoordinates_throwsError() = runTest {
        // Given: invalid coordinates and a failed conversion to 3wa
        val coordinates = dummyCoordinates

        every {
            textDataSource.convertTo3wa(coordinates, any())
        }.answers {
            W3WResult.Failure(W3WError("Invalid coordinates"))
        }

        // When: calling setSelectedAt with the invalid coordinates
        mapManager.setSelectedAt(coordinates)

        // Then: expect that an exception is thrown
        verify(exactly = 1) { textDataSource.convertTo3wa(coordinates, any()) }
    }

    @Test
    fun setSelectedAt_withValidW3WSuggestion_setSelectedAddress() = runTest {
        // Given: an suggestion with a valid center
        val suggestion = DummyUtils.dummyW3WSuggestion

        // When: calling setSelectedAt with the suggestion
        mapManager.setSelectedAt(suggestion)

        // Then: expect that the selected address is updated
        assertEquals(dummyW3WAddress, mapManager.getSelectedAddress())
    }

    @Test
    fun setSelectedAt_withW3WSuggestion_WithoutCenter_setSelectedAddressByWords() = runTest {
        // Given: an address without a center
        val address = W3WAddress(
            words = "filled.count.soap",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        val suggestion = W3WSuggestion(address, rank = 1, distanceToFocus = W3WDistance(1.0))

        every {
            textDataSource.convertToCoordinates(address.words)
        }.answers {
            W3WResult.Success(dummyW3WAddress)
        }

        // When: calling setSelectedAt with the address
        mapManager.setSelectedAt(suggestion)

        // Then: expect that the selected address is updated and convertToCoordinates is called
        verify(exactly = 1) { textDataSource.convertToCoordinates(address.words) }
        assertEquals(dummyW3WAddress, mapManager.getSelectedAddress())
    }

    @Test(expected = W3WError::class)
    fun setSelectedAt_invalidW3WSuggestion_throwsError() = runTest {
        // Given: an address without a center
        val address = W3WAddress(
            words = "///invalid.words.address",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        val suggestion = W3WSuggestion(address, rank = 1, distanceToFocus = W3WDistance(1.0))

        every {
            textDataSource.convertToCoordinates(address.words)
        }.answers {
            W3WResult.Failure(W3WError("Invalid address"))
        }

        // When: calling setSelectedAt with the address
        mapManager.setSelectedAt(suggestion)

        // Then: expect that an exception is thrown
        verify(exactly = 1) { textDataSource.convertToCoordinates(address.words) }
    }

    @Test
    fun setSelectedAt_withValidW3WAddress_setSelectedAddress() = runTest {
        // Given: an address with a valid center
        val address = dummyW3WAddress

        // When: calling setSelectedAt with the address
        mapManager.setSelectedAt(address)

        // Then: expect that the selected address is updated
        assertEquals(address, mapManager.getSelectedAddress())
    }

    @Test
    fun setSelectedAt_withW3WAddressWithoutCenter_setSelectedAddressByWords() = runTest {
        // Given: an address without a center
        val address = W3WAddress(
            words = "filled.count.soap",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        every {
            textDataSource.convertToCoordinates(address.words)
        }.answers {
            W3WResult.Success(dummyW3WAddress)
        }

        // When: calling setSelectedAt with the address
        mapManager.setSelectedAt(address)

        // Then: expect that the selected address is updated and convertToCoordinates is called
        verify(exactly = 1) { textDataSource.convertToCoordinates(address.words) }
        assertEquals(dummyW3WAddress, mapManager.getSelectedAddress())
    }

    @Test(expected = W3WError::class)
    fun setSelectedAt_invalidW3WAddress_throwsError() = runTest {
        // Given: an address without a center
        val address = W3WAddress(
            words = "///invalid.words.address",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        every {
            textDataSource.convertToCoordinates(address.words)
        }.answers {
            W3WResult.Failure(W3WError("Invalid address"))
        }

        // When: calling setSelectedAt with the address
        mapManager.setSelectedAt(address)

        // Then: expect that an exception is thrown
        verify(exactly = 1) { textDataSource.convertToCoordinates(address.words) }
    }

    @Test
    fun clearSelectedAddress_calledMultipleTimes_remainsNull() = runTest {
        // Given: A selected address is set and then cleared
        mapManager.setSelectedAt(dummyW3WAddress)
        assertNotNull(mapManager.getSelectedAddress())

        // When: Clearing the selected address
        mapManager.clearSelectedAddress()

        // Then: The selected address remains null
        assertNull(mapManager.getSelectedAddress())
    }
}