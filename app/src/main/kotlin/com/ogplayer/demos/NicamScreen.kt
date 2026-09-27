package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ogplayer.ads.ima.ImaAdsProvider
import com.ogplayer.api.ContentRating
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.api.ads.AdTagConfig
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView

private const val PREROLL_TAG =
    "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/" +
        "single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1" +
        "&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="

/**
 * Content-rating icons (NICAM/Kijkwijzer style): per-item ratings via
 * OGMediaItem.setContentRatings, shown at content start (after ads), fixed
 * top-right inline with the Cast button (the Kijkwijzer convention — not
 * repositionable). Defaults are SDK art; hosts supply licensed official
 * pictograms via ContentRating.Custom.
 */
@Composable
internal fun NicamDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember {
        OGPlayer.Builder(context)
            .setAdsProvider(ImaAdsProvider(context))
            .build()
    }
    val log = remember { EventLogState() }

    var age by remember { mutableStateOf(ContentRating.Age.SIXTEEN) }
    var descriptors by remember {
        mutableStateOf(setOf(ContentRating.Descriptor.VIOLENCE, ContentRating.Descriptor.FEAR))
    }
    var custom by remember { mutableStateOf(false) }
    var withPreroll by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        player.attachAdEventLogging(log)
        onDispose { player.release() }
    }

    LaunchedEffect(age, descriptors, custom, withPreroll) {
        val ratings = buildList {
            add(age)
            addAll(descriptors)
            if (custom) add(ContentRating.Custom(R.drawable.demo_custom_rating))
        }
        player.load(
            OGMediaItem.Builder("https://media.ogplayer.tv/tos/master.m3u8")
                .setStreamType(StreamType.VOD)
                .setTitle("NICAM demo")
                .setContentRatings(ratings)
                .setAdBreaks(if (withPreroll) AdTagConfig(PREROLL_TAG) else null)
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
                    ContentRating.Age.entries.forEach { option ->
                        FilterChip(
                            selected = age == option,
                            onClick = { age = option },
                            label = { Text(option.name) },
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    ContentRating.Descriptor.entries.forEach { option ->
                        FilterChip(
                            selected = option in descriptors,
                            onClick = {
                                descriptors = if (option in descriptors) {
                                    descriptors - option
                                } else {
                                    descriptors + option
                                }
                            },
                            label = { Text(option.name.take(4)) },
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                ) {
                    FilterChip(
                        selected = custom,
                        onClick = { custom = !custom },
                        label = { Text("Custom bitmap") },
                    )
                    FilterChip(
                        selected = withPreroll,
                        onClick = { withPreroll = !withPreroll },
                        label = { Text("With preroll") },
                    )
                }
                Text(
                    text = "Icons show at content start (after ads) for 5s. Defaults are " +
                        "SDK art — licensed hosts pass official NICAM pictograms via " +
                        "ContentRating.Custom.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
