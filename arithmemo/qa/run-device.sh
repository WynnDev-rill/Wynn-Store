#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output
ADB_BIN=${ADB_BIN:-adb}
"$ADB_BIN" wait-for-device
"$ADB_BIN" shell input keyevent 82
"$ADB_BIN" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB_BIN" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$ADB_BIN" shell pm clear id.wynn.arithmemo.debug
"$ADB_BIN" logcat -c || true
"$ADB_BIN" shell am instrument -w -r id.wynn.arithmemo.debug.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/instrumentation.txt
python3 - <<'PY'
from pathlib import Path
s=Path('qa/output/instrumentation.txt').read_text()
assert 'OK (' in s and 'FAILURES' not in s and 'INSTRUMENTATION_FAILED' not in s,s
PY
for shot in home flashcard stats settings recall feedback; do
  "$ADB_BIN" exec-out run-as id.wynn.arithmemo.debug cat "files/qa-$shot.png" > "qa/output/$shot.png"
done
capture_home() {
python3 - "$ADB_BIN" "$1" <<'PYCAPTURE'
import pathlib,subprocess,sys,time
adb,name=sys.argv[1:]
folder=pathlib.Path('qa/output')
for attempt in range(15):
    try:
        if attempt%3==0:
            subprocess.run([adb,'shell','am','start','-W','-S','-n','id.wynn.arithmemo.debug/id.wynn.arithmemo.MainActivity'],check=True,stdout=subprocess.DEVNULL,timeout=20)
        remote='/sdcard/arithmemo-'+name+'.xml'
        subprocess.run([adb,'shell','uiautomator','dump',remote],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=15)
        result=subprocess.run([adb,'shell','cat',remote],capture_output=True,text=True,timeout=10)
        if result.returncode==0 and 'id.wynn.arithmemo.debug' in result.stdout and 'Sedikit setiap hari.' in result.stdout:
            (folder/(name+'.xml')).write_text(result.stdout)
            with (folder/(name+'.png')).open('wb') as image:
                subprocess.run([adb,'exec-out','screencap','-p'],stdout=image,check=True,timeout=15)
            break
    except (subprocess.TimeoutExpired,subprocess.CalledProcessError):pass
    time.sleep(1)
else:raise AssertionError('Application UI did not become ready for '+name)
PYCAPTURE
}
capture_home relaunch
"$ADB_BIN" shell cmd uimode night yes
"$ADB_BIN" shell settings put system font_scale 1.6
capture_home dark-large
"$ADB_BIN" shell settings put system font_scale 1.0
"$ADB_BIN" shell cmd uimode night no
"$ADB_BIN" shell settings put system accelerometer_rotation 0
"$ADB_BIN" shell settings put system user_rotation 1
capture_home landscape
"$ADB_BIN" shell settings put system user_rotation 0
"$ADB_BIN" logcat -d -s AndroidRuntime:E > qa/output/crashes.txt
python3 - <<'PY'
from pathlib import Path
s=Path('qa/output/relaunch.xml').read_text()
assert 'Lanjutkan sesi' in s,'Interrupted session was not restored after force-stop'
assert 'FATAL EXCEPTION' not in Path('qa/output/crashes.txt').read_text(),'Android crash recorded'
print('UI instrumentation, restart persistence and accessibility captures completed')
PY
