package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig

/**
 * Custom error messages: the host maps the SDK's stable error codes (2000
 * network, 3000 source, 4000 DRM, 5000 renderer, 6000 live, 9000 unknown) to
 * its own copy in any language via `setErrorMessageProvider`. This screen
 * loads a stream URL that doesn't exist, so playback always fails —
 * type a message and reload to see yours on the error overlay. The raw
 * OGPlayerError still reaches onError unchanged for logging.
 */
private const val MISSING_STREAM = "https://media.ogplayer.tv/tos/does-not-exist.m3u8"

@Composable
internal fun ErrorMessagesDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }
    val player = remember { OGPlayer.Builder(context).build() }

    var message by remember { mutableStateOf("") }
    val messageState = remember { mutableStateOf(message) }
    messageState.value = message

    // Retry demo modes: the SDK button as-is, a relabeled one, none at all,
    // or a fully host-owned overlay rendered on the player surface.
    // 0=SDK, 1=label, 2=none, 3=custom overlay, 4=branded (0.13.1 theming)
    var retryMode by remember { mutableStateOf(0) }

    val uiConfig = remember(retryMode) {
        OGUiConfig.Builder()
            .setShowRetryButton(retryMode != 2)
            // The stream never loads: seeking, rate, tracks and the timeline
            // have nothing to act on, so those controls are hidden.
            .setShowSeekButtons(false)
            .setShowProgressBar(false)
            .setShowTimeLabels(false)
            .setShowSpeedButton(false)
            .setShowQualityButton(false)
            .setShowAudioTrackButton(false)
            .setShowSubtitleButton(false)
            .apply { if (retryMode == 1) setRetryButtonLabel("Probeer opnieuw") }
            .setErrorMessageProvider { error ->
                messageState.value.takeIf { it.isNotBlank() }
                    ?.replace("{code}", error.code.toString())
            }
            .apply {
                // 0.13.1: the error overlay is themeable — serif text + a
                // blue button prove the SDK look is not baked in.
                if (retryMode == 4) {
                    setErrorTextStyle(
                        androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        ),
                    )
                    setRetryButtonStyle(
                        containerColor = Color(0xFF3D6EF5),
                        contentColor = Color.White,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        ),
                    )
                }
            }
            .build()
    }

    fun reload() {
        player.load(
            OGMediaItem.Builder(MISSING_STREAM)
                .setTitle("Custom error demo")
                .build(),
        )
    }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { player.release() }
    }
    LaunchedEffect(retryMode) { reload() }

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
                // Mode 3: OUR OWN error UI on the player surface — the SDK
                // shows/clears it and hands us the error + retry.
                errorOverlay = if (retryMode == 3) {
                    { error, retry -> HostErrorPanel(error, retry) }
                } else {
                    null
                },
            )
            // Fullscreen means the player and nothing else.
            if (!isFullscreen) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Your error message — {code} = error code") },
                        )
                        Button(
                            onClick = { reload() },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF6C445),
                                contentColor = Color(0xFF1A1A1A),
                            ),
                        ) { Text("Reload") }
                    }
                    // Four modes don't fit one row on phones — wrap freely.
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        listOf("SDK Retry", "Custom label", "No retry button", "Custom overlay").forEachIndexed { i, label ->
                            FilterChip(
                                selected = retryMode == i,
                                onClick = { retryMode = i },
                                label = { Text(label) },
                            )
                        }
                        FilterChip(
                            selected = retryMode == 4,
                            onClick = { retryMode = 4 },
                            label = { Text("Branded style") },
                        )
                    }
                    Text(
                        text = when (retryMode) {
                            3 ->
                                "errorOverlay slot — the panel below the code is 100% " +
                                "app UI, rendered by the SDK on the player surface, " +
                                "with the error and a retry() handed in."
                            2 ->
                                "setShowRetryButton(false) — the overlay shows only your " +
                                "message; recovery is your app's call."
                            1 ->
                                "setRetryButtonLabel(\"Probeer opnieuw\") — the SDK's " +
                                "Retry button, your text, any language."
                            else ->
                                "This screen loads a missing stream URL, so it always " +
                                "fails — your text (any language) replaces the SDK's " +
                                "default error overlay via setErrorMessageProvider. Nothing plays " +
                                "here, so the seek, timeline, speed and track controls are hidden."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}


/** 100% host-owned error UI for the "Custom overlay" mode — deliberately
 *  styled unlike the SDK chrome (accent card, rounded button) to make clear
 *  this is the app's design language, not OGPlayer's. */
@Composable
private fun HostErrorPanel(error: com.ogplayer.api.OGPlayerError, retry: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xE6121317)),
        contentAlignment = Alignment.Center,
    ) {
        // Compact: must fit inside an embedded 16:9 player on a phone.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(12.dp)
                .background(
                    Color(0xFF1D1F24),
                    RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                "😕  Something broke on our side",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
            Text(
                "Error ${error.code} — this whole panel is the demo app's UI.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xB3FFFFFF),
                modifier = Modifier.padding(top = 3.dp),
            )
            Button(
                onClick = retry,
                contentPadding = PaddingValues(
                    horizontal = 18.dp, vertical = 6.dp,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6C445),
                    contentColor = Color(0xFF131313),
                ),
                modifier = Modifier.padding(top = 8.dp).heightIn(min = 34.dp),
            ) { Text("Try again", fontWeight = FontWeight.SemiBold) }
        }
    }
}
