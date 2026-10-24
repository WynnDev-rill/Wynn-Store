#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output screenshots
collect() {
  adb logcat -d > qa/output/production-upgrade-logcat.txt || true
  adb shell uiautomator dump /sdcard/upgrade-window.xml || true
  adb pull /sdcard/upgrade-window.xml qa/output/upgrade-window.xml || true
  adb exec-out screencap -p > qa/output/upgrade-final.png || true
  adb pull /sdcard/Android/data/id.wynn.screenguard.debug/files/screenshots ./screenshots || true
}
trap collect EXIT
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
# Read the live feed, including changes published earlier in this very workflow.
# The checked-out stable.json can still refer to the preceding release.
python3 - <<'PY'
import urllib.request,json,time,pathlib
url='https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/screenguard-pro/updates/stable.json'
import re
minimum=int(re.search(r'versionCode = (\d+)',pathlib.Path('app/build.gradle.kts').read_text()).group(1))
for attempt in range(24):
    with urllib.request.urlopen(urllib.request.Request(url,headers={'Cache-Control':'no-cache'}),timeout=20) as r: feed=json.load(r)
    if feed['versionCode']>=minimum: break
    time.sleep(10)
else: raise SystemExit('Live stable feed did not activate candidate')
pathlib.Path('qa/output/upgrade-feed.json').write_text(json.dumps(feed))
previous='1.5.0' if feed['versionCode']>=7 else '1.4.0' if feed['versionCode']>=6 else '1.3.0'
pathlib.Path('qa/output/upgrade-baseline.txt').write_text(previous)
print('Live feed active:',feed['versionName'],feed['sha256'])
PY
release_version="$(python3 -c 'import json;print(json.load(open("qa/output/upgrade-feed.json"))["versionName"])')"
baseline="$(cat qa/output/upgrade-baseline.txt)"
adb install -r "distribution/ScreenGuard-Pro-${baseline}.apk"
adb shell appops set id.wynn.screenguard REQUEST_INSTALL_PACKAGES allow
adb shell am instrument -w -e class id.wynn.screenguard.ProductionUpgradeTest -e upgrade true -e releaseVersion "$release_version" -e installUpdate true -e manualCheck "${SG_MANUAL_UPDATE_CHECK:-false}" id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/production-live-upgrade.txt
grep -q 'OK (1 test)' qa/output/production-live-upgrade.txt
adb shell am instrument -w -e class id.wynn.screenguard.ProductionUpdateTest -e production true id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/production-live-feed.txt
grep -q 'OK (1 test)' qa/output/production-live-feed.txt
adb shell dumpsys package id.wynn.screenguard > qa/output/production-installed-package.txt
adb pull /sdcard/Android/data/id.wynn.screenguard.debug/files/screenshots ./screenshots
mkdir -p dist/production
cp "distribution/ScreenGuard-Pro-${release_version}.apk" dist/production/
sha256sum "dist/production/ScreenGuard-Pro-${release_version}.apk" > dist/production/RELEASE-SHA256.txt
