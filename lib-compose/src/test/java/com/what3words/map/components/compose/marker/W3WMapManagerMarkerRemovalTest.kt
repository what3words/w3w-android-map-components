package com.what3words.map.components.compose.marker

import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WProprietaryLanguage
import com.what3words.map.components.compose.BaseW3WMapManagerTest
import com.what3words.map.components.compose.DummyUtils.Companion.dummyCoordinates
import com.what3words.map.components.compose.DummyUtils.Companion.dummySquare
import com.what3words.map.components.compose.DummyUtils.Companion.dummyW3WAddress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class W3WMapManagerMarkerRemovalTest: BaseW3WMapManagerTest()  {
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
        val removedMarkers = mapManager.removeMarkerAt(
            W3WAddress(
            words = "filled.count.soap",
            center = null,
            square = dummySquare,
            language = W3WProprietaryLanguage("en", null, null, null),
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )
        )

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
        mapManager.addMarkerAt(
            W3WSuggestion(
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

        val removedMarkers = mapManager.removeMarkerAt(
            W3WSuggestion(
            w3wAddress = addressWithoutCenter,
            rank = 1,
            distanceToFocus = null
        )
        )

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
}