package com.ogplayer.demos

import com.ogplayer.api.OGPlayerSdk
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.BrandingWatermark
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Single-activity app: every demo is a composable destination in one
 * NavHost — the architecture integrators actually ship. Each screen owns
 * its player via remember/DisposableEffect; leaving the destination
 * disposes the composition and releases the player.
 *
 * (AppCompatActivity + AppCompat theme only because the Cast media-route
 * dialog requires it.)
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Test hook: `adb shell am start ... -e og.demo <route>` (or an
        // instrumentation intent extra) jumps straight into a demo so
        // behavior can be verified by automation.
        val startRoute = intent.getStringExtra("og.demo") ?: "menu"
        setContent { DemoApp(startRoute) }
    }
}

// Design tokens — "OGPlayer demo proposal" launcher spec.
private object Ink {
    val Background = Color(0xFF0E0E10)
    val Accent = Color(0xFFF6C445)
    val RowSurface = Color(0x0BFFFFFF) // #FFF · 4.5%
    val RowBorder = Color(0x12FFFFFF) // #FFF · 7%
    val Title = Color(0xFFFFFFFF)
    val Description = Color(0x7AFFFFFF) // #FFF · 48%
    val GroupHeader = Color(0x61FFFFFF) // #FFF · 38%
    val Chevron = Color(0x4DFFFFFF) // #FFF · 30%
    val TagBg = Color(0x17FFFFFF) // #FFF · 9%
    val IconTile = Color(0x1FF6C445) // accent · 12%
}


private data class Demo(
    val route: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tag: String? = null,
)

private data class DemoGroup(val header: String, val demos: List<Demo>)

private val demoGroups = listOf(
    DemoGroup(
        "PLAYBACK",
        listOf(
            Demo("orientation", "Orientation & fullscreen",
                "Rotation, embedded ⇄ fullscreen, insets and cutouts.", Icons.Outlined.ScreenRotation),
            Demo("controls", "Controls on/off",
                "Default chrome, per-control hide, fully headless.", Icons.Outlined.Tune),
            Demo("customactions", "Custom action icons",
                "Up to 8 host icons inline in the controls, with callbacks.",
                Icons.Outlined.AddCircleOutline),
            Demo("pip", "Picture-in-picture",
                "Auto-enter on Home, dismiss pauses.",
                Icons.Outlined.PictureInPictureAlt),
            Demo("startfullscreen", "Starts in fullscreen",
                "Opens directly in fullscreen; host-intercepted exit.", Icons.Outlined.OpenInFull),
            Demo("playlist", "Playlist & up next",
                "Queue clips that auto-advance, with a themeable countdown card.",
                Icons.AutoMirrored.Outlined.PlaylistPlay),
            Demo("verticalfeed", "Vertical feed",
                "Swipeable portrait feed: preloaded neighbours, loop, sponsored items.",
                Icons.Outlined.SwipeVertical),
            Demo("errormessages", "Custom error messages",
                "Your copy, your language, on our stable error codes.", Icons.Outlined.ErrorOutline),
        ),
    ),
    DemoGroup(
        "STREAMING",
        listOf(
            Demo("live", "Live & DVR",
                "Live edge, seekable window, behind-edge state.", Icons.Outlined.Sensors),
            Demo("drm", "DRM",
                "Widevine licence acquisition and silent recovery.", Icons.Outlined.Lock, tag = "Widevine"),
            Demo("downloads", "Offline downloads",
                "Download-to-go with persistent Widevine licences — plays in airplane mode.",
                Icons.Outlined.Download),
        ),
    ),
    DemoGroup(
        "TRACKS & DEVICES",
        listOf(
            Demo("tracks", "Subtitles & audio",
                "Track selection, styling, positioned VTT cues.", Icons.Outlined.ClosedCaption),
            Demo("cast", "Chromecast",
                "Discovery, hand-off, remote playback state.", Icons.Outlined.Cast),
        ),
    ),
    DemoGroup(
        "MONETISATION",
        listOf(
            Demo("ads", "Ads",
                "Pre/mid/post-roll pods, skip, cue markers.", Icons.Outlined.Movie, tag = "IMA"),
            Demo("freewheel", "FreeWheel",
                "Native slot provider, SDK ad chrome. Bring your own FreeWheel SDK.",
                Icons.Outlined.Movie, tag = "FW"),
        ),
    ),
    DemoGroup(
        "OVERLAYS",
        listOf(
            Demo("watermarks", "Watermarks",
                "A watermark in any of nine slots, live.", Icons.Outlined.BrandingWatermark),
            Demo("nicam", "Content ratings",
                "Age + descriptor icons at program start.", Icons.Outlined.Shield, tag = "Kijkwijzer"),
        ),
    ),
)

