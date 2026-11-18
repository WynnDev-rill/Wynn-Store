---
name: VocaMemo
description: Native Android vocabulary lookup and daily recall.
colors:
  primary: "#245CCA"
  on-primary: "#FFFFFF"
  primary-container: "#DEE9FF"
  on-primary-container: "#102F68"
  secondary: "#806000"
  on-secondary: "#FFFFFF"
  secondary-container: "#FFE8A6"
  on-secondary-container: "#513A00"
  tertiary: "#006C58"
  on-tertiary: "#FFFFFF"
  tertiary-container: "#C2F0DE"
  on-tertiary-container: "#004D3D"
  background: "#F4F7FD"
  on-background: "#172842"
  surface: "#FFFFFF"
  on-surface: "#172842"
  surface-variant: "#E7EDF8"
  on-surface-variant: "#475770"
  outline: "#718099"
  dark-primary: "#AFC9FF"
  dark-on-primary: "#002D70"
  dark-primary-container: "#183E7B"
  dark-on-primary-container: "#DEE9FF"
  dark-secondary: "#EAC45F"
  dark-secondary-container: "#594300"
  dark-on-secondary-container: "#FFE8A6"
  dark-tertiary: "#83D7B7"
  dark-tertiary-container: "#005340"
  dark-on-tertiary-container: "#C2F0DE"
  dark-background: "#101B2D"
  dark-on-background: "#E5EDFF"
  dark-surface: "#18253A"
  dark-on-surface: "#E5EDFF"
  dark-surface-variant: "#28364D"
  dark-on-surface-variant: "#C1CDE2"
  dark-outline: "#91A1BC"
typography:
  display-large:
    fontFamily: "Nunito"
    fontSize: "57sp"
    fontWeight: 400
    lineHeight: "64sp"
    letterSpacing: "-0.25sp"
  display-medium:
    fontFamily: "Nunito"
    fontSize: "45sp"
    fontWeight: 400
    lineHeight: "52sp"
    letterSpacing: "0sp"
  display-small:
    fontFamily: "Nunito"
    fontSize: "36sp"
    fontWeight: 400
    lineHeight: "44sp"
    letterSpacing: "0sp"
  headline-large:
    fontFamily: "Nunito"
    fontSize: "32sp"
    fontWeight: 400
    lineHeight: "40sp"
    letterSpacing: "0sp"
  headline-medium:
    fontFamily: "Nunito"
    fontSize: "28sp"
    fontWeight: 400
    lineHeight: "36sp"
    letterSpacing: "0sp"
  headline-small:
    fontFamily: "Nunito"
    fontSize: "24sp"
    fontWeight: 400
    lineHeight: "32sp"
    letterSpacing: "0sp"
  title-large:
    fontFamily: "Nunito"
    fontSize: "22sp"
    fontWeight: 400
    lineHeight: "28sp"
    letterSpacing: "0sp"
  title-medium:
    fontFamily: "Nunito"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
    letterSpacing: "0.15sp"
  title-small:
    fontFamily: "Nunito"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
  body-large:
    fontFamily: "Nunito"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.5sp"
  body-medium:
    fontFamily: "Nunito"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "0.25sp"
  body-small:
    fontFamily: "Nunito"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
    letterSpacing: "0.4sp"
  label-large:
    fontFamily: "Nunito"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
  label-medium:
    fontFamily: "Nunito"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
  label-small:
    fontFamily: "Nunito"
    fontSize: "11sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
  page-heading:
    fontFamily: "Nunito"
    fontSize: "32sp"
    fontWeight: 800
    lineHeight: "40sp"
    letterSpacing: "0sp"
  recall-prompt:
    fontFamily: "Nunito"
    fontSize: "32sp"
    fontWeight: 800
    lineHeight: "40sp"
    letterSpacing: "0sp"
  recall-answer:
    fontFamily: "Nunito"
    fontSize: "28sp"
    fontWeight: 700
    lineHeight: "36sp"
    letterSpacing: "0sp"
