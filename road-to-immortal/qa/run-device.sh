#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output
adb logcat -c
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -W -n id.wynn.roadtoimmortal/.MainActivity > qa/output/cold-start.txt
sleep 4
adb exec-out screencap -p > qa/output/00-standalone-launch.png
adb shell am force-stop id.wynn.roadtoimmortal
adb install -r qa-driver/build/outputs/apk/debug/qa-driver-debug.apk
adb install -r qa-driver/build/outputs/apk/androidTest/debug/qa-driver-debug-androidTest.apk
# Tests run in their own process: no dependency on unminified consumer classes.
set +e
adb shell am instrument -w -r -e class id.wynn.roadtoimmortal.UserJourneyTest id.wynn.roadtoimmortal.qa.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/instrumentation.txt
instrument_result=${PIPESTATUS[0]}
# Do not uninstall the app before collecting its screenshots (UTP normally does).
adb pull /sdcard/Android/data/id.wynn.roadtoimmortal.qa/files/qa/. qa/output/ || true
adb logcat -d > qa/output/logcat.txt
adb shell dumpsys gfxinfo id.wynn.roadtoimmortal > qa/output/gfxinfo-final.txt
adb shell dumpsys meminfo id.wynn.roadtoimmortal > qa/output/meminfo-final.txt
adb shell dumpsys package id.wynn.roadtoimmortal > qa/output/package.txt
if [ "$instrument_result" -ne 0 ]; then exit "$instrument_result"; fi
python3 - <<'PY'
from pathlib import Path
import re
text = Path('qa/output/instrumentation.txt').read_text()
assert re.search(r'OK \([1-9][0-9]* tests?\)', text), 'Installed APK user journey did not pass; inspect QA evidence.'
assert not any(x in text for x in ['FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed']), text
PY
