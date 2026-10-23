"""Verify the exact public production bytes before install; copy current release to Actions artifacts."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

feed = json.loads(Path('updates/stable.json').read_text())
apk = Path('distribution') / f'ScreenGuard-Pro-{feed["versionName"]}.apk'
data = apk.read_bytes()
assert len(data) == feed['sizeBytes'], 'Published APK size mismatch'
assert hashlib.sha256(data).hexdigest() == feed['sha256'], 'Published APK SHA-256 mismatch'
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
cert = subprocess.check_output([str(tools / 'apksigner'), 'verify', '--print-certs', str(apk)], text=True)
assert 'certificate SHA-256 digest: ' + feed['signerSha256'] in cert, 'Production signing certificate mismatch'
package = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)], text=True).splitlines()[0]
assert f"name='{feed['packageName']}'" in package
assert f"versionCode='{feed['versionCode']}'" in package
assert f"versionName='{feed['versionName']}'" in package
build_version = re.search(r'versionName = "([^"]+)"', Path('app/build.gradle.kts').read_text()).group(1)
if build_version == feed['versionName']:
    out = Path('dist/production'); out.mkdir(parents=True, exist_ok=True)
    shutil.copy2(apk, out / apk.name)
    (out / 'RELEASE-SHA256.txt').write_text(f'{feed["sha256"]}  {apk.name}\n')
    print(f'Current privately signed release verified: {apk.name}')
else:
    print(f'Published baseline {feed["versionName"]} verified; source is {build_version}')
