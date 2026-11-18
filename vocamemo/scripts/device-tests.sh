#!/bin/bash
set -euxo pipefail
mkdir -p device-results
adb shell getprop ro.build.version.sdk > device-results/api.txt
trap 'timeout 20 adb logcat -d -s AndroidRuntime > device-results/runtime.txt || true
      timeout 20 adb pull /sdcard/Android/data/id.wynn.vocamemo.debug/files/screenshots device-results/ || true' EXIT
adb install -r "$(find device-input -name app-debug.apk -print -quit)"
adb install -r "$(find device-input -name '*androidTest.apk' -print -quit)"
# Enforce offline independently of old Android svc implementations.
# adbd uses the emulator pipe, so guest IP firewalling does not block the test driver.
for attempt in 1 2 3; do
  timeout 20 adb wait-for-device || continue
  adb root || continue
  timeout 20 adb wait-for-device || continue
  if [[ $(adb shell id -u | tr -d '\r') == 0 ]]; then break; fi
done
test "$(adb shell id -u | tr -d '\r')" = 0
adb shell iptables -I OUTPUT -j REJECT
adb shell ip6tables -I OUTPUT -j REJECT
adb shell iptables -C OUTPUT -j REJECT
adb shell ip6tables -C OUTPUT -j REJECT
adb shell iptables -S > device-results/offline-firewall.txt
runner=id.wynn.vocamemo.debug.test/androidx.test.runner.AndroidJUnitRunner
smoke=id.wynn.vocamemo.ReleaseSmokeTest
run_tests() {
  local output=$1
  shift
  adb shell am instrument -w "$@" "$runner" | tee "$output"
  if ! grep -Fq 'OK (' "$output"; then
    adb logcat -d > device-results/logcat.txt
    exit 1
  fi
}

# Use the exact previous public APK, with non-debuggable production signing.
adb install vocamemo/distribution/VocaMemo-1.1.0.apk
adb shell am start -W -n id.wynn.vocamemo/.MainActivity
run_tests device-results/upgrade-seed.txt -e class "$smoke#seedLegacyMemo" -e upgradePhase seed
adb shell am force-stop id.wynn.vocamemo
adb install -r release-input/VocaMemo.apk
# Read the old and upgraded production data through the app and assert the review
# separately from the presence of the Memo.
adb shell am force-stop id.wynn.vocamemo
db=/data/data/id.wynn.vocamemo/databases/vocamemo-personal.db
adb pull "$db" device-results/local-check.db
adb pull "$db-wal" device-results/local-check.db-wal || true
python3 - <<'PYDATA'
import sqlite3
db = sqlite3.connect('device-results/local-check.db')
assert db.execute("select count(*) from memos where word='apple' and meaning='apel' and repetitions>=1").fetchone()[0] == 1
assert db.execute("select count(*) from reviews where word='apple' and rating=3").fetchone()[0] == 1
print("Memo and review survived the update")
PYDATA
rm device-results/local-check.db*
adb shell am start -W -n id.wynn.vocamemo/.MainActivity | tee device-results/release-launch.txt
run_tests device-results/upgrade-smoke.txt -e class "$smoke#productionLookupAndRetainedMemoOffline"
# Run onboarding before the other tests create a database in the isolated debug package.
run_tests device-results/fresh-guest.txt -e class id.wynn.vocamemo.UiTest#freshInstallCanContinueAsGuest -e freshGuest true
run_tests device-results/instrumentation.txt -e notClass "$smoke,id.wynn.vocamemo.UiTest#freshInstallCanContinueAsGuest"
adb shell am start -W -n id.wynn.vocamemo/.MainActivity
sleep 3
adb shell pidof id.wynn.vocamemo
adb shell screencap -p /sdcard/vocamemo-release.png
adb pull /sdcard/vocamemo-release.png device-results/release.png
adb logcat -d -s AndroidRuntime > device-results/runtime.txt
if grep -q 'FATAL EXCEPTION' device-results/runtime.txt; then exit 1; fi
