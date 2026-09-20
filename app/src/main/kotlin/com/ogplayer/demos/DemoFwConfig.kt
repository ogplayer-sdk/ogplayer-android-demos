package com.ogplayer.demos

import android.content.Context
import com.ogplayer.ads.freewheel.FreewheelConfig

/**
 * FreeWheel demo configuration — fill in with YOUR network's values.
 *
 * Everything below comes from your FreeWheel (MRM) account; your FreeWheel
 * account manager can provide all of it:
 *
 *  - **Network id** — your MRM network id (a number).
 *  - **Ad server URL** — your network's ad server,
 *    e.g. `https://<hash>.v.fwmrm.net`.
 *  - **Player profile** — usually `<networkId>:<profile_name>`, one per
 *    platform (FreeWheel provisions e.g. `..._android_live`).
 *  - **Site section id** — the placement of this player in your app, as
 *    registered in MRM.
 *  - **Video asset id** — the content's id as ingested in MRM; ad rules
 *    (pre/mid/postroll schedule) are configured against it, and its exact
 *    duration must be passed so midroll positions resolve.
 *
 * Consent & identity parameters (`_fw_gdpr`, `_fw_gdpr_consent`, `_fw_did`,
 * user agent, custom key-values…) are the app's responsibility and go into
 * `setGlobalParameters` verbatim — OGPlayer never fabricates consent or
 * device identifiers. The SDK adds only `pvrn`/`vprn` request randomizers.
 */
internal object DemoFwConfig {

    private const val NETWORK_ID = 0 /* put your FreeWheel network id here */
    private const val SERVER_URL = "" /* put your ad server URL here */
    private const val PROFILE = "" /* put your player profile here */
    private const val SITE_SECTION_ID = "" /* put your site section id here */
    private const val VIDEO_ASSET_ID = "" /* put your MRM video asset id here */

    /** Exact duration (ms) of the asset behind [VIDEO_ASSET_ID]. */
    private const val VIDEO_DURATION_MS = 0L /* put your asset duration here */

    /** True once the placeholders above have been filled in. */
    val isConfigured: Boolean
        get() = NETWORK_ID > 0 && SERVER_URL.isNotBlank() && PROFILE.isNotBlank() &&
            SITE_SECTION_ID.isNotBlank() && VIDEO_ASSET_ID.isNotBlank() &&
            VIDEO_DURATION_MS > 0

    @Suppress("UNUSED_PARAMETER")
    fun build(context: Context): FreewheelConfig =
        FreewheelConfig.Builder(
            serverUrl = SERVER_URL,
            networkId = NETWORK_ID,
            profile = PROFILE,
            siteSectionId = SITE_SECTION_ID,
            videoAssetId = VIDEO_ASSET_ID,
            videoDurationMs = VIDEO_DURATION_MS,
        )
            .setGlobalParameters(
                mapOf(
                    /* put your consent / identity / targeting key-values
                       here, e.g.:
                       "_fw_gdpr" to "1",
                       "_fw_gdpr_consent" to "<your TCF consent string>",
                       "_fw_did" to "android_id:<device id>", */
                ),
            )
            .build()
}
