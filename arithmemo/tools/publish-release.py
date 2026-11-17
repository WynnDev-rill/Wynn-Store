#!/usr/bin/env python3
"""Publish already-verified artifacts and activate the stable feed after a download check."""
import base64,hashlib,json,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parent.parent
repo='WynnDev-rill/Wynn-Store';out=root/'distribution'
m=json.loads((out/'update.json').read_text());tag='arithmemo-v'+m['versionName']
def gh(*args,**kwargs):return subprocess.check_output(['gh',*args],text=True,**kwargs)
endpoint=f'repos/{repo}/contents/arithmemo/updates/stable.json'
prior=subprocess.run(['gh','api',endpoint],capture_output=True,text=True)
sha=None
if prior.returncode==0:
 old=json.loads(prior.stdout);sha=old['sha'];data=json.loads(base64.b64decode(old['content']))
 assert data['versionCode']<m['versionCode'],'Stable feed cannot go backwards or repeat a version'
 assert data['signerSha256']==m['signerSha256'],'Existing release signing identity differs'
else:
 assert '404' in prior.stderr,'Cannot read stable feed: '+prior.stderr
assert hashlib.sha256((out/'ArithMemo.apk').read_bytes()).hexdigest()==m['sha256']
gh('release','create',tag,str(out/'ArithMemo.apk'),str(out/'SHA256SUMS'),str(out/'update.json'),str(out/'signing-certificate.sha256'),'--repo',repo,'--title','ArithMemo '+m['versionName'],'--notes-file',str(root/'docs/CHANGELOG.md'),'--latest=false')
with tempfile.TemporaryDirectory() as folder:
 gh('release','download',tag,'--repo',repo,'--pattern','ArithMemo.apk','--dir',folder)
 assert hashlib.sha256((pathlib.Path(folder)/'ArithMemo.apk').read_bytes()).hexdigest()==m['sha256'],'Uploaded APK differs; stable feed unchanged'
payload={'message':'release(arithmemo): activate '+m['versionName'],'content':base64.b64encode((out/'update.json').read_bytes()).decode(),'branch':'main'}
if sha:payload['sha']=sha
gh('api','--method','PUT',endpoint,'--input','-',input=json.dumps(payload))
print('Published and verified https://github.com/'+repo+'/releases/tag/'+tag)
