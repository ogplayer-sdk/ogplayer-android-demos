# OGPlayer — Android demo app

Integration demos for the [OGPlayer](https://ogplayer.tv) Android SDK: VOD,
live & DVR, multi-DRM (Widevine), Google IMA ads, subtitles & audio tracks,
Chromecast, content ratings, watermarks, custom error handling and more —
each demo is a small, readable Compose screen you can lift code from. The
same app runs on Android TV, Google TV and Fire TV (see below).

## Run it

Open in Android Studio and press Run. That's it — the SDK resolves from
Maven Central:

```kotlin
implementation("tv.ogplayer:ogplayer-core:1.3.0")
implementation("tv.ogplayer:ogplayer-ui:1.3.0")
```

Requires Android 8.0+ (minSdk 26). Docs: https://ogplayer.tv/docs ·
Live web demo: https://demo.ogplayer.tv

## On a TV

The APK is the same one: it declares a Leanback launcher entry
(`MainTvActivity`), so on Android TV, Google TV and Fire TV the app opens a
TV launcher with the scenarios on the SDK's remote-control chrome — VOD,
live & DVR, Widevine, subtitles & audio (including positioned cues),
playlists with previous/next, Google IMA ads, content ratings, custom
actions and the error overlay. Run it on the Android TV emulator (an AVD
with an Android TV system image) or on a TV over adb; a scenario can be
opened directly:

```sh
adb shell am start -n com.ogplayer.demos/.MainTvActivity -e og.demo vod
```

The SDK switches to the remote chrome only when the host asks
(`OGUiConfig.Builder.setInputMode(OGInputMode.REMOTE)`); `MainTvActivity`
shows the one-line switch. Guide: https://ogplayer.tv/docs/getting-started/android-tv/

## Notes

- **FreeWheel:** the demo is included. `ogplayer-ads-freewheel` resolves
  from Maven Central like the other modules, but FreeWheel's own AdManager
  SDK is licensed to FreeWheel customers and not bundled — the demo screen
  shows setup steps until you add your `FWAdManager.aar` to `app/libs/`
  (and your network config in `DemoFwConfig`).
- **Licensing:** this demo code is MIT. The OGPlayer SDK itself is a
  commercial product — free to evaluate with a watermark; production use
  requires a license. See https://ogplayer.tv/terms/
- **Read-only repository:** issues and pull requests are closed —
  questions and reports are welcome at hello@ogplayer.tv.

Demo content: Tears of Steel — (CC) Blender Foundation · mango.blender.org
