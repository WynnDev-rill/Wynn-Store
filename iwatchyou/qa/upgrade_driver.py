"""Actual public feed -> verified download -> Android installer -> retained app data.
Runs only on a disposable Android 16 CI emulator. No production permission bypass in app code.
"""
import pathlib,re,subprocess,time,xml.etree.ElementTree as ET
out=pathlib.Path('iwatchyou/qa/upgrade-output');out.mkdir(parents=True,exist_ok=True)
def adb(*args):return subprocess.check_output(['adb',*args],text=True,timeout=40)
def shell(command):return adb('shell',command)
def tree():
 shell('uiautomator dump /sdcard/iw-upgrade.xml >/dev/null')
 text=shell('cat /sdcard/iw-upgrade.xml');(out/'last-ui.xml').write_text(text)
 return ET.fromstring(text)
def find(label,attr='text'):
 return next((n for n in tree().iter('node') if n.get(attr)==label),None)
def wait_for(label,timeout=35,attr='text'):
 end=time.monotonic()+timeout
 while time.monotonic()<end:
  n=find(label,attr)
  if n is not None:return n
  time.sleep(.5)
 raise AssertionError('UI did not show: '+label)
def tap(node):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
 shell(f'input tap {(x1+x2)//2} {(y1+y2)//2}')
def version():
 x=shell('dumpsys package id.wynn.iwatchyou')
 return int(re.search(r'versionCode=(\d+)',x).group(1))
def duration():
 for n in tree().iter('node'):
  m=re.fullmatch(r'(?:(\d+)j )?(\d+)m (\d+)d',n.get('text',''))
  if m:return int(m[1] or 0)*3600+int(m[2])*60+int(m[3])
 raise AssertionError('Screen time not found')
def capture(name):
 with open(out/name,'wb') as f:subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
try:
 adb('install','-r','iwatchyou/qa/fixtures/upgrade-baseline.apk')
 assert version()==1
 shell('pm grant id.wynn.iwatchyou android.permission.POST_NOTIFICATIONS')
 shell('appops set id.wynn.iwatchyou REQUEST_INSTALL_PACKAGES allow')
 shell('am start -n id.wynn.iwatchyou/.MainActivity')
 # The fixture differs from the actual production APK only in its manifest version.
 tap(wait_for('Nanti',timeout=50))
 shell('settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService')
 shell('settings put secure accessibility_enabled 1')
 time.sleep(3);shell('am start -a android.settings.SETTINGS');time.sleep(5)
 shell('am start -n id.wynn.iwatchyou/.MainActivity');wait_for('IWatchYou')
 before=duration();assert before>=4
 capture('before.png')
 tap(wait_for('Pengaturan',attr='content-desc'))
 tap(wait_for('Periksa pembaruan'))
 tap(wait_for('Unduh & pasang',timeout=50))
 end=time.monotonic()+75
 while time.monotonic()<end:
  root=tree()
  button=next((n for n in root.iter('node') if n.get('text') in ('Update','Install','Perbarui','Pasang') and n.get('enabled')=='true'),None)
  if button is not None:tap(button);break
  time.sleep(1)
 else:raise AssertionError('Android installation consent did not appear')
 end=time.monotonic()+50
 while time.monotonic()<end and version()!=2:time.sleep(1)
 assert version()==2,'Production update did not install'
 shell('am start -n id.wynn.iwatchyou/.MainActivity');wait_for('IWatchYou')
 after=duration();assert after>=before,'Private daily data was reset by update'
 capture('after.png')
 (out/'result.txt').write_text(f'PASS: public stable feed, production signer, in-app download, Android consent, version 1 -> 2, retained screen seconds {before} -> {after}\n')
 print((out/'result.txt').read_text())
finally:
 (out/'package.txt').write_text(shell('dumpsys package id.wynn.iwatchyou'))
 (out/'logcat.txt').write_text(adb('logcat','-d'))
 capture('final.png')
