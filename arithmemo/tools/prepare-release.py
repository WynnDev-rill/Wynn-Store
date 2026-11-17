#!/usr/bin/env python3
"""Verify a production APK and prepare public release artifacts, never keys."""
import hashlib,json,os,pathlib,re,shutil,subprocess,sys
root=pathlib.Path(__file__).resolve().parent.parent
apk=pathlib.Path(sys.argv[1]).resolve()
sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/workspace/android-sdk')))
build=sdk/'build-tools'/'36.0.0'
def run(*args):return subprocess.check_output([str(x) for x in args],text=True)
verification=run(build/'apksigner','verify','--verbose','--print-certs',apk)
assert 'Number of signers: 1' in verification,'Expected exactly one production signer'
cert=re.search(r'certificate SHA-256 digest: ([0-9a-fA-F]+)',verification).group(1).lower()
pinned=(root/'signing-certificate.sha256').read_text().strip().lower()
assert cert==pinned,'Production certificate mismatch; refusing release'
badging=run(build/'aapt','dump','badging',apk)
assert 'application-debuggable' not in badging,'Debuggable QA APK cannot be released'
m=re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",badging)
assert m and m[1]=='id.wynn.arithmemo','Wrong package'
code=int(m[2]);assert code>0,'QA version code cannot be released'
version=m[3];assert re.fullmatch(r'\d+\.\d+\.\d+',version)
minimum=int(re.search(r"sdkVersion:'(\d+)'",badging)[1])
out=root/'distribution';out.mkdir(exist_ok=True)
if apk != (out/'ArithMemo.apk').resolve():shutil.copyfile(apk,out/'ArithMemo.apk')
digest=hashlib.sha256(apk.read_bytes()).hexdigest()
manifest={'versionCode':code,'versionName':version,'packageName':m[1],'minSdk':minimum,'apkUrl':f'https://github.com/WynnDev-rill/Wynn-Store/releases/download/arithmemo-v{version}/ArithMemo.apk','sha256':digest,'signerSha256':cert,'sizeBytes':apk.stat().st_size,'notes':(root/'docs/CHANGELOG.md').read_text()[:8000]}
(out/'update.json').write_text(json.dumps(manifest,indent=2)+'\n')
(out/'SHA256SUMS').write_text(digest+'  ArithMemo.apk\n')
(out/'signing-certificate.sha256').write_text(cert+'\n')
print(f'Verified ArithMemo {version} ({code}); SHA-256 {digest}; signer pinned')
