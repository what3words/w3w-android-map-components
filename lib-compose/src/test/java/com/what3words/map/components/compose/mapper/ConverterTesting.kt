package com.what3words.map.components.compose.mapper

import androidx.compose.ui.graphics.Color
import com.google.maps.android.compose.MapType
import com.mapbox.maps.Style
import com.what3words.components.compose.maps.W3WMapDefaults.MARKER_COLOR_DEFAULT
import com.what3words.components.compose.maps.mapper.toGoogleLatLng
import com.what3words.components.compose.maps.mapper.toGoogleMapType
import com.what3words.components.compose.maps.mapper.toMapBoxMapType
import com.what3words.components.compose.maps.mapper.toW3WMarker
import com.what3words.components.compose.maps.models.W3WMapType
import com.what3words.components.compose.maps.models.W3WMarker
import com.what3words.components.compose.maps.models.W3WMarkerColor
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WRFC5646Language
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ConverterTesting {
    @Test
    fun toW3WMarker_converts_W3WAddress_to_W3WMarker_with_default_color() {
        // Given: A valid W3WAddress
        val address = W3WAddress(
            words = "filled.count.soap",
            center = W3WCoordinates(lat = 51.520847, lng = -0.195521),
            square = W3WRectangle(
                southwest = W3WCoordinates(lat = 51.520847, lng = -0.195521),
                northeast = W3WCoordinates(lat = 51.520847, lng = -0.195521)
            ),
            language = W3WRFC5646Language.EN_GB,
            country = W3WCountry("GB"),
            nearestPlace = "London"
        )

        // When: Converting the address to a marker using the default color
        val marker: W3WMarker = address.toW3WMarker(MARKER_COLOR_DEFAULT)

        // Then: The marker should have the same words, square, center, and the default color
        assertEquals(address.words, marker.words)
        assertEquals(address.square, marker.square)
        assertEquals(address.center, marker.center)
        assertEquals(MARKER_COLOR_DEFAULT, marker.color)
    }

    @Test
    fun toW3WMarker_converts_W3WAddress_to_W3WMarker_with_custom_color() {
        // Given: A valid W3WAddress
        val address = W3WAddress(
            words = "surfed.ironic.handbags",
            center = W3WCoordinates(lat = 10.780361, lng = 106.705986),
            square = W3WRectangle(
                southwest = W3WCoordinates(lat = 10.780361, lng = 106.705986),
                northeast = W3WCoordinates(lat = 10.780361, lng = 106.705986)
            ),
            language = W3WRFC5646Language.VI,
            country = W3WCountry("VN"),
            nearestPlace = "Ho Chi Minh"
        )

        // Define a custom marker color
        val customColor = W3WMarkerColor(
            slash = Color.Red,
            background = Color.Black
        )

        // When: Converting the address to a marker using the custom color
        val marker: W3WMarker = address.toW3WMarker(customColor)

        // Then: The marker should have the same words, square, center, and the custom color
        assertEquals(address.words, marker.words)
        assertEquals(address.square, marker.square)
        assertEquals(address.center, marker.center)
        assertEquals(customColor, marker.color)
    }

    @Test
    fun convertW3WMapType_toGoogleMapType_converts_correctly() {
        // Given: All possible W3WMapType values
        val normalMapType = W3WMapType.NORMAL
        val satelliteMapType = W3WMapType.SATELLITE
        val hybridMapType = W3WMapType.HYBRID
        val terrainMapType = W3WMapType.TERRAIN

        // When: Converting W3WMapType to MapType
        val normalResult = normalMapType.toGoogleMapType()
        val satelliteResult = satelliteMapType.toGoogleMapType()
        val hybridResult = hybridMapType.toGoogleMapType()
        val terrainResult = terrainMapType.toGoogleMapType()

        // Then: Verify that each W3WMapType is correctly mapped to the expected MapType
        assertEquals(MapType.NORMAL, normalResult)
        assertEquals(MapType.SATELLITE, satelliteResult)
        assertEquals(MapType.HYBRID, hybridResult)
        assertEquals(MapType.TERRAIN, terrainResult)
    }

    // Test for W3WCoordinates.toGoogleLatLng()
    @Test
    fun convertW3WCoordinates_toGoogleLatLng_converts_correctly() {
        // Given: A valid W3WCoordinates object
        val coordinates = W3WCoordinates(lat = 51.520847, lng = -0.195521)

        // When: Converting W3WCoordinates to LatLng
        val latLng = coordinates.toGoogleLatLng()

        // Then: Verify that the lat and lng values match
        assertEquals(coordinates.lat, latLng.latitude)
        assertEquals(coordinates.lng, latLng.longitude)
    }

    @Test
    fun convertW3WCoordinates_toGoogleLatLng_with_edge_case_coordinates() {
        // Given: Edge case coordinates (latitude and longitude at extreme values)
        val edgeCoordinates = W3WCoordinates(lat = 90.0, lng = 180.0)

        // When: Converting W3WCoordinates to LatLng
        val edgeLatLng = edgeCoordinates.toGoogleLatLng()

        // Then: Verify that the lat and lng values match
        assertEquals(90.0, edgeLatLng.latitude)
        assertEquals(-180.0, edgeLatLng.longitude)
    }

    @Test
    fun convertW3WMapType_NORMAL_converts_to_MapBox_STANDARD() {
        // Given: W3WMapType.NORMAL
        val w3wMapType = W3WMapType.NORMAL

        // When: Converting W3WMapType to MapBox style string
        val result = w3wMapType.toMapBoxMapType()

        // Then: Verify that the result matches Style.STANDARD
        assertEquals(Style.STANDARD, result)
    }

    @Test
    fun convertW3WMapType_SATELLITE_converts_to_MapBox_SATELLITE_STREETS() {
        // Given: W3WMapType.SATELLITE
        val w3wMapType = W3WMapType.SATELLITE

        // When: Converting W3WMapType to MapBox style string
        val result = w3wMapType.toMapBoxMapType()

        // Then: Verify that the result matches Style.SATELLITE_STREETS
        assertEquals(Style.SATELLITE_STREETS, result)
    }

    @Test
    fun convertW3WMapType_HYBRID_converts_to_MapBox_SATELLITE_STREETS() {
        // Given: W3WMapType.HYBRID
        val w3wMapType = W3WMapType.HYBRID

        // When: Converting W3WMapType to MapBox style string
        val result = w3wMapType.toMapBoxMapType()

        // Then: Verify that the result matches Style.SATELLITE_STREETS
        assertEquals(Style.SATELLITE_STREETS, result)
    }

    @Test
    fun convertW3WMapType_TERRAIN_converts_to_MapBox_OUTDOORS() {
        // Given: W3WMapType.TERRAIN
        val w3wMapType = W3WMapType.TERRAIN

        // When: Converting W3WMapType to MapBox style string
        val result = w3wMapType.toMapBoxMapType()

        // Then: Verify that the result matches Style.OUTDOORS
        assertEquals(Style.OUTDOORS, result)
    }
}