"""Pinned Godot Android build. Signing secrets stay outside the repository.
Set GODOT, ANDROID_HOME, JAVA_HOME. Pass --signing to a private directory with
astraslash.keystore and credentials.json {alias,password}, or set the official
GODOT_ANDROID_KEYSTORE_RELEASE_PATH / USER / PASSWORD environment variables.
"""
import argparse,os,pathlib,subprocess,json,hashlib
p=argparse.ArgumentParser();p.add_argument('--signing',type=pathlib.Path);p.add_argument('--desktop',action='store_true');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];dist=root/'dist';dist.mkdir(exist_ok=True);ev=root/'evidence';ev.mkdir(exist_ok=True)
env=os.environ.copy();godot=env.get('GODOT','godot');version=subprocess.check_output([godot,'--version'],text=True).strip()
if not version.startswith('4.6.1.'):raise SystemExit('Godot 4.6.1 required; found '+version)
if a.signing:
 c=json.loads((a.signing/'credentials.json').read_text());env.update({'GODOT_ANDROID_KEYSTORE_RELEASE_PATH':str((a.signing/'astraslash.keystore').resolve()),'GODOT_ANDROID_KEYSTORE_RELEASE_USER':c['alias'],'GODOT_ANDROID_KEYSTORE_RELEASE_PASSWORD':c['password']})
if not a.desktop:
 for k in ['ANDROID_HOME','JAVA_HOME','GODOT_ANDROID_KEYSTORE_RELEASE_PATH','GODOT_ANDROID_KEYSTORE_RELEASE_USER','GODOT_ANDROID_KEYSTORE_RELEASE_PASSWORD']:
  if not env.get(k):raise SystemExit('Required: '+k)
 settings=pathlib.Path.home()/'.config/godot/editor_settings-4.6.tres';settings.parent.mkdir(parents=True,exist_ok=True)
 settings.write_text('[gd_resource type="EditorSettings" format=3]\n[resource]\nexport/android/android_sdk_path = '+json.dumps(env['ANDROID_HOME'])+'\nexport/android/java_sdk_path = '+json.dumps(env['JAVA_HOME'])+'\n')
artifact=dist/('AstraSlash.x86_64' if a.desktop else 'AstraSlash.apk');logpath=ev/('build-linux.txt' if a.desktop else 'build-android.txt')
with logpath.open('w') as log:
 for args in [['--editor','--import','--quit'],['--export-release','Linux' if a.desktop else 'Android',str(artifact)]]:
  r=subprocess.run([godot,'--headless','--path',str(root)]+args,env=env,stdout=log,stderr=log,timeout=300)
  if r.returncode:raise SystemExit('Build failed; see '+str(logpath))
if not artifact.exists() or 'SCRIPT ERROR:' in logpath.read_text():raise SystemExit('Build failed validation')
m={'engine':version,'artifact':artifact.name,'bytes':artifact.stat().st_size,'sha256':hashlib.sha256(artifact.read_bytes()).hexdigest()}
if not a.desktop:
 bt=pathlib.Path(env['ANDROID_HOME'])/'build-tools/35.0.1'
 (ev/'apk-signature.txt').write_text(subprocess.check_output([str(bt/'apksigner'),'verify','--verbose','--print-certs',str(artifact)],env=env,text=True))
 badging=subprocess.check_output([str(bt/'aapt2'),'dump','badging',str(artifact)],env=env,text=True);(ev/'apk-badging.txt').write_text(badging)
 assert "package: name='id.wynn.astraslash'" in badging and "application-label:'AstraSlash'" in badging
(ev/('manifest-linux.json' if a.desktop else 'manifest-android.json')).write_text(json.dumps(m,indent=2)+'\n');print(json.dumps(m,indent=2))
