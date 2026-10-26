#!/usr/bin/env python3
"""Owner-only build. Keep user-owned model bytes outside git and public workflows."""
import os,pathlib,shutil,subprocess,sys,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
if len(sys.argv)!=2: raise SystemExit("Usage: python scripts/build-personal.py /absolute/path/to/model.zip")
source=pathlib.Path(sys.argv[1]).resolve()
if source.stat().st_size>500*1024*1024: raise SystemExit("ZIP exceeds 500 MiB")
with zipfile.ZipFile(source) as z:
 if not any(n.endswith(".model3.json") for n in z.namelist()): raise SystemExit("No Cubism model3 file")
dest=root/".deps/personal-assets/personal/model.zip";dest.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(source,dest)
env=os.environ.copy();env["FP_PERSONAL_MODEL"]="true"
subprocess.run([sys.executable,str(root/"scripts/build-release.py")],env=env,check=True)
