# NeonPlayer — TODO

Deferred work. Nothing here is in progress; pick items up when there is a reason to cut a new build.

## UI polish (needs versionCode 3 + a Play upload to reach the phone)

- [ ] **Folders tab is clipped** in the landscape library tab row. It renders as "FOL" at the right edge (confirmed on the Pixel 6, vc2). The row scrolls, but the cut-off label looks broken. Options: shrink tab padding/letter spacing, drop the letter spacing on tabs only, or use a scrollable row with an edge fade so the clipping reads as intentional.
- [ ] **Visualizer idle state looks like a dotted line.** With playback paused, the neon-bars visualizer draws every band at its minimum height, which reads as a row of magenta dots across the card instead of an empty visualizer area. Fix in `ui/visualizer/Visualizer.kt`: fade the whole visualizer out (alpha) when `VisualizerBus.level` is ~0 for a moment, or draw a single faint baseline instead of minimum-height bars. Not a seek-bar bug — the seek bar renders correctly.

## Backdrops

- [ ] **All three bundled video thumbnails look identical** in the Backdrops picker (reported on the Pixel 6, vc2). The three tiles show the static synthwave image instead of a frame from each video. Cause is in `ui/backdrop/BackdropCatalog.kt`: `thumbUri` is `file:///android_asset/backdrops/<file>`, and Coil's video frame decoder needs a real file or file descriptor, which an APK asset does not provide. The `AsyncImage` in `BackdropThumbnail` (`ui/backdrop/Backdrop.kt`) then hits `AsyncImagePainter.State.Error`, sets `failed = true`, and falls back to `StaticBackdrop` — so every video tile renders the same picture. Fix options: extract a poster frame with `MediaMetadataRetriever` from `assets.openFd(...).fileDescriptor` and cache it to `cacheDir`, or ship three small poster JPGs next to the videos and use those as `thumbUri`. Needs on-device verification.
- [ ] **Video backdrop plays once and then stops** (reported on the Pixel 6, vc2). After the first pass the backdrop reverts to the static image, including in Stage Mode, which shares the same root `Backdrop` composable (`ui/main/NeonPlayerApp.kt`). `VideoBackdrop` sets `repeatMode = Player.REPEAT_MODE_ONE` before `prepare()`, so looping should hold; the likely culprits are the lifecycle observer calling `pause()` on `ON_STOP` and `play()` on `ON_START` (a player left in `STATE_ENDED` ignores `play()`), or `onPlayerError` firing and clearing `rendered`. Diagnose with `adb logcat` on the ExoPlayer tag while the backdrop is running, then fix at the root. Needs on-device verification.

## Version display

- [ ] **Settings → About shows a hardcoded "NeonPlayer 1.0"** (`ui/settings/SettingsScreen.kt`, `AboutSection`). Wire it to `BuildConfig.VERSION_NAME` / `VERSION_CODE` so it reads e.g. `1.0.0 (build 2)`. Requires enabling `buildFeatures.buildConfig = true` in `app/build.gradle.kts`.

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
