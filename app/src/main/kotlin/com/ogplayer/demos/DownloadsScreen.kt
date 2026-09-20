package com.ogplayer.demos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.ogplayer.api.downloads.OGDownload
import com.ogplayer.api.downloads.OGDownloadConfig
import com.ogplayer.api.downloads.OGDownloadListener
import com.ogplayer.api.downloads.OGDownloadState
import com.ogplayer.api.downloads.OGDownloads
import com.ogplayer.ui.ActivityFullscreenHandler
import com.ogplayer.ui.OGPlayerView
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Download-to-go: queue a stream for offline playback, watch progress, then
 * play it with networking OFF (airplane mode) — including the DRM'd stream,
 * whose persistent Widevine licence was fetched at download time and is
 * restored at playback with no licence request and no token call.
 *
 * - "Clear (ToS)": the Tears of Steel ladder from media.ogplayer.tv.
 * - "Widevine": a public multi-DRM test vector — its entitlement explicitly allows
 *   licence persistence (`allow_persistence: true`), so it demonstrates the
 *   full offline-DRM lifecycle incl. "Renew licence".
 */
private enum class DownloadStream(val label: String) { CLEAR("Clear (ToS)"), DRM("Widevine") }

private fun drmUrl(): String =
    "https://media.axprod.net/TestVectors/Cmaf/protected_1080p_h264_cbcs/manifest.mpd"

/** Same entitlement the DRM demo uses — `allow_persistence: true`. */
private const val PERSISTENT_DRM_TOKEN =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ewogICJ2ZXJzaW9uIjogMSwKICAiY29tX2tleV9pZCI6ICI2OWU1NDA4OC1lOWUwLTQ1MzAtOGMxYS0xZWI2ZGNkMGQxNGUiLAogICJtZXNzYWdlIjogewogICAgInR5cGUiOiAiZW50aXRsZW1lbnRfbWVzc2FnZSIsCiAgICAidmVyc2lvbiI6IDIsCiAgICAibGljZW5zZSI6IHsKICAgICAgImFsbG93X3BlcnNpc3RlbmNlIjogdHJ1ZQogICAgfSwKICAgICJjb250ZW50X2tleXNfc291cmNlIjogewogICAgICAiaW5saW5lIjogWwogICAgICAgIHsKICAgICAgICAgICJpZCI6ICIzMDJmODBkZC00MTFlLTQ4ODYtYmNhNS1iYjFmODAxOGEwMjQiLAogICAgICAgICAgImVuY3J5cHRlZF9rZXkiOiAicm9LQWcwdDdKaTFpNDNmd3YremZ0UT09IiwKICAgICAgICAgICJ1c2FnZV9wb2xpY3kiOiAiUG9saWN5IEEiCiAgICAgICAgfQogICAgICBdCiAgICB9LAogICAgImNvbnRlbnRfa2V5X3VzYWdlX3BvbGljaWVzIjogWwogICAgICB7CiAgICAgICAgIm5hbWUiOiAiUG9saWN5IEEiLAogICAgICAgICJwbGF5cmVhZHkiOiB7CiAgICAgICAgICAibWluX2RldmljZV9zZWN1cml0eV9sZXZlbCI6IDE1MCwKICAgICAgICAgICJwbGF5X2VuYWJsZXJzIjogWwogICAgICAgICAgICAiNzg2NjI3RDgtQzJBNi00NEJFLThGODgtMDhBRTI1NUIwMUE3IgogICAgICAgICAgXQogICAgICAgIH0KICAgICAgfQogICAgXQogIH0KfQ._NfhLVY7S6k8TJDWPeMPhUawhympnrk6WAZHOVjER6M"

private fun clearItem(): OGMediaItem =
    OGMediaItem.Builder("https://media.ogplayer.tv/tos/master.m3u8")
        .setStreamType(StreamType.VOD)
        .setTitle("Tears of Steel")
        .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
        .setThumbnailTrack("https://media.ogplayer.tv/tos/storyboard/storyboard.vtt")
        .build()

private fun drmItem(log: EventLogState): OGMediaItem =
    OGMediaItem.Builder("https://media.axprod.net/TestVectors/Cmaf/protected_1080p_h264_cbcs/manifest.mpd")
        .setStreamType(StreamType.VOD)
        .setTitle("Multi-DRM demo (encrypted)")
        .setPosterUrl("https://media.ogplayer.tv/posters/tos-mech.jpg")
        .setDrm(
            DrmConfig.Builder("https://drm-widevine-licensing.axprod.net/AcquireLicense")
                .setTokenProvider("X-AxDRM-Message") { request ->
                    // Called for the offline-licence fetch at download time,
                    // and again only on renewLicense — never during offline play.
                    log.add("tokenProvider called (${if ("renew" in request.licenseUrl) "renewal" else "licence"})")
                    PERSISTENT_DRM_TOKEN
                }
                .build(),
        )
        .build()

