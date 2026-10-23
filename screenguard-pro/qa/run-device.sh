#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output screenshots
adb shell settings put system screen_off_timeout 2147483647
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
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
# Direct instrumentation retains installed APKs and their screenshot files.
# Gradle connected tests uninstall test APKs by default on completion.
adb shell am instrument -w id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/ui-tests.txt || ui_status=$?
if ! grep -q '^OK (' qa/output/ui-tests.txt; then ui_status=1; fi
adb pull /sdcard/Android/data/id.wynn.screenguard.debug/files/screenshots ./screenshots || true

# Verify system reduced-motion support with the actual dock interaction.
adb shell settings put global animator_duration_scale 0
adb shell am instrument -w -e class id.wynn.screenguard.NavigationUiTest#dockSupportsLargeTextAndSelectedSemantics id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/reduced-motion.txt
adb shell settings put global animator_duration_scale 1
grep -q 'OK (1 test)' qa/output/reduced-motion.txt

privileged_status=0
adb shell am instrument -w -e class id.wynn.screenguard.PrivilegedDeviceTest#shellShizukuActuallyBlocksNetworkSuspendsDisablesAndRestores -e privileged true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/privileged-test.txt || privileged_status=$?
if ! grep -q 'OK (1 test)' qa/output/privileged-test.txt; then
  privileged_status=1
  adb shell uiautomator dump /sdcard/guard-window.xml || true
  adb pull /sdcard/guard-window.xml qa/output/permission-window.xml || true
  adb exec-out screencap -p > qa/output/permission-screen.png
fi
adb logcat -d > qa/output/logcat.txt
if [ "$privileged_status" -ne 0 ]; then exit "$privileged_status"; fi
adb shell am instrument -w -e class id.wynn.screenguard.UsageDeviceAccuracyTest -e accuracy true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/usage-accuracy.txt
grep -q 'OK (1 test)' qa/output/usage-accuracy.txt
# Independently prove that refreeze survives killing the app process, not just closing an Activity.
adb shell am instrument -w -e class id.wynn.screenguard.PrivilegedDeviceTest#armDaemonTimerForExternalForceStopCheck -e watchdog true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/watchdog-arm.txt
grep -q 'OK (1 test)' qa/output/watchdog-arm.txt
adb shell am force-stop id.wynn.screenguard.debug
sleep 12
adb shell pm list packages -d | tr -d '\r' | tee qa/output/watchdog-disabled.txt | grep -Fx 'package:id.wynn.guardfixture'
adb shell pm enable --user 0 id.wynn.guardfixture
adb logcat -d > qa/output/logcat.txt
adb install -r app/build/outputs/apk/preview/app-preview.apk
adb logcat -c
adb shell am start -W -n id.wynn.screenguard.preview/id.wynn.screenguard.MainActivity > qa/output/preview-launch.txt
sleep 3
adb logcat -d -b crash > qa/output/preview-crash.txt
if grep -q 'Error' qa/output/preview-launch.txt; then exit 1; fi
if grep -q 'Process: id.wynn.screenguard.preview' qa/output/preview-crash.txt; then exit 1; fi
adb shell pidof id.wynn.screenguard.preview
adb exec-out screencap -p > qa/output/preview-onboarding.png
production_apk="$(python3 -c 'import json,pathlib; f=pathlib.Path("updates/stable.json"); print("distribution/ScreenGuard-Pro-"+json.loads(f.read_text())["versionName"]+".apk")')"
if [ -f "$production_apk" ]; then
  python3 qa/verify-distribution.py
  # Upgrade previous production APK in place; a signature mismatch must fail the run.
  if [ "$production_apk" != "distribution/ScreenGuard-Pro-1.1.0.apk" ]; then
    adb install -r distribution/ScreenGuard-Pro-1.1.0.apk
    release_version="$(python3 -c 'import json; print(json.load(open("updates/stable.json"))["versionName"])')"
    adb shell am instrument -w -e class id.wynn.screenguard.ProductionUpgradeTest -e upgrade true -e releaseVersion "$release_version" id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/production-upgrade.txt
    grep -q 'OK (1 test)' qa/output/production-upgrade.txt
  fi
  "$ANDROID_HOME/build-tools/36.0.0/apksigner" verify "$production_apk"
  adb install -r "$production_apk"
  adb logcat -c
  adb shell am start -W -n id.wynn.screenguard/id.wynn.screenguard.MainActivity > qa/output/production-launch.txt
  sleep 3
  adb shell pidof id.wynn.screenguard
  adb logcat -d -b crash > qa/output/production-crash.txt
  if grep -Fq 'Process: id.wynn.screenguard,' qa/output/production-crash.txt; then exit 1; fi
  adb exec-out screencap -p > qa/output/production-onboarding.png
  adb shell am instrument -w -e class id.wynn.screenguard.ProductionUpdateTest -e production true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/production-update.txt
  grep -q 'OK (1 test)' qa/output/production-update.txt
  adb pull /sdcard/Android/data/id.wynn.screenguard.debug/files/screenshots ./screenshots || true
fi
test "$ui_status" -eq 0 && test "$privileged_status" -eq 0
bash qa/run-candidate-upgrade.sh
