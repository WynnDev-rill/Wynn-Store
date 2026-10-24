#!/usr/bin/env python3
"""Runs only after verified production signing in the protected release environment."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

repo = os.environ['GITHUB_REPOSITORY']
assert repo == 'WynnDev-rill/Wynn-Store'
apk = Path(os.environ.get('SG_APK_PATH', 'app/build/outputs/apk/release/app-release.apk'))
build = Path('app/build.gradle.kts').read_text()
version = re.search(r'versionName = "([0-9]+\.[0-9]+\.[0-9]+)"', build).group(1)
code = int(re.search(r'versionCode = (\d+)', build).group(1))
verification = subprocess.check_output([f'{os.environ["ANDROID_HOME"]}/build-tools/36.0.0/apksigner', 'verify', '--print-certs', str(apk)], text=True)
signer = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)', verification).group(1)
assert signer == 'eca17e8afa84775e941e0e490741b01636e7941da545c508f85b8a256a4f346a', 'Original production signer required'
badging = subprocess.check_output([f'{os.environ["ANDROID_HOME"]}/build-tools/36.0.0/aapt', 'dump', 'badging', str(apk)], text=True)
assert f"name='id.wynn.screenguard' versionCode='{code}' versionName='{version}'" in badging
tag = f'screenguard-v{version}'
name = f'ScreenGuard-Pro-{version}.apk'
output = Path('dist'); output.mkdir(exist_ok=True)
public_apk = output / name; public_apk.write_bytes(apk.read_bytes())
manifest = dict(versionCode=code, versionName=version, packageName='id.wynn.screenguard', minSdk=30,
    apkUrl=f'https://github.com/{repo}/releases/download/{tag}/{name}', sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
    signerSha256=signer, sizeBytes=apk.stat().st_size, notes='Memulihkan tampilan dan fitur versi 1.4: Beranda, Bekukan, Jaringan, Pengaturan, serta statistik sebelumnya. Pembaruan dapat dipasang di atas 1.5 tanpa uninstall; koleksi aplikasi, tema, dan data penggunaan tetap dipertahankan.')
metadata = output / 'update.json'; metadata.write_text(json.dumps(manifest, indent=2) + '\n')
endpoint = f'repos/{repo}/contents/screenguard-pro/updates/stable.json'
previous = subprocess.run(['gh', 'api', endpoint], text=True, capture_output=True)
request = dict(message=f'chore(screenguard): publish update feed {version}', branch='main', content=base64.b64encode(metadata.read_bytes()).decode())
if previous.returncode != 0 and '404' not in previous.stderr:
    raise RuntimeError('Cannot read current update feed; refusing to publish')
if previous.returncode == 0:
    old = json.loads(previous.stdout)
    old_metadata = json.loads(base64.b64decode(old['content']))
    if code == old_metadata['versionCode']:
        assert manifest['sha256'] == old_metadata['sha256'] and manifest['apkUrl'] == old_metadata['apkUrl'], 'Existing release differs'
        print('Stable release already active with identical bytes'); raise SystemExit(0)
    assert code > old_metadata['versionCode'], 'versionCode must increase'
    assert signer == old_metadata['signerSha256'], 'Signing certificate must match existing stable channel'
    request['sha'] = old['sha']
# Never replace existing assets. A draft surviving a failed feed write can be resumed.
existing = subprocess.run(['gh', 'release', 'view', tag, '--repo', repo, '--json', 'assets'], capture_output=True, text=True)
if existing.returncode == 0:
    assets = json.loads(existing.stdout)['assets']
    assert any(a['name'] == name and a['size'] == manifest['sizeBytes'] for a in assets), 'Existing release asset mismatch'
    verify_dir = output / 'downloaded'; verify_dir.mkdir(exist_ok=True)
    subprocess.run(['gh', 'release', 'download', tag, '--repo', repo, '--pattern', name, '--dir', str(verify_dir), '--clobber'], check=True)
    assert hashlib.sha256((verify_dir / name).read_bytes()).hexdigest() == manifest['sha256'], 'Published bytes differ'
else:
    subprocess.run(['gh', 'release', 'create', tag, str(public_apk), str(metadata), '--repo', repo, '--target', os.environ['GITHUB_SHA'],
    '--title', f'ScreenGuard Pro {version}', '--notes', f'Native Android 11+. Source: {os.environ["GITHUB_SHA"]}. SHA-256: {manifest["sha256"]}', '--latest=false'], check=True)
subprocess.run(['gh', 'api', '--method', 'PUT', endpoint, '--input', '-'], input=json.dumps(request), text=True, check=True, stdout=subprocess.DEVNULL)
