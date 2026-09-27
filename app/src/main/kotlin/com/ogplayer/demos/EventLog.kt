package com.ogplayer.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogplayer.api.DrmRenewalReason
import com.ogplayer.api.OGPlayer
import com.ogplayer.api.OGPlayerError
import com.ogplayer.api.PlaybackListener
import com.ogplayer.api.PlaybackState
import com.ogplayer.api.ads.AdBreakType
import com.ogplayer.api.ads.AdInfo
import com.ogplayer.api.ads.AdListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Rolling in-memory event log shown under the demo players. */
class EventLogState {
    val entries = mutableStateListOf<String>()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun add(message: String) {
        entries.add("${timeFormat.format(Date())}  $message")
        if (entries.size > MAX_ENTRIES) entries.removeAt(0)
    }

    private companion object {
        const val MAX_ENTRIES = 300
    }
}

/** Logs every SDK callback: playback, analytics, and (optionally) ads. */
fun OGPlayer.attachEventLogging(log: EventLogState, includeProgress: Boolean = true) {
    addListener(object : PlaybackListener {
        private var lastProgressSecond = -1L

        override fun onStateChanged(state: PlaybackState) = log.add("onStateChanged: $state")

        override fun onCastStateChanged(state: com.ogplayer.api.cast.CastState) =
            log.add("onCastStateChanged: $state")

        override fun onIsPlayingChanged(isPlaying: Boolean) =
            log.add("onIsPlayingChanged: $isPlaying")

        override fun onPlay() = log.add("onPlay")

        override fun onPause() = log.add("onPause")

        override fun onResume() = log.add("onResume")

        override fun onSeekStarted(fromMs: Long, toMs: Long) =
            log.add("onSeekStarted: ${fromMs / 1000}s -> ${toMs / 1000}s")

        override fun onSeekCompleted(positionMs: Long) =
            log.add("onSeekCompleted: ${positionMs / 1000}s")

        override fun onPlaybackCompleted() = log.add("onPlaybackCompleted")

        override fun onError(error: OGPlayerError) = log.add("onError: $error")

        override fun onDrmSessionRenewed(reason: DrmRenewalReason) =
            log.add("onDrmSessionRenewed: $reason")

        override fun onLiveEdgeChanged(atLiveEdge: Boolean) =
            log.add("onLiveEdgeChanged: $atLiveEdge")

        override fun onProgress(positionMs: Long, bufferedMs: Long, durationMs: Long) {
            if (!includeProgress) return
            val second = positionMs / 1000
            if (second != lastProgressSecond) {
                lastProgressSecond = second
                log.add("onProgress: ${second}s / ${durationMs / 1000}s (buffered ${bufferedMs / 1000}s)")
            }
        }
    })
    addAnalyticsListener { event -> log.add("analytics: $event") }
}

/** Logs the full ad lifecycle. */
fun OGPlayer.attachAdEventLogging(log: EventLogState) {
    addAdListener(object : AdListener {
        override fun onAdBreakStarted(breakType: AdBreakType, totalAds: Int) =
            log.add("onAdBreakStarted: $breakType, $totalAds ads")

        override fun onAdStarted(ad: AdInfo) = log.add("onAdStarted: $ad")

        override fun onAdSkipped(ad: AdInfo) = log.add("onAdSkipped: ${ad.adId}")

        override fun onAdPaused(ad: AdInfo) = log.add("onAdPaused: ${ad.adId}")

        override fun onAdResumed(ad: AdInfo) = log.add("onAdResumed: ${ad.adId}")

        override fun onAdCompleted(ad: AdInfo) = log.add("onAdCompleted: ${ad.adId}")

        override fun onAdBreakCompleted(breakType: AdBreakType) =
            log.add("onAdBreakCompleted: $breakType")

        private var lastProgressSecond = -1L
        override fun onAdProgress(ad: AdInfo, positionMs: Long, durationMs: Long) {
            val second = positionMs / 2000
            if (second != lastProgressSecond) {
                lastProgressSecond = second
                log.add("onAdProgress: ${positionMs}ms / ${durationMs}ms (${ad.adId})")
            }
        }

        override fun onAdSkippableStateChanged(ad: AdInfo, isSkippable: Boolean, skipOffsetMs: Long) =
            log.add("onAdSkippableStateChanged: $isSkippable @${skipOffsetMs}ms")

        override fun onAdError(error: com.ogplayer.api.ads.OGAdError) = log.add("onAdError: $error")
    })
}

/** Terminal-style auto-scrolling list of logged callbacks. */
@Composable
fun EventLogView(log: EventLogState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(log.entries.size) {
        if (log.entries.isNotEmpty()) listState.animateScrollToItem(log.entries.size - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF101418))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        items(log.entries) { entry ->
            androidx.compose.material3.Text(
                text = entry,
                color = Color(0xFF9CCC65),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