@Composable
private fun DemoApp(startRoute: String = "menu") {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.Accent,               // yellow (selected chips/buttons)
            onPrimary = Color(0xFF1A1A1A),      // black text on yellow
            secondary = Ink.Accent,
            onSecondary = Color(0xFF1A1A1A),
            secondaryContainer = Ink.Accent,    // FilterChip selected = yellow
            onSecondaryContainer = Color(0xFF1A1A1A),
            background = Ink.Background,
            onBackground = Color.White,
            surface = Ink.Background,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1C1C20), // unselected chips/fields
            onSurfaceVariant = Color(0xB3FFFFFF),
            outline = Color(0x33FFFFFF),
        ),
    ) {
        DemoNavHost(startRoute)
    }
}

@Composable
private fun DemoNavHost(startRoute: String) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startRoute) {
        composable("menu") { MenuScreen(nav) }
        composable("orientation") { OrientationDemo() }
        composable("live") { LiveDemo() }
        composable("drm") { DrmDemo() }
        composable("downloads") { DownloadsDemo() }
        composable("tracks") { TracksDemo() }
        composable("cast") { CastDemo() }
        composable("customactions") { CustomActionsDemo() }
        composable("pip") { PipDemo() }
        composable("ads") { AdsDemo() }
        composable("controls") { ControlsDemo() }
        composable("watermarks") { WatermarkDemo() }
        composable("startfullscreen") { StartFullscreenDemo(onClose = { nav.popBackStack() }) }
        composable("verticalfeed") { VerticalFeedDemo() }
        composable("playlist") { PlaylistDemo() }
        composable("nicam") { NicamDemo() }
        composable("errormessages") { ErrorMessagesDemo() }
        composable("freewheel") { FreewheelDemo() }
    }
}

@Composable
private fun MenuScreen(nav: NavHostController) {
    val bars = WindowInsets.systemBars.asPaddingValues()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 20.dp + bars.calculateTopPadding(),
            bottom = 20.dp + bars.calculateBottomPadding(),
        ),
    ) {
        item { Header() }
        item { Spacer(Modifier.height(4.dp)) }
        demoGroups.forEach { group ->
            item { GroupHeader(group.header) }
            items(group.demos.size) { i ->
                val demo = group.demos[i]
                DemoRow(demo) { nav.navigate(demo.route) }
                Spacer(Modifier.height(5.dp))
            }
        }
        item {
            Text(
                text = "Unlicensed build — demos render the OGPlayer watermark.",
                color = Ink.GroupHeader,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun Header() {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(Modifier.width(8.dp))
            Row {
                Text("OG", color = Ink.Accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Player", color = Ink.Description, fontSize = 17.sp)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .background(Ink.TagBg, RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text("v" + OGPlayerSdk.VERSION, color = Ink.Description, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Integration demos", color = Ink.Title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Every SDK capability, demonstrated end to end.",
            color = Ink.Description,
            fontSize = 12.sp,
        )
    }
}

/** Compact OGPlayer identity mark (chamfered aperture + play triangle). */
@Composable
private fun BrandMark() {
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val u = size.width / 32f
        val frame = androidx.compose.ui.graphics.Path().apply {
            moveTo(12f * u, 3f * u); lineTo(29f * u, 3f * u); lineTo(29f * u, 20f * u)
            lineTo(20f * u, 29f * u); lineTo(3f * u, 29f * u); lineTo(3f * u, 12f * u); close()
        }
        // Aperture frame white 72% (matches the SDK watermark); only the
        // play triangle is accent yellow.
        drawPath(
            frame, Color.White.copy(alpha = 0.72f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.6f * u, join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
        val play = androidx.compose.ui.graphics.Path().apply {
            moveTo(13f * u, 10f * u); lineTo(22.5f * u, 16f * u); lineTo(13f * u, 22f * u); close()
        }
        drawPath(play, Ink.Accent)
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        color = Ink.GroupHeader,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
    )
}

@Composable
private fun DemoRow(demo: Demo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Ink.RowSurface)
            .border(1.dp, Ink.RowBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(Ink.IconTile, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(demo.icon, contentDescription = null, tint = Ink.Accent, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(demo.title, color = Ink.Title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (demo.tag != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .background(Ink.TagBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(demo.tag, color = Ink.Description, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(demo.description, color = Ink.Description, fontSize = 10.5.sp)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink.Chevron,
            modifier = Modifier.size(14.dp),
        )
    }
}
