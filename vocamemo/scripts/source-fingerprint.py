#!/usr/bin/env python3
import pathlib,hashlib
root=pathlib.Path(__file__).resolve().parents[1]
files=[p for p in (root/'app/src/main').rglob('*') if p.is_file()]
files.extend(root/p for p in ['app/proguard-rules.pro','app/build.gradle.kts','build.gradle.kts','settings.gradle.kts','gradle.properties','gradle/wrapper/gradle-wrapper.properties'])
d=hashlib.sha256()
for p in sorted(files,key=lambda p:p.relative_to(root).as_posix()):
 d.update(p.relative_to(root).as_posix().encode());d.update(b'\0')
 with p.open('rb') as f:
  while b:=f.read(32768):d.update(b)
print(d.hexdigest())
