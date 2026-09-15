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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.ogplayer.cast.OGCastConnector
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig

/**
 * Control-visibility playground: every built-in control has an on/off
 * switch, plus the master hideAllControls(). OGUiConfig is Compose state,
 * so changes apply to the running player instantly — no reload needed.
 */

private const val SEEK_BUTTONS = "Seek buttons (±10s)"
private const val PROGRESS_BAR = "Progress bar"
private const val TIME_LABELS = "Time labels"
private const val SUBTITLES = "Subtitle button"
private const val AUDIO = "Audio-track button"
private const val QUALITY = "Quality button"
private const val SPEED = "Speed button"
private const val VOLUME = "Volume button"
private const val CAST = "Cast button"
private const val FULLSCREEN = "Fullscreen button"

private val ALL_CONTROLS = listOf(
    SEEK_BUTTONS, PROGRESS_BAR, TIME_LABELS,
    SUBTITLES, AUDIO, QUALITY, SPEED, VOLUME, CAST, FULLSCREEN,
)

@Composable
internal fun ControlsDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity) { isFullscreen = it }
    }
    val player = remember {
        OGPlayer.Builder(context)
            .setCastConnector(OGCastConnector(context))
            .build()
    }
    var hideAll by remember { mutableStateOf(false) }
    var liveContent by remember { mutableStateOf(false) }
    val toggles = remember { mutableStateMapOf<String, Boolean>().apply { ALL_CONTROLS.forEach { put(it, true) } } }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { player.release() }
    }

    androidx.compose.runtime.LaunchedEffect(liveContent) {
        player.load(
            if (liveContent) {
                OGMediaItem.Builder(
                    "https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8",
                )
                    .setStreamType(StreamType.LIVE_DVR)
                    .setTitle("Controls playground — live")
                    .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                    .build()
            } else {
                OGMediaItem.Builder(
                    "https://media.ogplayer.tv/tos/master.m3u8",
                )
                    .setStreamType(StreamType.VOD)
                    .setTitle("Controls playground")
                    .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                    .build()
            },
            playWhenReady = false,
        )
    }

    val uiConfig = if (hideAll) {
        OGUiConfig.Builder().hideAllControls().build()
    } else {
        OGUiConfig.Builder()
            .setShowSeekButtons(toggles.getValue(SEEK_BUTTONS))
            .setShowProgressBar(toggles.getValue(PROGRESS_BAR))
            .setShowTimeLabels(toggles.getValue(TIME_LABELS))
            .setShowSubtitleButton(toggles.getValue(SUBTITLES))
            .setShowAudioTrackButton(toggles.getValue(AUDIO))
            .setShowQualityButton(toggles.getValue(QUALITY))
            .setShowSpeedButton(toggles.getValue(SPEED))
            .setShowVolumeButton(toggles.getValue(VOLUME))
            .setShowCastButton(toggles.getValue(CAST))
            .setShowFullscreenButton(toggles.getValue(FULLSCREEN))
            .build()
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
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Flip a switch — the running player updates instantly " +
                            "(OGUiConfig is state; no reload). Play/pause and the " +
                            "LIVE chip are always visible by SDK design.",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    SwitchRow("Live content (shows LIVE chip)", liveContent, bold = true) {
                        liveContent = it
                    }
                    SwitchRow("hideAllControls()", hideAll, bold = true) { hideAll = it }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    ALL_CONTROLS.forEach { name ->
                        SwitchRow(
                            label = name,
                            checked = !hideAll && toggles.getValue(name),
                            enabled = !hideAll,
                        ) { toggles[name] = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    bold: Boolean = false,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
