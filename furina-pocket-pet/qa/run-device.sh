#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "$0")/.." && pwd)"
output="$task_root/qa/output"
mkdir -p "$output"
adb install -r "$task_root/app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$task_root/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
adb shell appops set id.wynn.furina SYSTEM_ALERT_WINDOW allow
adb shell pm grant id.wynn.furina android.permission.POST_NOTIFICATIONS || true
adb logcat -c
adb shell am instrument -w -r id.wynn.furina.test/androidx.test.runner.AndroidJUnitRunner | tee "$output/instrumentation.txt"
grep -Eq 'OK \([0-9]+ tests?\)' "$output/instrumentation.txt"
adb shell am start -n id.wynn.furina/.MainActivity
sleep 2
adb exec-out screencap -p > "$output/settings.png"
adb shell dumpsys meminfo id.wynn.furina > "$output/memory.txt"
adb logcat -d > "$output/logcat.txt"
if grep -E 'FATAL EXCEPTION|Fatal signal [0-9]+.*id.wynn.furina' "$output/logcat.txt"; then exit 1; fi
