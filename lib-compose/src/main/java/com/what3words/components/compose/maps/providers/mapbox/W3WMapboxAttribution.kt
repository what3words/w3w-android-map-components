package com.what3words.components.compose.maps.providers.mapbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.maps.extension.compose.ornaments.attribution.MapAttributionScope
import com.mapbox.maps.plugin.attribution.Attribution
import com.mapbox.maps.plugin.attribution.isMapboxFeedback
import kotlinx.coroutines.launch
import com.mapbox.maps.extension.compose.R as MapboxComposeR

// Where Mapbox's own Attribution draws its icon, so the taller one sits in the same place.
private val MAPBOX_ATTRIBUTION_START = 92.dp
private val MAPBOX_ATTRIBUTION_MARGIN = 4.dp
private val MAPBOX_ATTRIBUTION_ICON_SIZE = 21.dp
private val MAPBOX_ATTRIBUTION_COLOR = Color(0xFF1E8CAB)

/** What tapping an attribution link does, as Mapbox's own dialog decides it. */
internal sealed interface AttributionLinkAction {
    data object Telemetry : AttributionLinkAction
    data object Geofencing : AttributionLinkAction

    /** "Improve this map": its page needs the map's position, built when tapped. */
    data object Feedback : AttributionLinkAction
    data class WebPage(val url: String) : AttributionLinkAction
    data object None : AttributionLinkAction
}

internal fun Attribution.linkAction(): AttributionLinkAction = when {
    url == Attribution.ABOUT_TELEMETRY_URL -> AttributionLinkAction.Telemetry
    url == Attribution.GEOFENCING_URL_MARKER -> AttributionLinkAction.Geofencing
    url.isEmpty() -> AttributionLinkAction.None
    isMapboxFeedback() -> AttributionLinkAction.Feedback
    else -> AttributionLinkAction.WebPage(url)
}

/**
 * The map attribution: Mapbox's own, unless [touchTarget] asks for a taller one.
 *
 * @param isClickable False shows the attribution but opens nothing, e.g. while driving
 * @param touchTarget The height of the area that opens it, spanning the Mapbox logo and the icon,
 * which keep their size. Mapbox's own only responds on its 21 dp icon
 */
@Composable
internal fun MapAttributionScope.W3WAttribution(
    contentPadding: PaddingValues,
    isClickable: Boolean,
    touchTarget: Dp?,
) {
    when {
        !isClickable -> Attribution(
            modifier = Modifier.padding(contentPadding),
            // Still shown (Mapbox ToS), but a tap closes the dialog it would open at once.
            attributionDialog = { _, onDismissRequest, _ ->
                LaunchedEffect(Unit) { onDismissRequest() }
            }
        )

        touchTarget != null -> TallAttribution(contentPadding, touchTarget)
        else -> Attribution(modifier = Modifier.padding(contentPadding))
    }
}

/**
 * Mapbox's icon and dialogs behind a taller tap area, driven by Mapbox's public attribution state:
 * Mapbox's own Attribution puts the click on its icon alone, so it cannot be made taller. Web links
 * open through [LocalUriHandler], so an app can decide how, e.g. where there is no browser.
 */
@OptIn(MapboxExperimental::class)
@Composable
private fun MapAttributionScope.TallAttribution(contentPadding: PaddingValues, touchTarget: Dp) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val attributionState = remember { MapAttributionScope.AttributionState() }
    // Null leaves telemetry and geofencing as they are; set only once the user chooses
    var requestedConsent by remember { mutableStateOf<MapAttributionScope.UserConsentState?>(null) }
    var showAttribution by remember { mutableStateOf(false) }
    var showTelemetry by remember { mutableStateOf(false) }
    var showGeofencing by remember { mutableStateOf(false) }

    AttributionControl(requestedConsent, attributionState)

    fun showWebPage(url: String) {
        // The default handler throws when no app can open it; Mapbox's own does nothing then too
        runCatching { uriHandler.openUri(url) }
    }

    fun consent(telemetry: Boolean? = null, geofencing: Boolean? = null) {
        // Null until the state is bound to the map; its dialogs cannot open before that
        val current = attributionState.userConsentState ?: return
        requestedConsent = current.toBuilder().apply {
            telemetry?.let { setTelemetryEnableState(it) }
            geofencing?.let { setGeofencingUserConsentState(it) }
        }.build()
    }

    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(
                    width = MAPBOX_ATTRIBUTION_START + MAPBOX_ATTRIBUTION_ICON_SIZE + MAPBOX_ATTRIBUTION_MARGIN,
                    height = touchTarget,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showAttribution = true }
        ) {
            Image(
                painter = painterResource(MapboxComposeR.drawable.mapbox_attribution_default),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MAPBOX_ATTRIBUTION_COLOR),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = MAPBOX_ATTRIBUTION_START, bottom = MAPBOX_ATTRIBUTION_MARGIN)
                    .size(MAPBOX_ATTRIBUTION_ICON_SIZE),
            )
        }
    }

    if (showAttribution) {
        AttributionDialog(attributionState.attributions, onDismissRequest = { showAttribution = false }) { attribution ->
            showAttribution = false
            when (val action = attribution.linkAction()) {
                AttributionLinkAction.Telemetry -> showTelemetry = true
                AttributionLinkAction.Geofencing -> showGeofencing = true
                AttributionLinkAction.Feedback -> coroutineScope.launch {
                    showWebPage(attributionState.buildMapboxFeedbackUrl())
                }
                is AttributionLinkAction.WebPage -> showWebPage(action.url)
                AttributionLinkAction.None -> Unit
            }
        }
    }
    if (showTelemetry) {
        TelemetryDialog(
            onDismissRequest = { showTelemetry = false },
            onMoreInfo = {
                showTelemetry = false
                showWebPage(Attribution.ABOUT_TELEMETRY_URL)
            },
            onDisagree = {
                showTelemetry = false
                consent(telemetry = false)
            },
            onAgree = {
                showTelemetry = false
                consent(telemetry = true)
            },
        )
    }
    if (showGeofencing) {
        GeofencingConsentDialog(
            onDismissRequest = { showGeofencing = false },
            onDisagree = {
                showGeofencing = false
                consent(geofencing = false)
            },
            onAgree = {
                showGeofencing = false
                consent(geofencing = true)
            },
            attributionState.userConsentState?.geofencingUserConsentState ?: false,
        )
    }
}