@Composable
internal fun DownloadsDemo() {
    val context = LocalContext.current
    val activity = context as Activity
    val player = remember { OGPlayer.Builder(context).build() }
    val log = remember { EventLogState() }
    val manager = remember { OGDownloads.manager(context) }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    var selected by remember { mutableStateOf(DownloadStream.CLEAR) }
    var isFullscreen by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf(manager.downloads) }
    val fullscreenHandler = remember {
        ActivityFullscreenHandler(activity, lockLandscape = true) { isFullscreen = it }
    }

    fun item(stream: DownloadStream): OGMediaItem =
        if (stream == DownloadStream.CLEAR) clearItem() else drmItem(log)

    // The item a download row corresponds to (DRM item carries the token
    // provider so deletion can authenticate the licence release).
    fun itemFor(url: String): OGMediaItem =
        if (url == item(DownloadStream.DRM).url) item(DownloadStream.DRM) else item(DownloadStream.CLEAR)

    // A COMPLETED download auto-loads into the player (paused) exactly once —
    // the player's own play button is the demo's play control; Delete resets
    // the latch. Offline pickup is keyed by URL, no special code.
    var loadedDownloadUrl by remember { mutableStateOf<String?>(null) }
    fun loadCompletedIfNeeded() {
        val d = manager.downloads.firstOrNull { it.state == OGDownloadState.COMPLETED }
        if (d == null) {
            // Delete → back to the pre-download placeholder (RN-demo parity);
            // pause so a playing item can't keep sounding behind it.
            if (manager.downloads.isEmpty() && loadedDownloadUrl != null) {
                player.pause()
                loadedDownloadUrl = null
            }
            return
        }
        if (loadedDownloadUrl == d.url) return
        val target = itemFor(d.url)
        loadedDownloadUrl = d.url
        log.add("— ${target.title} downloaded, loading into the player (plays offline) —")
        player.load(target, playWhenReady = false)
    }

    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        player.attachEventLogging(log, includeProgress = false)
        val listener = object : OGDownloadListener {
            override fun onDownloadStateChanged(download: OGDownload) {
                log.add("download ${download.state}: ${download.title}")
                snapshot = manager.downloads
                loadCompletedIfNeeded()
            }

            override fun onDownloadProgress(download: OGDownload) {
                snapshot = manager.downloads
            }

            override fun onDownloadFailed(download: OGDownload, error: com.ogplayer.api.OGPlayerError) {
                // Include the message — "7100" alone hides e.g. an offline
                // device (UnknownHostException) behind an opaque code.
                log.add("download FAILED ${error.code} ${error.codeName}: ${error.message}")
            }
        }
        manager.addListener(listener)
        // Restart persistence: a download completed in a previous process
        // (force-kill skips demo cleanup) goes straight into the player.
        loadCompletedIfNeeded()
        onDispose {
            // Demo hygiene: leaving the screen deletes the downloads (and
            // releases DRM licences with the item's credentials). Force-kill
            // skips this, so restart-persistence remains demonstrable.
            manager.downloads.forEach { d ->
                manager.removeDownload(
                    d.url,
                    d.url.takeIf { it == drmUrl() }?.let { item(DownloadStream.DRM) },
                )
            }
            manager.removeListener(listener)
            scope.cancel()
            player.release()
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(if (isFullscreen) Modifier.fillMaxSize() else Modifier.safeDrawingPadding()) {
            if (loadedDownloadUrl != null) {
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
            } else {
                // Pre-download placeholder where the player will appear —
                // returns after Delete (the finished download auto-loads).
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                ) {
                    Text(
                        "Download below — the finished download loads here.",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (!isFullscreen) {
                // One download at a time: while anything is downloaded or in
                // flight, the stream chooser and Download button give way to
                // the download's own row — Delete brings the choices back.
                // A COMPLETED download auto-loads into the player above, so
                // its own play button is the only play control (the row keeps
                // just Delete).
                if (snapshot.isEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        DownloadStream.entries.forEach { stream ->
                            FilterChip(
                                selected = selected == stream,
                                onClick = { selected = stream },
                                label = { Text(stream.label) },
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        OutlinedButton(onClick = {
                            log.add("— queueing ${selected.label} (720p cap) —")
                            manager.add(
                                item(selected),
                                OGDownloadConfig.Builder().setMaxVideoHeight(720).build(),
                            )
                            snapshot = manager.downloads
                        }) { Text("Download") }
                    }
                }
                snapshot.forEach { d ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${d.title ?: d.url} · ${d.state}" +
                                (if (d.progressPercent >= 0) " ${d.progressPercent.toInt()}%" else "") +
                                (d.license?.let { lic ->
                                    " · licence " + (lic.expiresAtMs?.let { t ->
                                        (if (lic.isExpired) "EXPIRED " else "until ") +
                                            DateFormat.getDateTimeInstance().format(Date(t))
                                    } ?: "unlimited")
                                } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        when (d.state) {
                            OGDownloadState.DOWNLOADING, OGDownloadState.QUEUED ->
                                OutlinedButton(onClick = { manager.pause(d.url) }) { Text("Pause") }
                            OGDownloadState.PAUSED, OGDownloadState.FAILED ->
                                OutlinedButton(onClick = { manager.resume(d.url) }) { Text("Resume") }
                            else -> Spacer(Modifier.width(0.dp))
                        }
                        OutlinedButton(onClick = {
                            // Pass the item for DRM'd downloads so the Widevine
                            // release request can authenticate (frees the
                            // server-side offline slot, not just local data).
                            val drmUrl = item(DownloadStream.DRM).url
                            manager.removeDownload(
                                d.url,
                                if (d.url == drmUrl) item(DownloadStream.DRM) else null,
                            )
                            snapshot = manager.downloads
                            loadCompletedIfNeeded() // resets the auto-load latch
                        }) { Text("Delete") }
                    }
                }
                Text(
                    text = "Download, then toggle airplane mode and press play on the " +
                        "player — both streams keep playing. The DRM stream restores " +
                        "its persistent licence with no network and no token call.",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
                EventLogView(log, Modifier.weight(1f))
            }
        }
    }
}
