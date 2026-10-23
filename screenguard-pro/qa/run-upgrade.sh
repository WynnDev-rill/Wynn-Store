#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output screenshots
adb shell settings put system screen_off_timeout 2147483647
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Install only the real previous production version. The new APK must arrive through its own updater.
adb install -r distribution/ScreenGuard-Pro-1.2.0.apk
adb shell appops set id.wynn.screenguard REQUEST_INSTALL_PACKAGES allow
# GitHub's raw CDN may briefly cache the old feed after the release commit.
python3 - <<'PY'
import urllib.request,json,time
url='https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/screenguard-pro/updates/stable.json'
for attempt in range(24):
    with urllib.request.urlopen(urllib.request.Request(url,headers={'Cache-Control':'no-cache'}),timeout=20) as r: feed=json.load(r)
    if feed['versionCode']==4 and feed['versionName']=='1.3.0': break
    time.sleep(10)
else: raise SystemExit('Live stable feed did not activate 1.3.0')
print('Live feed active:',feed['versionName'],feed['sha256'])
PY
adb shell am instrument -w -e class id.wynn.screenguard.ProductionUpgradeTest -e upgrade true -e releaseVersion 1.3.0 -e installUpdate true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/production-1.2-to-1.3.txt
grep -q 'OK (1 test)' qa/output/production-1.2-to-1.3.txt
adb shell am instrument -w -e class id.wynn.screenguard.ProductionUpdateTest -e production true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/production-1.3-live-feed.txt
grep -q 'OK (1 test)' qa/output/production-1.3-live-feed.txt
adb shell dumpsys package id.wynn.screenguard > qa/output/production-installed-package.txt
adb pull /sdcard/Android/data/id.wynn.screenguard.debug/files/screenshots ./screenshots
adb logcat -d -b crash > qa/output/production-upgrade-crash.txt
if grep -Fq 'Process: id.wynn.screenguard,' qa/output/production-upgrade-crash.txt; then exit 1; fi
