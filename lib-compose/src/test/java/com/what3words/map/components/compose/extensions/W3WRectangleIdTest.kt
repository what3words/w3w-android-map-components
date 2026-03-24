package com.what3words.map.components.compose.extensions

import com.what3words.components.compose.maps.extensions.id
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class W3WRectangleIdTest {

    @Test
    fun `should generate unique IDs for different rectangles`() {
        val rect1 = W3WRectangle(
            southwest = W3WCoordinates(10.780078, 106.705424),
            northeast = W3WCoordinates(10.780105, 106.705451)
        )
        val rect2 = W3WRectangle(
            southwest = W3WCoordinates(10.780590, 106.705424),
            northeast = W3WCoordinates(10.780617, 106.705451)
        )

        val id1 = rect1.id
        val id2 = rect2.id

        assertNotEquals("Different rectangles should have different IDs", id1, id2)
    }

    @Test
    fun `should generate same ID for identical rectangles`() {
        val rect1 = W3WRectangle(
            southwest = W3WCoordinates(10.780078, 106.705424),
            northeast = W3WCoordinates(10.780105, 106.705451)
        )
        val rect2 = W3WRectangle(
            southwest = W3WCoordinates(10.780078, 106.705424),
            northeast = W3WCoordinates(10.780105, 106.705451)
        )

        val id1 = rect1.id
        val id2 = rect2.id

        assertEquals("Identical rectangles should have the same ID", id1, id2)
    }

    @Test
    fun `should generate different IDs for previously colliding rectangles - case 1`() {
        // These two rectangles previously generated the same ID: 9222864837722059307
        val rect1 = W3WRectangle(
            southwest = W3WCoordinates(10.780078, 106.705424),
            northeast = W3WCoordinates(10.780105, 106.705451)
        )
        val rect2 = W3WRectangle(
            southwest = W3WCoordinates(10.780590, 106.705424),
            northeast = W3WCoordinates(10.780617, 106.705451)
        )

        val id1 = rect1.id
        val id2 = rect2.id

        assertNotEquals("Previously colliding rectangles (case 1) should now have different IDs", id1, id2)
    }

    @Test
    fun `should generate different IDs for previously colliding rectangles - case 2`() {
        // These two rectangles previously generated the same ID: 9214139332489523810
        val rect1 = W3WRectangle(
            southwest = W3WCoordinates(10.780051, 106.705479),
            northeast = W3WCoordinates(10.780078, 106.705506)
        )
        val rect2 = W3WRectangle(
            southwest = W3WCoordinates(10.780563, 106.705479),
            northeast = W3WCoordinates(10.780590, 106.705506)
        )

        val id1 = rect1.id
        val id2 = rect2.id

        assertNotEquals("Previously colliding rectangles (case 2) should now have different IDs", id1, id2)
    }

    @Test
    fun `should generate different IDs for rectangles with very small coordinate differences`() {
        val rect1 = W3WRectangle(
            southwest = W3WCoordinates(54.672341, 89.467876),
            northeast = W3WCoordinates(54.672351, 89.467886)
        )
        val rect2 = W3WRectangle(
            southwest = W3WCoordinates(54.672342, 89.467875),
            northeast = W3WCoordinates(54.672352, 89.467885)
        )

        val id1 = rect1.id
        val id2 = rect2.id

        assertNotEquals("Rectangles with micro-degree differences should have different IDs", id1, id2)
    }

    @Test
    fun `should generate different IDs when sw and ne are swapped`() {
        val rect1 = W3WRectangle(
            southwest = W3WCoordinates(10.0, 100.0),
            northeast = W3WCoordinates(10.001, 100.001)
        )
        val rect2 = W3WRectangle(
            southwest = W3WCoordinates(10.001, 100.001),
            northeast = W3WCoordinates(10.0, 100.0)
        )

        val id1 = rect1.id
        val id2 = rect2.id

        assertNotEquals("Swapped coordinates should produce different IDs", id1, id2)
    }

    @Test
    fun `should generate different IDs for adjacent grid squares`() {
        // Simulating adjacent w3w grid squares (3m x 3m approximately)
        val rect1 = W3WRectangle(
            southwest = W3WCoordinates(51.520847, -0.195521),
            northeast = W3WCoordinates(51.520870, -0.195497)
        )
        val rect2 = W3WRectangle(
            southwest = W3WCoordinates(51.520870, -0.195521),
            northeast = W3WCoordinates(51.520893, -0.195497)
        )

        val id1 = rect1.id
        val id2 = rect2.id

        assertNotEquals("Adjacent grid squares should have different IDs", id1, id2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw exception for invalid latitude above range`() {
        val rect = W3WRectangle(
            southwest = W3WCoordinates(91.0, 100.0),
            northeast = W3WCoordinates(10.0, 100.0)
        )
        rect.id
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw exception for invalid latitude below range`() {
        val rect = W3WRectangle(
            southwest = W3WCoordinates(-91.0, 100.0),
            northeast = W3WCoordinates(10.0, 100.0)
        )
        rect.id
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw exception for invalid longitude above range`() {
        val rect = W3WRectangle(
            southwest = W3WCoordinates(10.0, 181.0),
            northeast = W3WCoordinates(10.0, 100.0)
        )
        rect.id
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw exception for invalid longitude below range`() {
        val rect = W3WRectangle(
            southwest = W3WCoordinates(10.0, -181.0),
            northeast = W3WCoordinates(10.0, 100.0)
        )
        rect.id
    }

    @Test
    fun `should generate consistent IDs across multiple calls`() {
        val rect = W3WRectangle(
            southwest = W3WCoordinates(10.780078, 106.705424),
            northeast = W3WCoordinates(10.780105, 106.705451)
        )

        val ids = (1..100).map { rect.id }
        val uniqueIds = ids.toSet()

        assertEquals("ID should be consistent across multiple calls", 1, uniqueIds.size)
    }

    @Test
    fun `should generate unique IDs for large batch of random rectangles`() {
        val rectangles = (1..1000).map { index ->
            val lat = -90.0 + (index % 180) + (index * 0.000001)
            val lng = -180.0 + (index % 360) + (index * 0.000001)
            W3WRectangle(
                southwest = W3WCoordinates(lat, lng),
                northeast = W3WCoordinates(lat + 0.00003, lng + 0.00003)
            )
        }

        val ids = rectangles.map { it.id }
        val uniqueIds = ids.toSet()

        assertEquals("All ${rectangles.size} rectangles should have unique IDs", rectangles.size, uniqueIds.size)
    }
}
