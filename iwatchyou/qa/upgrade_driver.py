"""Actual public feed -> verified download -> Android installer -> retained app data.
Runs only on a disposable Android 16 CI emulator. No production permission bypass in app code.
"""
import pathlib,re,subprocess,time,xml.etree.ElementTree as ET,os,json,urllib.request
out=pathlib.Path('iwatchyou/qa/upgrade-output');out.mkdir(parents=True,exist_ok=True)
def adb(*args):return subprocess.check_output(['adb',*args],text=True,timeout=40)
def shell(command):return adb('shell',command)
def tree():
 shell('rm -f /sdcard/iw-upgrade.xml')
 shell('uiautomator dump /sdcard/iw-upgrade.xml >/dev/null')
 result=subprocess.run(['adb','shell','cat /sdcard/iw-upgrade.xml'],capture_output=True,text=True,timeout=30)
 text=result.stdout
 if result.returncode or not text.lstrip().startswith('<?xml'):return ET.Element('hierarchy')
 (out/'last-ui.xml').write_text(text)
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
expected=int(re.search(r'versionCode = (\d+)',pathlib.Path('iwatchyou/app/build.gradle.kts').read_text()).group(1))
try:
 # Reset the disposable smoke-test installation before seeding the lower-version fixture.
 subprocess.run(['adb','uninstall','id.wynn.iwatchyou'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 adb('install','-r','iwatchyou/qa/fixtures/upgrade-baseline.apk')
 assert version()==expected-1
 shell('pm grant id.wynn.iwatchyou android.permission.POST_NOTIFICATIONS')
 shell('appops set id.wynn.iwatchyou REQUEST_INSTALL_PACKAGES allow')
 shell('am start -n id.wynn.iwatchyou/.MainActivity')
 # The fixture differs from the actual production APK only in its manifest version.
 try:tap(wait_for('Nanti',timeout=20))
 except AssertionError:
  # Automatic startup can precede public-feed propagation or emulator network readiness.
  # Probe with Android's own Java/TLS stack, without changing trust or network policy.
  probe=out/'network-probe';probe.mkdir(exist_ok=True)
  subprocess.run(['javac','-source','8','-target','8','-d',str(probe),'iwatchyou/qa/NetworkProbe.java'],check=True)
  subprocess.run([os.environ['ANDROID_HOME']+'/build-tools/36.0.0/d8','--min-api','30','--output',str(probe/'probe.jar'),str(probe/'NetworkProbe.class')],check=True)
  adb('push',str(probe/'probe.jar'),'/data/local/tmp/iw-probe.jar')
  result=subprocess.run(['adb','shell','CLASSPATH=/data/local/tmp/iw-probe.jar app_process /system/bin NetworkProbe https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/iwatchyou/updates/stable.json?probe='+str(int(time.time()*1000))],capture_output=True,text=True,timeout=40)
  (out/'android-network.txt').write_text(result.stdout+result.stderr)
  print('Android network probe:',result.stdout+result.stderr,flush=True)
  wait_for('Pengaturan',attr='content-desc')
 shell('settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService')
 shell('settings put secure accessibility_enabled 1')
 time.sleep(3);shell('am start -a android.settings.SETTINGS');time.sleep(5)
 shell('am start -n id.wynn.iwatchyou/.MainActivity');wait_for('Pengaturan',attr='content-desc')
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
 while time.monotonic()<end and version()!=expected:time.sleep(1)
 assert version()==expected,'Production update did not install'
 # Dismiss the installer's completion dialog through its normal Open action.
 tap(wait_for('Open',timeout=25))
 shell('am start -n id.wynn.iwatchyou/.MainActivity');wait_for('Pengaturan',attr='content-desc')
 end=time.monotonic()+15
 after=duration()
 while after<before and time.monotonic()<end:time.sleep(1);after=duration()
 assert after>=before,'Private daily data was reset by update'
 capture('after.png')
 (out/'result.txt').write_text(f'PASS: public stable feed, production signer, in-app download, Android consent, version {expected-1} -> {expected}, retained screen seconds {before} -> {after}\n')
 print((out/'result.txt').read_text())
finally:
 (out/'package.txt').write_text(shell('dumpsys package id.wynn.iwatchyou'))
 (out/'logcat.txt').write_text(adb('logcat','-d'))
 capture('final.png')
