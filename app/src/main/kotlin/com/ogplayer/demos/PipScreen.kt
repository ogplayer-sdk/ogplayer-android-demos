package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityPipHandler
import com.ogplayer.ui.CustomAction
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig
import com.ogplayer.ui.PipListener

/**
 * Picture-in-picture: no button in the chrome — the developer decides.
 * Passing an [ActivityPipHandler] enables PiP; leaving the app (Home /
 * recents) while playing auto-enters it, and closing the PiP window
 * pauses playback.
 * While the window is up the SDK strips all chrome (and this screen hides
 * its own panel — in PiP the whole activity IS the little window).
 */
@Composable
internal fun PipDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var autoEnter by remember { mutableStateOf(true) }
    var inPip by remember { mutableStateOf(false) }
    val player = remember { OGPlayer.Builder(context).build() }
    val pipHandler = remember { ActivityPipHandler(activity, player) }
    val log = remember { EventLogState() }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit
        // left behind — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        val pipListener = PipListener { event -> log.add("pip: $event") }
        pipHandler.addPipListener(pipListener)
        // The demo panel must vanish while the whole activity is the PiP
        // window; the SDK handles its own chrome the same way.
        val uiListener = PipListener { inPip = pipHandler.isInPip }
        pipHandler.addPipListener(uiListener)
        player.load(
            OGMediaItem.Builder("https://media.ogplayer.tv/tos/master.m3u8")
                .setStreamType(StreamType.VOD)
                .setTitle("Tears of Steel")
                .build(),
        )
        onDispose {
            pipHandler.removePipListener(pipListener)
            pipHandler.removePipListener(uiListener)
            // A non-released handler leaves the single activity armed to
            // auto-enter PiP from the other demo screens.
            pipHandler.release()
            player.release()
        }
    }

    val uiConfig = remember { OGUiConfig.Builder().build() }

    Surface(Modifier.fillMaxSize()) {
        Column(if (inPip) Modifier.fillMaxSize() else Modifier.safeDrawingPadding()) {
            OGPlayerView(
                player = player,
                modifier = if (inPip) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                },
                pipHandler = pipHandler,
                autoEnterPipOnBackground = autoEnter,
                uiConfig = uiConfig,
            )
            if (!inPip) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Checkbox(checked = autoEnter, onCheckedChange = { autoEnter = it })
                    Text(
                        "Auto-enter PiP when leaving the app",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "Press Home while playing — PiP enters " +
                        "automatically. The chrome is stripped in the little " +
                        "window, its play/pause remote action drives the " +
                        "player, and closing it (✕) pauses playback — all " +
                        "logged below.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
