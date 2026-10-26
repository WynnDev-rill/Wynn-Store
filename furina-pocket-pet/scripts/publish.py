#!/usr/bin/env python3
"""Runs only after the CI publication-approval gate. Never replaces an existing version."""
import hashlib,json,os,pathlib,re,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
subprocess.run([str(root/'gradlew'),'-p',str(root),':app:assembleRelease','--console=plain'],check=True)
apk=root/'app/build/outputs/apk/release/app-release.apk'
signer=subprocess.check_output([os.environ['ANDROID_HOME']+'/build-tools/36.0.0/apksigner','verify','--print-certs',str(apk)],text=True)
digest=re.search(r'Signer #1 certificate SHA-256 digest: (\w+)',signer).group(1)
expected=(root/'docs/signing-sha256.txt').read_text().strip()
assert digest==expected,'Signing key differs from the initial APK. Refusing an incompatible update.'
version=re.search(r'versionName = "([0-9.]+)"',(root/'app/build.gradle.kts').read_text()).group(1)
out=root/'build/release';out.mkdir(parents=True,exist_ok=True)
name=f'Furina-Pocket-Pet-{version}.apk';dest=out/name;dest.write_bytes(apk.read_bytes())
meta=out/'sha256.txt';meta.write_text(hashlib.sha256(dest.read_bytes()).hexdigest()+'  '+name+'\n')
subprocess.run(['gh','release','create',f'furina-v{version}',str(dest),str(meta),'--repo','WynnDev-rill/Wynn-Store','--target',os.environ['GITHUB_SHA'],'--title',f'Furina Pocket Pet {version}','--notes','Native Android Live2D pet. No Furina model bundled. Import only properly licensed local models.','--latest=false'],check=True)
