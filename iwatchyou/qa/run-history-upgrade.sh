#!/usr/bin/env bash
# Dedicated test device only. Both APKs must be debug fixtures signed by the same test key.
set -euo pipefail
: "${IW_UPGRADE_BASELINE:?Set path to the older debug fixture APK}"
out=iwatchyou/qa/upgrade-output
mkdir -p "$out"
adb install -r "$IW_UPGRADE_BASELINE"
adb shell am start -W -n id.wynn.iwatchyou/.MainActivity >/dev/null
adb shell am force-stop id.wynn.iwatchyou
python3 - "$out/baseline.db" <<'PY'
import sqlite3,json,sys
with sqlite3.connect(sys.argv[1]) as db:
 db.execute('PRAGMA user_version=1')
 db.execute('CREATE TABLE days(day TEXT PRIMARY KEY, screen INTEGER NOT NULL, apps TEXT NOT NULL, hours TEXT NOT NULL)')
 db.execute('INSERT INTO days VALUES(?,?,?,?)',('2026-01-02',120000,json.dumps({'upgrade.fixture':90000}),json.dumps([120000]+[0]*23)))
PY
adb push "$out/baseline.db" /data/local/tmp/iwatchyou-upgrade.db >/dev/null
adb shell run-as id.wynn.iwatchyou mkdir -p databases
adb shell run-as id.wynn.iwatchyou cp /data/local/tmp/iwatchyou-upgrade.db databases/history.db
adb install -r iwatchyou/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n id.wynn.iwatchyou/.MainActivity >/dev/null
adb exec-out run-as id.wynn.iwatchyou cat databases/history.db > "$out/after.db"
python3 - "$out/after.db" <<'PY'
import sqlite3,json,sys
with sqlite3.connect(sys.argv[1]) as db:
 row=db.execute('SELECT screen,apps,hours FROM days WHERE day=?',('2026-01-02',)).fetchone()
 assert row and row[0]==120000
 assert json.loads(row[1])=={'upgrade.fixture':90000}
 assert json.loads(row[2])==[120000]+[0]*23
 assert db.execute('SELECT count(*) FROM days WHERE day<?',('2026-01-02',)).fetchone()[0]==0
print('PASS: installing 1.4 over the older fixture retains all daily/app/hourly history.')
PY
