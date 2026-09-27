package com.ogplayer.demos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogplayer.ads.ima.ImaAdsProvider
import com.ogplayer.api.ContentRating
import com.ogplayer.api.DrmConfig
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.api.SubtitleSource
import com.ogplayer.api.ads.AdTagConfig
import com.ogplayer.ui.CustomAction
import com.ogplayer.ui.OGInputMode
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig

/**
 * Android TV / Google TV / Fire TV entry (LEANBACK_LAUNCHER): the same SDK
 * on the remote-control chrome. One switch does it —
 * `OGUiConfig.Builder().setInputMode(OGInputMode.REMOTE)` — the player then
 * walks its controls with the D-pad, seeks with Left/Right on the scrub bar,
 * and hands Back to this activity once its own chrome is down (the
 * [BackHandler] below pops to the launcher).
 *
 * Test hook: `adb shell am start -n com.ogplayer.demos/.MainTvActivity -e og.demo <route>`
 * deep-launches a scenario, like MainActivity.
 */
class MainTvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startRoute = intent.getStringExtra("og.demo") ?: "menu"
        // Test hook: `-e og.hide seek,progress,time,subtitles,audio,quality,speed,title`
        // turns the matching show* flags off (the phone demo's Controls switches).
        val hide = intent.getStringExtra("og.hide")?.split(',')?.map { it.trim() }?.toSet() ?: emptySet()
        // Test hook: `-e og.controlsTimeout 30000` lengthens the chrome's auto-hide
        // (slow TVs poll the UI slower than the 5 s default hides it).
        val controlsTimeoutMs = intent.getStringExtra("og.controlsTimeout")?.toLongOrNull()
        // Test hook: `-e og.adTag <url>` swaps the IMA tag on the ads route (e.g. Google's
        // single_preroll_skippable sample to look at skip handling on a TV).
        val adTag = intent.getStringExtra("og.adTag")
        // Test hook: `-e og.skipIcons custom` swaps the playlist previous/next glyphs for the
        // demo's brand discs (setSkipPreviousIcon / setSkipNextIcon).
        val customSkipIcons = intent.getStringExtra("og.skipIcons") == "custom"
        setContent { TvApp(startRoute, hide, controlsTimeoutMs, adTag, customSkipIcons) }
    }
}

private data class TvDemo(val route: String, val title: String, val description: String)

private val tvDemos = listOf(
    TvDemo("vod", "VOD playback", "Adaptive HLS — D-pad chrome, OK, Back, key-driven scrub with storyboard preview."),
    TvDemo("live", "Live & DVR", "Live edge chip, DVR window scrubbing, live gating of the chrome."),
    TvDemo("drm", "Multi-DRM", "DASH + Widevine with a rotating-token provider."),
    TvDemo("uhd", "4K / UHD", "Big Buck Bunny, 180p → 2160p — the quality menu must offer 2160p on a 4K panel."),
    TvDemo("tracks", "Subtitles & audio", "Sideloaded WebVTT and the manifest's audio tracks, ten-foot menus."),
    TvDemo("playlist", "Playlist & up next", "Three short clips; the Up-next card is focusable — OK skips ahead."),
    TvDemo("ads", "Ads (IMA)", "Pre/mid/post-roll VMAP on the TV chrome; play/pause key drives the ad."),
    TvDemo("nicam", "Content ratings", "Kijkwijzer age + descriptor icons at program start."),
    TvDemo("customactions", "Custom action icons", "Host icons in the chrome, reachable with the D-pad."),
    TvDemo("errormessages", "Error overlay", "A dead stream: the Retry button takes focus."),
)

@Composable
private fun TvApp(
    startRoute: String,
    hide: Set<String> = emptySet(),
    controlsTimeoutMs: Long? = null,
    adTag: String? = null,
    customSkipIcons: Boolean = false,
) {
    var route by remember { mutableStateOf(startRoute) }
    // Back with the player's chrome DOWN is not consumed by the SDK — it
    // lands here and returns to the launcher.
    BackHandler(enabled = route != "menu") { route = "menu" }
    if (route == "menu") TvLauncher(onOpen = { route = it }) else TvPlayerScreen(route, hide, controlsTimeoutMs, adTag, customSkipIcons)
}

