#!/usr/bin/env bash
# Dedicated emulator only; production stays installed to prove coexistence.
set -euo pipefail
out=iwatchyou/qa/output
mkdir -p "$out"
trap "adb shell settings delete secure enabled_accessibility_services >/dev/null" EXIT
adb install -r iwatchyou/app/build/outputs/apk/preview/app-preview.apk
adb shell pm path id.wynn.iwatchyou > "$out/production-installed.txt"
adb shell pm path id.wynn.iwatchyou.preview > "$out/preview-installed.txt"
adb shell am start -W -n id.wynn.iwatchyou.preview/id.wynn.iwatchyou.MainActivity > "$out/preview-launch.txt"
adb shell settings put secure enabled_accessibility_services id.wynn.iwatchyou.preview/id.wynn.iwatchyou.WatchService
adb shell settings put secure accessibility_enabled 1
adb shell am start -W -a android.settings.SETTINGS > /dev/null
visible=false
for attempt in {1..20}; do
 # Shell uiautomator dump suppresses Accessibility services; inspect window
 # metadata instead so observing the preview cannot disconnect its service.
 adb shell dumpsys window windows > "$out/preview-window.txt"
 if python3 - "$out/preview-window.txt" <<'PYCODE'
import re,sys
text=open(sys.argv[1]).read()
windows=re.split(r'(?m)^  Window #\d+ ',text)
sys.exit(0 if any('id.wynn.iwatchyou.preview' in w.splitlines()[0]
 and ('ty=ACCESSIBILITY_OVERLAY' in w or 'ty=2032' in w)
 and 'isOnScreen=true' in w for w in windows if w.splitlines()) else 1)
PYCODE
 then visible=true;break;fi
 sleep 1
done
test "$visible" = true
adb shell screencap -p /sdcard/preview.png
adb pull /sdcard/preview.png "$out/preview.png" > /dev/null
echo 'Preview installs alongside production, launches and displays its timer.'
