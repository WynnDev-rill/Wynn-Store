#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output screenshots
collect() {
  adb logcat -d > qa/output/production-upgrade-logcat.txt || true
  adb shell dumpsys connectivity > qa/output/upgrade-connectivity.txt || true
  adb shell uiautomator dump /sdcard/upgrade-window.xml || true
  adb pull /sdcard/upgrade-window.xml qa/output/upgrade-window.xml || true
  adb exec-out screencap -p > qa/output/upgrade-final.png || true
  adb pull /sdcard/Android/data/id.wynn.screenguard.debug/files/screenshots ./screenshots || true
}
trap collect EXIT
# A cold emulator can finish booting before Android validates internet access.
for attempt in $(seq 1 30); do
  if adb shell dumpsys connectivity | grep -q VALIDATED; then break; fi
  sleep 2
done
adb shell dumpsys connectivity | grep -q VALIDATED
adb shell settings put system screen_off_timeout 2147483647
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Install the preceding production version; the new APK must arrive through its own updater.
if [ "$(python3 -c 'import json;print(json.load(open("updates/stable.json"))["versionCode"])')" -eq 4 ]; then export SG_BASELINE_APK=distribution/ScreenGuard-Pro-1.2.0.apk; fi
adb install -r "${SG_BASELINE_APK:-distribution/ScreenGuard-Pro-1.3.0.apk}"
adb shell appops set id.wynn.screenguard REQUEST_INSTALL_PACKAGES allow
# GitHub's raw CDN may briefly cache the old feed after the release commit.
python3 - <<'PY'
import urllib.request,json,time
url='https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/screenguard-pro/updates/stable.json'
for attempt in range(24):
    with urllib.request.urlopen(urllib.request.Request(url,headers={'Cache-Control':'no-cache'}),timeout=20) as r: feed=json.load(r)
    if feed['versionCode']>=4: break
    time.sleep(10)
else: raise SystemExit('Live stable feed did not activate 1.3.0')
print('Live feed active:',feed['versionName'],feed['sha256'])
PY
adb shell am instrument -w -e class id.wynn.screenguard.ProductionUpgradeTest -e upgrade true -e releaseVersion "$(python3 -c 'import json;print(json.load(open("updates/stable.json"))["versionName"])')" -e installUpdate true -e manualCheck "${SG_MANUAL_UPDATE_CHECK:-false}" id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/production-1.2-to-1.3.txt
grep -q 'OK (1 test)' qa/output/production-1.2-to-1.3.txt
adb shell am instrument -w -e class id.wynn.screenguard.ProductionUpdateTest -e production true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/production-1.3-live-feed.txt
grep -q 'OK (1 test)' qa/output/production-1.3-live-feed.txt
adb shell dumpsys package id.wynn.screenguard > qa/output/production-installed-package.txt
adb pull /sdcard/Android/data/id.wynn.screenguard.debug/files/screenshots ./screenshots
adb logcat -d -b crash > qa/output/production-upgrade-crash.txt
if grep -Fq 'Process: id.wynn.screenguard,' qa/output/production-upgrade-crash.txt; then exit 1; fi

mkdir -p dist/production
cp "distribution/ScreenGuard-Pro-$(python3 -c 'import json;print(json.load(open("updates/stable.json"))["versionName"])').apk" dist/production/
sha256sum dist/production/ScreenGuard-Pro-1.3.0.apk > dist/production/RELEASE-SHA256.txt
