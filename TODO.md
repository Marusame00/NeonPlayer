# NeonPlayer — TODO

Deferred work. Nothing here is in progress; pick items up when there is a reason to cut a new build.

## Done in versionCode 3 (verified on the Pixel 6)

- [x] **Folders tab is clipped** in the landscape library tab row (rendered as "FOL" at the right edge in vc2). Fixed in `ui/components/Inputs.kt`: `NeonTabs` row spacing 14dp → 10dp and tab label letter spacing 1.2sp, so all five tabs fit without clipping.
- [x] **Visualizer idle state looked like a dotted line.** Fixed in `ui/visualizer/Visualizer.kt`: `drawBars` skips bands below a 0.015 floor and draws a single faint baseline when the level is ~0, instead of minimum-height bars. Not a seek-bar bug — the seek bar renders correctly.
- [x] **All three bundled video thumbnails looked identical** in the Backdrops picker. Fixed in `ui/backdrop/BackdropCatalog.kt`: `BackdropOption` carries `assetFile`, and `posterFor(context, assetFile)` extracts a frame with `MediaMetadataRetriever` from `assets.openFd(...)` and caches it as a JPG in `cacheDir`. `BackdropThumbnail` (`ui/backdrop/Backdrop.kt`) loads that poster via `AsyncImage(File(path))` on the IO dispatcher and only falls back to `StaticBackdrop` if extraction fails.
- [x] **Video backdrop played once and then stopped**, including in Stage Mode. Fixed in `ui/backdrop/Backdrop.kt`: `VideoBackdrop` now has a player listener (`STATE_ENDED` → `seekTo(0)` + `play()`, `onRenderedFirstFrame` → `rendered = true`, `onPlayerError` → `rendered = false`) plus a lifecycle observer that pauses on `ON_STOP` and resumes on `ON_START`. Looping confirmed in the main player and in Stage Mode.
- [x] **Settings → About showed a hardcoded "NeonPlayer 1.0".** Fixed in `ui/settings/SettingsScreen.kt`: reads `BuildConfig.VERSION_NAME` / `VERSION_CODE` (with `buildFeatures.buildConfig = true`), so it now shows `NeonPlayer 1.0.0 (build 3)`.
- [x] **Backdrops picker gave no hint whether a tile is a picture or a video.** Added a small `STILL` / `VIDEO` pill in the bottom-start corner of every tile in `ui/settings/BackdropPickerScreen.kt` (cyan for video, magenta for still).

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
