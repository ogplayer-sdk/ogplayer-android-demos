package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView

/**
 * Orientation showcase: an embedded 16:9 player in a portrait page, using
 * the SDK's [ActivityFullscreenHandler] with `lockLandscape = true`:
 * tapping the fullscreen button rotates into landscape fullscreen, tapping
 * it again returns to the embedded portrait layout — even while the device
 * is still physically held in landscape. Sensor rotation stays functional
 * (the handler releases its orientation locks once the device aligns), so
 * rotating the device also enters/exits fullscreen.
 */
@Composable
internal fun OrientationDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    val player = remember { OGPlayer.Builder(context).build() }
    var isFullscreen by remember { mutableStateOf(false) }

    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }

    DisposableEffect(Unit) {
        // Sensor drives orientation; the player view reacts to it.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.load(
            OGMediaItem.Builder(
                "https://media.ogplayer.tv/tos/master.m3u8",
            )
                .setStreamType(StreamType.VOD)
                .setTitle("Orientation demo")
                .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                // Storyboard VTT: preview thumbnails appear while scrubbing.
                .setThumbnailTrack(
                    "https://media.ogplayer.tv/tos/storyboard/storyboard.vtt",
                )
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
            )
            if (!isFullscreen) {
                Text(
                    text = "Embedded 16:9 player.\n\n" +
                        "Tap the fullscreen button: the page rotates into " +
                        "landscape fullscreen. Tap it again (icon changes to " +
                        "“collapse”) and the player returns here, back in " +
                        "portrait. Rotating the device physically does the same.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
