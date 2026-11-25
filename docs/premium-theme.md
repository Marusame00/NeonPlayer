# Theme expansions (not yet shipped)

NeonPlayer may later offer an optional paid theme pack as a donation-style extra: a colour
scheme plus extra backdrops, no additional functionality. This document records the seam that
exists in the code today so a future update does not have to retrofit it, and so nobody
re-introduces the approach that was explicitly rejected.

## The rule: no on-disc DLC

The paid content must **not** ship inside the free app. A palette that is present in the base
APK and merely hidden behind a flag is on-disc DLC, and the project owner rejected that
outright. If no add-on is installed, the base app must contain no premium colours, no premium
backdrops and no premium strings.

## What exists now (versionCode 11)

- `domain/expansion/ThemeExpansion.kt` — `ThemeExpansion` data class and the `ExpansionSource`
  interface, plus `PackageExpansionSource`, which discovers installed add-ons by package-name
  prefix and reads their accent from manifest meta-data.
- `ui/theme/ExpansionPalette.kt` — `LocalThemeExpansions` and the `paletteFor(accentId,
  expansions)` resolver.
- `AppSettings.accent` is a **String id**, not an enum, so an expansion can contribute an id
  without a schema change. Built-in ids are the `AccentMode` names (`CYAN`, `MAGENTA`).
- `NeonTheme(accentId, expansions, …)` provides `LocalThemeExpansions` to the tree.
- Settings → Theme accent renders the built-in chips, then one chip per installed expansion.

With no add-on installed, `PackageExpansionSource.installed()` returns an empty list and the UI
is identical to before. Verified on the Pixel 6 (2026-11-25): the accent row shows only
"Cyan-dominant" and "Magenta-dominant".

## Add-on package contract

A future expansion is a separate APK with a package name starting `com.neonplayer.expansion.`
and these `<meta-data>` entries in its manifest:

| Key | Type | Meaning |
|---|---|---|
| `neonplayer.accent.id` | string | Stable id stored in settings, e.g. `gold` |
| `neonplayer.accent.label` | string | Chip label, e.g. `Gold` |
| `neonplayer.accent.primary` | int | ARGB primary colour |
| `neonplayer.accent.secondary` | int | ARGB secondary colour |
| `neonplayer.accent.tertiary` | int | ARGB tertiary colour |
| `neonplayer.backdrops` | string | Comma-separated asset names, optional |

## Customer-first entitlement

There is no licence check, no network call and no verification. If the package is installed,
the theme is available. That is deliberate: a customer keeps what they paid for across app
updates, reinstalls and device moves. `paletteFor` falls back to Cyan for an unknown or
uninstalled id rather than leaving the UI on a palette that no longer exists.

## Play Console declarations to change when this ships

- **Contains digital purchases** → Yes (currently No).
- **Data safety** → unchanged; the add-on adds no data collection.
- Store listing → mention the optional theme pack, and keep the free app fully functional
  without it.
- The add-on needs its own listing, or ship it as an in-app product — decide at the time.

## Do not

- Do not add premium enum values, colours or strings to the base app.
- Do not add a boolean "unlocked" flag that gates content already present in the APK.
- Do not add runtime purchase verification that can revoke a theme a customer already has.
