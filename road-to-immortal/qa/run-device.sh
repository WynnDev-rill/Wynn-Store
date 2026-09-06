#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output
adb logcat -c
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install -r app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Both variants share the explicit development certificate. The test driver uses
# accessibility and shell only, so it can verify the actual R8-optimized APK.
set +e
adb shell am instrument -w -r -e class id.wynn.roadtoimmortal.UserJourneyTest id.wynn.roadtoimmortal.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/instrumentation.txt
instrument_result=${PIPESTATUS[0]}
# Do not uninstall the app before collecting its screenshots (UTP normally does).
adb pull /sdcard/Android/data/id.wynn.roadtoimmortal/files/qa/. qa/output/ || true
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
