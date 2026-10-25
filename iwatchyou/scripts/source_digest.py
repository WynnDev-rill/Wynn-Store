"""Content identity for the native app; excludes reports, feed, and signed binaries."""
import hashlib
from pathlib import Path

def source_digest(root=Path('iwatchyou')):
    paths=[p for p in (root/'app/src/main').rglob('*') if p.is_file()]
    paths += [root/n for n in ('app/build.gradle.kts','app/proguard-rules.pro','build.gradle.kts','gradle.properties','settings.gradle.kts')]
    digest=hashlib.sha256()
    for p in sorted(paths):
        digest.update(str(p.relative_to(root)).encode()+b'\0'+p.read_bytes()+b'\0')
    return digest.hexdigest()

if __name__=='__main__': print(source_digest())
