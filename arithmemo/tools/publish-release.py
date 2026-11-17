#!/usr/bin/env python3
"""Stage verified public artifacts, then activate only after device/upgrade gates."""
import base64,hashlib,json,pathlib,subprocess,sys,tempfile
root=pathlib.Path(__file__).resolve().parent.parent
repo='WynnDev-rill/Wynn-Store';out=root/'distribution'
assert sys.argv[1:] in [['--stage'],['--activate']], 'Use --stage, verify devices/upgrades, then --activate'
stage=sys.argv[1]=='--stage'
m=json.loads((out/'update.json').read_text());tag='arithmemo-v'+m['versionName']
def gh(*args,**kwargs):return subprocess.check_output(['gh',*args],text=True,**kwargs)
endpoint=f'repos/{repo}/contents/arithmemo/updates/stable.json'
prior=subprocess.run(['gh','api',endpoint],capture_output=True,text=True)
sha=None
already_active=False
if prior.returncode==0:
 old=json.loads(prior.stdout);sha=old['sha'];data=json.loads(base64.b64decode(old['content']))
 already_active=data==m
 assert data['versionCode']<m['versionCode'] or (not stage and already_active),'Stable feed cannot go backwards or change a published version'
 assert data['signerSha256']==m['signerSha256'],'Existing release signing identity differs'
else:
 assert '404' in prior.stderr,'Cannot read stable feed: '+prior.stderr
assert hashlib.sha256((out/'ArithMemo.apk').read_bytes()).hexdigest()==m['sha256']
assets=[str(out/name) for name in ['ArithMemo.apk','SHA256SUMS','update.json','signing-certificate.sha256']]
existing=subprocess.run(['gh','release','view',tag,'--repo',repo,'--json','isPrerelease'],capture_output=True,text=True)
if stage:
 if existing.returncode==0:
  assert json.loads(existing.stdout)['isPrerelease'],'Published final releases are immutable'
  gh('release','upload',tag,*assets,'--repo',repo,'--clobber')
 else:
  head=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
  gh('release','create',tag,*assets,'--repo',repo,'--target',head,'--title','ArithMemo '+m['versionName'],'--notes-file',str(root/'docs/CHANGELOG.md'),'--latest=false','--prerelease')
else:
 assert existing.returncode==0,'Stage a verified prerelease first'
with tempfile.TemporaryDirectory() as folder:
 gh('release','download',tag,'--repo',repo,'--pattern','ArithMemo.apk','--dir',folder)
 assert hashlib.sha256((pathlib.Path(folder)/'ArithMemo.apk').read_bytes()).hexdigest()==m['sha256'],'Uploaded APK differs; stable feed unchanged'
if stage:
 print('Staged verified prerelease. Run device and upgrade gates before activation.')
else:
 payload={'message':'release(arithmemo): activate '+m['versionName'],'content':base64.b64encode((out/'update.json').read_bytes()).decode(),'branch':'main'}
 if sha:payload['sha']=sha
 if not already_active:gh('api','--method','PUT',endpoint,'--input','-',input=json.dumps(payload))
 gh('release','edit',tag,'--repo',repo,'--prerelease=false','--latest=false')
 print('Activated https://github.com/'+repo+'/releases/tag/'+tag)
