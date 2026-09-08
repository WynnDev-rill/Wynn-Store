#!/usr/bin/env python3
"""Black-box Android APK verification using physical screen input and lifecycle.
No game test flags or save injection are used for this run.
"""
import json,subprocess,time,re
from pathlib import Path
OUT=Path('evidence/android'); OUT.mkdir(parents=True,exist_ok=True)
PKG='com.wynndev.astraworld'
def adb(*args,raw=False):
    b=subprocess.check_output(['adb',*args]); return b if raw else b.decode(errors='replace')
def pause(n=1):time.sleep(n)
def tap(x,y):adb('shell','input','tap',str(x),str(y));pause(.7)
def screenshot(name):
    (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p',raw=True))
def launch():
    adb('shell','monkey','-p',PKG,'-c','android.intent.category.LAUNCHER','1');pause(14)
def alive():
    assert adb('shell','pidof',PKG).strip(),'Game process not running'

adb('logcat','-c')
adb('install','-r','build/Astra-World-test.apk')
adb('shell','wm','size','720x1280');adb('shell','wm','density','240')
launch();alive();screenshot('01-title')
# Title buttons and dialogue are fixed logical coordinates at 1280×720.
tap(275,480)
for _ in range(4):tap(1050,640)
pause(3);alive();screenshot('02-opening')
# Walk with an actual touch-drag, then independently rotate the camera.
adb('shell','input','swipe','130','590','130','530','2500');pause(.5)
adb('shell','input','swipe','760','300','560','320','700');pause(.5)
screenshot('03-touch-movement')
tap(1185,92);screenshot('04-map');tap(1145,61)
tap(1244,33);screenshot('05-pause')
tap(620,296);screenshot('06-settings')
adb('shell','input','keyevent','4');pause(1)
adb('shell','input','keyevent','3');pause(2)
launch();alive();screenshot('07-resume')
adb('shell','am','force-stop',PKG);pause(1);launch();alive();screenshot('08-relaunch')
tap(270,416);pause(3);alive();screenshot('09-continue')
log=adb('logcat','-d'); (OUT/'logcat.txt').write_text(log)
errors=[l for l in log.splitlines() if ('SCRIPT ERROR:' in l or 'FATAL EXCEPTION' in l or 'Fatal signal' in l or 'Shader compilation failed' in l)]
(OUT/'memory.txt').write_text(adb('shell','dumpsys','meminfo',PKG))
(OUT/'package.txt').write_text(adb('shell','dumpsys','package',PKG))
(OUT/'result.json').write_text(json.dumps({'passed':not errors,'package':PKG,'errors':errors,'device':adb('shell','getprop','ro.product.model').strip(),'android':adb('shell','getprop','ro.build.version.release').strip(),'scope':'APK installation, title/opening, touch input, map/pause/settings, home/resume and process restart; screenshots require visual review. No physical-device FPS claim.'},indent=2))
assert not errors,'Android runtime errors: '+str(errors[:10])
print('ASTRA_ANDROID_SMOKE_OK')
