#!/usr/bin/env bash
# Dedicated test device only. All APKs use the same disposable QA signer.
set -euo pipefail
: "${IW_UPGRADE_BASELINE:?Set path to the older debug fixture APK}"
out=iwatchyou/qa/upgrade-output
mkdir -p "$out"
adb install --no-incremental -r "$IW_UPGRADE_BASELINE"
adb install --no-incremental -r iwatchyou/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Write through the older APK's SQLite API and close normally. Replacing a live
# database file from adb would bypass WAL semantics and create false data loss.
adb shell am instrument -w -e class id.wynn.iwatchyou.UpgradeHistoryFixture#seedOlderInstallation id.wynn.iwatchyou.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/seed.txt"
grep -Eq 'OK \(1 test\)' "$out/seed.txt"
adb install --no-incremental -r iwatchyou/app/build/outputs/apk/debug/app-debug.apk
adb shell am instrument -w -e class id.wynn.iwatchyou.UpgradeHistoryFixture#verifyUpgradedInstallation id.wynn.iwatchyou.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/verify.txt"
grep -Eq 'OK \(1 test\)' "$out/verify.txt"
echo 'PASS: installing 1.4 over the older fixture retains all daily/app/hourly history.'
