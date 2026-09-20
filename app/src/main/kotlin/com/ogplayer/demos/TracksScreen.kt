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
import com.ogplayer.api.SubtitleSource
import com.ogplayer.api.SubtitleStyle
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.api.VolumeControlMode
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView

/**
 * Track selection showcase: the Tears of Steel ladder carries several
 * audio languages and subtitle tracks. The player controls show an audio
 * chooser (speaker icon, appears when more than one audio track exists)
 * and the CC subtitle chooser — both open a selection menu.
 */
private enum class SubtitleDemoSource(val label: String, val short: String) {
    EMBEDDED("Embedded (in manifest)", "Embedded"),
    MULTI_AUDIO("Multi-audio (stereo · 5.1 · M&E)", "Multi-audio"),
    SIDELOADED("Sideloaded VTT", "Sideloaded VTT"),
    POSITIONED("Positioned VTT (local file, line/position cues)", "Positioned VTT"),
}

/** Subtitle text-size presets → SubtitleStyle.textSizeFraction (default × scale). */
private val subtitleSizeOptions = listOf(
    "Small" to 0.8f,
    "Default" to 1.0f,
    "Large" to 1.4f,
    "X-Large" to 1.8f,
)

@Composable
internal fun TracksDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember { OGPlayer.Builder(context).build() }
    val log = remember { EventLogState() }

    var source by remember { mutableStateOf(SubtitleDemoSource.EMBEDDED) }
    var volumeMode by remember { mutableStateOf(VolumeControlMode.DEVICE) }
    var textScale by remember { mutableStateOf(1.0f) }
    var captionSerif by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        onDispose { player.release() }
    }

    LaunchedEffect(source) {
        log.add("— loading ${source.label} —")
        val item = when (source) {
            // Tears of Steel with three real audio tracks in one manifest — the
            // audio-menu showcase.
            SubtitleDemoSource.MULTI_AUDIO ->
                OGMediaItem.Builder(
                    "https://media.ogplayer.tv/tos/master.m3u8",
                )
                    .setStreamType(StreamType.VOD)
                    .setTitle("Tears of Steel — three audio tracks")
                    .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                    .build()

            // Subtitles delivered inside the DASH manifest: nothing to set,
            // the player discovers them as textTracks automatically. Same
            // movie as the sideloaded case, with five subtitle languages
            // muxed into the manifest. ((CC) Blender Foundation)
            SubtitleDemoSource.EMBEDDED ->
                OGMediaItem.Builder(
                    "https://media.ogplayer.tv/tos/master.m3u8",
                )
                    .setStreamType(StreamType.VOD)
                    .setTitle("Tears of Steel — embedded subtitles (5 languages)")
                    .build()

            // Local asset VTT exercising every cue setting: line/position/
            // align/size/vertical — shows authored positioning being kept
            // while the SDK's title-safe margins keep cues off the edges.
            SubtitleDemoSource.POSITIONED ->
                OGMediaItem.Builder(
                    "https://media.ogplayer.tv/tos/master.m3u8",
                )
                    .setStreamType(StreamType.VOD)
                    .setTitle("Tears of Steel — positioned local VTT")
                    .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                    .setSideloadedSubtitles(
                        listOf(
                            SubtitleSource(
                                url = "asset:///test_cue_settings.vtt",
                                language = "en",
                                label = "Cue settings test (local)",
                                isDefault = true,
                            ),
                        ),
                    )
                    .build()

            // Stream WITHOUT subtitles: the app attaches external VTT files —
            // the movie's real dialog subs in five languages, bundled locally.
            SubtitleDemoSource.SIDELOADED ->
                OGMediaItem.Builder(
                    "https://media.ogplayer.tv/tos/master.m3u8",
                )
                    .setStreamType(StreamType.VOD)
                    .setTitle("Tears of Steel — sideloaded VTT (5 languages)")
                    .setSideloadedSubtitles(
                        listOf(
                            SubtitleSource("asset:///tears_of_steel_en.vtt", "en", "English", isDefault = true),
                            SubtitleSource("asset:///tears_of_steel_de.vtt", "de", "Deutsch"),
                            SubtitleSource("asset:///tears_of_steel_fr.vtt", "fr", "Français"),
                            SubtitleSource("asset:///tears_of_steel_es.vtt", "es", "Español"),
                            SubtitleSource("asset:///tears_of_steel_ru.vtt", "ru", "Русский"),
                        ),
                    )
                    .build()
        }
        player.load(item, playWhenReady = false)
        // Embedded subs are off by default — auto-select the first so the
        // "Embedded" source actually shows subtitles (sideloaded/positioned are
        // auto-selected by the SDK via isDefault). Poll until tracks resolve.
        if (source == SubtitleDemoSource.EMBEDDED) {
            repeat(25) {
                kotlinx.coroutines.delay(300)
                val tracks = player.textTracks
                if (tracks.isNotEmpty() && tracks.none { it.isSelected }) {
                    player.selectTextTrack(tracks.first().id)
                    return@LaunchedEffect
                }
            }
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
                fullscreenHandler = fullscreenHandler,
                autoFullscreenOnRotate = true,
            )
            // Fullscreen means the player and nothing else.
            if (!isFullscreen) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    // Split: subtitle SOURCE on the left, subtitle SIZE on the
                    // right — the size drives SubtitleStyle.textSizeFraction
                    // (default × scale), applying to embedded + sideloaded cues.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Subtitle source", modifier = Modifier.padding(bottom = 2.dp))
                            SubtitleDemoSource.entries.forEach { option ->
                                FilterChip(
                                    selected = source == option,
                                    onClick = { source = option },
                                    label = { Text(option.short) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Font size", modifier = Modifier.padding(bottom = 2.dp))
                            subtitleSizeOptions.forEach { (label, scale) ->
                                FilterChip(
                                    selected = textScale == scale,
                                    onClick = {
                                        textScale = scale
                                        player.setSubtitleStyle(
                                            SubtitleStyle.Builder()
                                                .setTextSizeFraction(
                                                    SubtitleStyle.DEFAULT_TEXT_SIZE_FRACTION * scale,
                                                )
                                                .setTypeface(
                                                    if (captionSerif) android.graphics.Typeface.SERIF else null,
                                                )
                                                .build(),
                                        )
                                        log.add("subtitle size → ${scale}×")
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    // Caption typeface — SubtitleStyle.setTypeface(). Serif is
                    // used as the obviously-different "custom" font; a real app
                    // would pass its brand Typeface here.
                    Text("Caption font:", modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("System (default)" to false, "Serif (custom)" to true).forEach { (label, serif) ->
                            FilterChip(
                                selected = captionSerif == serif,
                                onClick = {
                                    captionSerif = serif
                                    player.setSubtitleStyle(
                                        SubtitleStyle.Builder()
                                            .setTextSizeFraction(
                                                SubtitleStyle.DEFAULT_TEXT_SIZE_FRACTION * textScale,
                                            )
                                            .setTypeface(
                                                if (serif) android.graphics.Typeface.SERIF else null,
                                            )
                                            .build(),
                                    )
                                    log.add("caption font → ${if (serif) "serif (custom)" else "system"}")
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        "Volume slider controls:",
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                    VolumeControlMode.entries.forEach { m ->
                        FilterChip(
                            selected = volumeMode == m,
                            onClick = { volumeMode = m; player.volumeControlMode = m },
                            label = {
                                Text(
                                    if (m == VolumeControlMode.DEVICE) {
                                        "Device volume — hardware buttons move the slider"
                                    } else {
                                        "Player only — hardware buttons ignored"
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
