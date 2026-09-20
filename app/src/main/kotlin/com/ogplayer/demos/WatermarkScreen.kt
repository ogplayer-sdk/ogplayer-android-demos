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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OverlaySlot

/**
 * Watermark showcase: toggle a watermark in ANY of the nine overlay slots (the
 * 3x3 grid mirrors the on-screen positions), live and mid-playback. The SDK
 * keeps them clear of the controls (top slots drop below the top bar, bottom
 * slots lift above it) and hides them during ad breaks.
 */
private val slotGrid = listOf(
    listOf(OverlaySlot.TOP_START, OverlaySlot.TOP_CENTER, OverlaySlot.TOP_END),
    listOf(OverlaySlot.CENTER_START, OverlaySlot.CENTER, OverlaySlot.CENTER_END),
    listOf(OverlaySlot.BOTTOM_START, OverlaySlot.BOTTOM_CENTER, OverlaySlot.BOTTOM_END),
)

private fun OverlaySlot.readableName(): String = when (this) {
    OverlaySlot.TOP_START -> "Top-left"
    OverlaySlot.TOP_CENTER -> "Top-center"
    OverlaySlot.TOP_END -> "Top-right"
    OverlaySlot.CENTER_START -> "Left"
    OverlaySlot.CENTER -> "Center"
    OverlaySlot.CENTER_END -> "Right"
    OverlaySlot.BOTTOM_START -> "Bottom-left"
    OverlaySlot.BOTTOM_CENTER -> "Bottom-center"
    OverlaySlot.BOTTOM_END -> "Bottom-right"
}

@Composable
internal fun WatermarkDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember { OGPlayer.Builder(context).build() }

    var enabled by remember { mutableStateOf(setOf(OverlaySlot.TOP_END)) }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { player.release() }
    }

    LaunchedEffect(Unit) {
        player.load(
            OGMediaItem.Builder("https://media.ogplayer.tv/tos/master.m3u8")
                .setStreamType(StreamType.VOD)
                .setTitle("Watermark demo")
                .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                .build(),
            playWhenReady = false,
        )
    }

    val overlays: Map<OverlaySlot, @Composable () -> Unit> = buildMap {
        OverlaySlot.entries.forEachIndexed { index, slot ->
            if (slot in enabled) {
                put(slot) {
                    Text(
                        text = "WATERMARK ${index + 1}",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
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
                overlays = overlays,
            )
            // Fullscreen means the player and nothing else.
            if (!isFullscreen) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "Tap any slot to place a watermark there — all nine positions. The " +
                            "SDK keeps them clear of the controls (top slots drop below the top " +
                            "bar, bottom slots lift above it).",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    slotGrid.forEach { rowSlots ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowSlots.forEach { slot ->
                                val on = slot in enabled
                                // Square; yellow when on, gray when off, black text —
                                // matches the launcher's option style.
                                Button(
                                    onClick = { enabled = if (on) enabled - slot else enabled + slot },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (on) Color(0xFFF6C445) else Color(0xFFE4E4E7),
                                        contentColor = Color(0xFF1A1A1A),
                                    ),
                                ) {
                                    Text(
                                        slot.readableName(),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
