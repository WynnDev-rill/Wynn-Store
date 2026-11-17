#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p qa/output/upgrade qa/fixtures
python3 - <<'PY'
import hashlib,json,pathlib,urllib.request,os,time
m=json.loads(pathlib.Path('qa/upgrade-fixture.json').read_text())
if os.environ.get('UPDATE_MANIFEST_FILE'):m['candidate']=json.loads(pathlib.Path(os.environ['UPDATE_MANIFEST_FILE']).read_text())
for name,item in m['files'].items():
 assert item['url'].startswith('https://github.com/WynnDev-rill/Wynn-Store/releases/download/arithmemo-qa-upgrade/')
 for attempt in range(12):
  try:
   data=urllib.request.urlopen(item['url'],timeout=120).read();break
  except Exception:
   if attempt==11:raise
   time.sleep(5)
 assert hashlib.sha256(data).hexdigest()==item['sha256'],name
 pathlib.Path('qa/fixtures',name).write_bytes(data)
pathlib.Path('qa/fixtures/manifest.json').write_text(json.dumps(m['candidate']))
PY
adb install -r qa/fixtures/ArithMemo-baseline.apk
adb install -r qa/fixtures/ArithMemo-upgrade-test.apk
adb shell pm clear id.wynn.arithmemo
adb shell appops set id.wynn.arithmemo REQUEST_INSTALL_PACKAGES allow
manifest=$(base64 -w0 qa/fixtures/manifest.json)
adb shell am instrument -w -r -e class id.wynn.arithmemo.UpgradeTest -e updateManifest "$manifest" id.wynn.arithmemo.test/androidx.test.runner.AndroidJUnitRunner | tee qa/output/upgrade/instrumentation.txt
python3 - <<'PY'
from pathlib import Path
s=Path('qa/output/upgrade/instrumentation.txt').read_text()
assert 'OK (' in s and 'FAILURES' not in s and 'INSTRUMENTATION_FAILED' not in s,s
PY
python3 - <<'PY'
import pathlib,re,subprocess,time,xml.etree.ElementTree as E
out=pathlib.Path('qa/output/upgrade')
for attempt in range(30):
 subprocess.run(['adb','shell','uiautomator','dump','/sdcard/upgrade.xml'],check=True,stdout=subprocess.DEVNULL)
 text=subprocess.check_output(['adb','shell','cat','/sdcard/upgrade.xml'],text=True)
 (out/'installer.xml').write_text(text)
 root=E.fromstring(text[text.index('<hierarchy'):])
 target=next((n for n in root.iter('node') if n.attrib.get('text','').lower() in ['update','install','pasang','perbarui'] and n.attrib.get('enabled')=='true'),None)
 if target is not None:
  x1,y1,x2,y2=map(int,re.findall(r'\d+',target.attrib['bounds']))
  subprocess.run(['adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2)],check=True)
  break
 time.sleep(1)
else:raise AssertionError('Android installer confirmation not found')
expected=__import__('json').loads(pathlib.Path('qa/fixtures/manifest.json').read_text())['versionCode']
for attempt in range(60):
 package=subprocess.check_output(['adb','shell','dumpsys','package','id.wynn.arithmemo'],text=True)
 if re.search(r'versionCode='+str(expected)+r'\b',package):break
 time.sleep(1)
else:raise AssertionError('Production upgrade did not install')
(out/'installed-package.txt').write_text(package)
subprocess.run(['adb','shell','am','start','-W','-n','id.wynn.arithmemo/id.wynn.arithmemo.MainActivity'],check=True)
for attempt in range(20):
 subprocess.run(['adb','shell','uiautomator','dump','/sdcard/upgraded.xml'],check=True,stdout=subprocess.DEVNULL)
 text=subprocess.check_output(['adb','shell','cat','/sdcard/upgraded.xml'],text=True)
 if 'Lanjutkan sesi' in text and '1 jawaban tersimpan' in text:break
 time.sleep(1)
else:raise AssertionError('Saved arithmetic answer/session did not survive upgrade')
(out/'after-upgrade.xml').write_text(text)
with (out/'after-upgrade.png').open('wb') as f:subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
print('Real verified in-app GitHub download, Android installer upgrade and data preservation PASS')
PY
