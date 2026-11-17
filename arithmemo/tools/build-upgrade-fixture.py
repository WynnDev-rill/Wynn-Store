#!/usr/bin/env python3
"""Build test-only baseline APKs with the production identity; key stays local."""
import json,os,pathlib,shutil,subprocess,sys
root=pathlib.Path(__file__).resolve().parent.parent
private=pathlib.Path(os.environ.get('ARITHMEMO_SIGNING_FILE','/workspace/private/arithmemo/signing.json'))
values=json.loads(private.read_text());env={**os.environ,**values}
subprocess.run(['./gradlew','-PqaBaseline','-PtestProduction','-PreleaseCode=1','-PreleaseName=0.9.9',':app:assembleRelease',':app:assembleReleaseAndroidTest','--console=plain'],cwd=root,env=env,check=True)
out=root/'qa/fixtures';out.mkdir(parents=True,exist_ok=True)
shutil.copyfile(root/'app/build/outputs/apk/release/app-release.apk',out/'ArithMemo-baseline.apk')
sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/workspace/android-sdk')))
signer=sdk/'build-tools/36.0.0/apksigner'
source=root/'app/build/outputs/apk/androidTest/release/app-release-androidTest.apk'
target=out/'ArithMemo-upgrade-test.apk'
subprocess.run([str(signer),'sign','--ks',values['AM_KEYSTORE_PATH'],'--ks-key-alias',values['AM_KEY_ALIAS'],'--ks-pass','env:AM_STORE_PASSWORD','--key-pass','env:AM_KEY_PASSWORD','--out',str(target),str(source)],env=env,check=True)
for apk in out.glob('*.apk'):
 result=subprocess.check_output([str(signer),'verify','--print-certs',str(apk)],text=True)
 assert (root/'signing-certificate.sha256').read_text().strip() in result,'QA certificate mismatch'
print('Signed test-only baseline and instrumentation prepared; no key uploaded')
