#!/usr/bin/env python3
import base64, hashlib, json, os, pathlib, re, subprocess, shutil

repo = os.environ['GITHUB_REPOSITORY']
apk = pathlib.Path('iwatchyou/app/build/outputs/apk/release/app-release.apk')
version = '1.0.0'
tag = 'iwatchyou-v' + version
name = 'IWatchYou-' + version + '.apk'
if not apk.is_file(): raise SystemExit('APK not built')
badging = subprocess.check_output([os.environ['ANDROID_HOME']+'/build-tools/36.0.0/aapt','dump','badging',str(apk)],text=True)
m = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
if not m or (m.group(1),m.group(2),m.group(3)) != ('id.wynn.iwatchyou','1','1.0.0'):
    raise SystemExit('APK identity/version mismatch')
sig = subprocess.check_output([os.environ['ANDROID_HOME']+'/build-tools/36.0.0/apksigner','verify','--verbose','--print-certs',str(apk)],text=True)
signer = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)',sig)
if not signer: raise SystemExit('Signing certificate missing')
subprocess.run(['gh','release','view',tag,'--repo',repo],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
if subprocess.run(['gh','release','view',tag,'--repo',repo],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode:
    subprocess.run(['gh','release','create',tag,'--repo',repo,'--title','IWatchYou v'+version,'--notes','Rilis awal IWatchYou. Stopwatch overlay dan statistik waktu layar aktif.'],check=True)
upload_apk = apk.with_name(name)
shutil.copy2(apk,upload_apk)
subprocess.run(['gh','release','upload',tag,str(upload_apk),'--repo',repo,'--clobber'],check=True)
feed = {
    'versionCode':1,'versionName':version,'packageName':'id.wynn.iwatchyou','minSdk':30,
    'apkUrl':f'https://github.com/{repo}/releases/download/{tag}/{name}',
    'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
    'signerSha256':signer.group(1),'sizeBytes':apk.stat().st_size,
    'notes':'Rilis pertama IWatchYou.'
}
endpoint=f'repos/{repo}/contents/iwatchyou/updates/stable.json'
current=json.loads(subprocess.check_output(['gh','api',endpoint],text=True))
payload={'message':'chore(iwatchyou): publish verified APK feed '+version,'branch':'main',
         'sha':current['sha'],'content':base64.b64encode((json.dumps(feed,indent=2)+'\n').encode()).decode()}
subprocess.run(['gh','api','--method','PUT',endpoint,'--input','-'],input=json.dumps(payload),text=True,check=True,stdout=subprocess.DEVNULL)
print('Published verified IWatchYou APK:', feed['apkUrl'])
