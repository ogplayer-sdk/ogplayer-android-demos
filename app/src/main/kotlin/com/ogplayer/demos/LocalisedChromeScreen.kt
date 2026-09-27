package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGStrings
import com.ogplayer.ui.OGUiConfig

/**
 * Localised chrome: every text the player shows or speaks comes from one flat
 * `key → text` map — here a complete Dutch one — handed to
 * `OGUiConfig.Builder().setStrings(OGStrings.fromMap(map))`. The keys are the
 * same on every OGPlayer platform, so the one map localises them all. The map
 * lives in the app; the SDK ships English and never guesses a language from
 * the device.
 */
private val DUTCH: Map<String, String> = mapOf(
    "play" to "Afspelen",
    "pause" to "Pauzeren",
    "replay" to "Opnieuw afspelen",
    "seekForward" to "{seconds} seconden vooruit",
    "seekBackward" to "{seconds} seconden terug",
    "next" to "Volgende",
    "previous" to "Vorige",
    "volume" to "Volume",
    "mute" to "Dempen",
    "unmute" to "Dempen opheffen",
    "enterFullscreen" to "Volledig scherm",
    "exitFullscreen" to "Volledig scherm sluiten",
    "seekBar" to "Afspeelpositie",
    "customAction" to "Actie {n}",
    "subtitles" to "Ondertiteling",
    "subtitlesOff" to "Uit",
    "audio" to "Audio",
    "playbackSpeed" to "Afspeelsnelheid",
    "speedNormal" to "Normaal",
    "speedValue" to "{speed}×",
    "quality" to "Videokwaliteit",
    "qualityAuto" to "Automatisch",
    "qualityHeight" to "{height}p",
    "qualityBitrate" to "{kbps} kbps",
    "qualityAdaptive" to "adaptief",
    "audioDefault" to "Standaard",
    "trackUnknown" to "Onbekend",
    "audioFallback" to "Audio {n}",
    "subtitlesFallback" to "Ondertiteling {n}",
    "audioChannels" to "{name} · {channels} kanalen",
    "live" to "LIVE",
    "goLive" to "Naar live",
    "upNext" to "Volgende over {seconds}",
    "playNext" to "Volgende afspelen: {title}",
    "nextVideo" to "volgende video",
    "ad" to "RECLAME",
    "adPod" to "{index} van {count}",
    "learnMore" to "Meer informatie",
    "skipAd" to "Advertentie overslaan",
    "skipIn" to "Overslaan over {seconds}",
    "pauseAd" to "Advertentie pauzeren",
    "resumeAd" to "Advertentie hervatten",
    "adBlockedTitle" to "Advertenties worden geblokkeerd",
    "adBlockedText" to "Deze video wordt aangeboden met advertenties, maar je adblocker houdt ze tegen.",
    "adBlockedTextHard" to "Deze video is alleen beschikbaar met advertenties. Schakel je adblocker uit en laad de pagina opnieuw.",
    "adBlockedDismiss" to "Begrepen",
    "adBlockedReload" to "Uitgeschakeld — opnieuw laden",
    "dismiss" to "Sluiten",
    "errorGeneric" to "Afspeelfout {code}",
    "retry" to "Opnieuw proberen",
    "downloading" to "Downloaden",
    "downloadingItemsOne" to "1 item downloaden",
    "downloadingItems" to "{count} items downloaden",
    "cast" to "Casten",
    "castConnecting" to "Verbinden met cast-apparaat",
    "casting" to "Bezig met casten",
    "castConnectingStatus" to "Verbinden…",
    "castingTo" to "Casten naar {device}",
    "airPlay" to "AirPlay",
    "airPlayTo" to "AirPlay — {device}",
    "pipPlaying" to "Speelt af in beeld-in-beeld",
    "sponsored" to "Gesponsord",
)

private enum class LocalisedMode(val label: String) {
    VOD("On demand"),
    LIVE("Live DVR"),
    ERROR("Error overlay"),
}

/** A URL that never loads — the errors demo's pattern, for the overlay copy. */
private const val MISSING_STREAM = "https://media.ogplayer.tv/tos/does-not-exist.m3u8"

@Composable
internal fun LocalisedChromeDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember { OGPlayer.Builder(context).build() }
    val dutch = remember { OGStrings.fromMap(DUTCH) }
    var mode by remember { mutableStateOf(LocalisedMode.VOD) }

    val uiConfig = remember(mode) {
        OGUiConfig.Builder()
            .setStrings(dutch)
            .apply {
                when (mode) {
                    LocalisedMode.VOD -> Unit
                    // The live stream carries no subtitles and one audio language.
                    LocalisedMode.LIVE -> setShowSubtitleButton(false).setShowAudioTrackButton(false)
                    // Nothing plays: no timeline, rate or tracks to act on.
                    LocalisedMode.ERROR -> setShowSeekButtons(false)
                        .setShowProgressBar(false)
                        .setShowTimeLabels(false)
                        .setShowSpeedButton(false)
                        .setShowQualityButton(false)
                        .setShowAudioTrackButton(false)
                        .setShowSubtitleButton(false)
                }
            }
            .build()
    }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { player.release() }
    }

    LaunchedEffect(mode) {
        when (mode) {
            // Tears of Steel: five subtitle languages and three audio tracks in one
            // manifest — every menu has rows to show.
            LocalisedMode.VOD -> player.load(
                OGMediaItem.Builder("https://media.ogplayer.tv/tos/master.m3u8")
                    .setStreamType(StreamType.VOD)
                    .setTitle("Tears of Steel")
                    .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                    .build(),
                playWhenReady = false,
            )
            LocalisedMode.LIVE -> player.load(
                OGMediaItem.Builder("https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8")
                    .setStreamType(StreamType.LIVE_DVR)
                    .setTitle("Live")
                    .build(),
                playWhenReady = true,
            )
            LocalisedMode.ERROR -> player.load(
                OGMediaItem.Builder(MISSING_STREAM).setTitle("Tears of Steel").build(),
            )
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
            // Fullscreen means the player and nothing else.
            if (!isFullscreen) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    LocalisedMode.entries.forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { mode = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text(
                    text = when (mode) {
                        LocalisedMode.VOD ->
                            "One Dutch strings map relabels the whole chrome: open the " +
                                "subtitle, audio, speed and quality menus, and TalkBack reads " +
                                "the Dutch names. setStrings(OGStrings.fromMap(map)) — the same " +
                                "keys on every OGPlayer platform."
                        LocalisedMode.LIVE ->
                            "Live with a seekable window: the LIVE chip and the behind-live " +
                                "readout; the chip's action is spoken as \"Naar live\"."
                        LocalisedMode.ERROR ->
                            "A stream that never loads: the overlay shows errorGeneric " +
                                "(\"Afspeelfout {code}\") and the Retry button reads \"Opnieuw " +
                                "proberen\". setErrorMessageProvider and setRetryButtonLabel " +
                                "still win when set."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
