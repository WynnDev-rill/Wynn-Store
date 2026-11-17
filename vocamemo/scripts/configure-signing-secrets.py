#!/usr/bin/env python3
"""Read private signing.json outside Git and write Actions secrets through gh stdin."""
import argparse,json,pathlib,subprocess,base64
p=argparse.ArgumentParser();p.add_argument('credentials',type=pathlib.Path);a=p.parse_args()
v=json.loads(a.credentials.read_text())
key=pathlib.Path(v['VOCA_KEYSTORE_PATH'])
if not key.exists():key=a.credentials.parent/'release.p12'
values={name:v[name] for name in ['VOCA_STORE_PASSWORD','VOCA_KEY_ALIAS','VOCA_KEY_PASSWORD']}
values['VOCA_KEYSTORE_BASE64']=base64.b64encode(key.read_bytes()).decode()
for name,value in values.items():subprocess.run(['gh','secret','set',name,'--repo','WynnDev-rill/Wynn-Store'],input=value.encode(),check=True)
print('Signing secrets configured. Keep the private recovery package secure.')
