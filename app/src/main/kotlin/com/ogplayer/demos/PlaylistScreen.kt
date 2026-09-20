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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogplayer.ads.ima.ImaAdsProvider
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.ads.AdTagConfig
import com.ogplayer.api.PlaybackListener
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig

/**
 * Playlist & "Up next": three short clips queue and auto-advance; a
 * countdown card appears in the lead window before each item ends (tap it
 * to skip immediately). Every knob has a mode chip: the lead time, the
 * card's text template, its colors and font — or no card at all (silent
 * advance). The card is deliberately config-only: no custom-view slot.
 */
// Google's public IMA sample tags — a skippable preroll (VAST) and a
// postroll-only ad rule (VMAP).
private const val SKIPPABLE_PREROLL =
    "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/" +
        "single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1" +
        "&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
private const val POSTROLL_ONLY =
    "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples" +
        "&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&ad_rule=1&output=vmap" +
        "&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue" +
        "&cust_params=sample_ar%3Dpostonly&correlator="

/** Ads are PER ITEM: with [withAds], clip 1 opens with a skippable preroll,
 *  clip 2 ends with a postroll (played BEFORE the queue advances), clip 3
 *  stays ad-free. */
private fun playlistItems(withAds: Boolean) = listOf(
    Triple("w169-01", "The old church", SKIPPABLE_PREROLL),
    Triple("w169-02", "Crossing the bridge", POSTROLL_ONLY),
    Triple("w169-03", "The machine waits", null),
).map { (file, title, adTag) ->
    OGMediaItem.Builder("https://media.ogplayer.tv/shorts/v3/$file.mp4")
        .setStreamType(StreamType.VOD)
        .setTitle(title)
        .setPosterUrl("https://media.ogplayer.tv/shorts/v3/$file.jpg")
        .apply { if (withAds && adTag != null) setAdBreaks(AdTagConfig(adTag)) }
        .build()
}

@Composable
internal fun PlaylistDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember {
        OGPlayer.Builder(context)
            .setAdsProvider(ImaAdsProvider(context))
            .build()
    }
    val log = remember { EventLogState() }

    // 0=default, 1=short lead, 2=custom text, 3=branded, 4=hidden, 5=with ads
    var mode by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // Playlist demo: just the queue lifecycle — no analytics noise.
        player.attachAdEventLogging(log)
        player.addListener(object : PlaybackListener {
            override fun onPlaylistItemChanged(index: Int, item: OGMediaItem) {
                log.add("onPlaylistItemChanged: #$index \u00b7 ${item.title}")
            }

            override fun onPlaylistItemSkipped(fromIndex: Int, toIndex: Int) {
                log.add("onPlaylistItemSkipped: #$fromIndex \u2192 #$toIndex")
            }

            override fun onPlaybackCompleted() = log.add("onPlaybackCompleted")

            override fun onError(error: com.ogplayer.api.OGPlayerError) =
                log.add("onError: $error")
        })
        onDispose { player.release() }
    }

    LaunchedEffect(mode) {
        log.add(
            if (mode == 5) "— loading playlist with per-item IMA tags —"
            else "— loading playlist (3 clips, 14s each) —",
        )
        player.loadPlaylist(playlistItems(withAds = mode == 5))
    }

    val uiConfig = remember(mode) {
        OGUiConfig.Builder()
            // Progressive clips: one audio track, no text tracks, no ladder —
            // buttons that would open an empty menu are hidden in every mode.
            .setShowSubtitleButton(false)
            .setShowAudioTrackButton(false)
            .setShowQualityButton(false)
            .apply {
                when (mode) {
                    1 -> setUpNextLeadSeconds(5)
                    2 -> setUpNextText("{title} starts in {seconds}s…")
                    3 -> {
                        setUpNextText("Up next · {title} · {seconds}")
                        setUpNextStyle(
                            backgroundColor = Color(0xE6F6C445),
                            textStyle = TextStyle(
                                color = Color(0xFF131313),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                            ),
                        )
                    }
                    4 -> setShowUpNext(false)
                }
            }
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
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    listOf(
                        "Default", "Lead 5s", "Custom text", "Branded style", "Hidden",
                        "With ads",
                    ).forEachIndexed { i, label ->
                        FilterChip(
                            selected = mode == i,
                            onClick = { mode = i },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    text = when (mode) {
                        1 -> "setUpNextLeadSeconds(5) — the card appears 5 seconds " +
                            "before the end instead of the default 10."
                        2 -> "setUpNextText(\"{title} starts in {seconds}s…\") — your " +
                            "copy, any language; {seconds} and {title} are substituted."
                        3 -> "setUpNextStyle(background, textStyle) — brand the card: " +
                            "accent background, serif font, dark text."
                        4 -> "setShowUpNext(false) — no card at all; the playlist " +
                            "still auto-advances silently."
                        5 -> "Ads are per item (setAdBreaks on each OGMediaItem): " +
                            "clip 1 opens with a skippable preroll, clip 2 ends with " +
                            "a postroll that plays before the queue advances, clip 3 " +
                            "is ad-free."
                        else -> "Three 14-second clips auto-advance; the \"Up next\" " +
                            "card counts down during the last 10 seconds — tap it to " +
                            "skip immediately. Changing modes reloads the playlist. " +
                            "The clips are progressive MP4s with one audio track, no " +
                            "subtitles and no ladder, so the subtitle, audio and " +
                            "quality buttons are hidden."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
