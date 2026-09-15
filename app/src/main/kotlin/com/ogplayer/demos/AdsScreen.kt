package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogplayer.ads.ima.ImaAdsProvider
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.api.ads.AdTagConfig
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig
import com.ogplayer.ui.OverlaySlot

/**
 * Ads showcase: Google IMA skippable preroll. The log below captures the
 * complete ad lifecycle — break started, ad started (with pod info),
 * skipped/completed, break completed — alongside the playback callbacks.
 *
 * A watermark overlay sits in the top-end slot: the SDK hides overlay
 * slots automatically while an ad break plays, so the watermark only
 * appears once content starts.
 */
// Google's public IMA sample tags (VAST single ad + VMAP ad-rule scenarios).
private const val VMAP_BASE =
    "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples" +
        "&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&ad_rule=1&output=vmap" +
        "&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="

private enum class AdScenario(val label: String, val tag: String) {
    SINGLE_PRE(
        "Skippable preroll",
        "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/" +
            "single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1" +
            "&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator=",
    ),
    PRE_MID_POST("Pre + mid + post", "$VMAP_BASE&cust_params=sample_ar%3Dpremidpost"),
    MID_POD_3("Mid-roll pod (3 ads)", "$VMAP_BASE&cust_params=sample_ar%3Dpremidpostpod"),
    MID_POD_5("Long pod (5 ads)", "$VMAP_BASE&cust_params=sample_ar%3Dpremidpostlongpod"),
    BROKEN(
        "Broken tag (error)",
        "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/does_not_exist" +
            "&sz=640x480&gdfp_req=1&output=vast&env=vp&impl=s&correlator=",
    ),
}

@Composable
internal fun AdsDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    var scenario by remember { mutableStateOf(AdScenario.SINGLE_PRE) }
    val player = remember {
        OGPlayer.Builder(context)
            .setAdsProvider(ImaAdsProvider(context))
            .build()
    }
    val log = remember { EventLogState() }
    // The clip is a progressive MP4: one audio track, no text tracks, no
    // ladder — buttons that would open an empty menu are hidden.
    val uiConfig = remember {
        OGUiConfig.Builder()
            .setShowSubtitleButton(false)
            .setShowAudioTrackButton(false)
            .setShowQualityButton(false)
            .build()
    }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        player.attachAdEventLogging(log)
        onDispose { player.release() }
    }

    androidx.compose.runtime.LaunchedEffect(scenario) {
        log.add("— loading ${scenario.label} —")
        player.load(
            OGMediaItem.Builder("https://media.ogplayer.tv/tos-clip-60s.mp4")
                .setStreamType(StreamType.VOD)
                .setTitle("Ads demo — ${scenario.label}")
                .setAdBreaks(AdTagConfig(scenario.tag))
                .build(),
            playWhenReady = true,
        )
    }

    Surface(Modifier.fillMaxSize()) {
        Column(if (isFullscreen) Modifier.fillMaxSize() else Modifier.safeDrawingPadding()) {
            OGPlayerView(
                player = player,
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                },
                uiConfig = uiConfig,
                fullscreenHandler = fullscreenHandler,
                autoFullscreenOnRotate = true,
            )
            // Fullscreen means the player and nothing else.
            if (!isFullscreen) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    AdScenario.entries.forEach { option ->
                        FilterChip(
                            selected = scenario == option,
                            onClick = { scenario = option },
                            label = { Text(option.label, maxLines = 1) },
                        )
                    }
                }
                Text(
                    text = "IMA ad scenarios — IMA renders its own skip and " +
                        "clickthrough UI; the SDK draws the yellow bar, AD chip " +
                        "and countdown. The clip is a progressive MP4 with one audio " +
                        "track, no subtitles and no quality ladder, so those three " +
                        "buttons are hidden (showSubtitleButton / showAudioTrackButton / " +
                        "showQualityButton = false).",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
