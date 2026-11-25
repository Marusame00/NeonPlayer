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

## Play Console checklist

### Do now, while still in testing (slow, one-time, harmless if the pack never ships)

1. **Payments profile** — Play Console → **Setup → Payments profile**. Required before you can sell
   anything at all, including a separate paid expansion app. Needs bank account and tax details,
   and verification can take several days. This is the long pole; start it early.
2. **License testing** — Play Console → **Setup → License testing**. Add your tester Gmail
   addresses. Test purchases then complete without charging the card. Do this before any billing
   test, otherwise testers pay for real.

Neither changes any current declaration, so they are safe to set up now.

### Decide the shape before doing anything else

- **Option A — separate paid expansion app** (`com.neonplayer.expansion.gold`). Own listing, no
  Play Billing library, no `BILLING` permission. Matches the seam already in the code. The
  one-time developer registration fee covers unlimited apps, so a second listing costs nothing
  extra. Simplest and the recommended route.
- **Option B — in-app product inside NeonPlayer.** Requires the Play Billing library, the
  `com.android.vending.BILLING` permission, products configured under **Monetise → Products →
  In-app products**, and the app live on a track before products can be purchased.

### Change at ship time, not before

- **Content rating questionnaire** (App content → Content rating) — answer the digital purchases
  question as Yes and resubmit. The current rating assumes no purchases.
- **Data safety** — re-check the form. Google processes the payment itself, so most answers stay
  "No data collected", but purchase history is a declarable type; confirm against the current form
  rather than assuming.
- **Store listing** — Play adds an "In-app purchases" indicator automatically once products exist.
  Mention the optional pack in the description and state that the free app is fully functional
  without it.
- **Option B only** — the build carrying billing must be live on a track before products can be
  bought, so billing cannot be tested from a draft.

### Unverified

Whether a second paid listing from a personal developer account needs its own closed test before
production access. Confirm in Play Console when the time comes rather than assuming either way.

## Do not

- Do not add premium enum values, colours or strings to the base app.
- Do not add a boolean "unlocked" flag that gates content already present in the APK.
- Do not add runtime purchase verification that can revoke a theme a customer already has.
