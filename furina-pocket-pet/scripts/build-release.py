#!/usr/bin/env python3
"""Build with the existing private stable key; never silently regenerate a signing identity."""
import json, os, pathlib, subprocess
root=pathlib.Path(__file__).resolve().parents[1]
env=os.environ.copy()
credential=root/'signing/credentials.json'
if credential.is_file():
 c=json.loads(credential.read_text())
 env.setdefault('FP_STORE_PASSWORD',c['storePassword'])
 env.setdefault('FP_KEY_PASSWORD',c['keyPassword'])
 env.setdefault('FP_KEY_ALIAS',c['keyAlias'])
if not env.get('FP_STORE_PASSWORD'): raise SystemExit('Restore the original signing backup or configure FP_KEYSTORE / FP_STORE_PASSWORD / FP_KEY_ALIAS / FP_KEY_PASSWORD. Do not use a new key for an update.')
gradle=env.get('GRADLE_EXECUTABLE',str(root/'gradlew'))
subprocess.run([gradle,'-p',str(root),':app:testDebugUnitTest',':app:lintRelease',':app:assembleRelease',':app:assembleDebug',':app:assembleDebugAndroidTest','--console=plain'],env=env,check=True)
