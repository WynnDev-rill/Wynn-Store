#!/usr/bin/env bash
set -euo pipefail
out=iwatchyou/qa/production-output
mkdir -p "$out"
adb install -r iwatchyou/distribution/IWatchYou-1.0.1.apk
adb shell pm grant id.wynn.iwatchyou android.permission.POST_NOTIFICATIONS
adb shell am start -n id.wynn.iwatchyou/.MainActivity
sleep 3
adb shell uiautomator dump /sdcard/iw-home.xml
adb pull /sdcard/iw-home.xml "$out/home.xml"
grep -q 'text="IWatchYou"' "$out/home.xml"
grep -q 'Pemantauan' "$out/home.xml"
adb exec-out screencap -p > "$out/home.png"
adb shell settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService
adb shell settings put secure accessibility_enabled 1
sleep 2
adb shell am start -a android.settings.SETTINGS
sleep 3
adb exec-out screencap -p > "$out/overlay.png"
adb shell dumpsys accessibility > "$out/accessibility.txt"
adb shell dumpsys meminfo id.wynn.iwatchyou > "$out/background-meminfo.txt"
adb shell dumpsys cpuinfo > "$out/cpuinfo.txt"
grep -q 'id.wynn.iwatchyou' "$out/accessibility.txt"
adb shell input keyevent 26
sleep 1
adb shell dumpsys notification --noredact > "$out/locked-notification.txt"
adb shell input keyevent 26
adb shell wm dismiss-keyguard
adb shell am start -n id.wynn.iwatchyou/.MainActivity
sleep 2
adb shell dumpsys meminfo id.wynn.iwatchyou > "$out/ui-meminfo.txt"
adb shell dumpsys gfxinfo id.wynn.iwatchyou > "$out/gfxinfo.txt"
adb logcat -d > "$out/logcat.txt"
if grep -A3 'FATAL EXCEPTION' "$out/logcat.txt" | grep -q 'Process: id.wynn.iwatchyou,'; then exit 1; fi
