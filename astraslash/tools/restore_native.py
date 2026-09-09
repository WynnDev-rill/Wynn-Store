"""Local Ubuntu tool extraction for constrained CI workspaces (no global apt install).
Uses Ubuntu archive metadata SHA-256 for package validation.
"""
from pathlib import Path
import gzip,json,urllib.request,hashlib,concurrent.futures,subprocess,sys,os
root=Path(sys.argv[1]).resolve();cache=root/'debs';cache.mkdir(exist_ok=True);dest=root/'sysroot';dest.mkdir(exist_ok=True)
meta=root/'ubuntu-packages.json'
if meta.exists():index=json.loads(meta.read_text())
else:
 index={}
 for dist in ['noble','noble-updates']:
  for section in ['main','universe']:
   u=f'https://archive.ubuntu.com/ubuntu/dists/{dist}/{section}/binary-amd64/Packages.gz'
   data=gzip.decompress(urllib.request.urlopen(u,timeout=55).read()).decode()
   for block in data.split('\n\n'):
    fields=dict(x.split(': ',1) for x in block.splitlines() if ': ' in x and not x.startswith(' '))
    if 'Filename' in fields:index[fields['Package']]={k:fields[k] for k in ['Filename','SHA256']}
 meta.write_text(json.dumps(index))
extra='xvfb xserver-common libxfont2 libxkbfile1 libxdo3 xdotool libxtst6 libxnvctrl0 x11-xkb-utils libyaml-cpp0.8 libspnav0 libxcb-xfixes0 libxcb-shape0 libxcb-keysyms1 libxcb-icccm4 libxcb-image0 libxcb-render-util0 libxcb-xinerama0 libxcb-xinput0 libxcb-cursor0 libxcb-util1 mesa-vulkan-drivers libllvm20 libegl-mesa0 libgbm1 libgl1-mesa-dri libglx-mesa0 openjdk-17-jdk-headless libpython3.12-stdlib libpython3.12-minimal python3-numpy blender blender-data'.split()
names=set([p.name.split('_')[0] for p in cache.glob('*.deb')]+extra)
def get(name):
 d=index[name];p=cache/Path(d['Filename']).name
 if not p.exists() or hashlib.sha256(p.read_bytes()).hexdigest()!=d['SHA256']:
  url='https://archive.ubuntu.com/ubuntu/'+d['Filename']
  for i in range(4):
   try:
    b=urllib.request.urlopen(url,timeout=55).read();assert hashlib.sha256(b).hexdigest()==d['SHA256'];p.write_bytes(b);break
   except Exception:
    if i==3:raise
 return name,p
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
 for name,p in pool.map(get,sorted(names)):
  target=root/'blender-fixed' if name=='blender' else dest;target.mkdir(exist_ok=True)
  subprocess.run(['dpkg-deb','-x',str(p),str(target)],check=True)
  print(name,flush=True)
src=Path('/usr/lib/jvm/java-17-openjdk-amd64');jdk=dest/'usr/lib/jvm/java-17-openjdk-amd64'
for p in src.rglob('*'):
 t=jdk/p.relative_to(src)
 if t.exists() or t.is_symlink():continue
 if p.is_dir():t.mkdir(parents=True,exist_ok=True)
 else:t.symlink_to(p)
link=Path('/usr/bin/xkbcomp')
if not link.exists():link.symlink_to(dest/'usr/bin/xkbcomp')
print('NATIVE_TOOLS_READY',flush=True)