rounded:
  field: "18dp"
  compact-card: "20dp"
  panel: "24dp"
  recall: "28dp"
  pill: "50%"
spacing:
  step-4: "4dp"
  step-6: "6dp"
  step-8: "8dp"
  step-10: "10dp"
  step-12: "12dp"
  step-16: "16dp"
  step-18: "18dp"
  step-20: "20dp"
  step-22: "22dp"
  step-24: "24dp"
  step-28: "28dp"
  step-32: "32dp"
components:
  search-field:
    rounded: "{rounded.compact-card}"
    width: "fillMaxWidth"
  information-panel:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    rounded: "{rounded.panel}"
    padding: "22dp"
  practice-invitation:
    backgroundColor: "{colors.secondary-container}"
    textColor: "{colors.on-secondary-container}"
    rounded: "{rounded.panel}"
    padding: "20dp"
  recall-card:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    rounded: "{rounded.recall}"
    padding: "24dp"
    width: "fillMaxWidth"
  bottom-navigation:
    backgroundColor: "{colors.surface}"
    typography: "{typography.label-medium}"
---
# Design System: VocaMemo

## Overview

**Creative North Star: "A meaning carried into memory"**

VocaMemo is a calm, compact native Android learning surface: cool blue ground, white cards, yellow bookmarks and rounded Nunito. The layered flashcard identity connects dictionary lookup to a personal daily recall routine. Indonesian interface copy and source attribution remain readable parts of the interface.

The implementation uses Kotlin, Jetpack Compose and Material 3. Native touch, back navigation, ripples, system text scaling and lifecycle behavior govern components; these tokens use Android dp and sp, not web layout units.

**Key Characteristics:**

- Blue and white learning surfaces with yellow emphasis.
- Rounded cards and Material rounded vector icons.
- One bundled type family across every Material typography role.
- Scrollable content with focused practice and persistent main navigation.

### Direction contract

THESIS: a word's selected meaning travels intact from lookup to daily recall.
OWN-WORLD: cool pale blue ground, white surfaces, blue type accents, yellow bookmark highlights, dark navy body text. Nunito, Material rounded icons, 20–28dp cards, 20dp outer gutters, 8dp rhythm.
STORY: search a real word, inspect contextual senses, save one meaning, practise and see actual progress.
FIRST VIEWPORT: compact brand bar; large two-line purpose heading; prominent labelled search; offline dictionary source/count panel; real quick-word shortcuts; daily practice invitation. This is the home composition; smaller viewports scroll to later content.
FORM: code-led native Material 3 with bottom navigation and focused practice surface; seed: none (pinned), user requested supplied blue icon and education references.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.

The contract above preserves the accepted direction. The tokens and component descriptions below record the implemented system. Product facts belong in PRODUCT.md; research and reference limitations remain in docs/research.md.

## Colors

The primary blue leads actions and information; warm yellow marks saved/daily-study emphasis, while green communicates successful progress. Frontmatter records the explicit Theme.kt overrides for light and dark modes. Other Material roles, including error colors and dark on-secondary/on-tertiary, inherit the installed Material 3 scheme; they are not custom palette tokens.

### Primary

- **Study Blue:** primary actions, pronunciation accents and focus treatment.
- **Pale Blue / Deep Blue containers:** offline information, adaptive review invitation and recall cards, paired with on-primary-container text.

### Secondary

- **Bookmark Yellow:** the daily-practice card, favourite emphasis and selected navigation indicator. The deeper secondary ink supports labels on the yellow container.

### Tertiary

- **Progress Green:** completion and positive feedback; success also has readable text or icon context.

### Neutral

- **Cool Paper:** page background and app-bar ground.
- **White / Night Surface:** resting content cards and bottom navigation.
- **Navy / Pale Ink:** primary reading text; on-surface-variant handles supporting copy, outline handles field boundaries.

