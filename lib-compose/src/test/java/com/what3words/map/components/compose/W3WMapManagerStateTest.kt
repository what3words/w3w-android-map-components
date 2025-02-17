package com.what3words.map.components.compose

import com.what3words.components.compose.maps.mapper.toW3WMarker
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WDistance
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WProprietaryLanguage
import com.what3words.map.components.compose.DummyUtils.Companion.dummyCoordinates
import com.what3words.map.components.compose.DummyUtils.Companion.dummySquare
import com.what3words.map.components.compose.DummyUtils.Companion.dummyW3WAddress
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

class W3WMapManagerStateTest: BaseW3WMapManagerTest() {

    @Test
    fun isDarkModeEnabled_returnsDefaultValue() {
        // Given: Initial dark mode
        val isDarkModeEnabled = false

        // When: calling isDarkModeEnabled
        val result = mapManager.isDarkModeEnabled()

        // Then: expect that the dark mode status will be 'false'
        assertFalse(result)
    }

    @Test
    fun enableDarkMode_true_returnsTrue() {
        // Given: valid input for enabling dark mode
        val enableDarkMode = true

        // When: calling enableDarkMode with the 'true' value
        mapManager.enableDarkMode(enableDarkMode)

        // Then: expect that the dark mode status will be 'true'
        assertTrue(mapManager.isDarkModeEnabled())
    }

    @Test
    fun enableDarkMode_false_returnsFalse() {
        // Given: valid input for disabling dark mode
        val enableDarkMode = false

        // When: calling enableDarkMode with the 'false' value
        mapManager.enableDarkMode(enableDarkMode)

        // Then: expect that the dark mode status will be 'false'
        assertFalse(mapManager.isDarkModeEnabled())
    }

    @Test
    fun getMapType_defaultMapType_returnsNormal() {
        // Given: the initial map type is NORMAL
        val expectedMapType = W3WMapType.NORMAL

        // When: calling getMapType
        val actualMapType = mapManager.getMapType()

        // Then: expect that the returned map type is NORMAL
        assertEquals(expectedMapType, actualMapType)
    }

    @Test
    fun setMapType_normalMapType_updatesStateToNormal() {
        // Given: a new map type (NORMAL)
        val newMapType = W3WMapType.NORMAL

        // When: calling setMapType with the new map type
        mapManager.setMapType(newMapType)

        // Then: expect that the map state is updated to NORMAL
        assertEquals(newMapType, mapManager.getMapType())
    }

    @Test
    fun setMapType_hybridMapType_updatesStateToHybrid() {
        // Given: a new map type (HYBRID)
        val newMapType = W3WMapType.HYBRID

        // When: calling setMapType with the new map type
        mapManager.setMapType(newMapType)

        // Then: expect that the map state is updated to HYBRID
        assertEquals(newMapType, mapManager.getMapType())
    }

    @Test
    fun setMapType_satelliteMapType_updatesStateToSatellite() {
        // Given: a new map type (SATELLITE)
        val newMapType = W3WMapType.SATELLITE

        // When: calling setMapType with the new map type
        mapManager.setMapType(newMapType)

        // Then: expect that the map state is updated to SATELLITE
        assertEquals(newMapType, mapManager.getMapType())
    }

    @Test
    fun setMapType_terrainMapType_updatesStateToTerrain() {
        // Given: a new map type (TERRAIN)
        val newMapType = W3WMapType.TERRAIN

        // When: calling setMapType with the new map type
        mapManager.setMapType(newMapType)

        // Then: expect that the map state is updated to TERRAIN
        assertEquals(newMapType, mapManager.getMapType())
    }

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
    fun removeAllMarkers_clearsAllMarkers() = runTest {
        //Given: Add marker
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE)

        // When: calling removeAllMarkers
        mapManager.removeAllMarkers()

        // Then: all markers should be removed from markersMap
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarker_removesSpecifiedMarker() = runTest {
        //Given: Add markers
        val result = mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE)
        val anotherMarker = (mapManager.addMarkerAt(
            W3WAddress(
                words = "///surfed.ironic.handbags",
                center = W3WCoordinates(10.780361, 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            ),
            zoomOption = W3WZoomOption.NONE) as W3WResult.Success).value

        val markerToRemove = (result as W3WResult.Success).value

        // When: calling removeMarker
        mapManager.removeMarker(markerToRemove)

        // Then: markerToRemove should no longer be present
        assertFalse(mapManager.markers.contains(markerToRemove))

        // And: anotherMarker should still be present
        assertTrue(mapManager.markers.contains(anotherMarker))
    }

