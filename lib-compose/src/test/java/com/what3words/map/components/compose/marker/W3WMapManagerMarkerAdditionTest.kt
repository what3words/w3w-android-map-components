package com.what3words.map.components.compose.marker

import androidx.compose.ui.graphics.Color
import com.what3words.components.compose.maps.W3WMapManager
import com.what3words.components.compose.maps.mapper.toW3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.components.compose.maps.models.W3WZoomOption
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WProprietaryLanguage
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.map.components.compose.BaseW3WMapManagerTest
import com.what3words.map.components.compose.DummyUtils.Companion.dummyCoordinates
import com.what3words.map.components.compose.DummyUtils.Companion.dummySquare
import com.what3words.map.components.compose.DummyUtils.Companion.dummyW3WAddress
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class W3WMapManagerMarkerAdditionTest: BaseW3WMapManagerTest() {

    @Test
    fun addMarkerAt_withValidWords_returnsSuccess() = runTest {
        // Given: valid three-word address
        val words = "filled.count.soap"
        val expectedMarker = dummyW3WAddress.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))
        coEvery { textDataSource.convertToCoordinates(words) } returns W3WResult.Success(
            dummyW3WAddress
        )

        // When: calling addMarkerAt with the words
        val result = mapManager.addMarkerAt(
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            words = words,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)).any { it == expectedMarker })
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withValidWords_customList_returnsSuccess() = runTest {
        // Given: valid three-word address
        val words = "filled.count.soap"
        val listName = "myCustomList"
        val expectedMarker = dummyW3WAddress.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))
        coEvery { textDataSource.convertToCoordinates(words) } returns W3WResult.Success(
            dummyW3WAddress
        )

        // When: calling addMarkerAt with the words, custom list name
        val result = mapManager.addMarkerAt(
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            listName = listName,
            words = words,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(listName)).any { it == expectedMarker })
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
        assertTrue(mapManager.markers.isEmpty())
        assertEquals(expectedError, (result as W3WResult.Failure).error)
    }

    @Test
    fun addMarkerAt_withValidCoordinates_returnsSuccess() = runTest {
        // Given: valid coordinates
        val coordinates = dummyCoordinates
        val dummyAddress = dummyW3WAddress
        val expectedMarker = dummyAddress.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))

        coEvery { textDataSource.convertTo3wa(coordinates, any()) } returns W3WResult.Success(dummyAddress)

        // When: calling addMarkerAt with the coordinates
        val result = mapManager.addMarkerAt(
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            coordinates = coordinates,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)).any { it == expectedMarker })
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withValidCoordinates_customList__returnsSuccess() = runTest {
        // Given: valid coordinates
        val coordinates = dummyCoordinates
        val dummyAddress = dummyW3WAddress
        val listName = "myCustomList"
        val expectedMarker = dummyAddress.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))

        coEvery { textDataSource.convertTo3wa(coordinates, any()) } returns W3WResult.Success(dummyAddress)

        // When: calling addMarkerAt with the coordinates, custom list name
        val result = mapManager.addMarkerAt(
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            coordinates = coordinates,
            listName = listName,
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(listName)).any { it == expectedMarker })
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
        assertTrue(mapManager.markers.isEmpty())
        assertEquals(expectedError, (result as W3WResult.Failure).error)
    }

    @Test
    fun addMarkerAt_withAddressHavingCenter_returnsSuccess() = runTest {
        // Given: a valid address with center
        val addressWithCenter = dummyW3WAddress
        val expectedMarker = addressWithCenter.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))

        // No need to mock conversion as the center is already present

        // When: calling addMarkerAt with the address
        val result = mapManager.addMarkerAt(
            address = addressWithCenter,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)).any { it == expectedMarker })
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

        val expectedMarker = dummyW3WAddress.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))

        coEvery { textDataSource.convertToCoordinates(addressWithoutCenter.words) } returns W3WResult.Success(
            dummyW3WAddress
        )

        // When: calling addMarkerAt with the address
        val result = mapManager.addMarkerAt(
            address = addressWithoutCenter,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)).any { it == expectedMarker })
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withAddress_customList_returnsSuccess() = runTest {
        // Given: a valid address with center
        val addressWithCenter = dummyW3WAddress
        val listName = "myCustomList"
        val expectedMarker = addressWithCenter.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))

        // No need to mock conversion as the center is already present

        // When: calling addMarkerAt with the address
        val result = mapManager.addMarkerAt(
            address = addressWithCenter,
            listName = listName,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(listName)).any { it == expectedMarker })
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
        assertTrue(mapManager.markers.isEmpty())
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
        val expectedMarker = addressWithCenter.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))

        // When: calling addMarkerAt with the suggestion
        val result = mapManager.addMarkerAt(
            suggestion = suggestionWithCenter,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)).any { it == expectedMarker })
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
        val expectedMarker = expectedAddress.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))

        coEvery { textDataSource.convertToCoordinates(addressWithoutCenter.words) } returns W3WResult.Success(expectedAddress)

        // When: calling addMarkerAt with the suggestion
        val result = mapManager.addMarkerAt(
            suggestionWithoutCenter,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)).any { it == expectedMarker })
        assertEquals(expectedMarker, (result as W3WResult.Success).value)
    }

    @Test
    fun addMarkerAt_withSuggestion_customList_returnsSuccess() = runTest {
        // Given: a valid suggestion with a center in its address
        val addressWithCenter = dummyW3WAddress
        val listName = "myCustomList"
        val suggestionWithCenter = W3WSuggestion(
            w3wAddress = addressWithCenter,
            rank = 1,
            distanceToFocus = null
        )
        val expectedMarker = addressWithCenter.toW3WMarker(W3WMarkerColor(slash = Color.Red, background = Color.White))

        // When: calling addMarkerAt with the suggestion
        val result = mapManager.addMarkerAt(
            suggestion = suggestionWithCenter,
            listName = listName,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: expect a successful result
        assertTrue(result is W3WResult.Success)
        assertTrue((mapManager.getMarkersInList(listName)).any { it == expectedMarker })
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
        assertTrue(mapManager.markers.isEmpty())
        assertEquals(expectedError, (result as W3WResult.Failure).error)
    }

    @Test
    fun addMarkersAtListWords_addsMarkersToDefaultList() = runTest {
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
        val results = mapManager.addMarkersAt(
            listWords = listWords,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }

        val markersInTestList = mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White)})
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
        val results = mapManager.addMarkersAt(
            listWords = listWords,
            listName = "Test List",
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }

        val markersInTestList = mapManager.getMarkersInList("Test List")
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White)})
    }

    @Test
    fun addMarkersAtListCoordinates_addsMarkersToDefaultList() = runTest {
        // Given: A list of W3WCoordinates
        val listCoordinates = listOf(
            W3WCoordinates(lat = 51.520847, lng = -0.195521),
            W3WCoordinates(lat = 10.780361, lng = 106.705986)
        )

        // Mock responses from textDataSource for coordinate conversion
        coEvery { textDataSource.convertTo3wa(listCoordinates[0], any()) } returns W3WResult.Success(
            dummyW3WAddress
        )
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
        val results = mapManager.addMarkersAt(
            listCoordinates = listCoordinates,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }
        val markersInTestList = mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
    }

    @Test
    fun addMarkersAtListCoordinates_addsMarkersToSpecifiedList() = runTest {
        // Given: A list of W3WCoordinates
        val listCoordinates = listOf(
            W3WCoordinates(lat = 51.520847, lng = -0.195521),
            W3WCoordinates(lat = 10.780361, lng = 106.705986)
        )

        // Mock responses from textDataSource for coordinate conversion
        coEvery { textDataSource.convertTo3wa(listCoordinates[0], any()) } returns W3WResult.Success(
            dummyW3WAddress
        )
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
        val results = mapManager.addMarkersAt(
            listCoordinates = listCoordinates,
            listName = "Test List",
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }
        val markersInTestList = mapManager.getMarkersInList("Test List")
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
    }

    @Test
    fun addMarkersAtAddresses_addsMarkersToDefaultList() = runTest {
        // Given: A list of W3WAddress objects
        val addresses = listOf(
            W3WAddress(
                words = "filled.count.soap",
                center = W3WCoordinates(lat = 51.520847, lng = -0.195521),
                square = W3WRectangle(
                    southwest = W3WCoordinates(51.520847, -0.195521),
                    northeast = W3WCoordinates(51.520847, -0.195521)
                ),
                language = W3WRFC5646Language.EN_GB,
                country = W3WCountry("GB"),
                nearestPlace = "London"
            ),
            W3WAddress(
                words = "surfed.ironic.handbags",
                center = W3WCoordinates(lat = 10.780361, lng = 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WRFC5646Language.VI,
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )

        // Mock responses from textDataSource for address conversion
        coEvery { textDataSource.convertToCoordinates(any()) } returns W3WResult.Success(addresses[0])

        // When: Adding markers to a specified list
        val results = mapManager.addMarkersAt(
            addresses = addresses,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }
        val markersInTestList = mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
    }

    @Test
    fun addMarkersAtAddresses_addsMarkersToSpecifiedList() = runTest {
        // Given: A list of W3WAddress objects
        val addresses = listOf(
            W3WAddress(
                words = "filled.count.soap",
                center = W3WCoordinates(lat = 51.520847, lng = -0.195521),
                square = W3WRectangle(
                    southwest = W3WCoordinates(51.520847, -0.195521),
                    northeast = W3WCoordinates(51.520847, -0.195521)
                ),
                language = W3WRFC5646Language.EN_GB,
                country = W3WCountry("GB"),
                nearestPlace = "London"
            ),
            W3WAddress(
                words = "surfed.ironic.handbags",
                center = W3WCoordinates(lat = 10.780361, lng = 106.705986),
                square = W3WRectangle(
                    southwest = W3WCoordinates(10.780361, 106.705986),
                    northeast = W3WCoordinates(10.780361, 106.705986)
                ),
                language = W3WRFC5646Language.VI,
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            )
        )

        // Mock responses from textDataSource for address conversion
        coEvery { textDataSource.convertToCoordinates(any()) } returns W3WResult.Success(addresses[0])

        // When: Adding markers to a specified list
        val results = mapManager.addMarkersAt(
            addresses = addresses,
            listName = "Test List",
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }
        val markersInTestList = mapManager.getMarkersInList("Test List")
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
    }

    @Test
    fun addMarkersAtSuggestions_addsMarkersToDefaultList() = runTest {
        // Given: A list of W3WSuggestion objects
        val suggestions = listOf(
            W3WSuggestion(
                w3wAddress = W3WAddress(
                    words = "filled.count.soap",
                    center = W3WCoordinates(lat = 51.520847, lng = -0.195521),
                    square = W3WRectangle(
                        southwest = W3WCoordinates(51.520847, -0.195521),
                        northeast = W3WCoordinates(51.520847, -0.195521)
                    ),
                    language = W3WRFC5646Language.EN_GB,
                    country = W3WCountry("GB"),
                    nearestPlace = "London"
                ),
                rank = 1,
                distanceToFocus = null
            ),
            W3WSuggestion(
                w3wAddress = W3WAddress(
                    words = "surfed.ironic.handbags",
                    center = W3WCoordinates(lat = 10.780361, lng = 106.705986),
                    square = W3WRectangle(
                        southwest = W3WCoordinates(10.780361, 106.705986),
                        northeast = W3WCoordinates(10.780361, 106.705986)
                    ),
                    language = W3WRFC5646Language.VI,
                    country = W3WCountry("VN"),
                    nearestPlace = "Ho Chi Minh"
                ),
                rank = 2,
                distanceToFocus = null
            )
        )

        // Mock responses from textDataSource for address conversion
        coEvery { textDataSource.convertToCoordinates(any()) } returns W3WResult.Success(
            dummyW3WAddress
        )

        // When: Adding markers to a specified list
        val results = mapManager.addMarkersAt(
            suggestions = suggestions,
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }
        val markersInTestList = mapManager.getMarkersInList(W3WMapManager.LIST_DEFAULT_ID)
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
    }

    @Test
    fun addMarkersAtSuggestions_addsMarkersToSpecifiedList() = runTest {
        // Given: A list of W3WSuggestion objects
        val suggestions = listOf(
            W3WSuggestion(
                w3wAddress = W3WAddress(
                    words = "filled.count.soap",
                    center = W3WCoordinates(lat = 51.520847, lng = -0.195521),
                    square = W3WRectangle(
                        southwest = W3WCoordinates(51.520847, -0.195521),
                        northeast = W3WCoordinates(51.520847, -0.195521)
                    ),
                    language = W3WRFC5646Language.EN_GB,
                    country = W3WCountry("GB"),
                    nearestPlace = "London"
                ),
                rank = 1,
                distanceToFocus = null
            ),
            W3WSuggestion(
                w3wAddress = W3WAddress(
                    words = "surfed.ironic.handbags",
                    center = W3WCoordinates(lat = 10.780361, lng = 106.705986),
                    square = W3WRectangle(
                        southwest = W3WCoordinates(10.780361, 106.705986),
                        northeast = W3WCoordinates(10.780361, 106.705986)
                    ),
                    language = W3WRFC5646Language.VI,
                    country = W3WCountry("VN"),
                    nearestPlace = "Ho Chi Minh"
                ),
                rank = 2,
                distanceToFocus = null
            )
        )

        // Mock responses from textDataSource for address conversion
        coEvery { textDataSource.convertToCoordinates(any()) } returns W3WResult.Success(
            dummyW3WAddress
        )

        // When: Adding markers to a specified list
        val results = mapManager.addMarkersAt(
            suggestions = suggestions,
            listName = "Test List",
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate each marker is successfully added
        results.forEach { result ->
            assertTrue(result is W3WResult.Success)
        }
        val markersInTestList = mapManager.getMarkersInList("Test List")
        assertEquals(2, markersInTestList.size)
        assertTrue(markersInTestList.any { it.words == dummyW3WAddress.words && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
        assertTrue(markersInTestList.any { it.words == "surfed.ironic.handbags" && it.color == W3WMarkerColor(slash = Color.Red, background = Color.White) })
    }

    @Test
    fun addMarkersAt_mixedSuccessAndError_resultsReflectSuccessAndFailure() = runTest {
        // Given: A list of what3words addresses with mixed validity
        val listWords = listOf("filled.count.soap", "invalid.words.address")

        // Mock the textDataSource to return success for the first word and failure for the second
        coEvery { textDataSource.convertToCoordinates("filled.count.soap") } returns W3WResult.Success(dummyW3WAddress)
        coEvery { textDataSource.convertToCoordinates("invalid.words.address") } returns W3WResult.Failure(
            W3WError("Invalid address")
        )

        // When: Adding markers to the specified list
        val results = mapManager.addMarkersAt(
            listWords = listWords,
            listName = "Test List",
            markerColor = W3WMarkerColor(slash = Color.Red, background = Color.White),
            zoomOption = W3WZoomOption.NONE
        )

        // Then: Validate that the first marker was added successfully and the second failed
        assertTrue(results[0] is W3WResult.Success)
        assertTrue(results[1] is W3WResult.Failure)

        // And: Verify that only the successfully added marker exists in the list
        val markersInTestList = mapManager.getMarkersInList("Test List")
        assertEquals(1, markersInTestList.size)
        assertTrue(
            markersInTestList.any {
                it.words == dummyW3WAddress.words &&
                        it.color == W3WMarkerColor(slash = Color.Red, background = Color.White)
            }
        )
    }

    @Test
    fun addDuplicateMarker_differentListName_returnsSuccess() = runTest {
        // Given: A valid marker with list name "List 1"
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE, listName = "List 1")

        // When: Adding marker with difference word but sample coordinate data
        val addressToAdd = W3WAddress(
            words = "surfed.ironic.handbags",
            center = W3WCoordinates(lat = 51.520847, -0.195521),
            square = W3WRectangle(
                southwest = W3WCoordinates(51.520847, -0.195521),
                northeast = W3WCoordinates(51.520847, -0.195521)
            ),
            language = W3WRFC5646Language.VI,
            country = W3WCountry("VN"),
            nearestPlace = "Ho Chi Minh"
        )
        val result = mapManager.addMarkerAt(
            address = addressToAdd,
            zoomOption = W3WZoomOption.NONE,
            listName = "List 2"
        )

        // Then: expect a success result
        assertTrue(result is W3WResult.Success)
        assertEquals(addressToAdd.toW3WMarker(), (result as W3WResult.Success).value)
    }

    @Test
    fun addDuplicateMarker_sameListName_returnError() = runTest {
        // Given: A valid marker with list name "List 1"
        val listName = "List 1"
        mapManager.addMarkerAt(dummyW3WAddress, zoomOption = W3WZoomOption.NONE, listName = listName)

        // When: Adding marker with difference word but sample coordinate data
        val result = mapManager.addMarkerAt(
            W3WAddress(
                words = "surfed.ironic.handbags",
                center = W3WCoordinates(lat = 51.520847, -0.195521),
                square = W3WRectangle(
                    southwest = W3WCoordinates(51.520847, -0.195521),
                    northeast = W3WCoordinates(51.520847, -0.195521)
                ),
                language = W3WRFC5646Language.VI,
                country = W3WCountry("VN"),
                nearestPlace = "Ho Chi Minh"
            ),
            zoomOption = W3WZoomOption.NONE,
            listName = listName
        )

        // Then: expect a failure result
        val expectedError = W3WError("Marker with coordinates ${dummyW3WAddress.center} already exists in the list '${listName}'.")
        assertTrue(result is W3WResult.Failure)
        assertEquals(expectedError.message, (result as W3WResult.Failure).error.message)
    }
}