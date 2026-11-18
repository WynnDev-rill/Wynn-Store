---
name: VocaMemo
description: Kamus dan latihan kosakata Inggris–Indonesia di Android.
colors:
  primary: "#5943A8"
  primary-container: "#EBE1FF"
  secondary-container: "#F9E0EB"
  tertiary-container: "#D7F0E8"
  background: "#F8F5FC"
  on-background: "#261F30"
  dark-primary: "#D2BFFF"
  dark-background: "#1D1921"
  dark-surface: "#27212D"
  dark-on-background: "#F1E8F4"
typography:
  headline:
    fontFamily: Poppins
    fontWeight: 600
  body:
    fontFamily: Nunito
    fontWeight: 400
rounded:
  field: 24dp
  entry: 18dp
  panel: 30dp
---

# VocaMemo visual system

## Direction

The user pinned the Vocap visual language and a content-first pastel dictionary. The native interpretation combines a lavender canvas, clear dark ink, soft rose and mint, Poppins headings, Nunito reading text, colourful word photographs and restrained surface depth. The VocaMemo launcher retains its original identity.

The phone navigation capsule follows the system insets. A moving lavender/rose highlight locates the active destination; it animates between positions using Compose's animation scale. Labels and selected semantics identify the active destination independently of colour. Wider windows switch to a navigation rail. Search, settings, system Back, IME and TalkBack remain native.

## Screens

- **Kamus.** Use a compact context bar and shared search control. Show three browse categories, Semua Kata, Belum Disimpan and Bergambar. Keep A–Z, discovery and random lists available as direct actions. Row hierarchy is headword, matching gloss, then any spelling suggestion. Maintain the existing paging, backed search and position on return from detail.
- **Detail.** Use a pastel title and pronunciation panel, then individual sense cards with clear save states. Photos identify a concrete meaning. Retain usage examples and their sources.
- **Memo.** Use the shared field, existing collection/status filters and low-contrast white or plum cards. Keep editing, exact meaning, favorite, mastery, notes and pronunciation.
- **Latihan.** Lead with the due queue, then direction, collection and the existing modes. Recall, typed feedback, timers and session results use Material controls, shared roles and full-size touch targets.
- **Profil.** Start with guest or Google identity, then daily target, a seven-day review chart, totals, accuracy, achievements and words that need review. Values come from local Room data. TalkBack gets day and answer count for each bar.
- **Onboarding and sign-in.** Single first-run screen with a usable guest path. Users who already have a dictionary or personal database go straight to the app. Google account switching uses Android Credential Manager.
- **Setelan.** Group controls under appearance, goals, backup, TTS, updates and data sources. Use Material 3 dialogs for decisions that need focus.

## Layout and assets

Use 18–20 dp outer gutters, 6–12 dp within groups and 14–20 dp between groups. Search and pill containers take their shape roles from Theme.kt. Use white over pale lavender or warm plum over near-black plum. Keep pastel accents readable using the corresponding on-container role. Use compact word initials for absent photos. Render bundled WebP photos only for composed rows and the selected detail, without a network load or growing cache. Keep per-file credits in the app and repository. Use Poppins for headings and Nunito for labels, Indonesian glosses and long reading.
