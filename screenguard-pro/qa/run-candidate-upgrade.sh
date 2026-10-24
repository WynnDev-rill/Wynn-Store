#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
version="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts)"
code="$(sed -n 's/.*versionCode = \([0-9]*\).*/\1/p' app/build.gradle.kts)"
candidate="distribution/ScreenGuard-Pro-${version}.apk"
if [ ! -f "$candidate" ]; then exit 0; fi
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$candidate" > qa/output/candidate-signature.txt
grep -q 'certificate SHA-256 digest: eca17e8afa84775e941e0e490741b01636e7941da545c508f85b8a256a4f346a' qa/output/candidate-signature.txt
"$ANDROID_HOME/build-tools/36.0.0/aapt" dump badging "$candidate" > qa/output/candidate-package.txt
grep -q "name='id.wynn.screenguard' versionCode='${code}' versionName='${version}'" qa/output/candidate-package.txt
adb uninstall id.wynn.screenguard || true
adb install distribution/ScreenGuard-Pro-1.5.0.apk
adb shell appops set id.wynn.screenguard GET_USAGE_STATS allow
adb shell am instrument -w -e class id.wynn.screenguard.CandidateUpgradeTest -e candidate seed id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/candidate-seed.txt
grep -q 'OK (' qa/output/candidate-seed.txt
adb shell dumpsys package id.wynn.screenguard | sed -n '/userId=/p;/firstInstallTime=/p' > qa/output/candidate-before.txt
adb install -r "$candidate"
adb shell dumpsys package id.wynn.screenguard | sed -n '/userId=/p;/firstInstallTime=/p' > qa/output/candidate-after.txt
diff -u qa/output/candidate-before.txt qa/output/candidate-after.txt
adb shell am instrument -w -e class id.wynn.screenguard.CandidateUpgradeTest -e candidate verify id.wynn.screenguard.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/candidate-retained.txt
grep -q 'OK (' qa/output/candidate-retained.txt
adb exec-out screencap -p > qa/output/candidate-installed.png
