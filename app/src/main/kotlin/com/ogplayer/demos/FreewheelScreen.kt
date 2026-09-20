package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ogplayer.ads.freewheel.FreewheelAdsProvider
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView

/**
 * FreeWheel demo — bring your own FreeWheel SDK.
 *
 * NOTE FOR DEVELOPERS: FreeWheel's AdManager SDK is licensed to FreeWheel
 * customers and is NOT bundled with OGPlayer. To run this demo:
 *
 *  1. Obtain `FWAdManager.aar` from your FreeWheel account (MRM support
 *     portal or your FreeWheel account manager).
 *  2. Copy it to `app/libs/FWAdManager.aar` and add to `dependencies`:
 *     `implementation(files("libs/FWAdManager.aar"))`
 *  3. Fill in your network configuration in [DemoFwConfig].
 *
 * The demo then exercises the native slot-provider path: FreeWheel request,
 * SDK-owned ad chrome (yellow bar, countdown, skip/learn-more), prerolls
 * before content, playhead-driven midrolls, postrolls, and error fallback
 * to content. (A FreeWheel network can alternatively be reached as a plain
 * VMAP tag through the IMA provider — `AdTagConfig` with your network's
 * `/ad/g/1` URL — with IMA rendering the ads instead.)
 */
@Composable
internal fun FreewheelDemo() {
    val fwSdkPresent = remember {
        runCatching { Class.forName("tv.freewheel.ad.AdManager") }.isSuccess
    }
    if (!fwSdkPresent || !DemoFwConfig.isConfigured) {
        FreewheelSetupNotice(fwSdkPresent)
        return
    }
    FreewheelPlayer()
}

/** What is missing and how to supply it (see the file-header steps). */
@Composable
private fun FreewheelSetupNotice(fwSdkPresent: Boolean) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("FreeWheel setup needed", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (!fwSdkPresent) {
                    "The FreeWheel AdManager SDK is not in this build. It is " +
                        "licensed to FreeWheel customers and not bundled: add your " +
                        "FWAdManager.aar to app/libs/ and the gradle dependency, " +
                        "then rebuild. See the notes at the top of FreewheelScreen.kt."
                } else {
                    "Add your FreeWheel network configuration (network id, ad " +
                        "server URL, profile, site section, video asset) in " +
                        "DemoFwConfig.kt — every value comes from your FreeWheel " +
                        "(MRM) account."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FreewheelPlayer() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember {
        OGPlayer.Builder(context)
            .setAdsProvider(FreewheelAdsProvider(activity))
            .build()
    }
    val log = remember { EventLogState() }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        player.attachAdEventLogging(log)
        onDispose { player.release() }
    }

    LaunchedEffect(Unit) {
        log.add("— loading with FreeWheel —")
        player.load(
            OGMediaItem.Builder(
                "https://media.ogplayer.tv/tos/master.m3u8",
            )
                .setStreamType(StreamType.VOD)
                .setTitle("FreeWheel demo — Tears of Steel")
                .setAdBreaks(DemoFwConfig.build(context))
                .build(),
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
                Text(
                    text = "Native FreeWheel provider — the SDK owns the full ad chrome " +
                        "(yellow bar, countdown, skip/learn-more); FreeWheel renders only " +
                        "the ad media. Note: emulator video decoders can stall FreeWheel's " +
                        "ad renderer (black ad, PLAY timeout, auto-advance); verify " +
                        "rendering on a device.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
