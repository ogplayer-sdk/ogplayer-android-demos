package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogplayer.api.ContentRating
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.CustomAction
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig
import com.ogplayer.ui.OverlaySlot

/**
 * Custom action icons: up to 8 host-supplied, icon-only buttons rendered
 * inline in the top-end control row, left of where the Cast button sits.
 * They are part of the chrome — they show/hide with the controls — while
 * watermarks (overlay slots) are a separate layer below that line. Every
 * tap lands in the host's callback (logged below). Toggle icons one by one
 * to judge spacing; add the watermark + NICAM row to see all three layers.
 */
private val demoIcons = listOf(
    R.drawable.og_demo_action_share,
    R.drawable.og_demo_action_favorite,
    R.drawable.og_demo_action_info,
    R.drawable.og_demo_action_star,
    R.drawable.og_demo_action_search,
    R.drawable.og_demo_action_chat,
    R.drawable.og_demo_action_download,
    R.drawable.og_demo_action_clock,
)

@Composable
internal fun CustomActionsDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    var enabledIcons by remember { mutableStateOf(setOf(0, 1)) }
    var watermark by remember { mutableStateOf(false) }
    var nicam by remember { mutableStateOf(false) }
    val player = remember { OGPlayer.Builder(context).build() }
    val log = remember { EventLogState() }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        onDispose { player.release() }
    }

    // Ratings render once at content start, so toggling NICAM reloads the
    // item to re-trigger the row.
    LaunchedEffect(nicam) {
        player.load(
            OGMediaItem.Builder(
                "https://media.ogplayer.tv/tos/master.m3u8",
            )
                .setStreamType(StreamType.VOD)
                .apply {
                    if (nicam) {
                        setContentRatings(
                            listOf(ContentRating.Age.SIXTEEN, ContentRating.Descriptor.FEAR),
                        )
                    }
                }
                .build(),
            playWhenReady = true,
        )
    }

    val uiConfig = remember(enabledIcons) {
        OGUiConfig.Builder()
            .setCustomActions(
                enabledIcons.sorted().map { index ->
                    CustomAction(
                        iconRes = demoIcons[index],
                        contentDescription = "Icon ${index + 1}",
                    ) { log.add("custom icon${index + 1} tap callback") }
                },
            )
            .build()
    }
    val overlays: Map<OverlaySlot, @Composable () -> Unit> = if (watermark) {
        mapOf(
            OverlaySlot.TOP_END to {
                Text(
                    text = "WATERMARK",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            },
        )
    } else {
        emptyMap()
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
                uiConfig = uiConfig,
                overlays = overlays,
            )
            // Fullscreen means the player and nothing else.
            if (!isFullscreen) {
                // Icons 1-8, individually toggleable (max 8 = the SDK cap).
                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    listOf(0..3, 4..7).forEach { range ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            range.forEach { index ->
                                Checkbox(
                                    checked = index in enabledIcons,
                                    onCheckedChange = { checked ->
                                        enabledIcons =
                                            if (checked) enabledIcons + index else enabledIcons - index
                                    },
                                )
                                // The icon itself (dark outline on the panel) so
                                // it's obvious which glyph each checkbox adds.
                                Icon(
                                    painterResource(demoIcons[index]),
                                    contentDescription = "Icon ${index + 1}",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = watermark, onCheckedChange = { watermark = it })
                        Text("Watermark", style = MaterialTheme.typography.bodySmall)
                        Checkbox(checked = nicam, onCheckedChange = { nicam = it })
                        Text("NICAM (reloads)", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    text = "White outline icons sit inline left of the cast position " +
                        "and hide with the controls; each tap fires the host callback " +
                        "(logged below). Watermark is a separate overlay layer below " +
                        "the control line; NICAM icons show at content start.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
