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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Public LL-HLS test stream: ~1 s fMP4 parts (EXT-X-PART), blocking
 *  playlist reload and a PART-HOLD-BACK in EXT-X-SERVER-CONTROL, with a UTC
 *  clock burned into the picture. */
private const val LOW_LATENCY_URL =
    "https://stream.mux.com/v69RSHhFelSm4701snP22dYz2jICy4E4FUyk02rW4gxRM.m3u8"

/**
 * Low-latency HLS: the same OGMediaItem as any live stream — no flag, no
 * tuning. The engine reads the playlist's PART-HOLD-BACK and holds the
 * playhead that far behind the newest part, so the picture runs a few
 * seconds behind real time instead of the three target durations of
 * regular HLS. The test stream keeps only ~20 s of history, so it plays as
 * plain LIVE: the LIVE chip, no rewind bar, no seek buttons. The readout
 * under the player is one number — how far the playhead's program
 * date-time ([com.ogplayer.api.LiveInfo.playheadWallClockMs]) runs behind
 * this device's clock — polled twice a second.
 */
@Composable
internal fun LowLatencyLiveDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember { OGPlayer.Builder(context).build() }
    val log = remember { EventLogState() }
    // One audio rendition and no text tracks: the two buttons that would open
    // an empty menu are hidden, and speed goes too (no rate changes at the
    // live edge). The quality ladder stays.
    val uiConfig = remember {
        OGUiConfig.Builder()
            .setShowSubtitleButton(false)
            .setShowAudioTrackButton(false)
            .setShowSpeedButton(false)
            .build()
    }
    var behindMs by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        // Plain LIVE: a ~20 s window is no DVR — the chrome shows the LIVE
        // chip and no rewind bar.
        log.add("— loading LIVE low-latency HLS —")
        player.load(
            OGMediaItem.Builder(LOW_LATENCY_URL)
                .setStreamType(StreamType.LIVE)
                .setTitle("Low-latency live")
                .build(),
            playWhenReady = true,
        )
        onDispose { player.release() }
    }

    // Twice a second: how far the playhead's program date-time
    // (EXT-X-PROGRAM-DATE-TIME, via LiveInfo.playheadWallClockMs) runs behind
    // this device's clock — one number, the delay a viewer actually
    // experiences.
    LaunchedEffect(player) {
        while (isActive) {
            behindMs = player.liveInfo?.playheadWallClockMs?.let {
                (System.currentTimeMillis() - it).coerceAtLeast(0L)
            }
            delay(500)
        }
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
            if (!isFullscreen) {
                Text(
                    text = "Behind real time: " +
                        (behindMs?.let { String.format(Locale.US, "%.1f s", it / 1000.0) } ?: "—"),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    // The imperative twin of tapping the LIVE chip in the chrome.
                    FilterChip(
                        selected = false,
                        onClick = { player.seekToLiveEdge() },
                        label = { Text("To live edge") },
                    )
                }
                Text(
                    text = "Nothing to configure — the same item as any live stream. The engine " +
                        "holds the playlist's PART-HOLD-BACK and plays a few seconds behind real " +
                        "time; part of that delay is the stream's own, before it reaches the " +
                        "player. This test stream keeps only about 20 s of history, so it plays " +
                        "as plain live: no rewind bar. Pause, and \u201cTo live edge\u201d " +
                        "(seekToLiveEdge()) brings the delay back down. The clock painted into " +
                        "the picture comes from the stream's encoder and can differ from the " +
                        "stream's time stamps by a second or two.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Text(
                    text = "Content: Big Buck Bunny — (CC) Blender Foundation, via Mux's public " +
                        "low-latency test stream",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 6.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
