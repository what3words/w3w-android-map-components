package com.what3words.map.components.compose.attribution

import com.mapbox.maps.plugin.attribution.Attribution
import com.what3words.components.compose.maps.providers.mapbox.AttributionLinkAction
import com.what3words.components.compose.maps.providers.mapbox.linkAction
import org.junit.Assert.assertEquals
import org.junit.Test

class AttributionLinkActionTest {

    @Test
    fun `telemetry opens Mapbox's telemetry dialog`() {
        assertEquals(
            AttributionLinkAction.Telemetry,
            Attribution(Attribution.TELEMETRY_SETTINGS, Attribution.ABOUT_TELEMETRY_URL).linkAction(),
        )
    }

    @Test
    fun `geofencing opens Mapbox's geofencing dialog`() {
        assertEquals(
            AttributionLinkAction.Geofencing,
            Attribution(Attribution.GEOFENCING, Attribution.GEOFENCING_URL_MARKER).linkAction(),
        )
    }

    @Test
    fun `improve this map builds its page when tapped`() {
        assertEquals(
            AttributionLinkAction.Feedback,
            Attribution("Improve this map", "https://apps.mapbox.com/feedback/").linkAction(),
        )
    }

    @Test
    fun `credits and the privacy policy are web pages`() {
        listOf(
            Attribution("© Mapbox", Attribution.ABOUT_MAPS_URL),
            Attribution("© OpenStreetMap", "https://www.openstreetmap.org/copyright"),
            Attribution(Attribution.PRIVACY_POLICY, Attribution.PRIVACY_POLICY_URL),
        ).forEach { attribution ->
            assertEquals(AttributionLinkAction.WebPage(attribution.url), attribution.linkAction())
        }
    }

    @Test
    fun `an attribution without a link does nothing`() {
        assertEquals(AttributionLinkAction.None, Attribution("© Somebody", "").linkAction())
    }
}
