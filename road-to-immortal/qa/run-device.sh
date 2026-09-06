#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output
adb logcat -c
./gradlew connectedDebugAndroidTest --stacktrace
journey_result=$?
# Collect evidence even when an assertion fails. The emulator runner invokes each
# script line in its own shell, so status and cleanup live together in this file.
adb pull /sdcard/Android/data/id.wynn.roadtoimmortal/files/qa/. qa/output/ || true
adb logcat -d > qa/output/logcat.txt
adb shell dumpsys gfxinfo id.wynn.roadtoimmortal > qa/output/gfxinfo.txt
adb shell dumpsys meminfo id.wynn.roadtoimmortal > qa/output/meminfo.txt
exit "$journey_result"
