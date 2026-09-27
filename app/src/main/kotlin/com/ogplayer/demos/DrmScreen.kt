package com.ogplayer.demos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import android.app.Activity
import android.content.pm.ActivityInfo
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
import com.ogplayer.api.DrmConfig
import com.ogplayer.api.OGMediaItem
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.StreamType
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import com.ogplayer.ui.OGUiConfig

/**
 * DRM showcase: two public Widevine test streams.
 *
 * - "Widevine (open)": Google's test asset against the public
 *   widevine_test license proxy — plain licenseUrl, no token.
 * - "Widevine (token header)": a public multi-DRM test vector where a JWT is sent in
 *   X-AxDRM-Message. The SDK invokes the token provider on EVERY license
 *   request (watch the log) — this is the mechanism that fixes the classic
 *   frozen-token/resume-after-pause failure.
 *
 * Note: emulators only have Widevine L3 (software); real devices with L1
 * play these too, but L1-secured output would black out screenshots.
 */
private const val DRM_TOKEN =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ewogICJ2ZXJzaW9uIjogMSwKICAiY29tX2tleV9pZCI6ICI2OWU1NDA4OC1lOWUwLTQ1MzAtOGMxYS0xZWI2ZGNkMGQxNGUiLAogICJtZXNzYWdlIjogewogICAgInR5cGUiOiAiZW50aXRsZW1lbnRfbWVzc2FnZSIsCiAgICAidmVyc2lvbiI6IDIsCiAgICAibGljZW5zZSI6IHsKICAgICAgImFsbG93X3BlcnNpc3RlbmNlIjogdHJ1ZQogICAgfSwKICAgICJjb250ZW50X2tleXNfc291cmNlIjogewogICAgICAiaW5saW5lIjogWwogICAgICAgIHsKICAgICAgICAgICJpZCI6ICIzMDJmODBkZC00MTFlLTQ4ODYtYmNhNS1iYjFmODAxOGEwMjQiLAogICAgICAgICAgImVuY3J5cHRlZF9rZXkiOiAicm9LQWcwdDdKaTFpNDNmd3YremZ0UT09IiwKICAgICAgICAgICJ1c2FnZV9wb2xpY3kiOiAiUG9saWN5IEEiCiAgICAgICAgfQogICAgICBdCiAgICB9LAogICAgImNvbnRlbnRfa2V5X3VzYWdlX3BvbGljaWVzIjogWwogICAgICB7CiAgICAgICAgIm5hbWUiOiAiUG9saWN5IEEiLAogICAgICAgICJwbGF5cmVhZHkiOiB7CiAgICAgICAgICAibWluX2RldmljZV9zZWN1cml0eV9sZXZlbCI6IDE1MCwKICAgICAgICAgICJwbGF5X2VuYWJsZXJzIjogWwogICAgICAgICAgICAiNzg2NjI3RDgtQzJBNi00NEJFLThGODgtMDhBRTI1NUIwMUE3IgogICAgICAgICAgXQogICAgICAgIH0KICAgICAgfQogICAgXQogIH0KfQ._NfhLVY7S6k8TJDWPeMPhUawhympnrk6WAZHOVjER6M"

private enum class DrmStream(val label: String) { OPEN("Widevine (open)"), TOKEN("Token header") }

@Composable
internal fun DrmDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    val player = remember { OGPlayer.Builder(context).build() }
    val log = remember { EventLogState() }
    var selected by remember { mutableStateOf(DrmStream.OPEN) }
    var isFullscreen by remember { mutableStateOf(false) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }

    DisposableEffect(Unit) {
        // Clear any orientation lock a previous screen's fullscreen exit left
        // behind (ActivityFullscreenHandler releases its lock only once the
        // device physically aligns) — sensor drives orientation here.
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        onDispose { player.release() }
    }

    LaunchedEffect(selected) {
        log.add("— loading ${selected.label} —")
        val item = when (selected) {
            DrmStream.OPEN ->
                OGMediaItem.Builder("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd")
                    .setStreamType(StreamType.VOD)
                    .setTitle("Tears — Widevine test asset")
                    .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                    .setDrm(
                        DrmConfig.Builder("https://proxy.uat.widevine.com/proxy?provider=widevine_test")
                            .build(),
                    )
                    .build()

            DrmStream.TOKEN ->
                // The same encrypted stream, DASH flavor.
                OGMediaItem.Builder(
                    "https://media.axprod.net/TestVectors/Cmaf/protected_1080p_h264_cbcs/manifest.mpd",
                )
                    .setStreamType(StreamType.VOD)
                    .setTitle("Multi-DRM demo (encrypted)")
                    .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
                    .setDrm(
                        DrmConfig.Builder("https://drm-widevine-licensing.axprod.net/AcquireLicense")
                            .setTokenProvider("X-AxDRM-Message") { request ->
                                // Called fresh on EVERY license request (incl.
                                // renewals) — fetch/refresh a real token here.
                                log.add("tokenProvider called for ${request.mediaItem.title}")
                                DRM_TOKEN
                            }
                            .build(),
                    )
                    .build()
        }
        player.load(item, playWhenReady = false)
    }

    // The open Widevine asset has one audio track and no text tracks; the
    // encrypted vector has three of each. Buttons that would open an empty
    // menu are hidden per stream.
    val uiConfig = remember(selected) {
        OGUiConfig.Builder()
            .apply {
                if (selected == DrmStream.OPEN) {
                    setShowSubtitleButton(false)
                    setShowAudioTrackButton(false)
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
            // Fullscreen means the player and nothing else.
            if (!isFullscreen) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    DrmStream.entries.forEach { stream ->
                        FilterChip(
                            selected = selected == stream,
                            onClick = { selected = stream },
                            label = { Text(stream.label) },
                        )
                    }
                }
                Text(
                    text = "Watch for DrmKeysLoaded in the log; the token-header " +
                        "stream also logs each tokenProvider call. The open Widevine " +
                        "asset has one audio track and no subtitles, so those two " +
                        "buttons are hidden for it; the encrypted stream has three of " +
                        "each and shows both.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