**The Semantic Pairing Rule.** Use the active Material colorScheme and its matching on-color for each surface; carry the same role into dark mode.

Themes are selected in Setelan as Sistem, Terang or Gelap. Sistem follows Android's theme. The window background matches the active page background; status and navigation bar icon appearance follows the same theme. Scaffold insets and the navigation surface maintain readable system-bar areas.

## Typography

**Display, body and label font:** bundled static Nunito. Theme.kt maps Normal (400), Medium (500), SemiBold (600), Bold (700) and ExtraBold (800) to local resources. No font download is needed. Android supplies missing IPA glyphs.

The frontmatter lists all 15 inherited Material 3 base roles and three recurring emphasis overrides. Size, line-height and tracking retain Typography() defaults; the font family is replaced for every role. Display roles are available to the theme but are not a separate hero treatment in the current screens.

- **Headline:** page headings and recall prompts use headlineLarge with ExtraBold; revealed recall answers use headlineMedium with Bold.
- **Title:** app bar, section titles, list words and mode names; common overrides use Bold or ExtraBold while retaining the role's size.
- **Body:** bodyLarge is the main reading role; bodyMedium carries context and secondary descriptions; bodySmall carries attribution, caveats and hints.
- **Label:** Material controls, pills, navigation and compact counts. Case follows the Indonesian/word content, with no enforced all-caps style.

**The One Family Rule.** Every Material typography role uses bundled Nunito; retain Android glyph fallback for IPA and other unsupported characters.

## Layout

A single centered content column fills the available width up to 840dp. Main screen lazy lists use 20dp content padding, usually 16dp gaps; settings uses 20dp gaps. The 8dp rhythm is supplemented by observed 6dp, 10dp, 18dp and 22dp component spacing. Rounded surfaces generally contain 18–24dp padding.

Scaffold reserves system bars, app bar and bottom navigation; the content applies keyboard insets. Main destinations use LazyColumn. Practice, completion and editor content use native vertical scrolling; action groups wrap through the Compose flow layout. Large text can push secondary content below the first viewport, including at 150% text scaling. Do not replace this with fixed-height text boxes. The recall card has a minimum height of 230dp, and grows with its content.

## Elevation & Depth

Depth primarily comes from tonal containers against the cool page ground, rounded silhouettes and native Material state layers. Resting result and mode cards explicitly use surface color; standard Card and Surface elevation remain Material defaults. Dialogs, menus and snackbars use their native Material treatment. There is no custom shadow-token system to replicate. The supplied app icon carries its own layered card artwork; its raster depth is not a rule for casting hard shadows under UI cards.

## Shapes

Soft corners distinguish fields, ordinary cards, information panels and the larger recall surface. Frontmatter records the recurring 18dp, 20dp, 24dp and 28dp shapes. The compact pill uses RoundedCornerShape(50), a percentage shape. Material buttons, filter/suggestion chips, navigation indicators and default cards keep their library shapes rather than receiving one global corner override. Material rounded vector icons are used throughout; meaningful icon actions have accessible descriptions.

## Components

### Buttons

Native Material Button, OutlinedButton, TextButton and IconButton provide primary, secondary and compact actions. Buttons inherit semantic colors, Nunito labels, native ripple, focus and disabled states. Preserve Material minimum interactive touch targets of 48dp, including when the visible chip or icon is smaller. Primary practice/answer actions often span the content width; four self-grades wrap rather than being forced into a single row.

### Chips and pills

SuggestionChip starts real dictionary lookups. FilterChip carries theme, study direction and filters with visible selected state. The Pill helper uses primaryContainer, labelMedium, optional 16dp icon and 12dp horizontal / 6dp vertical padding; it is informational rather than an invented button.

### Cards / Containers

Result and study-mode cards use surface and 20dp corners. The offline panel and daily-practice invitation use 24dp corners with primaryContainer and secondaryContainer respectively. Individual sense and memo cards preserve attribution and selected-meaning context. Containers expand with content; decorative shadows do not replace surface/text contrast.

