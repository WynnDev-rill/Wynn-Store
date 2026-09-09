"""Fetch pinned official Godot editor and just the two required export templates.
ZIP members are range-fetched, size and ZIP CRC verified before atomic replacement.
Usage: python3 fetch_engine.py /absolute/toolchain
"""
import concurrent.futures, pathlib, urllib.request, struct, zlib, sys, zipfile, os, time
ROOT=pathlib.Path(sys.argv[1]).resolve(); ROOT.mkdir(parents=True,exist_ok=True)
BASE='https://github.com/godotengine/godot-builds/releases/download/4.6.1-stable/'
def fetch(url,a,b):
 for attempt in range(5):
  try:
   with urllib.request.urlopen(urllib.request.Request(url,headers={'Range':f'bytes={a}-{b}'}),timeout=55) as r:
    data=r.read(); assert r.status==206 and len(data)==b-a+1,(r.status,len(data),b-a+1)
    return data,r.headers.get('Content-Range','')
  except Exception:
   if attempt==4: raise
   time.sleep(.5)
def parts(url,start,length,label):
 cache=ROOT/'chunks';cache.mkdir(exist_ok=True)
 spans=[(i,min(i+4*1024*1024,start+length)-1) for i in range(start,start+length,4*1024*1024)]
 def get(span):
  a,b=span;p=cache/f'{label}-{a}'
  if not p.exists() or p.stat().st_size!=b-a+1:
   data=fetch(url,a,b)[0];temp=pathlib.Path(str(p)+'.tmp');temp.write_bytes(data);temp.replace(p)
  return p.read_bytes()
 with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
  return b''.join(pool.map(get,spans))
def zip_members(url,wanted):
 total=int(fetch(url,0,0)[1].split('/')[-1]);tail=fetch(url,total-65536,total-1)[0]
 e=struct.unpack_from('<4s4H2IH',tail,tail.rfind(b'PK\x05\x06'));cd=fetch(url,e[6],e[6]+e[5]-1)[0];o=0
 while o<len(cd):
  h=struct.unpack_from('<4s6H3I5H2I',cd,o);n,x,c=h[10:13];name=cd[o+46:o+46+n].decode();o+=46+n+x+c
  key=pathlib.Path(name).name
  if key not in wanted:continue
  out=wanted[key]
  if out.exists() and zlib.crc32(out.read_bytes())==h[7]:print('Verified',out.name,flush=True);continue
  start=h[-1];lh=struct.unpack_from('<4s5H3I2H',fetch(url,start,start+511)[0]);off=start+30+lh[-2]+lh[-1]
  raw=parts(url,off,h[8],key);data=zlib.decompress(raw,-15) if h[4]==8 else raw
  assert len(data)==h[9] and zlib.crc32(data)==h[7],key
  out.parent.mkdir(parents=True,exist_ok=True);temp=out.with_suffix('.tmp');temp.write_bytes(data);temp.replace(out);out.chmod(0o755)
  print('Installed',key,len(data),flush=True)
zip_members(BASE+'Godot_v4.6.1-stable_linux.x86_64.zip',{'Godot_v4.6.1-stable_linux.x86_64':ROOT/'godot'})
templates=ROOT/'templates'
zip_members(BASE+'Godot_v4.6.1-stable_export_templates.tpz',{k:templates/k for k in ['android_release.apk','linux_release.x86_64']})
dst=pathlib.Path.home()/'.local/share/godot/export_templates/4.6.1.stable';dst.mkdir(parents=True,exist_ok=True)
for p in templates.iterdir():
 target=dst/p.name
 if target.is_symlink():target.unlink()
 if not target.exists():target.symlink_to(p)
print('ENGINE_READY',flush=True)
