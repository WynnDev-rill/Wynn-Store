#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output
ADB_BIN=${ADB_BIN:-adb}
"$ADB_BIN" wait-for-device
"$ADB_BIN" shell input keyevent 82
"$ADB_BIN" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB_BIN" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$ADB_BIN" shell pm clear id.wynn.arithmemo.debug
"$ADB_BIN" logcat -c || true
"$ADB_BIN" shell am instrument -w -r id.wynn.arithmemo.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/instrumentation.txt
python3 - <<'PY'
from pathlib import Path
s=Path('qa/output/instrumentation.txt').read_text()
assert 'OK (' in s and 'FAILURES' not in s and 'INSTRUMENTATION_FAILED' not in s,s
PY
for shot in home flashcard stats settings recall feedback; do
  "$ADB_BIN" exec-out run-as id.wynn.arithmemo.debug cat "files/qa-$shot.png" > "qa/output/$shot.png"
done
"$ADB_BIN" shell am force-stop id.wynn.arithmemo.debug
"$ADB_BIN" shell am start -W -n id.wynn.arithmemo.debug/id.wynn.arithmemo.MainActivity
"$ADB_BIN" shell uiautomator dump /sdcard/arithmemo.xml
"$ADB_BIN" pull /sdcard/arithmemo.xml qa/output/relaunch.xml
"$ADB_BIN" exec-out screencap -p > qa/output/home.png
"$ADB_BIN" shell cmd uimode night yes
"$ADB_BIN" shell settings put system font_scale 1.6
"$ADB_BIN" shell am force-stop id.wynn.arithmemo.debug
"$ADB_BIN" shell am start -W -n id.wynn.arithmemo.debug/id.wynn.arithmemo.MainActivity
"$ADB_BIN" shell uiautomator dump /sdcard/arithmemo-large.xml
"$ADB_BIN" pull /sdcard/arithmemo-large.xml qa/output/large-text.xml
"$ADB_BIN" exec-out screencap -p > qa/output/dark-large.png
"$ADB_BIN" shell settings put system font_scale 1.0
"$ADB_BIN" shell cmd uimode night no
"$ADB_BIN" shell settings put system accelerometer_rotation 0
"$ADB_BIN" shell settings put system user_rotation 1
"$ADB_BIN" shell am force-stop id.wynn.arithmemo.debug
"$ADB_BIN" shell am start -W -n id.wynn.arithmemo.debug/id.wynn.arithmemo.MainActivity
"$ADB_BIN" shell uiautomator dump /sdcard/arithmemo-landscape.xml
"$ADB_BIN" exec-out screencap -p > qa/output/landscape.png
"$ADB_BIN" shell settings put system user_rotation 0
"$ADB_BIN" logcat -d -s AndroidRuntime:E > qa/output/crashes.txt
python3 - <<'PY'
from pathlib import Path
s=Path('qa/output/relaunch.xml').read_text()
assert 'Lanjutkan sesi' in s,'Interrupted session was not restored after force-stop'
assert 'FATAL EXCEPTION' not in Path('qa/output/crashes.txt').read_text(),'Android crash recorded'
print('UI instrumentation, restart persistence and accessibility captures completed')
PY
