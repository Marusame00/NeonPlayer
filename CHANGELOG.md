# Changelog

Version codes are what Google Play tracks. `versionName` stays `1.0.0` for all of these.

## versionCode 5 — build 5

The UI is now translatable, with a language picker in Settings.

- **Every user-facing string moved out of Kotlin into `res/values/strings.xml`** — about 180 `string` and `plurals` entries covering the library, Now Playing, Stage Mode, equalizer, backdrops, playlists, settings and the toast messages. Enum labels (`AccentMode`, `VisualizerStyle`, `StageLayout`, `OrientationLock`, `ReplayGainMode`, `SortOrder`, `LibraryTab`) are now resolved through `ui/components/EnumLabels.kt` instead of hardcoded `label` properties.
- **14 translations ship in the APK**: Spanish, Portuguese (Brazil), French, German, Italian, Russian, Polish, Turkish, Hindi, Indonesian, Chinese (Traditional), Chinese (Simplified), Japanese and Korean. Language names are always shown in their own language and are marked `translatable="false"`.
- **Settings → Language** offers *System default* plus the 15 languages. The default is the system language, so nothing changes for existing users. The choice is stored in `AppSettings.language` and mirrored into SharedPreferences by `data/settings/AppLocale.kt`, which `MainActivity.attachBaseContext` applies before any resource is resolved; a change recreates the activity so the whole UI switches immediately.
- Not yet verified on the Pixel 6 — the build was cut while the phone was off. Verify by opening Settings → Language and switching between System default, Español and 日本語.

## versionCode 4 — build 4

Fixes a backdrop bug found on the Pixel 6 after build 3.

- **Fixed: switching video backdrops killed video playback until an app restart.** The `AndroidView` hosting the video surface only ran its factory once, so after a switch the `PlayerView` still pointed at the released player and nothing rendered. The view is now keyed on the video URI with `update`/`onRelease`, so the new player is bound on every switch.
- Verified on the Pixel 6 by switching Neon Horizon → Sunset Drift → Grid Run → Neon Horizon and into Stage Mode, measuring frame movement in the backdrop area each time (0.6–2.2 before, 0.000 after a switch on build 3).

## versionCode 3 — build 3

First round of fixes from the TODO list. All verified on the Pixel 6.

- **Backdrop thumbnails are now real poster frames.** Previously every video tile fell back to the static synthwave image because Coil cannot decode a frame from an APK asset URI. Frames are extracted with `MediaMetadataRetriever` from `assets.openFd(...)` and cached as JPGs in `cacheDir`.
- **Video backdrops loop.** `STATE_ENDED` now seeks to 0 and resumes, and the lifecycle observer resumes playback on `ON_START` instead of leaving the player ended.
- **Backdrops picker shows the kind of each tile.** A small `VIDEO` (cyan) or `STILL` (magenta) pill sits in the bottom-left corner.
- **Library tabs no longer clip.** The Folders tab rendered as "FOL" at the right edge; tab spacing and letter spacing were tightened so all five fit.
- **Visualizer idle state.** Paused playback drew every band at its minimum height, which read as a row of dots. Bands below a small floor are now skipped and a faint baseline is drawn instead.
- **Settings → About shows the real build.** Reads `BuildConfig.VERSION_NAME` / `VERSION_CODE`, so it displays `NeonPlayer 1.0.0 (build 3)` instead of a hardcoded "1.0".

## versionCode 2 — build 2

- **Landscape is declared optional.** The activity is still locked to landscape, but the manifest no longer tells Play the app requires a landscape screen, so tablets, Chromebooks and desktop-window devices are not filtered out.
- No behaviour changes.

## versionCode 1 — build 1

- First release: landscape two-pane library and Now Playing, Stage Mode with four layouts, visualizers fed by a PCM audio processor, equalizer with presets, playlists with M3U import/export, queue with drag reorder, glass or flat icons, static and video backdrops, background playback with notification and lock-screen controls.
