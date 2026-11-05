#!/usr/bin/env python3
"""Publish only a staged, independently signed, verified production APK."""
import base64,hashlib,json,os,pathlib,re,subprocess
from source_digest import source_digest
repo=os.environ['GITHUB_REPOSITORY']
assert repo=='WynnDev-rill/Wynn-Store'
root=pathlib.Path('iwatchyou')
build=(root/'app/build.gradle.kts').read_text()
version=re.search(r'versionName = "([0-9.]+)"',build).group(1)
code=int(re.search(r'versionCode = (\d+)',build).group(1))
name=f'IWatchYou-{version}.apk';tag=f'iwatchyou-v{version}'
apk=root/'distribution'/name
meta=json.loads(apk.with_suffix('.json').read_text())
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
cert='04ed3a280377b2671ad1f6fb6f7fd1c335684302ab35d50d0000ca2b23b585f5'
signature=subprocess.check_output([str(sdk/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
assert 'Signer #1 certificate SHA-256 digest: '+cert in signature
assert 'Verified using v3 scheme (APK Signature Scheme v3): true' in signature
v2=subprocess.check_output([str(sdk/'apksigner'),'verify','--min-sdk-version','24','--max-sdk-version','27','--verbose',str(apk)],text=True)
assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in v2
badging=subprocess.check_output([str(sdk/'aapt'),'dump','badging',str(apk)],text=True)
assert f"name='id.wynn.iwatchyou' versionCode='{code}' versionName='{version}'" in badging
assert 'application-debuggable' not in badging
assert "sdkVersion:'30'" in badging
assert meta['sourceDigest']==source_digest(root),'Staged APK does not match current source identity'
assert meta['sha256']==hashlib.sha256(apk.read_bytes()).hexdigest()
assert meta['sizeBytes']==apk.stat().st_size and meta['signerSha256']==cert
assert meta['versionCode']==code and meta['versionName']==version
# QA is read from GitHub itself, not merely asserted by a metadata field.
run=json.loads(subprocess.check_output(['gh','api',f'repos/{repo}/actions/runs/{meta["workflowRunId"]}'],text=True))
assert run['head_sha']==meta['sourceCommit'] and run['conclusion']=='success','Source build and emulator checks must pass'
manifest=dict(versionCode=code,versionName=version,packageName='id.wynn.iwatchyou',minSdk=30,
 apkUrl=f'https://github.com/{repo}/releases/download/{tag}/{name}',sha256=meta['sha256'],signerSha256=cert,sizeBytes=apk.stat().st_size,
 notes=('Perbaikan timer yang tidak mulai pada sebagian perangkat: deteksi langsung dari event perpindahan aplikasi, dengan fallback saat data jendela belum tersedia.' if version=='1.0.2' else 'Desain baru IWatchYou dan timer mengambang. Rilis produksi dengan kunci baru yang disetujui pemilik; pemasangan ulang diperlukan dari garis rilis lama. Pembaruan berikutnya memakai sertifikat baru yang sama.'))
feed=root/'distribution/update.json';feed.write_text(json.dumps(manifest,indent=2)+'\n')
endpoint=f'repos/{repo}/contents/iwatchyou/updates/stable-v2.json'
response=subprocess.run(['gh','api',endpoint],text=True,capture_output=True)
if response.returncode:
 assert 'HTTP 404' in response.stderr, response.stderr
 old=None;previous={}
else:
 old=json.loads(response.stdout)
 previous=json.loads(base64.b64decode(old['content']))
if previous.get('versionCode',0)>=code:
 assert previous==manifest,'A published version cannot be overwritten'
 print('Identical stable release already published');raise SystemExit(0)
if previous.get('signerSha256'):assert previous['signerSha256']==cert
existing=subprocess.run(['gh','release','view',tag,'--repo',repo,'--json','assets'],text=True,capture_output=True)
if existing.returncode==0:
 verify=root/'distribution/verify';verify.mkdir(exist_ok=True)
 subprocess.run(['gh','release','download',tag,'--repo',repo,'--pattern',name,'--dir',str(verify),'--clobber'],check=True)
 assert hashlib.sha256((verify/name).read_bytes()).hexdigest()==manifest['sha256']
else:
 notes=f'Android 11+. APK produksi. Build dan pengujian emulator: {run["html_url"]}. Pergantian kunci produksi disetujui pemilik pada 2026-11-05. Rilis lama perlu dihapus dan dipasang ulang; simpan data yang diperlukan terlebih dahulu. Rilis baru memakai kanal stable-v2; pembaruan selanjutnya tetap di dalam aplikasi. HyperOS fisik dan konsumsi baterai jangka panjang belum diuji.'
 subprocess.run(['gh','release','create',tag,str(apk),str(feed),'--repo',repo,'--target',os.environ['GITHUB_SHA'],'--title',f'IWatchYou {version}','--notes',notes,'--latest=false'],check=True)
# Verify the actual release bytes before advertising them to installed clients.
verify=root/'distribution/verified';verify.mkdir(exist_ok=True)
subprocess.run(['gh','release','download',tag,'--repo',repo,'--pattern',name,'--dir',str(verify),'--clobber'],check=True)
assert hashlib.sha256((verify/name).read_bytes()).hexdigest()==manifest['sha256']
request=dict(message=f'chore(iwatchyou): publish verified stable feed {version}',branch='main',content=base64.b64encode(feed.read_bytes()).decode())
if old:request['sha']=old['sha']
subprocess.run(['gh','api','--method','PUT',endpoint,'--input','-'],input=json.dumps(request),text=True,stdout=subprocess.DEVNULL,check=True)
print('Stable APK verified and published:',manifest['apkUrl'])