### Inputs / Fields

OutlinedTextField keeps a persistent label, native focus/error/disabled treatment and semantic outline. Search is full width, single-line, with a 20dp shape, leading search icon and labelled clear action. The recall answer field uses an 18dp shape. Search has a loading state and an actionable empty-state explanation; short labels remain distinct from examples and explanatory text.

### Navigation

The app bar combines the supplied icon, VocaMemo title and labelled settings action. A surface-colored NavigationBar exposes Kamus, Memo, Latihan and Statistik with icon, text label and selected indicator. Settings and active practice hide the bottom navigation; practice replaces the brand bar with its mode title and labelled close action. Back and close confirm exit from active practice; deleting a memo also uses an explicit confirmation dialog.

### Recall card and motion

The primaryContainer recall card uses 28dp corners, 24dp inner padding, centered prompt/context and a divider before the revealed answer. Non-typing cards can reveal on tap, with a visible alternative answer button. The prompt stays readable while animateContentSize and AnimatedContent handle content growth and answer changes using Compose defaults and the system animation duration scale. No custom duration/easing token is defined.

**The Readable Recall Rule.** The prompt and revealed answer remain full-contrast text in a stable card; motion communicates the answer change without making reading depend on animation.

Mechanical autoplay waits 3000ms before revealing and 2400ms before advancing. It runs only while the screen is resumed and does not add graded-answer statistics. Speed-test elapsed time is accumulated only while resumed. Backgrounding also stops speech. Correctness feedback includes explicit text and a polite live region, with enabled/disabled controls reflecting the answer state.

### Feedback and empty states

Hint uses a primary rounded vector icon, Bold title and readable body copy centered in an open column. Native progress indicators distinguish loading, Snackbar reports outcomes, and dialogs keep confirmation/dismiss actions explicit. Preserve these states when extending screens instead of leaving silent blank regions.

## Do's and Don'ts

### Do:

- **Do** use active semantic color pairs and the bundled Nunito weights.
- **Do** keep text regions content-driven, native scrolling available and action touch targets at least 48dp.
- **Do** preserve labelled icon actions, Material focus/ripple feedback and Android back behavior.
- **Do** retain source/sense attribution, distinguish personal notes and keep dictionary licence notices accessible.
- **Do** check light, dark and 150% text presentations when extending screens.

### Don't:

- **Don't** hardcode light-theme colors into new screens.
- **Don't** shrink type or clip text to preserve a screenshot composition.
- **Don't** dim recall answers, encode correctness through color alone or require animation to understand the card.
- **Don't** replace native controls with web hover patterns or glyph characters posing as icons.
- **Don't** advance timers or automatic mechanical repetition while the app is in the background.

### Implementation evidence and scope

Recorded from Theme.kt and sampled MainActivity.kt components, with existing rendered evidence in /workspace/downloads/final35/screenshots (home, dark large text and revealed flashcard) on 2026-11-17. The screenshot review is emulator evidence; this documentation pass did not run runtime or hardware tests. Default Material behaviors are described as defaults rather than restated as custom tokens. Nunito, native scrolling, stable recall contrast and system-bar background pairing replace the provisional shorthand with implementation facts.

Ponytail Full controls code simplicity; UI UX Pro Max supplies accessibility/touch/Compose checks; Impeccable supplies visual quality and final rendered review. User brief and native conventions win conflicts. Skill copies and provenance remain in docs/tooling.md. No generic web hover, CSS breakpoint or synthetic tonal-ramp rules are added to this Android system.

## Kamus 1.1

The first viewport uses the existing Nunito and Material 3 tokens. It shows a compact labelled search field, browse filters, alphabet and discovery controls, then English headwords with Indonesian summaries. Search switches to language controls. The alphabet dialog uses a wrapping grid of 48 dp targets. The dictionary keeps its position when returning from detail. Each row opens the existing sense picker and Memo save action. Corpus information now appears in Setelan.
