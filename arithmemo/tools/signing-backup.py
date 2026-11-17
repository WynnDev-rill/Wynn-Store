#!/usr/bin/env python3
"""Authenticated AES-256-GCM backup/restore for signing material outside git.
Requires cryptography. Password is read from a prompt or ARITHMEMO_BACKUP_PASSWORD.
"""
import base64,getpass,hashlib,json,os,pathlib,secrets,sys
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
os.umask(0o077)
mode,source,target=sys.argv[1:]
source=pathlib.Path(source);target=pathlib.Path(target).resolve()
repo=pathlib.Path(__file__).resolve().parents[2]
assert target!=repo and repo not in target.parents,'Signing material must stay outside the repository'
password=os.environ.get('ARITHMEMO_BACKUP_PASSWORD') or getpass.getpass('Backup password: ')
assert len(password)>=16,'Use a strong password of at least 16 characters'
header=b'AMBK1'
if mode=='create':
 assert not target.exists(),'Refusing to overwrite an existing signing backup'
 credentials=json.loads(source.read_text())
 payload=json.dumps({'credentials':credentials,'keystore':base64.b64encode(pathlib.Path(credentials['AM_KEYSTORE_PATH']).read_bytes()).decode()}).encode()
 salt=secrets.token_bytes(16);nonce=secrets.token_bytes(12)
 key=hashlib.pbkdf2_hmac('sha256',password.encode(),salt,600000,32)
 target.write_bytes(header+salt+nonce+AESGCM(key).encrypt(nonce,payload,header))
 target.chmod(0o600)
elif mode=='restore':
 data=source.read_bytes();assert data[:5]==header and len(data)>49,'Invalid signing backup'
 key=hashlib.pbkdf2_hmac('sha256',password.encode(),data[5:21],600000,32)
 payload=json.loads(AESGCM(key).decrypt(data[21:33],data[33:],header))
 assert not target.exists() or not any(target.iterdir()),'Restore destination must be empty'
 target.mkdir(mode=0o700,parents=True,exist_ok=True);target.chmod(0o700)
 credentials=payload['credentials'];credentials['AM_KEYSTORE_PATH']=str(target/'arithmemo-release.p12')
 pathlib.Path(credentials['AM_KEYSTORE_PATH']).write_bytes(base64.b64decode(payload['keystore'],validate=True))
 (target/'signing.json').write_text(json.dumps(credentials,indent=2)+'\n')
 for item in target.iterdir():item.chmod(0o600)
else:raise SystemExit('Use create SIGNING_JSON BACKUP_FILE or restore BACKUP_FILE EMPTY_DIRECTORY')
print('Signing '+mode+' completed: '+str(target))
