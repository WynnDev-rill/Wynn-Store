#!/usr/bin/env python3
"""Upload secrets over gh stdin; requires an account with Actions secrets write."""
import base64,json,os,pathlib,subprocess,sys
p=pathlib.Path(os.environ.get('ARITHMEMO_SIGNING_FILE','/workspace/private/arithmemo/signing.json'))
d=json.loads(p.read_text())
values={k:v for k,v in d.items() if k!='AM_KEYSTORE_PATH'}
values['AM_KEYSTORE_BASE64']=base64.b64encode(pathlib.Path(d['AM_KEYSTORE_PATH']).read_bytes()).decode()
for key,value in values.items():
    r=subprocess.run(['gh','secret','set',key,'--repo','WynnDev-rill/Wynn-Store'],input=value,text=True,capture_output=True)
    if r.returncode:
        sys.exit('GitHub denied Actions secrets write. Use an authorized account to run this script. '+r.stderr.strip())
    print(key+': configured')
