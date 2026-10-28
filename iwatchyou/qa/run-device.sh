#!/usr/bin/env bash
set -euo pipefail
out=iwatchyou/qa/output
mkdir -p "$out"
collect() {
 adb shell dumpsys meminfo id.wynn.iwatchyou > "$out/meminfo.txt" || true
 adb shell dumpsys gfxinfo id.wynn.iwatchyou > "$out/gfxinfo.txt" || true
 adb shell dumpsys accessibility > "$out/accessibility.txt" || true
 adb shell dumpsys window > "$out/windows.txt" || true
 adb shell dumpsys activity activities > "$out/activities.txt" || true
 adb shell dumpsys notification --noredact > "$out/notifications.txt" || true
 adb logcat -d > "$out/logcat.txt" || true
 adb exec-out screencap -p > "$out/home.png" || true
 adb pull /sdcard/Android/data/id.wynn.iwatchyou/files/qa "$out/test-screens" || true
}
trap collect EXIT
adb wait-for-device
adb shell wm dismiss-keyguard
# Artifact root is app/build; the APK and report relative paths are retained.
find iwatchyou/app/build -name '*.apk' -print
adb install -r iwatchyou/app/build/outputs/apk/debug/app-debug.apk
adb install -r iwatchyou/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
adb shell am instrument -w id.wynn.iwatchyou.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/instrumentation.txt"
if ! grep -Eq 'OK \([1-9][0-9]* tests?\)' "$out/instrumentation.txt"; then exit 1; fi
adb shell am start -n id.wynn.iwatchyou/.MainActivity
adb shell settings put system font_scale 1.0
adb shell settings put system user_rotation 0
adb shell dumpsys meminfo id.wynn.iwatchyou > "$out/meminfo.txt"
adb shell dumpsys gfxinfo id.wynn.iwatchyou > "$out/gfxinfo.txt"
adb shell dumpsys accessibility > "$out/accessibility.txt"
adb shell dumpsys notification --noredact > "$out/notifications.txt"
adb logcat -d > "$out/logcat.txt"
adb exec-out screencap -p > "$out/home.png"
# App-specific fatal exceptions are failures, not warnings hidden in a successful build.
if grep -A3 'FATAL EXCEPTION' "$out/logcat.txt" | grep -q 'Process: id.wynn.iwatchyou,'; then exit 1; fi
