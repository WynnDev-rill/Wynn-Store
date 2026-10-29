# Wynn Store: instructions for coding agents

This repository contains the Wynn Store website and several independent projects.
Work inside the requested project's directory; do not apply changes to other apps
just because they share this repository. Read that project's `AGENTS.md` before editing.

- `iwatchyou/`: native Android screen-time awareness app. See `iwatchyou/AGENTS.md`.
- `screenguard-pro/`: separate Android app with its own instructions. Its Gradle
  wrapper is also used to build IWatchYou with `-p iwatchyou`.
- `furina-pocket-pet/` and `karhutla-kalimantan/`: independent projects with their
  own instructions.
- `src/`, root `package.json`: the store website, not the Android app.
- `.github/workflows/iwatchyou-android.yml`: IWatchYou CI and distribution workflow.

Keep signing keys, credentials, local SDK paths, build caches and device logs out
of version control. Do not publish or change stable APK feeds as part of a source
fix unless the release has been explicitly requested and its checks have passed.
Report what was tested and distinguish emulator results from physical-device results.
