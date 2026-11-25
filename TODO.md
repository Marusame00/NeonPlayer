# NeonPlayer — TODO

Deferred work. Nothing here is in progress; pick items up when there is a reason to cut a new build.

## Open

Nothing outstanding.

## Won't fix — Play Console edge-to-edge warnings

Both warnings are advisory ("User experience"), do not block review or rollout, and cannot both be cleared at once.

- **"Edge-to-edge may not display for all users"** asks for a visible `enableEdgeToEdge()` call in the dex. R8 inlines the Kotlin extension into `MainActivity.onCreate`, so the scanner cannot see it. `-keep` rules in `app/proguard-rules.pro` did not prevent the inlining; `dexdump` on the vc8 bundle shows no `ComponentActivity.enableEdgeToEdge` method, only the `enableEdgeToEdge$lambda$1` name.
- **"Deprecated APIs or parameters for edge-to-edge"** narrowed from four locations on vc7 (`mj0.b`, `oj0.b`, `qj0.b`, `u2.m`) to one on vc8: `u2.m`. Disassembly shows `u2.m` is `const/4 v0, #1` followed by `setLayoutInDisplayCutoutMode`, i.e. `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` set by `androidx.activity.EdgeToEdgeApi28.adjustLayoutInDisplayCutoutMode` — inside `enableEdgeToEdge()` itself.

The two are mutually exclusive: clearing the second means dropping `enableEdgeToEdge()` for `WindowCompat.setDecorFitsSystemWindows(window, false)`, which brings the first back because Play's scanner does not recognise that call. Insets are genuinely handled (`WindowInsets.safeDrawing` on the main content, `imePadding()` on the root), verified visually on the Pixel 6, so there is no user-facing defect behind either warning. Leave both.

## Done in versionCode 7 (verified on the Pixel 6)

- [x] **Keyboard covers the search field.** Reported by a tester: "The keyboard covers the text field making it hard to see what to type." The root content in `NeonPlayerApp` now applies `imePadding()`. Verified: the filter field stays fully visible with the keyboard open and typed text renders with the cursor.
- [x] **Missing button in the now-playing transport row.** Reported by the same tester. `TransportRow` needs 318dp but the panel is about 289dp wide in landscape, so the repeat button was clipped off the right edge. The row now measures with `BoxWithConstraints` and scales all six buttons proportionally (floor 62%) when it would overflow. Verified: all six buttons render.

## Done in versionCode 8 (verified on the Pixel 6)

- [x] **Play Console still reported both edge-to-edge warnings on build 7.** "Edge-to-edge may not display for all users" fired because R8 inlined `enableEdgeToEdge()` into `MainActivity.onCreate`, so Play's dex scanner could not see the call; `proguard-rules.pro` now keeps `androidx.activity.EdgeToEdge*` to block the inlining, and the built APK contains `enableEdgeToEdge` plus the `EdgeToEdgeApi*` classes. "Deprecated APIs or parameters" — removed the deprecated `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` from `applyCutoutMode()` and switched the theme's cutout mode from `shortEdges` to `always`. The remaining `setStatusBarColor`/`setNavigationBarColor` refs are inside `enableEdgeToEdge()` itself (AndroidX) and are advisory per Google. Confirming the warnings clear needs a Play upload.

## Done in versionCode 6 (verified on the Pixel 6)

- [x] **Playback error popup on app launch.** `restoreQueue()` re-added saved queue entries without checking the media URI was still readable, so a stale entry raised `ERROR_CODE_IO_NO_PERMISSION` and surfaced as a "Source error" popup. Restored tracks are now filtered through `isReadable()` on the IO dispatcher. Verified: `state=ERROR` count 0 and no `ExoPlayerImplInternal` exceptions after a cold start.
- [x] **Library scroll position is lost when leaving Stage Mode.** The main content is wrapped in a `SaveableStateHolder` in `NeonPlayerApp`, so the lazy list states survive while Stage Mode is composed. Verified: the album list stayed on the same four rows after entering and leaving Stage Mode.
- [x] **Library tab row clips on narrower screens.** `NeonTabs` no longer scrolls horizontally; tabs share the width by label length, letter spacing is 0.8sp, and single-line ellipsis is the last resort. Verified: SONGS ALBUMS ARTISTS PLAYLISTS FOLDERS all render in full.
- [x] **Glass and flat icon previews in Settings were inert.** Both had `onClick = {}` at SettingsScreen.kt:211 and :217; they now set `glassIcons` and show an active tint. The `NeonSwitch` above them always worked.
- [x] **Play Console "For your next release" warnings.**
  - [x] *Edge-to-edge may not display for all users.* `enableEdgeToEdge()` is called at `MainActivity.kt:69` and the main content already applies `WindowInsets.safeDrawing`.
  - [x] *Deprecated APIs or parameters for edge-to-edge.* Removed `android:statusBarColor` and `android:navigationBarColor` from `res/values/themes.xml`.
  - [x] *Remove resizability and orientation restrictions for large screen devices.* Removed `android:screenOrientation="sensorLandscape"` from `AndroidManifest.xml`; the runtime lock from `AppSettings.orientation` (default `LANDSCAPE`) still applies on phones.

