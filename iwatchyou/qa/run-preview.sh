#!/usr/bin/env bash
# Dedicated emulator only; production stays installed to prove coexistence.
set -euo pipefail
out=iwatchyou/qa/output
mkdir -p "$out"
trap "adb shell settings put secure enabled_accessibility_services '' >/dev/null" EXIT
adb install -r iwatchyou/app/build/outputs/apk/preview/app-preview.apk
adb shell pm path id.wynn.iwatchyou > "$out/production-installed.txt"
adb shell pm path id.wynn.iwatchyou.preview > "$out/preview-installed.txt"
adb shell am start -W -n id.wynn.iwatchyou.preview/id.wynn.iwatchyou.MainActivity > "$out/preview-launch.txt"
adb shell settings put secure enabled_accessibility_services id.wynn.iwatchyou.preview/id.wynn.iwatchyou.WatchService
adb shell settings put secure accessibility_enabled 1
adb shell am start -W -a android.settings.SETTINGS > /dev/null
visible=false
for attempt in {1..20}; do
 adb shell uiautomator dump /sdcard/preview-window.xml > /dev/null
 adb shell cat /sdcard/preview-window.xml > "$out/preview-window.xml"
 if python3 - "$out/preview-window.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=ET.parse(sys.argv[1]).iter('node')
sys.exit(0 if any(n.get('class')=='android.widget.Chronometer' and n.get('package')=='id.wynn.iwatchyou.preview' for n in nodes) else 1)
PY
 then visible=true;break;fi
 sleep 1
done
test "$visible" = true
adb shell screencap -p /sdcard/preview.png
adb pull /sdcard/preview.png "$out/preview.png" > /dev/null
echo 'Preview installs alongside production, launches and displays its timer.'
