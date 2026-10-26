#!/usr/bin/env python3
"""Pinned official SDK + MIT test model. SDK binaries never enter source control.
Use --accept-sdk-license only after reviewing both Live2D agreements.
"""
import argparse, hashlib, json, pathlib, tempfile, urllib.request, zipfile

parser=argparse.ArgumentParser()
parser.add_argument('--accept-sdk-license',action='store_true')
args=parser.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
deps=root/'.deps'; deps.mkdir(exist_ok=True)
sdk=deps/'CubismSdkForJava-5-r.5'
SDK_SHA='2bccf7b3a6cc6fdaeade2cf1d7a6e544cb4ad71912d0131565648baa6d515bd3'
if not sdk.exists():
 if not args.accept_sdk_license:
  raise SystemExit('Read https://www.live2d.com/eula/live2d-proprietary-software-license-agreement_en.html and https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html then rerun with --accept-sdk-license for development. Publication is a separate approval.')
 archive=deps/'sdk.zip'
 urllib.request.urlretrieve('https://cubism.live2d.com/sdk-java/bin/CubismSdkForJava-5-r.5.zip',archive)
 assert hashlib.sha256(archive.read_bytes()).hexdigest()==SDK_SHA,'SDK checksum changed'
 with zipfile.ZipFile(archive) as z:
  for entry in z.infolist():
   target=(deps/entry.filename).resolve()
   assert target.is_relative_to(deps.resolve()),'Unsafe SDK archive'
  z.extractall(deps)
 archive.unlink()
assert (sdk/'Core/android/Live2DCubismCore.aar').is_file()
model=deps/'assets/fallback'; model.mkdir(parents=True,exist_ok=True)
commit='ea296fb06bb7daf68dbd390b7d11e7cf001d0b87'
lock=json.loads((root/'scripts/model-lock.json').read_text())
for name,sha in lock.items():
 dest=model/name
 if dest.is_file() and hashlib.sha256(dest.read_bytes()).hexdigest()==sha: continue
 path='LICENSE' if name=='LICENSE.txt' else 'source/webassets/model/'+name
 dest.parent.mkdir(parents=True,exist_ok=True)
 urllib.request.urlretrieve(f'https://raw.githubusercontent.com/7l-ui/whalegirl-pet/{commit}/{path}',dest)
 assert hashlib.sha256(dest.read_bytes()).hexdigest()==sha,f'Model checksum mismatch: {name}'
# Ship full upstream notices alongside generated assets.
notices=deps/'assets/licenses'; notices.mkdir(exist_ok=True)
for name,source in {'CubismCore.md':sdk/'Core/LICENSE.md','CubismFramework.md':sdk/'Framework/LICENSE.md','CubismRedistributableFiles.txt':sdk/'Core/RedistributableFiles.txt'}.items():
 (notices/name).write_bytes(source.read_bytes())
print('Verified Cubism Java R5 and pinned WhaleGirl MIT test model.')