## Done in versionCode 5 (verified on the Pixel 6)

- [x] **UI was English-only and hardcoded in Kotlin.** All user-facing text now lives in `res/values/strings.xml` (about 180 `string`/`plurals` entries) and 14 locale folders: `values-es`, `values-pt-rBR`, `values-fr`, `values-de`, `values-it`, `values-ru`, `values-pl`, `values-tr`, `values-hi`, `values-id`, `values-zh-rTW`, `values-zh-rCN`, `values-ja`, `values-ko`.
- [x] **No way to pick a language.** Settings → Language now lists *System default* plus the 15 languages. `AppSettings.language` (empty = system) is persisted by `SettingsRepository`, mirrored into SharedPreferences by `data/settings/AppLocale.kt`, applied in `MainActivity.attachBaseContext`, and a change recreates the activity.
- [x] Verified on the Pixel 6 (2026-11-24): tapping through the languages in Settings → Language swaps the UI text immediately.

## Done in versionCode 3 and 4 (verified on the Pixel 6)

- [x] **Folders tab is clipped** in the landscape library tab row (rendered as "FOL" at the right edge in vc2). Fixed in `ui/components/Inputs.kt`: `NeonTabs` row spacing 14dp → 10dp and tab label letter spacing 1.2sp, so all five tabs fit without clipping.
- [x] **Visualizer idle state looked like a dotted line.** Fixed in `ui/visualizer/Visualizer.kt`: `drawBars` skips bands below a 0.015 floor and draws a single faint baseline when the level is ~0, instead of minimum-height bars. Not a seek-bar bug — the seek bar renders correctly.
- [x] **All three bundled video thumbnails looked identical** in the Backdrops picker. Fixed in `ui/backdrop/BackdropCatalog.kt`: `BackdropOption` carries `assetFile`, and `posterFor(context, assetFile)` extracts a frame with `MediaMetadataRetriever` from `assets.openFd(...)` and caches it as a JPG in `cacheDir`. `BackdropThumbnail` (`ui/backdrop/Backdrop.kt`) loads that poster via `AsyncImage(File(path))` on the IO dispatcher and only falls back to `StaticBackdrop` if extraction fails.
- [x] **Video backdrop played once and then stopped**, including in Stage Mode. Fixed in `ui/backdrop/Backdrop.kt`: `VideoBackdrop` now has a player listener (`STATE_ENDED` → `seekTo(0)` + `play()`, `onRenderedFirstFrame` → `rendered = true`, `onPlayerError` → `rendered = false`) plus a lifecycle observer that pauses on `ON_STOP` and resumes on `ON_START`. Looping confirmed in the main player and in Stage Mode.
- [x] **Settings → About showed a hardcoded "NeonPlayer 1.0".** Fixed in `ui/settings/SettingsScreen.kt`: reads `BuildConfig.VERSION_NAME` / `VERSION_CODE` (with `buildFeatures.buildConfig = true`), so it now shows `NeonPlayer 1.0.0 (build 3)`.
- [x] **Backdrops picker gave no hint whether a tile is a picture or a video.** Added a small `STILL` / `VIDEO` pill in the bottom-start corner of every tile in `ui/settings/BackdropPickerScreen.kt` (cyan for video, magenta for still).
- [x] **Switching backdrops froze every video until an app restart** (reported on the Pixel 6, vc3). Fixed in `ui/backdrop/Backdrop.kt`: the `AndroidView` factory only runs once, so after a switch the `PlayerView` still held the old, released player and nothing rendered. The view is now keyed on the URI with `update = { it.player = exo }` and `onRelease = { it.player = null }`, and the `viewHolder` state was removed. Verified by switching Neon Horizon → Sunset Drift → Grid Run → Neon Horizon and back into Stage Mode, each confirmed moving by frame diff.

## Play Store

- [ ] Data safety form (no data collected or shared).
- [ ] Content rating questionnaire (Utility/Productivity, all "No" → ESRB Everyone / PEGI 3).
- [ ] Target audience: 13–15, 16–17, 18+ only; no under-13 boxes.
- [ ] Ads declaration: No.
- [ ] Closed testing (Alpha): add 12 testers and keep them opted in for 14 continuous days, then apply for production.

## Known limitations (documented, not planned)

- Crossfade is fade-out/fade-in, not an overlapping mix.
- "Edit tags" displays tags but does not write them back.
- Visualizer can lead the audio by roughly 100–200 ms.
