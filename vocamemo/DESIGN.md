---
name: VocaMemo
description: Offline English–Indonesian dictionary and recall on Android.
colors:
  jade: "#13584C"
  jade-tint: "#D6E9DF"
  ivory: "#F4F6F0"
  paper: "#FBFCF8"
  ink: "#1C2923"
  muted-ink: "#4C5B51"
  graphite: "#191A18"
  graphite-surface: "#222421"
  ivory-ink: "#F2F1E9"
  muted-ivory: "#C0C3B7"
  night-jade: "#A3D3BB"
  night-jade-tint: "#293B32"
typography:
  headline:
    fontFamily: "Plus Jakarta Sans"
    fontSize: "34sp"
    fontWeight: 700
    lineHeight: "42sp"
  section:
    fontFamily: "Plus Jakarta Sans"
    fontSize: "20sp"
    fontWeight: 700
    lineHeight: "28sp"
  title:
    fontFamily: "Plus Jakarta Sans"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "23sp"
  body:
    fontFamily: "Plus Jakarta Sans"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  supporting:
    fontFamily: "Plus Jakarta Sans"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "21sp"
  entry:
    fontFamily: "Literata"
    fontSize: "23sp"
    fontWeight: 500
    lineHeight: "29sp"
  entry-detail:
    fontFamily: "Literata"
    fontSize: "40sp"
    fontWeight: 500
    lineHeight: "50sp"
rounded:
  field: "18dp"
  panel: "22dp"
  dock: "28dp"
spacing:
  gutter: "20dp"
  compact: "8dp"
  section: "24dp"
components:
  search-field:
    rounded: "{rounded.field}"
    width: "fillMaxWidth"
  bottom-navigation:
    rounded: "{rounded.dock}"
    width: "500dp max"
  dictionary-word:
    typography: "{typography.entry}"
  today-target:
    backgroundColor: "{colors.jade-tint}"
    rounded: "{rounded.panel}"
    padding: "18dp"
---
# Design System: VocaMemo

## Overview

Open to a page of words. The same word keeps its typographic identity through reading, saving, and recall. A single alphabetical margin makes the browse list feel like a dictionary page. The floating controls use a glass finish with a subtle top rim, and the reading column uses a quiet background with clear text.

## Colors

Ivory and jade support daylight reading. Warm graphite, warm white and muted jade serve the night theme. Gold indicates favourites and muted jade indicates navigation selection and progress. Use Material semantic color pairs. In particular, use onPrimaryContainer on the daily target and onSurfaceVariant for supporting definitions. The selected theme persists locally and also controls system bar contrast.

## Typography

Plus Jakarta Sans covers navigation, controls, Indonesian definitions and statistics. Literata identifies English headwords in Kamus, Memo and English prompts in practice. Both families are bundled under SIL OFL. Use Android sp and allow long headwords, Indonesian definitions and settings labels to take extra lines. The interface relies on Android glyph fallback for unsupported IPA, scripts and symbols.

## Layout

Use a centered main column up to 720 dp, 20 dp side gutters and a bottom bar up to 500 dp. Reserve status, navigation and keyboard insets. The keyboard dismisses on lookup, and the dock hides during typing, settings and active practice. Keep search accessible above the lazy dictionary list. Open details without losing the browse position. The alphabet index and filters remain available in browse mode. Use native scrolling and wrapping with large text.

## Elevation & Depth

The search field, bottom navigation, practice controls and theme picker use translucent gradients over a muted background with a highlighted rim. The dock has a 7 dp native shadow. The effect uses Compose brushes and clipping on Android 8 and newer. Keep dictionary rows, settings data and long text in the reading layer.

## Shapes

Use 10 dp small controls, 16 dp middle controls, 22 dp panels and a 28 dp dock. Keep ripple, focus, disabled states and at least 48 dp interactive targets. Secondary rows get a hairline divider.

## Components

- **Kamus** presents headword, translation preview and saved status in that order. A–Z jumps to a letter. Temukan Kata opens an unsaved real entry. Search retains English and Indonesian directions.
- **Detail and Memo** keep word and meaning separate. The sense, source attribution, pronunciation and saved state remain associated with their original records.
- **Latihan** shows due reviews, direction, collection and five additional modes. An active session uses a stable card and animates the reveal with Compose, respecting the system animation scale.
- **Statistik** shows a daily target, seven local calendar days as a column chart, lifetime totals and difficult words. Each chart column exposes its exact date and count to accessibility services.
- **Setelan** groups appearance, learning, backup, updates and sources. Theme switches take effect immediately. The daily goal uses a focused editor. Licences stay available from Tentang.

## Do's and Don'ts

- Give controls short Indonesian labels and keep necessary status or error messages next to the affected action.
- Maintain at least 4.5:1 contrast for standard text and pair state colors with a visible label, icon or shape.
- Check system, light and dark themes, larger Android text, keyboard insets and offline navigation when modifying these screens.
