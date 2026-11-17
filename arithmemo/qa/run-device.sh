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
"$ADB_BIN" logcat -d -s AndroidRuntime:E > qa/output/crashes.txt
python3 - <<'PYCHECK'
from pathlib import Path
assert 'Process: id.wynn.arithmemo.debug' not in Path('qa/output/crashes.txt').read_text(),'ArithMemo crash recorded'
print('Native UI, dark/large/landscape and cold-process SQLite restoration PASS')
PYCHECK
