package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView

/**
 * Starts directly in fullscreen: OGPlayerView(startInFullscreen = true)
 * invokes the fullscreen handler on first composition — landscape, bars
 * hidden, playback rolling. The fullscreen button is intercepted via
 * onFullscreenExitRequest and closes the screen.
 */
@Composable
internal fun StartFullscreenDemo(onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember { OGPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.load(
            OGMediaItem.Builder(
                "https://media.ogplayer.tv/tos/master.m3u8",
            )
                .setStreamType(StreamType.VOD)
                .setTitle("Starts in fullscreen")
                .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                .build(),
            playWhenReady = false,
        )
        onDispose { player.release() }
    }

    Surface(Modifier.fillMaxSize()) {
        OGPlayerView(
            player = player,
            modifier = Modifier.fillMaxSize(),
            fullscreenHandler = fullscreenHandler,
            startInFullscreen = true,
            autoFullscreenOnRotate = true,
            onFullscreenExitRequest = onClose,
        )
    }
}