    @Test
    fun removeMarkerAt_allLists_removesMarkersWithWords() = runTest {
        //Given: Add markers
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List Name 1",zoomOption = W3WZoomOption.NONE)

        // When: calling removeMarker
        val removedMarkers = mapManager.removeMarkerAt(dummyW3WAddress.words)

        // Then: remove marker with words in all list
        assertTrue(removedMarkers.size == 2)
        assertTrue(mapManager.markers.isEmpty())

    }

    @Test
    fun removeMarkerAt_specificList_removesMarkersWithWords() = runTest {
        //Given: Add markers
        val result =  mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List Name 1",zoomOption = W3WZoomOption.NONE)

        // When: calling removeMarker
        val removedMarkers = mapManager.removeMarkerAt(dummyW3WAddress.words, "List Name 1")

        // Then: remove marker with words in list with name "List Name 1" only
        assertTrue(removedMarkers.size == 1)
        assertFalse(mapManager.markers.isEmpty())
        assertTrue(mapManager.markers.contains((result as W3WResult.Success).value))
    }

    @Test
    fun removeMarkerAtCoordinates_allLists_removesMarkers() = runTest {
        // Given: Add markers
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List Name 1", zoomOption = W3WZoomOption.NONE)

        // When: calling removeMarkerAt with coordinates, removing all instances
        val removedMarkers = mapManager.removeMarkerAt(dummyCoordinates)

        // Then: all markers with the specified coordinates should be removed in all lists
        assertTrue(removedMarkers.size == 2)
        assertFalse(mapManager.markers.contains(removedMarkers.first()))
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkerAtCoordinates_specificList_removesMarkers() = runTest {
        // Given: Add markers
        val result = mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List Name 1", zoomOption = W3WZoomOption.NONE)

        // When: calling removeMarkerAt with coordinates, only in specific list
        val removedMarkers = mapManager.removeMarkerAt(dummyCoordinates, "List Name 1")

        // Then: markers should be removed from the specified list only
        assertTrue(removedMarkers.size == 1)
        assertFalse(mapManager.markers.isEmpty())
        assertTrue(mapManager.markers.contains((result as W3WResult.Success).value))
    }

    @Test
    fun removeMarkerAtAddress_withCoordinates_removesMarkers() = runTest {
        // Given: Add markers
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE)

        // When: calling removeMarkerAt with Address have center data
        val removedMarkers = mapManager.removeMarkerAt(dummyW3WAddress)

        // Then: markers should be removed
        assertTrue(removedMarkers.size == 1)
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkerAtAddress_withWords_removesMarkers() = runTest {
        // Given: Add markers
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE)

        // When: calling removeMarkerAt with Address without center data
        val removedMarkers = mapManager.removeMarkerAt(W3WAddress(
            words = "filled.count.soap",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        ))

        // Then: markers should be removed
        assertTrue(removedMarkers.size == 1)
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkerAtSuggestion_withCoordinates_removesMarkers() = runTest {
        // Given: Add marker with a suggestion having center data
        val suggestionWithCenter = W3WSuggestion(
            w3wAddress = dummyW3WAddress, // This already has a center
            rank = 1,
            distanceToFocus = null
        )

        mapManager.addMarkerAt(suggestionWithCenter, zoomOption = W3WZoomOption.NONE)

        // When: calling removeMarkerAt with suggestion having center data
        val removedMarkers = mapManager.removeMarkerAt(suggestionWithCenter)

        // Then: the marker should be removed
        assertTrue(removedMarkers.size == 1)
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkerAtSuggestion_withoutCoordinates_removesMarkers() = runTest {
        // Given: Add marker with a suggestion without center data
        mapManager.addMarkerAt(W3WSuggestion(
            w3wAddress = dummyW3WAddress,
            rank = 1,
            distanceToFocus = null
        ), zoomOption = W3WZoomOption.NONE)

        // When: calling removeMarkerAt with suggestion without center data
        val addressWithoutCenter = W3WAddress(
            words = "filled.count.soap",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        val removedMarkers = mapManager.removeMarkerAt(W3WSuggestion(
            w3wAddress = addressWithoutCenter,
            rank = 1,
            distanceToFocus = null
        ))

        // Then: the marker should be removed
        assertTrue(removedMarkers.size == 1)
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkersAt_withListWords_removesAllMarkers() = runTest {
        // Given: Add markers
        val addresses = listOf(
            dummyW3WAddress,
            W3WAddress(
                words = "///surfed.ironic.handbags",
                center = W3WCoordinates(10.780361, 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )
        mapManager.addMarkersAt(addresses, zoomOption = W3WZoomOption.NONE)

        // When: removing markers with specific words
        val removedMarkers = mapManager.removeMarkersAt(listOf("///surfed.ironic.handbags", "filled.count.soap"))

        // Then: Validate all markers with specified words are removed
        assertEquals(2, removedMarkers.size)
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkersAt_withListWordsAndSpecificList_removesMarkersFromSpecificList() = runTest {
        // Given: Populate the manager with markers in specific lists
        val addresses = listOf(
            dummyW3WAddress,
            W3WAddress(
                words = "///surfed.ironic.handbags",
                center = W3WCoordinates(10.780361, 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )
        mapManager.addMarkersAt(addresses, zoomOption = W3WZoomOption.NONE, listName = "List 1")
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE, listName = "List 2")

        // When: removing markers with specific words from a specific list
        val removedMarkers = mapManager.removeMarkersAt(listOf("///surfed.ironic.handbags", "filled.count.soap"), listName = "List 1")

        // Then: Only markers from the specified list are removed
        assertEquals(2, removedMarkers.size)
        assertTrue(mapManager.getMarkersInList("List 1").isEmpty())
        assertFalse(mapManager.getMarkersInList("List 2").isEmpty())
        assertEquals(1, mapManager.markers.size)
    }

    @Test
    fun removeMarkersAt_withListCoordinates_removesAllMarkers() = runTest {
        // Given: Add markers
        val addresses = listOf(
            dummyW3WAddress,
            W3WAddress(
                words = "///surfed.ironic.handbags",
                center = W3WCoordinates(10.780361, 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )
        mapManager.addMarkersAt(addresses, zoomOption = W3WZoomOption.NONE)

        // When: removing markers with specific coordinates
        val removedMarkers = mapManager.removeMarkersAt( listOf(
            dummyCoordinates,
            W3WCoordinates(lat = 10.780361, lng = 106.705986)
        ))

        // Then: Validate all markers with specified coordinates are removed
        assertEquals(2, removedMarkers.size)
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkersAt_withListCoordinatesAndSpecificList_removesMarkersFromSpecificList() = runTest {
        // Given: Add markers
        val addresses = listOf(
            dummyW3WAddress,
            W3WAddress(
                words = "///surfed.ironic.handbags",
                center = W3WCoordinates(10.780361, 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )
        mapManager.addMarkersAt(addresses, zoomOption = W3WZoomOption.NONE, listName = "List 1")
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE, listName = "List 2")

        // When: removing markers with specific coordinates
        val removedMarkers = mapManager.removeMarkersAt(listOf(
            dummyCoordinates,
            W3WCoordinates(lat = 10.780361, lng = 106.705986)
        ),"List 1")

        // Then: Only markers from the specified list are removed
        assertEquals(2, removedMarkers.size)
        assertTrue(mapManager.getMarkersInList("List 1").isEmpty())
        assertFalse(mapManager.getMarkersInList("List 2").isEmpty())
        assertEquals(1, mapManager.markers.size)
    }

    @Test
    fun removeMarkersAt_withListAddresses_removesAllMarkers() = runTest {
        // Given: Add markers with addresses
        val addresses = listOf(
            dummyW3WAddress,
            W3WAddress(
                words = "///surfed.ironic.handbags",
                center = W3WCoordinates(10.780361, 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )
        mapManager.addMarkersAt(addresses, zoomOption = W3WZoomOption.NONE)

        // When: removing markers with specific addresses
        val removedMarkers = mapManager.removeMarkersAt(addresses)

        // Then: Validate all markers with specified addresses are removed
        assertEquals(2, removedMarkers.size)
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkersAt_withListAddressesAndSpecificList_removesMarkersFromSpecificList() = runTest {
        // Given: Populate manager with markers in specific lists
        val addresses = listOf(
            dummyW3WAddress,
            W3WAddress(
                words = "///surfed.ironic.handbags",
                center = W3WCoordinates(10.780361, 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )
        mapManager.addMarkersAt(addresses, zoomOption = W3WZoomOption.NONE, listName = "List 1")
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE, listName = "List 2")

        // When: removing markers with specific addresses from a specific list
        val removedMarkers = mapManager.removeMarkersAt(addresses, listName = "List 1")

        // Then: Only markers from the specified list are removed
        assertEquals(2, removedMarkers.size)
        assertTrue(mapManager.getMarkersInList("List 1").isEmpty())
        assertFalse(mapManager.getMarkersInList("List 2").isEmpty())
        assertEquals(1, mapManager.markers.size)
    }

    @Test
    fun removeMarkersAt_withListSuggestions_removesAllMarkers() = runTest {
        // Given: Add markers with suggestions
        val suggestions = listOf(
            W3WSuggestion(
                w3wAddress = dummyW3WAddress,
                rank = 1,
                distanceToFocus = null
            ),
            W3WSuggestion(
                w3wAddress = W3WAddress(
                    words = "///surfed.ironic.handbags",
                    center = W3WCoordinates(10.780361, 106.705986),
                    square = W3WRectangle(
                        southwest = W3WCoordinates(10.780361, 106.705986),
                        northeast = W3WCoordinates(10.780361, 106.705986)
                    ),
                    language = W3WProprietaryLanguage("vn", null, null, null),
                    country = W3WCountry("VN"),
                    nearestPlace = "Ho Chi Minh"
                ),
                rank = 2,
                distanceToFocus = null
            )
        )
        mapManager.addMarkersAt(suggestions, zoomOption = W3WZoomOption.NONE)

        // When: removing markers with specific suggestions
        val removedMarkers = mapManager.removeMarkersAt(suggestions)

        // Then: Validate all markers with specified suggestions are removed
        assertEquals(2, removedMarkers.size)
        assertTrue(mapManager.markers.isEmpty())
    }

    @Test
    fun removeMarkersAt_withListSuggestionsAndSpecificList_removesMarkersFromSpecificList() = runTest {
        // Given: Populate manager with markers in specific lists
        val suggestions = listOf(
            W3WSuggestion(
                w3wAddress = dummyW3WAddress,
                rank = 1,
                distanceToFocus = null
            ),
            W3WSuggestion(
                w3wAddress = W3WAddress(
                    words = "///surfed.ironic.handbags",
                    center = W3WCoordinates(10.780361, 106.705986),
                    square = W3WRectangle(
                        southwest = W3WCoordinates(10.780361, 106.705986),
                        northeast = W3WCoordinates(10.780361, 106.705986)
                    ),
                    language = W3WProprietaryLanguage("vn", null, null, null),
                    country = W3WCountry("VN"),
                    nearestPlace = "Ho Chi Minh"
                ),
                rank = 2,
                distanceToFocus = null
            )
        )
        mapManager.addMarkersAt(suggestions, zoomOption = W3WZoomOption.NONE, listName = "List 1")
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE, listName = "List 2")

        // When: removing markers with specific suggestions from a specific list
        val removedMarkers = mapManager.removeMarkersAt(suggestions, listName = "List 1")

        // Then: Only markers from the specified list are removed
        assertEquals(2, removedMarkers.size)
        assertTrue(mapManager.getMarkersInList("List 1").isEmpty())
        assertFalse(mapManager.getMarkersInList("List 2").isEmpty())
        assertEquals(1, mapManager.markers.size)
    }

    @Test
    fun removeListMarker_removesAllMarkersFromSpecifiedList() = runTest {
        // Given: Populate the manager with markers across multiple lists
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List 1", zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(
            W3WAddress(
                words = "///surfed.ironic.handbags",
                center = W3WCoordinates(10.780361, 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            ),
            listName = "List 2",
            zoomOption = W3WZoomOption.NONE
        )

        // When: removing all markers from a specific list
        mapManager.removeListMarker("List 1")

        // Then: Validate markers from "List 1" are removed and markers from other lists remain
        assertTrue(mapManager.getMarkersInList("List 1").isEmpty())
        assertFalse(mapManager.getMarkersInList("List 2").isEmpty())
        assertEquals(1, mapManager.markers.size) // Total markers from other lists should remain
    }

    @Test
    fun getMarkersAtCoordinates_retrievesAllMarkersAtGivenCoordinates() = runTest {
        // Given: Add multiple markers, some with the same coordinates
        val sharedCoordinates = W3WCoordinates(51.520847, -0.195521)

        val address1 = W3WAddress(
            words = "///tinh xảo.hòa hợp.đường tàu",
            center = sharedCoordinates,
            square = W3WRectangle(
                southwest = sharedCoordinates,
                northeast = sharedCoordinates
            ),
            language = W3WProprietaryLanguage("vn", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        val address2 = W3WAddress(
            words = "///filled.count.soap",
            center = sharedCoordinates,
            square = W3WRectangle(
                southwest = sharedCoordinates,
                northeast = sharedCoordinates
            ),
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        mapManager.addMarkerAt(address1, listName = "List 1", zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(address2, listName = "List 2", zoomOption = W3WZoomOption.NONE)

        // When: retrieving markers at the shared coordinates
        val markersAtCoordinates = mapManager.getMarkersAt(sharedCoordinates)

        // Then: Validate that both markers are retrieved
        assertEquals(2, markersAtCoordinates.size)
        assertTrue(markersAtCoordinates.any { it.listName == "List 1" })
        assertTrue(markersAtCoordinates.any { it.listName == "List 2" })
    }

    @Test
    fun getMarkersAtWords_retrievesAllMarkersWithSpecifiedWords() = runTest {
        // Given: Add markers with a common what3words address in different lists
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List 1", zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List 2", zoomOption = W3WZoomOption.NONE)

        // When: retrieving markers with the specified what3words address
        val markersWithWords = mapManager.getMarkersAt(dummyW3WAddress.words)

        // Then: Validate all markers with the specified what3words are retrieved
        assertEquals(2, markersWithWords.size)
        assertTrue(markersWithWords.any { it.listName == "List 1" })
        assertTrue(markersWithWords.any { it.listName == "List 2" })
    }

    @Test
    fun getMarkersAtAddress_retrievesAllMarkersForProvidedAddress() = runTest {
        // Given: Add markers with a common what3words address in different lists
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List 1", zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List 2", zoomOption = W3WZoomOption.NONE)

        // When: Retrieving markers using the address
        val markersWithWords = mapManager.getMarkersAt(dummyW3WAddress)

        // Then: Validate all markers with the specified what3words are retrieved
        assertEquals(2, markersWithWords.size)
        assertTrue(markersWithWords.any { it.listName == "List 1" })
        assertTrue(markersWithWords.any { it.listName == "List 2" })
    }

    @Test
    fun getMarkersAtSuggestion_retrievesAllMarkersForProvidedSuggestion() = runTest {
        // Given: Add markers with a common what3words address in different lists
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List 1", zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(dummyW3WAddress, listName = "List 2", zoomOption = W3WZoomOption.NONE)

        // When: Retrieving markers using the suggestion
        val suggestion = W3WSuggestion(
            w3wAddress = dummyW3WAddress,
            rank = 1,
            distanceToFocus = null
        )
        val markersFromSuggestion = mapManager.getMarkersAt(suggestion.w3wAddress)

        // Then: Validate all markers associated with the suggestion are retrieved
        assertEquals(2, markersFromSuggestion.size)
        assertTrue(markersFromSuggestion.any { it.listName == "List 1" })
        assertTrue(markersFromSuggestion.any { it.listName == "List 2" })
    }

    @Test
    fun getMarkersInList_retrievesAllMarkersForGivenListName() = runTest {
        // Given: Add markers to specific lists
        val address2 = W3WAddress(
            words = "///surfed.ironic.handbags",
            center = W3WCoordinates(lat = 10.780361, lng = 106.705986),
            square = W3WRectangle(
                southwest = W3WCoordinates(10.780361, 106.705986),
                northeast = W3WCoordinates(10.780361, 106.705986)
            ),
            language = W3WProprietaryLanguage("vn", null, null, null),
            country = W3WCountry("VN"),
            nearestPlace = "Ho Chi Minh"
        )

        mapManager.addMarkerAt(dummyW3WAddress, listName = "List 1", zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(address2, listName = "List 1", zoomOption = W3WZoomOption.NONE)
        mapManager.addMarkerAt(address2, listName = "List 2", zoomOption = W3WZoomOption.NONE)

        // When: Retrieving markers from "List 1"
        val markersInList1 = mapManager.getMarkersInList("List 1")

        // Then: Validate markers are correctly retrieved from "List 1"
        assertEquals(2, markersInList1.size)
        assertTrue(markersInList1.any { it.words == dummyW3WAddress.words })
        assertTrue(markersInList1.any { it.words == "///surfed.ironic.handbags" })

        // When: Retrieving markers from "List 2"
        val markersInList2 = mapManager.getMarkersInList("List 2")

        // Then: Validate that the list is empty
        assertEquals(1, markersInList2.size)
    }

    @Test
    fun addMarkerAt_withValidWords_returnsSuccess() = runTest {
        // Given: valid three-word address
        val words = "filled.count.soap"
        val expectedMarker = dummyW3WAddress.toW3WMarker()
        coEvery { textDataSource.convertToCoordinates(words) } returns W3WResult.Success(
            dummyW3WAddress
        )

        // When: calling addMarkerAt with the words
        val result = mapManager.addMarkerAt(
            words = words,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withInvalidWords_returnsFailure() = runTest {
        // Given: invalid three-word address
        val words = "invalid.words.address"
        val expectedError = W3WError("Invalid address")
        coEvery { textDataSource.convertToCoordinates(words) } returns W3WResult.Failure(expectedError)

        // When: calling addMarkerAt with the invalid words
        val result = mapManager.addMarkerAt(words)

        // Then: expect a failure result
        assertTrue(result is W3WResult.Failure)
        assertEquals(expectedError, (result as W3WResult.Failure).error)
    }

    @Test
    fun addMarkerAt_withValidCoordinates_returnsSuccess() = runTest {
        // Given: valid coordinates
        val coordinates = dummyCoordinates
        val dummyAddress = dummyW3WAddress
        val expectedMarker = dummyAddress.toW3WMarker()

        coEvery { textDataSource.convertTo3wa(coordinates, any()) } returns W3WResult.Success(dummyAddress)

        // When: calling addMarkerAt with the coordinates
        val result = mapManager.addMarkerAt(
            coordinates = coordinates,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withInvalidCoordinates_returnsFailure() = runTest {
        // Given: invalid coordinates resulting in an error
        val coordinates = dummyCoordinates
        val expectedError = W3WError("Invalid coordinates")

        coEvery { textDataSource.convertTo3wa(coordinates, any()) } returns W3WResult.Failure(expectedError)

        // When: calling addMarkerAt with the invalid coordinates
        val result = mapManager.addMarkerAt(coordinates)

        // Then: expect a failure result
        assertTrue(result is W3WResult.Failure)
        assertEquals(expectedError, (result as W3WResult.Failure).error)
    }

    @Test
    fun addMarkerAt_withAddressHavingCenter_returnsSuccess() = runTest {
        // Given: a valid address with center
        val addressWithCenter = dummyW3WAddress
        val expectedMarker = addressWithCenter.toW3WMarker()

        // No need to mock conversion as the center is already present

        // When: calling addMarkerAt with the address
        val result = mapManager.addMarkerAt(
            addressWithCenter,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withAddressNeedingConversion_returnsSuccess() = runTest {
        // Given: an address needing conversion
        val addressWithoutCenter = W3WAddress(
            words = "filled.count.soap",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        val expectedMarker = dummyW3WAddress.toW3WMarker()

        coEvery { textDataSource.convertToCoordinates(addressWithoutCenter.words) } returns W3WResult.Success(
            dummyW3WAddress
        )

        // When: calling addMarkerAt with the address
        val result = mapManager.addMarkerAt(
            addressWithoutCenter,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withInvalidAddress_returnsFailure() = runTest {
        // Given: invalid three-word address
        val addressWithInvalidWords = W3WAddress(
            words = "invalid.words.address",
            center = null,
            square = null,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )
        val expectedError = W3WError("Invalid coordinates")

        coEvery { textDataSource.convertToCoordinates(addressWithInvalidWords.words) } returns W3WResult.Failure(expectedError)

        // When: calling addMarkerAt with the invalid address
        val result = mapManager.addMarkerAt(
            addressWithInvalidWords,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a failure result
        assertTrue(result is W3WResult.Failure)
        assertEquals(expectedError, (result as W3WResult.Failure).error)
    }

    @Test
    fun addMarkerAt_withSuggestionHavingCenter_returnsSuccess() = runTest {
        // Given: a valid suggestion with a center in its address
        val addressWithCenter = dummyW3WAddress
        val suggestionWithCenter = W3WSuggestion(
            w3wAddress = addressWithCenter,
            rank = 1,
            distanceToFocus = null
        )
        val expectedMarker = addressWithCenter.toW3WMarker()

        // When: calling addMarkerAt with the suggestion
        val result = mapManager.addMarkerAt(
            suggestionWithCenter,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withSuggestionNeedingConversion_returnsSuccess() = runTest {
        // Given: a suggestion needing conversion
        val addressWithoutCenter = W3WAddress(
            words = "filled.count.soap",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )
        val suggestionWithoutCenter = W3WSuggestion(
            w3wAddress = addressWithoutCenter,
            rank = 1,
            distanceToFocus = null
        )
        val expectedAddress = dummyW3WAddress
        val expectedMarker = expectedAddress.toW3WMarker()

        coEvery { textDataSource.convertToCoordinates(addressWithoutCenter.words) } returns W3WResult.Success(expectedAddress)

        // When: calling addMarkerAt with the suggestion
        val result = mapManager.addMarkerAt(
            suggestionWithoutCenter,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withInvalidSuggestion_returnsFailure() = runTest {
        // Given: a suggestion with invalid words
        val addressWithInvalidWords = W3WAddress(
            words = "invalid.words.address",
            center = null,
            square = null,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )
        val invalidSuggestion = W3WSuggestion(
            w3wAddress = addressWithInvalidWords,
            rank = 1,
            distanceToFocus = null
        )
        val expectedError = W3WError("Invalid address")

        coEvery { textDataSource.convertToCoordinates(addressWithInvalidWords.words) } returns W3WResult.Failure(expectedError)

        // When: calling addMarkerAt with the invalid suggestion
        val result = mapManager.addMarkerAt(invalidSuggestion)

        // Then: expect a failure result
        assertTrue(result is W3WResult.Failure)
        assertEquals(expectedError, (result as W3WResult.Failure).error)
    }

    @Test
    fun addMarkersAtListWords_addsMarkersToSpecifiedList() = runTest {
        // Given: A list of what3words addresses
        val listWords = listOf("filled.count.soap", "surfed.ironic.handbags")

        coEvery { textDataSource.convertToCoordinates("filled.count.soap") } returns W3WResult.Success(
            dummyW3WAddress
        )

        coEvery { textDataSource.convertToCoordinates("surfed.ironic.handbags") } returns W3WResult.Success(
            W3WAddress(
                words = "surfed.ironic.handbags",
                center = W3WCoordinates(lat = 10.780361, lng = 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )

        // When: Adding markers to a specified list
        val results = mapManager.addMarkersAt(listWords, listName = "Test List", zoomOption = W3WZoomOption.NONE)

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }

        val markersInTestList = mapManager.getMarkersInList("Test List")
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == "filled.count.soap" })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" })
    }

    @Test
    fun addMarkersAtListCoordinates_addsMarkersToSpecifiedList() = runTest {
        // Given: A list of W3WCoordinates
        val listCoordinates = listOf(
            W3WCoordinates(lat = 51.520847, lng = -0.195521),
            W3WCoordinates(lat = 10.780361, lng = 106.705986)
        )

        // Mock responses from textDataSource for coordinate conversion
        coEvery { textDataSource.convertTo3wa(listCoordinates[0], any()) } returns W3WResult.Success(dummyW3WAddress)
        coEvery { textDataSource.convertTo3wa(listCoordinates[1], any()) } returns W3WResult.Success(
            W3WAddress(
                words = "surfed.ironic.handbags",
                center = W3WCoordinates(lat = 10.780361, lng = 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WProprietaryLanguage("vn", null, null, null),
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )

        // When: Adding markers to a specified list
        val results = mapManager.addMarkersAt(listCoordinates, listName = "Test List", zoomOption = W3WZoomOption.NONE)

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }
        val markersInTestList = mapManager.getMarkersInList("Test List")
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" })
    }

    
}