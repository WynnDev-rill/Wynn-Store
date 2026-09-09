"""Emit an exact repository-scoped manifest for connector Git object uploads."""
import pathlib,json,subprocess
out=[]
for p in sorted(pathlib.Path('astraslash').rglob('*')):
 if not p.is_file() or any(x in p.parts for x in ['.godot','dist','__pycache__']) or p.suffix in ['.tmp','.log']:continue
 b=p.read_bytes();r={'path':str(p),'size':len(b),'sha':subprocess.check_output(['git','hash-object',str(p)],text=True).strip()}
 try:r['content']=b.decode('utf-8')
 except UnicodeDecodeError:r['binary']=True
 out.append(r)
print(json.dumps(out))
