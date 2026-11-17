#!/usr/bin/env python3
"""Verify signed release and generate the Wynn-Store stable feed."""
import pathlib,subprocess,json,hashlib,re,os,zipfile
root=pathlib.Path(__file__).resolve().parents[1];apk=root/'dist/VocaMemo.apk'
sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/workspace/android-sdk')))
check=subprocess.check_output([str(sdk/'build-tools/36.0.0/apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
cert=re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)',check).group(1)
expected=(root/'signing-certificate.sha256').read_text().strip()
assert cert==expected,'Signing key changed!'
config=(root/'app/build.gradle.kts').read_text();version=re.search(r'versionName = "([0-9.]+)"',config).group(1);code=int(re.search(r'versionCode = (\d+)',config).group(1))
badging=subprocess.check_output([str(sdk/'build-tools/36.0.0/aapt'),'dump','badging',str(apk)],text=True)
identity=re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",badging)
assert identity and identity.groups()==('id.wynn.vocamemo',str(code),version),'APK package/version does not match source!'
assert re.search(r"sdkVersion:'26'",badging),'APK minimum SDK does not match source!'
expected_source=subprocess.check_output(['python3',str(root/'scripts/source-fingerprint.py')],text=True).strip()
with zipfile.ZipFile(apk) as archive:
 assert archive.read('assets/source-fingerprint.txt').decode().strip()==expected_source,'Staged APK does not match current production source! Rebuild with the private release key.'
manifest={'sourceFingerprint':expected_source,'versionCode':code,'versionName':version,'packageName':'id.wynn.vocamemo','minSdk':26,'apkUrl':f'https://github.com/WynnDev-rill/Wynn-Store/releases/download/vocamemo-v{version}/VocaMemo.apk','sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'signerSha256':cert,'sizeBytes':apk.stat().st_size,'notes':(root/'RELEASE_NOTES.md').read_text()[:8000]}
(root/'dist/stable.json').write_text(json.dumps(manifest,indent=2)+'\n')
(root/'dist/SHA256SUMS').write_text(manifest['sha256']+'  VocaMemo.apk\n')
(root/'dist/signature-verification.txt').write_text(check)
print(json.dumps(manifest,indent=2))