@Composable
private fun TvLauncher(onOpen: (String) -> Unit) {
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRow.requestFocus() }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
            .semantics { testTagsAsResourceId = true }
            .testTag("tv_menu")
            .padding(horizontal = 96.dp, vertical = 54.dp), // 5 % title-safe area
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("OG", color = Color(0xFFF6C445), fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Player", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text("TV", color = Color.White.copy(alpha = 0.6f), fontSize = 36.sp)
        }
        Text(
            "Android TV · Google TV · Fire TV — remote-control chrome",
            color = Color.White.copy(alpha = 0.48f), fontSize = 20.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(36.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(tvDemos) { index, demo ->
                var focused by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(12.dp)
                Row(
                    modifier = Modifier
                        .testTag("tv_row_${demo.route}")
                        .fillMaxWidth(0.6f)
                        .then(if (index == 0) Modifier.focusRequester(firstRow) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        // Focus is a fill, as in the SDK's own TV menus — a thick frame reads heavy
                        // after a 4K panel upscales the 1080p canvas.
                        .background(if (focused) Color(0x3DF6C445) else Color(0x0BFFFFFF), shape)
                        .border(1.dp, if (focused) Color(0x80F6C445) else Color(0x12FFFFFF), shape)
                        .clickable { onOpen(demo.route) }
                        .focusable()
                        .padding(horizontal = 26.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(56.dp).background(Color(0x1FF6C445), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("▶", color = Color(0xFFF6C445), fontSize = 22.sp) }
                    Spacer(Modifier.width(22.dp))
                    Column {
                        Text(demo.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        Text(demo.description, color = Color.White.copy(alpha = 0.48f), fontSize = 18.sp)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "▲ ▼ choose   OK open   Back exit",
            color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp,
        )
    }
}

private const val TOS = "https://media.ogplayer.tv/tos/master.m3u8"
private const val TOS_STORYBOARD = "https://media.ogplayer.tv/tos/storyboard/storyboard.vtt"
// Our own Big Buck Bunny ladder (180p → 2160p, H.264, one English audio track): the 4K / UHD
// scenario only — every other scenario plays Tears of Steel. No storyboard exists for Bunny.
private const val BBB_UHD = "https://media.ogplayer.tv/bbb/master.m3u8"
private const val LIVE_URL = "https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8"
private const val VMAP_PRE_MID_POST =
    "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples" +
        "&sz=640x480&cust_params=sample_ar%3Dpremidpost&ciu_szs=300x250&gdfp_req=1&ad_rule=1" +
        "&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
// Public multi-DRM test vector (CMAF, cbcs, single key) and its published token.
private const val DRM_TOKEN =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ewogICJ2ZXJzaW9uIjogMSwKICAiY29tX2tleV9pZCI6ICI2OWU1NDA4OC1lOWUwLTQ1MzAtOGMxYS0xZWI2ZGNkMGQxNGUiLAogICJtZXNzYWdlIjogewogICAgInR5cGUiOiAiZW50aXRsZW1lbnRfbWVzc2FnZSIsCiAgICAidmVyc2lvbiI6IDIsCiAgICAibGljZW5zZSI6IHsKICAgICAgImFsbG93X3BlcnNpc3RlbmNlIjogdHJ1ZQogICAgfSwKICAgICJjb250ZW50X2tleXNfc291cmNlIjogewogICAgICAiaW5saW5lIjogWwogICAgICAgIHsKICAgICAgICAgICJpZCI6ICIzMDJmODBkZC00MTFlLTQ4ODYtYmNhNS1iYjFmODAxOGEwMjQiLAogICAgICAgICAgImVuY3J5cHRlZF9rZXkiOiAicm9LQWcwdDdKaTFpNDNmd3YremZ0UT09IiwKICAgICAgICAgICJ1c2FnZV9wb2xpY3kiOiAiUG9saWN5IEEiCiAgICAgICAgfQogICAgICBdCiAgICB9LAogICAgImNvbnRlbnRfa2V5X3VzYWdlX3BvbGljaWVzIjogWwogICAgICB7CiAgICAgICAgIm5hbWUiOiAiUG9saWN5IEEiLAogICAgICAgICJwbGF5cmVhZHkiOiB7CiAgICAgICAgICAibWluX2RldmljZV9zZWN1cml0eV9sZXZlbCI6IDE1MCwKICAgICAgICAgICJwbGF5X2VuYWJsZXJzIjogWwogICAgICAgICAgICAiNzg2NjI3RDgtQzJBNi00NEJFLThGODgtMDhBRTI1NUIwMUE3IgogICAgICAgICAgXQogICAgICAgIH0KICAgICAgfQogICAgXQogIH0KfQ._NfhLVY7S6k8TJDWPeMPhUawhympnrk6WAZHOVjER6M"

@Composable
private fun TvPlayerScreen(
    route: String,
    hide: Set<String> = emptySet(),
    controlsTimeoutMs: Long? = null,
    adTag: String? = null,
    customSkipIcons: Boolean = false,
) {
    val context = LocalContext.current
    val log = remember { EventLogState() }
    val player = remember(route) {
        OGPlayer.Builder(context)
            .apply { if (route == "ads") setAdsProvider(ImaAdsProvider(context)) }
            .build()
    }
    val uiConfig = remember(route) {
        OGUiConfig.Builder()
            .setInputMode(OGInputMode.REMOTE)
            .setMediaSessionEnabled(true)
            .apply {
                controlsTimeoutMs?.let { setControlsTimeoutMs(it) }
                if ("seek" in hide) setShowSeekButtons(false)
                if ("progress" in hide) setShowProgressBar(false)
                if ("time" in hide) setShowTimeLabels(false)
                if ("subtitles" in hide) setShowSubtitleButton(false)
                if ("audio" in hide) setShowAudioTrackButton(false)
                if ("quality" in hide) setShowQualityButton(false)
                if ("speed" in hide) setShowSpeedButton(false)
                if ("title" in hide) setShowTitle(false)
                if (customSkipIcons) {
                    // Brand discs (drawn as delivered, not tinted) in place of the white arrows.
                    setSkipPreviousIcon(R.drawable.ic_demo_skip_previous_brand)
                    setSkipNextIcon(R.drawable.ic_demo_skip_next_brand)
                }
                if (route == "customactions") {
                    setCustomActions(
                        listOf(
                            // Monochrome glyphs (the chrome tints custom actions to its foreground colour).
                            CustomAction(R.drawable.og_demo_action_star, "Custom action 1") { log.add("custom action 1") },
                            CustomAction(R.drawable.og_demo_action_share, "Custom action 2") { log.add("custom action 2") },
                        ),
                    )
                }
            }
            .build()
    }
    DisposableEffect(player) {
        player.attachEventLogging(log, includeProgress = false)
        when (route) {
            "uhd" -> player.load(
                OGMediaItem.Builder(BBB_UHD).setTitle("Big Buck Bunny — 4K ladder").build(),
            )
            "live" -> player.load(
                OGMediaItem.Builder(LIVE_URL).setStreamType(StreamType.LIVE_DVR).setTitle("Live & DVR").build(),
            )
            "drm" -> player.load(
                OGMediaItem.Builder("https://media.axprod.net/TestVectors/Cmaf/protected_1080p_h264_cbcs/manifest.mpd")
                    .setStreamType(StreamType.VOD)
                    .setTitle("Multi-DRM demo (encrypted)")
                    .setDrm(
                        DrmConfig.Builder("https://drm-widevine-licensing.axprod.net/AcquireLicense")
                            .setTokenProvider("X-AxDRM-Message") { DRM_TOKEN }
                            .build(),
                    )
                    .build(),
            )
            "tracks" -> player.load(
                OGMediaItem.Builder(TOS)
                    .setTitle("Tears of Steel — sideloaded VTT")
                    .setSideloadedSubtitles(
                        listOf(
                            SubtitleSource("asset:///tears_of_steel_en.vtt", "en", "English", isDefault = true),
                            SubtitleSource("asset:///tears_of_steel_de.vtt", "de", "Deutsch"),
                            // The phone demo's positioned file: line / position / align / size cues.
                            SubtitleSource("asset:///test_cue_settings.vtt", "en", "Positioned (line/position cues)"),
                        ),
                    )
                    .build(),
            )
            // The phone demo's three 14 s clips: the Up-next card shows a few
            // seconds into each one and the queue auto-advances.
            "playlist" -> player.loadPlaylist(
                listOf("w169-01" to "The old church", "w169-02" to "Crossing the bridge", "w169-03" to "The machine waits")
                    .map { (file, title) ->
                        OGMediaItem.Builder("https://media.ogplayer.tv/shorts/v3/$file.mp4")
                            .setStreamType(StreamType.VOD)
                            .setTitle(title)
                            .setPosterUrl("https://media.ogplayer.tv/shorts/v3/$file.jpg")
                            .build()
                    },
            )
            "ads" -> player.load(
                OGMediaItem.Builder("https://media.ogplayer.tv/tos-clip-60s.mp4")
                    .setTitle("Ads on TV")
                    .setAdBreaks(AdTagConfig(adTag ?: VMAP_PRE_MID_POST))
                    .build(),
            )
            "nicam" -> player.load(
                OGMediaItem.Builder(TOS)
                    .setTitle("Content ratings")
                    .setContentRatings(
                        listOf(ContentRating.Age.TWELVE, ContentRating.Descriptor.VIOLENCE, ContentRating.Descriptor.FEAR),
                    )
                    .build(),
            )
            "errormessages" -> player.load(
                OGMediaItem.Builder("https://media.ogplayer.tv/does-not-exist/master.m3u8").setTitle("Error").build(),
            )
            // Storyboard VTT: the remote's key-seek shows the preview above the bar.
            else -> player.load(
                OGMediaItem.Builder(TOS).setTitle("Tears of Steel").setThumbnailTrack(TOS_STORYBOARD).build(),
            )
        }
        onDispose { player.release() }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        OGPlayerView(player = player, uiConfig = uiConfig, modifier = Modifier.fillMaxSize())
    }
}
