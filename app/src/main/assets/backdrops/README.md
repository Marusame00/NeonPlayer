# Backdrop videos — drop yours here

Every video file in **this folder** (`app/src/main/assets/backdrops/`) shows up automatically in **Settings → Backdrop → Choose backdrop**, after "Static synthwave". You don't need to change any code. Just rebuild the app.

## Naming

Files are sorted by name, and the label comes from the file name:

| File | Shown as |
|---|---|
| `01_neon_horizon.mp4` | Neon Horizon |
| `02_sunset_drift.mp4` | Sunset Drift |
| `03_grid_run.mp4` | Grid Run |
| `04_my_city_night.mp4` | My City Night |

The leading digits only control the order. Supported extensions are `.mp4`, `.webm` and `.mkv`.

## Recommended encoding

The video plays muted and looping behind the UI on a second, lightweight ExoPlayer. Keep it light:

- **H.264 (High profile) or HEVC**, hardware decoded
- **1280×720 or 1920×1080**, **24–30 fps**
- **5–20 s seamless loop.** The last frame should flow into the first one.
- **No audio track.** Any audio is ignored anyway, so strip it to save space.
- Aim for **under 5 MB per clip**, since everything in this folder ends up in the APK.

```bash
ffmpeg -i input.mov -an -vf "scale=1920:-2,fps=30" -c:v libx264 -profile:v high -crf 23 -preset slow -movflags +faststart 04_my_clip.mp4
```

## Without rebuilding

In the backdrop picker, tap **Add my own video** to pick any video on the phone through the system file picker. The app keeps a persistent permission to it. Custom videos can be removed from the picker again.

## Static image

The default static backdrop is `app/src/main/res/drawable-nodpi/backdrop_synthwave.jpg`. Replace that file, keeping the same name, to change it.

## Bundled samples

- `01_neon_horizon.mp4`: the provided sample loop
- `02_sunset_drift.mp4`: a slow Ken Burns drift over the static synthwave image, generated with ffmpeg
- `03_grid_run.mp4`: a procedural neon grid running toward the sun (a 4 s seamless loop), generated with ffmpeg
