# VocaMemo 1.2 design and engineering audit

## References

Reviewed all 12 published images in the [Vocap Behance project](https://www.behance.net/gallery/221855965/Language-Learning-Mobile-App-UI-UX-Design-Vocap), including the cover, product overview, design process, onboarding and auth, Poppins palette sheet, icon, learning search/detail, Library/Practice/Explore/Profile screen series, chat layouts, screen collage and closing detail. The sign-in hierarchy, pastel colour zones, thumbnail vocabulary rows, profile rhythm and floating navigation inform the Compose implementation. VocaMemo uses its own icon, screen composition, language and local learning logic. The project assets are reference-only.

The [Pastel Dictionary Dribbble shot](https://dribbble.com/shots/20675365-Dictionary-Mobile-App) returned an access challenge through direct requests, embed and indexing paths. The dictionary applies the requested compact pastel approach, grounded in the visible Vocap search/detail panels and the user's incumbent Kamus screenshot.

The supplied incumbent screenshots show stacked headers and controls before the first word, low differentiation across main tabs, and a Statistics screen without identity or achievements. The new system reduces the top bar, separates browse scope from actions, gives the due queue a clear entry point and combines existing learning history with the profile.

## Implementation audit

| Area | Evidence and design constraint |
| --- | --- |
| Dictionary | The previous corpus, keys, search directions, typo and lemma logic, pagination and contextual examples are intact. Bergambar performs an indexed, bound lookup for the curated headwords. |
| Persistence | Existing Room database, schema, preferences file, backup JSON and scheduler are preserved. New profile and onboarding values use independent preference keys. Sign-out removes only the local Google profile. |
| Learning | All practice modes, accuracy and daily goal remain available. Profile derives the chart and achievements from reviews, saved words and mastery rather than duplicating state. |
| Signing and updater | Build increments to version code 3 with the same app ID, API 26 minimum, and certificate. The current stable channel is unchanged pending a separate release request. |
| Assets | Considered Wikidata P18 bulk links, Commons file metadata, external image caching and packaged assets. Bulk images can misrepresent polysemous words and require remote fetch, attribution and eviction. The included, cropped Commons photos are manually reviewed, mapped to concrete senses and packaged as small WebP files with per-file licences. Lazy rows only compose visible thumbnails. Other headwords use a themed initial, with no network request. |
| Design tooling | Used the vendored Ponytail Full, Impeccable native context and UI UX Pro Max. Graphify covers the architecture and refreshed source. |

## Google sign-in

Android Credential Manager uses the Google provider and a Google Cloud Web application OAuth Client ID. It presents the Android account picker and obtains the selected profile. The app saves only display name and email in private preferences. It discards the ID token after obtaining the name and email, uses no backend resources, and never treats the local profile as an authorization grant. Signing in does not upload Memo or progress, and signing out keeps local history. Guest mode works without Google Play services or network access.

Configure the Google Auth Platform consent screen and publish or add test users. Create an Android OAuth client for `id.wynn.vocamemo` and the **SHA-1** signing certificate of the APK being tested, then create a **Web application** OAuth client in that project. The public release certificate SHA-1 is `3C:3C:F9:61:CC:10:52:21:65:A3:7F:89:DB:C8:8E:31:7F:9B:4F:BC`. For debug testing use a separate Android client for `id.wynn.vocamemo.debug` and the debug certificate. Provide the **Web** client ID as `VOCA_GOOGLE_WEB_CLIENT_ID` at build time, or set the GitHub Actions repository variable of that name. The ID is a public identifier. Android builds require no Google client secret. When no client is configured, the app keeps guest mode and explains its Google sign-in availability. A real Google account/device is required to verify the live provider flow.

[Google Credential Manager integration](https://developer.android.com/identity/sign-in/credential-manager-siwg) · [Google branding guidelines](https://developers.google.com/identity/branding-guidelines)
