package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogplayer.api.OGMediaItem
import com.ogplayer.ui.verticalfeed.OGVerticalFeedConfig
import com.ogplayer.ui.verticalfeed.OGVerticalFeedItem
import com.ogplayer.ui.verticalfeed.OGVerticalFeedView
import com.ogplayer.ui.verticalfeed.RailAction
import com.ogplayer.ui.verticalfeed.rememberOGVerticalFeedState

/**
 * Vertical feed. Portrait-only by design (no fullscreen/rotation API), so
 * the screen pins the activity to portrait while visible.
 *
 * Two modes:
 *  1. "OG vertical view" — the feed exactly as the SDK ships it: video,
 *     tap-to-pause, progress hairline. No title, no subtitle, no rail —
 *     the right rail is a placeholder the HOST fills.
 *  2. "Custom" — everything the host can add: title+subtitle with a custom
 *     font, custom play glyph, and up to six rail actions with labels
 *     (like with a live counter, share, mute wired to the feed's real
 *     mute state, and a ⋯ hook).
 */
@Composable
fun VerticalFeedDemo() {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    // null = chooser; 0 = OG vertical, 1 = custom, 2 = split text+video,
    // 3 = split video, 4/5 = error modes.
    // Fresh log per demo mode — the counter belongs to the mode you're in.
    var mode by remember { mutableStateOf<Int?>(null) }
    val analyticsLog = remember(mode) { EventLogState() }
    var showAnalytics by remember { mutableStateOf(false) }
    val feedState = rememberOGVerticalFeedState()
    androidx.activity.compose.BackHandler(enabled = mode != null) { mode = null }

    if (mode == null) {
        ModeChooser(onPick = { mode = it })
        return
    }
    val isCustom = mode == 1
    val isLetterbox = mode == 2

    // Host-side rail state: likes/shares per item — re-supplying the items
    // list with new RailActions is the supported update pattern.
    var liked by remember { mutableStateOf(setOf<Int>()) }
    var likeCounts by remember { mutableStateOf(mapOf(0 to 1240, 1 to 87, 3 to 356, 4 to 9, 5 to 2031)) }
    var shareCounts by remember { mutableStateOf(mapOf(0 to 61, 1 to 12, 3 to 44, 4 to 2, 5 to 118)) }

    val clips = listOf(
        Triple("The memory scan", "@tearsofsteel · Forty years on, they scan his memories of her — every one of them still intact. #scifi #amsterdam", 0),
        Triple("Forty years later", "@tearsofsteel · Old Thom returns to the ruined church where it all began. #shortfilm", 1),
        Triple("Sponsored — OGPlayer", "One player API — multi-DRM, ads, subtitles, casting. Free to evaluate at ogplayer.tv", 2),
        Triple("Rooftops of Amsterdam", "@tearsofsteel · The projection sweeps across the old city's rooftops. #vfx #blender", 3),
        Triple("Face to face", "@tearsofsteel · Thom and the machine that remembers him, alone in the ruins. #robots", 4),
        Triple("The final projection", "@tearsofsteel · He reaches out one last time — made with Blender, released CC-BY by the Blender Foundation. #ccby", 5),
    )

    // Landscape mode: 16:9 clips, letterboxed (FIT), text ABOVE the video in
    // the black band — plain config, no custom anything.
    val wideClips = listOf(
        Triple("The old church", "Amsterdam's canal belt, forty years on. The survivors kept the church exactly as it was on the night of the projection — every stone, every cable, every memory of her still wired into the walls.", 0),
        Triple("Crossing the bridge", "Thom walks the Oudezijds bridge one more time. The city looks ordinary — bicycles, canal houses, tourists — but the machines remember everything that happened here.", 1),
        Triple("The machine waits", "It has stood on the bridge for decades, silent and patient. Sixteen-by-nine footage sits letterboxed in the feed, and this text lives in the space above it.", 2),
    )
    val wideItems = wideClips.mapIndexed { wi, (title, subtitle, i) ->
        val n = "%02d".format(i + 1)
        OGVerticalFeedItem.Builder(
            OGMediaItem.Builder("https://media.ogplayer.tv/shorts/v3/w169-$n.mp4")
                .setTitle(title)
                .build(),
        )
            .setPosterUrl("https://media.ogplayer.tv/shorts/v3/w169-$n.jpg")
            .setTitle(title)
            .setSubtitle(subtitle)
            // Same rail as everywhere else — anchored inside the video section.
            .setRailActions(
                listOf(
                    RailAction(
                        iconRes = R.drawable.ic_demo_heart,
                        label = formatCount((likeCounts[wi] ?: 42) + if (wi + 10 in liked) 1 else 0),
                        isActive = wi + 10 in liked,
                        contentDescription = "Like",
                        onClick = { liked = if (wi + 10 in liked) liked - (wi + 10) else liked + (wi + 10) },
                    ),
                    RailAction(
                        iconRes = if (feedState.isMuted) R.drawable.ic_demo_volume_off else R.drawable.ic_demo_volume_on,
                        label = if (feedState.isMuted) "Unmute" else "Mute",
                        isActive = feedState.isMuted,
                        contentDescription = "Mute",
                        onClick = { feedState.isMuted = !feedState.isMuted },
                    ),
                ),
            )
            // Video fills the whole non-band region (FILL default); item 2
            // puts the text BELOW the video, the others ABOVE.
            .setTextPlacement(
                if (i == 1) {
                    com.ogplayer.ui.verticalfeed.TextPlacement.BELOW_VIDEO
                } else {
                    com.ogplayer.ui.verticalfeed.TextPlacement.ABOVE_VIDEO
                },
            )
            .build()
    }

    // Split video: two clips on one page — top + bottom, bottom muted.
    val dualItems = listOf(
        Pair("01" to "04", "Two sources, one page"),
        Pair("05" to "02", "Bottom owns the audio here"),
        Pair("06" to "03", "Bottom follows play/pause"),
    ).mapIndexed { i, (pair, title) ->
        val (top, bottom) = pair
        OGVerticalFeedItem.Builder(
            OGMediaItem.Builder("https://media.ogplayer.tv/shorts/v3/clip$top.mp4")
                .setTitle(title)
                .build(),
        )
            .setPosterUrl("https://media.ogplayer.tv/shorts/v3/clip$top.jpg")
            .setSecondaryMedia(
                OGMediaItem.Builder("https://media.ogplayer.tv/shorts/v3/clip$bottom.mp4").build(),
            )
            // Page 2 flips the audio to the bottom half (SplitAudioSource).
            .setSplitAudioSource(
                if (i == 1) {
                    com.ogplayer.ui.verticalfeed.SplitAudioSource.SECONDARY
                } else {
                    com.ogplayer.ui.verticalfeed.SplitAudioSource.PRIMARY
                },
            )
            .setTitle(title)
            // Same rail as the vertical view — callbacks and events are
            // identical on split-video pages.
            .setRailActions(
                listOf(
                    RailAction(
                        iconRes = R.drawable.ic_demo_heart,
                        label = formatCount((likeCounts[i] ?: 42) + if (i in liked) 1 else 0),
                        isActive = i in liked,
                        contentDescription = "Like",
                        onClick = { liked = if (i in liked) liked - i else liked + i },
                    ),
                    RailAction(
                        iconRes = R.drawable.ic_demo_share,
                        label = formatCount(shareCounts[i] ?: 7),
                        contentDescription = "Share",
                        onClick = {
                            shareCounts = shareCounts + (i to (shareCounts[i] ?: 7) + 1)
                            Toast.makeText(context, "Share tapped — your share sheet goes here", Toast.LENGTH_SHORT).show()
                        },
                    ),
                    RailAction(
                        iconRes = if (feedState.isMuted) R.drawable.ic_demo_volume_off else R.drawable.ic_demo_volume_on,
                        label = if (feedState.isMuted) "Unmute" else "Mute",
                        isActive = feedState.isMuted,
                        contentDescription = "Mute",
                        onClick = { feedState.isMuted = !feedState.isMuted },
                    ),
                    RailAction(
                        iconRes = R.drawable.ic_demo_more,
                        label = null,
                        contentDescription = "More",
                        onClick = {
                            Toast.makeText(context, "Your menu goes here", Toast.LENGTH_SHORT).show()
                        },
                    ),
                ),
            )
            .build()
    }

    // Error modes: posters resolve, streams 404 — every page fails on
    // purpose so the error surface (default or custom) shows.
    val errorItems = (1..3).map { n ->
        OGVerticalFeedItem.Builder(
            OGMediaItem.Builder("https://media.ogplayer.tv/shorts/v3/broken-clip0$n.mp4")
                .setTitle("Broken stream $n")
                .build(),
        )
            .setPosterUrl("https://media.ogplayer.tv/shorts/v3/clip0$n.jpg")
            .setTitle("Broken stream $n")
            .build()
    }

    val items = clips.map { (title, subtitle, i) ->
        val n = "%02d".format(i + 1)
        // The sponsored slot plays a real AD creative (server-inserted ad
        // items are how vertical-feed advertising works), not a movie clip.
        val mediaUrl = if (i == 2) {
            "https://media.ogplayer.tv/shorts/v3/ad-ogplayer.mp4"
        } else {
            "https://media.ogplayer.tv/shorts/v3/clip$n.mp4"
        }
        val posterUrl = if (i == 2) {
            "https://media.ogplayer.tv/shorts/v3/ad-ogplayer.jpg"
        } else {
            "https://media.ogplayer.tv/shorts/v3/clip$n.jpg"
        }
        OGVerticalFeedItem.Builder(
            OGMediaItem.Builder(mediaUrl)
                .setTitle(title)
                .build(),
        )
            .setPosterUrl(posterUrl)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setSponsored(i == 2)
            .setRailActions(
                if (!isCustom || i == 2) emptyList() else listOf(
                    RailAction(
                        iconRes = R.drawable.ic_demo_heart,
                        label = formatCount((likeCounts[i] ?: 0) + if (i in liked) 1 else 0),
                        isActive = i in liked,
                        contentDescription = "Like",
                        onClick = { liked = if (i in liked) liked - i else liked + i },
                    ),
                    RailAction(
                        iconRes = R.drawable.ic_demo_share,
                        label = formatCount(shareCounts[i] ?: 0),
                        contentDescription = "Share",
                        onClick = {
                            shareCounts = shareCounts + (i to (shareCounts[i] ?: 0) + 1)
                            Toast.makeText(context, "Share tapped — your share sheet goes here", Toast.LENGTH_SHORT).show()
                        },
                    ),
                    RailAction(
                        iconRes = if (feedState.isMuted) R.drawable.ic_demo_volume_off else R.drawable.ic_demo_volume_on,
                        label = if (feedState.isMuted) "Unmute" else "Mute",
                        isActive = feedState.isMuted,
                        contentDescription = "Mute",
                        onClick = { feedState.isMuted = !feedState.isMuted },
                    ),
                    RailAction(
                        iconRes = R.drawable.ic_demo_more,
                        label = null,
                        contentDescription = "More",
                        onClick = {
                            Toast.makeText(context, "Your menu goes here — quality, report, captions…", Toast.LENGTH_SHORT).show()
                        },
                    ),
                ),
            )
            .build()
    }

    val config = remember(mode) {
        if (mode == 3 || mode == 5) {
            OGVerticalFeedConfig.Builder()
                .setShowTitle(true)
                .build()
        } else if (mode == 4) {
            // Classical: the SDK's default error state, zero customization.
            OGVerticalFeedConfig.Builder().setShowTitle(true).build()
        } else if (isLetterbox) {
            // The letterbox knobs: band background, font, text sizes.
            OGVerticalFeedConfig.Builder()
                .setShowTitle(true)
                .setShowSubtitle(true)
                .setTitleTextSize(19.sp)
                .setSubtitleTextSize(14.sp)
                .setTextFontFamily(FontFamily.Serif)
                .setTextBandColor(Color(0xCC141821))
                .setTextBandFraction(0.36f)
                .build()
        } else if (isCustom) {
            OGVerticalFeedConfig.Builder()
                .setShowTitle(true)
                .setShowSubtitle(true)
                .setTextFontFamily(FontFamily.Serif) // your brand font here
                .setPlayIconRes(R.drawable.ic_demo_play_brand)
                .setProgressBarColor(Color(0xFFF6C445))
                .setProgressBarTrackColor(Color(0x40F6C445))
                .setProgressBarThickness(3.dp)
                .build()
        } else {
            // The feed exactly as it ships: bare.
            OGVerticalFeedConfig.Builder().build()
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // Demo action bar: title + cached-analytics button.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E0E10))
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                if (isCustom) "Vertical feed — custom" else "Vertical feed",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = { showAnalytics = true }) {
                Text(
                    "Analytics (${analyticsLog.entries.size})",
                    color = Color(0xFFF6C445),
                    fontSize = 13.sp,
                )
            }
        }
        OGVerticalFeedView(
            items = when (mode) {
                2 -> wideItems
                3 -> dualItems
                4, 5 -> errorItems
                else -> items
            },
            config = config,
            state = feedState,
            onItemSkipped = { index, reason ->
                Toast.makeText(context, "Item $index skipped: $reason", Toast.LENGTH_SHORT).show()
            },
            // Feed analytics accumulate here; the top bar's Analytics button
            // opens the cached log for this screen.
            onAnalyticsEvent = { event -> analyticsLog.add("analytics: $event") },
            // Double-tap is a plain callback — the SDK attaches no meaning.
            onItemDoubleTapped = { index, _ ->
                Toast.makeText(context, "Double-tap on item $index — your action here", Toast.LENGTH_SHORT).show()
            },
            // Mode 5: the feed's errorOverlay slot — 100% app UI on failed pages.
            errorOverlay = if (mode == 5) {
                { error, retry ->
                    Box(
                        Modifier.fillMaxSize().background(Color(0xE6121317)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(Color(0xFF1D1F24), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                .padding(horizontal = 22.dp, vertical = 16.dp),
                        ) {
                            Text("😕  This clip won't play", color = Color.White,
                                fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Text(
                                "Error ${error.code} — this panel is the demo app's UI.",
                                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            androidx.compose.material3.Button(
                                onClick = retry,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF6C445),
                                    contentColor = Color(0xFF131313),
                                ),
                                modifier = Modifier.padding(top = 10.dp),
                            ) { Text("Try again", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
                        }
                    }
                }
            } else null,
            // Custom mode also demos a HOST badge replacing the default chip.
            sponsoredBadge = if (isCustom) {
                { _ ->
                    Text(
                        "AD",
                        color = Color(0xFF131313), fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color(0xFFF6C445), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            } else null,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E0E10))
                .navigationBarsPadding()
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "OGPlayer demo — your tab bar goes here",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
        }
    }

    if (showAnalytics) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAnalytics = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAnalytics = false }) {
                    Text("Close", color = Color(0xFFF6C445))
                }
            },
            title = { Text("Feed analytics — this session") },
            text = {
                Box(Modifier.fillMaxWidth().height(380.dp)) {
                    if (analyticsLog.entries.isEmpty()) {
                        Text(
                            "No events yet — swipe through the feed, let a clip loop.",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp,
                        )
                    } else {
                        EventLogView(analyticsLog)
                    }
                }
            },
            containerColor = Color(0xFF17181C),
            titleContentColor = Color.White,
        )
    }
}

