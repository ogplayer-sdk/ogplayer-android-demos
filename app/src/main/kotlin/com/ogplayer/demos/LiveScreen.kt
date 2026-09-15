package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig

/**
 * Live showcase, both flavors: LIVE (locked to the edge, no seeking) and
 * LIVE_DVR (seekable window). Scrub behind the DVR edge and watch
 * onLiveEdgeChanged flip in the log; tap the LIVE chip to jump back.
 */
@Composable
internal fun LiveDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember { OGPlayer.Builder(context).build() }
    val log = remember { EventLogState() }
    // The stream carries one audio language and no text tracks — the two
    // buttons that would open an empty menu are hidden; playback rate has no
    // meaning at the live edge, so the speed button goes too.
    val uiConfig = remember {
        OGUiConfig.Builder()
            .setShowSubtitleButton(false)
            .setShowAudioTrackButton(false)
            .setShowSpeedButton(false)
            .build()
    }
    var dvr by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log)
        onDispose { player.release() }
    }

    LaunchedEffect(dvr) {
        log.add(if (dvr) "— loading LIVE_DVR (seekable window) —" else "— loading LIVE (locked to edge) —")
        player.load(
            OGMediaItem.Builder(
                "https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8",
            )
                .setStreamType(if (dvr) StreamType.LIVE_DVR else StreamType.LIVE)
                .setTitle(if (dvr) "Live DVR demo" else "Live demo")
                .build(),
            // Live starts immediately — no reason to sit paused at the edge.
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
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    FilterChip(
                        selected = !dvr,
                        onClick = { dvr = false },
                        label = { Text("Live (locked to edge)") },
                    )
                    FilterChip(
                        selected = dvr,
                        onClick = { dvr = true },
                        label = { Text("Live DVR (seekable)") },
                    )
                    if (dvr) {
                        // The imperative twin of tapping the LIVE chip in the
                        // chrome — hosts can jump back programmatically.
                        FilterChip(
                            selected = false,
                            onClick = { player.seekToLiveEdge() },
                            label = { Text("To live edge") },
                        )
                    }
                }
                Text(
                    text = (if (dvr) {
                        "LIVE_DVR — scrub behind the edge, then tap the LIVE " +
                            "chip or \"To live edge\" (seekToLiveEdge())"
                    } else {
                        "LIVE — no seeking: no progress bar, chip always at the edge"
                    }) + " One audio language and no subtitles: the subtitle and " +
                        "audio buttons are hidden (showSubtitleButton / " +
                        "showAudioTrackButton = false); speed goes too — no rate changes " +
                        "at the live edge (showSpeedButton = false).",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
