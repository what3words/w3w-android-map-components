package com.what3words.components.maps

import com.what3words.components.maps.extensions.generateUniqueId
import com.what3words.javawrapper.response.Coordinates
import junit.framework.TestCase.assertEquals
import org.junit.Test

class W3WCoordinateUniqueIdentifierTest {

    @Test
    fun `should generate unique IDs for different coordinate`() {
        val coordinate1 = Coordinates(10.822351, 106.630201)
        val coordinate2 = Coordinates( 10.822728, -25.630339)

        val uniqueId1 = coordinate1.generateUniqueId()
        val uniqueId2 = coordinate2.generateUniqueId()

        assertEquals(false, uniqueId1 == uniqueId2)
    }

    @Test
    fun `should generate different unique IDs for coordinates that are very close together`() {
        val coordinate1 = Coordinates(54.672341, 89.467876)
        val coordinate2 = Coordinates(54.672342, 89.467875)

        val uniqueId1 = coordinate1.generateUniqueId()
        val uniqueId2 = coordinate2.generateUniqueId()

        assertEquals(false, uniqueId1 == uniqueId2)
    }

    @Test
    fun `should generate same unique ID for same coordinates`() {
        val coordinate1 = Coordinates(10.822351, 106.630201)
        val coordinate2 = Coordinates(10.822351, 106.630201)

        val uniqueId1 = coordinate1.generateUniqueId()
        val uniqueId2 = coordinate2.generateUniqueId()

        assertEquals(true, uniqueId1 == uniqueId2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should handle coordinates with latitude and longitude outside the valid range`() {
        val suggestion = Coordinates(91.000000, -181.000000)
        suggestion.generateUniqueId()
    }
}