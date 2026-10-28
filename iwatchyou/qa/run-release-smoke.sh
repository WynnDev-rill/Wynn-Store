#!/usr/bin/env bash
# Dedicated test device. Optimized non-debuggable APK, disposable QA signer only.
set -euo pipefail
out=iwatchyou/qa/output/release
mkdir -p "$out"
adb shell "settings put secure enabled_accessibility_services ''"
adb install --no-incremental -r iwatchyou/app/build/outputs/apk/app-release-qa.apk
adb shell am force-stop id.wynn.iwatchyou
adb logcat -c
adb shell am start -W -f 0x10008000 -n id.wynn.iwatchyou/.MainActivity > "$out/launch.txt"
adb shell uiautomator dump /sdcard/iw-release-home.xml >/dev/null
adb pull /sdcard/iw-release-home.xml "$out/home.xml" >/dev/null
grep -q 'text="IWatchYou"' "$out/home.xml"
adb exec-out screencap -p > "$out/home.png"
adb shell settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService
adb shell settings put secure accessibility_enabled 1
adb shell am start -W -a android.settings.SETTINGS >/dev/null
visible=false
for attempt in {1..20}; do
 adb shell dumpsys window windows > "$out/windows.txt"
 if python3 - "$out/windows.txt" <<'PY'
import re,sys
windows=re.split(r'(?m)^  Window #\d+ ',open(sys.argv[1]).read())
sys.exit(0 if any('id.wynn.iwatchyou' in w.splitlines()[0]
 and ('ty=ACCESSIBILITY_OVERLAY' in w or 'ty=2032' in w)
 and 'isOnScreen=true' in w for w in windows if w.splitlines()) else 1)
PY
 then visible=true;break;fi
 sleep 1
done
test "$visible" = true
adb exec-out screencap -p > "$out/overlay.png"
adb logcat -d > "$out/logcat.txt"
if grep -A3 'FATAL EXCEPTION' "$out/logcat.txt" | grep -q 'Process: id.wynn.iwatchyou,'; then exit 1; fi
echo 'PASS: optimized release launches, retains app identity, and displays accessibility timer.'
