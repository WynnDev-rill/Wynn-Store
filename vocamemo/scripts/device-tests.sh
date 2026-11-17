#!/bin/bash
set -euo pipefail
mkdir -p device-results
adb install -r "$(find device-input -name app-debug.apk -print -quit)"
adb install -r "$(find device-input -name '*androidTest.apk' -print -quit)"
adb install -r release-input/VocaMemo.apk
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put global airplane_mode_on 1
adb shell am instrument -w id.wynn.vocamemo.debug.test/androidx.test.runner.AndroidJUnitRunner | tee device-results/instrumentation.txt
adb pull /sdcard/Android/data/id.wynn.vocamemo.debug/files/screenshots device-results/ || true
if ! grep -q 'OK (' device-results/instrumentation.txt; then
  adb logcat -d > device-results/logcat.txt
  exit 1
fi
adb install -r release-input/VocaMemo.apk
adb shell am start -W -n id.wynn.vocamemo/.MainActivity | tee device-results/release-launch.txt
sleep 3
adb shell pidof id.wynn.vocamemo
adb shell screencap -p /sdcard/vocamemo-release.png
adb pull /sdcard/vocamemo-release.png device-results/release.png
adb logcat -d -s AndroidRuntime > device-results/runtime.txt
if grep -q 'FATAL EXCEPTION' device-results/runtime.txt; then exit 1; fi