/** In-between screen: the two feed modes, each explained. */
@Composable
private fun ModeChooser(onPick: (mode: Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Vertical feed",
            color = Color.White, fontSize = 22.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        Text(
            "One component, four ways to ship it. Pick a mode:",
            color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp,
        )
        Text(
            "VERTICAL VIEW",
            color = Color(0xFFF6C445).copy(alpha = 0.8f), fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
        ModeCard(
            title = "OG vertical view",
            body = "The feed exactly as the SDK ships it: video, tap-to-pause, progress " +
                "hairline. No title, no subtitle, no icons — the right rail is an empty " +
                "placeholder your app fills (up to 6 actions).",
            onClick = { onPick(0) },
        )
        ModeCard(
            title = "Custom: icons, title & badge",
            body = "Everything the host can add: title + subtitle in a custom font, custom " +
                "play glyph, a custom AD badge on the sponsored item, and rail actions with " +
                "live state — like, share, mute (drives the feed's real mute) and a ⋯ hook.",
            onClick = { onPick(1) },
        )
        Text(
            "SPLIT VIEW",
            color = Color(0xFFF6C445).copy(alpha = 0.8f), fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp),
        )
        ModeCard(
            title = "Split screen: text + video",
            body = "The page splits into a text section and a video section — text " +
                "above or below (TextPlacement), sized by setTextBandFraction, with " +
                "band background color, custom font and text sizes. " +
                "Swipe: item 2 has the text below.",
            onClick = { onPick(2) },
        )
        ModeCard(
            title = "Split video: two sources",
            body = "Two videos on one page — the primary plays on top with audio, " +
                "scrubbing and analytics; the second plays below, muted, starting " +
                "when the page is active and pausing/resuming with the primary " +
                "(setSecondaryMedia).",
            onClick = { onPick(3) },
        )
        Text(
            "ERROR HANDLING",
            color = Color(0xFFF6C445).copy(alpha = 0.8f), fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp),
        )
        ModeCard(
            title = "Errors: SDK default",
            body = "Every stream in this feed 404s on purpose. The SDK shows its " +
                "default compact error state — message + Retry — and onItemError " +
                "fires per item. Swiping past a failed page always works.",
            onClick = { onPick(4) },
        )
        ModeCard(
            title = "Errors: custom overlay",
            body = "Same broken feed, but the errorOverlay slot renders the app's " +
                "own panel with its own button — the feed counterpart of the " +
                "standard player's custom error overlay.",
            onClick = { onPick(5) },
        )
    }
}

@Composable
private fun ModeCard(title: String, body: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF17181C), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = Color(0xFFF6C445), fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(body, color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp, lineHeight = 18.sp)
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000f)
    n >= 1_000 -> "%.1fK".format(n / 1_000f)
    else -> n.toString()
}
