package com.what3words.map.components.compose.marker

import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WProprietaryLanguage
import com.what3words.map.components.compose.BaseW3WMapManagerTest
import com.what3words.map.components.compose.DummyUtils.Companion.dummyW3WAddress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class W3WMapManagerMarkerRetrievalTest: BaseW3WMapManagerTest()  {
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
}