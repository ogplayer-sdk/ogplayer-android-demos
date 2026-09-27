package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.cast.OGCastConnector
import androidx.compose.ui.Alignment
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig

/**
 * Chromecast showcase. The SDK renders its own cast button inside the
 * player controls (top right) because a CastConnector is attached; tapping
 * it opens the device picker (the SDK scans). Connecting hands playback
 * off at the current position; disconnecting resumes locally.
 *
 * The connector targets Google's default media receiver unless the app
 * declares com.ogplayer.cast.RECEIVER_APP_ID manifest meta-data; the
 * customData lambda below is delivered to the receiver with each load.
 *
 * Uses AppCompatActivity because the media route dialogs require it.
 */
@Composable
internal fun CastDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember {
        OGPlayer.Builder(context)
            .setCastConnector(
                OGCastConnector(context) {
                    // Example customData a custom receiver could consume.
                    mapOf("platform" to "android", "playSessionId" to "demo-session")
                },
            )
            .build()
    }
    // Demonstrates OGUiConfig.setShowCastButton: with the SDK's button
    // hidden, the attached connector still works — an app can render its own
    // cast entry point (e.g. a themed MediaRouteButton) instead.
    var showCastButton by remember { mutableStateOf(true) }
    val log = remember { EventLogState() }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log)
        player.load(
            OGMediaItem.Builder(
                "https://media.ogplayer.tv/tos/master.m3u8",
            )
                .setStreamType(StreamType.VOD)
                .setTitle("Cast demo")
                .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                .build(),
            playWhenReady = false,
        )
        onDispose { player.release() }
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
                uiConfig = OGUiConfig.Builder()
                    .setShowCastButton(showCastButton)
                    .build(),
            )
            // Fullscreen means the player and nothing else.
            if (!isFullscreen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = "SDK cast button",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = showCastButton, onCheckedChange = { showCastButton = it })
                }
                Text(
                    text = "Tap the player: the cast button sits top-right in the " +
                        "controls and opens the device picker (a real cast device is " +
                        "needed for the handoff). Toggle the switch to hide the SDK's " +
                        "button — the connector keeps working, so an app can bring " +
                        "its own cast UI instead.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
