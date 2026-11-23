# NeonPlayer — TODO

Deferred work. Nothing here is in progress; pick items up when there is a reason to cut a new build.

## UI polish (needs versionCode 3 + a Play upload to reach the phone)

- [ ] **Folders tab is clipped** in the landscape library tab row. It renders as "FOL" at the right edge (confirmed on the Pixel 6, vc2). The row scrolls, but the cut-off label looks broken. Options: shrink tab padding/letter spacing, drop the letter spacing on tabs only, or use a scrollable row with an edge fade so the clipping reads as intentional.
- [ ] **Visualizer idle state looks like a dotted line.** With playback paused, the neon-bars visualizer draws every band at its minimum height, which reads as a row of magenta dots across the card instead of an empty visualizer area. Fix in `ui/visualizer/Visualizer.kt`: fade the whole visualizer out (alpha) when `VisualizerBus.level` is ~0 for a moment, or draw a single faint baseline instead of minimum-height bars. Not a seek-bar bug — the seek bar renders correctly.

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
