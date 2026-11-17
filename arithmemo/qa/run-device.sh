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
"$ADB_BIN" shell am instrument -w -r -e class id.wynn.arithmemo.NativeUiTest id.wynn.arithmemo.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/instrumentation.txt
python3 - <<'PYCHECK'
from pathlib import Path
s=Path('qa/output/instrumentation.txt').read_text()
assert 'OK (3 tests)' in s and 'FAILURES' not in s and 'INSTRUMENTATION_FAILED' not in s,s
PYCHECK
"$ADB_BIN" shell am force-stop id.wynn.arithmemo.debug
"$ADB_BIN" shell am instrument -w -r -e class id.wynn.arithmemo.RestorationTest id.wynn.arithmemo.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/restoration.txt
python3 - <<'PYCHECK'
from pathlib import Path
s=Path('qa/output/restoration.txt').read_text()
assert 'OK (1 test)' in s and 'FAILURES' not in s and 'INSTRUMENTATION_FAILED' not in s,s
PYCHECK
for shot in home flashcard stats settings recall feedback dark-large landscape relaunch; do
  "$ADB_BIN" exec-out run-as id.wynn.arithmemo.debug cat "files/qa-$shot.png" > "qa/output/$shot.png"
done
# UiAutomation screenshots can retain an incomplete rotated surface on API 36.
# Independently capture warm transitions after instrumentation has disconnected.
restore_display() {
  "$ADB_BIN" shell settings put system font_scale 1.0
  "$ADB_BIN" shell cmd uimode night no
  "$ADB_BIN" shell settings put system user_rotation 0
  "$ADB_BIN" shell settings put system accelerometer_rotation 1
}
trap restore_display EXIT
"$ADB_BIN" shell am start -W -n id.wynn.arithmemo.debug/id.wynn.arithmemo.MainActivity
sleep 2
capture_settled() {
  local name="$1"
  sleep 5
  "$ADB_BIN" shell dumpsys window windows > "qa/output/$name-window.txt"
  "$ADB_BIN" shell dumpsys display > "qa/output/$name-display.txt"
  python3 - "$name" <<'PYCHECK'
import re,sys
from pathlib import Path
s=Path('qa/output/'+sys.argv[1]+'-window.txt').read_text()
assert re.search(r'mCurrentFocus=.*id\.wynn\.arithmemo\.debug/',s),'ArithMemo is not the focused window'
PYCHECK
  "$ADB_BIN" exec-out screencap -p > "qa/output/$name.png"
  sleep 1
  "$ADB_BIN" exec-out screencap -p > "qa/output/$name-confirm.png"
}
"$ADB_BIN" shell settings put system font_scale 1.6
"$ADB_BIN" shell cmd uimode night yes
capture_settled dark-large
"$ADB_BIN" shell settings put system font_scale 1.0
"$ADB_BIN" shell cmd uimode night no
"$ADB_BIN" shell settings put system accelerometer_rotation 0
"$ADB_BIN" shell settings put system user_rotation 1
capture_settled landscape
restore_display
trap - EXIT
"$ADB_BIN" logcat -d -s AndroidRuntime:E > qa/output/crashes.txt
python3 - <<'PYCHECK'
from pathlib import Path
assert 'Process: id.wynn.arithmemo.debug' not in Path('qa/output/crashes.txt').read_text(),'ArithMemo crash recorded'
print('Native UI, dark/large/landscape and cold-process SQLite restoration PASS')
PYCHECK
