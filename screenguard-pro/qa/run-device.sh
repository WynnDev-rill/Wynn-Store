#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output screenshots
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb install -r device-fixture/build/outputs/apk/debug/device-fixture-debug.apk

# Official pinned Shizuku release. No root: libshizuku is executed as adb shell UID 2000.
shizuku_apk="${RUNNER_TEMP:-/tmp}/screenguard-shizuku.apk"
curl --fail --location --retry 2 --output "$shizuku_apk" https://github.com/RikkaApps/Shizuku/releases/download/v13.6.0/shizuku-v13.6.0.r1086.2650830c-release.apk
printf '%s  %s\n' '6e273ab0e991c4e79bc8b1bbb9b9dd739ccac1a8712a541a214078886b7b790f' "$shizuku_apk" | sha256sum --check
adb install -r "$shizuku_apk"
shizuku_dir="$(adb shell pm path moe.shizuku.privileged.api | tr -d '\r' | sed -n 's|^package:\(.*\)/base.apk$|\1|p' | head -1)"
test -n "$shizuku_dir"
adb shell "$shizuku_dir/lib/x86_64/libshizuku.so" > qa/output/shizuku-start.txt 2>&1

ui_status=0
./gradlew :app:connectedDebugAndroidTest || ui_status=$?
adb pull /sdcard/Android/data/id.wynn.screenguard.debug/files/screenshots ./screenshots || true

adb shell am instrument -w -e class id.wynn.screenguard.PrivilegedDeviceTest -e privileged true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/privileged-test.txt
privileged_status=0
if ! grep -q 'OK (1 test)' qa/output/privileged-test.txt; then
  privileged_status=1
  adb shell uiautomator dump /sdcard/guard-window.xml || true
  adb pull /sdcard/guard-window.xml qa/output/permission-window.xml || true
  adb exec-out screencap -p > qa/output/permission-screen.png
fi
adb logcat -d > qa/output/logcat.txt
adb install -r app/build/outputs/apk/preview/app-preview.apk
adb logcat -c
adb shell am start -W -n id.wynn.screenguard.preview/id.wynn.screenguard.MainActivity > qa/output/preview-launch.txt
sleep 3
adb logcat -d -b crash > qa/output/preview-crash.txt
if grep -q 'FATAL EXCEPTION' qa/output/preview-crash.txt; then exit 1; fi
adb exec-out screencap -p > qa/output/preview-onboarding.png
test "$ui_status" -eq 0 && test "$privileged_status" -eq 0
