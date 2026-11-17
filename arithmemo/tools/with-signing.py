#!/usr/bin/env python3
"""Run a command with private signing values; never prints credentials."""
import json, os, pathlib, subprocess, sys
source = pathlib.Path(os.environ.get('ARITHMEMO_SIGNING_FILE', '/workspace/private/arithmemo/signing.json'))
if not source.is_file():
    sys.exit('Set ARITHMEMO_SIGNING_FILE to the private signing JSON backup (outside git).')
values = json.loads(source.read_text())
if not all(values.get(k) for k in ('AM_KEYSTORE_PATH','AM_STORE_PASSWORD','AM_KEY_ALIAS','AM_KEY_PASSWORD')):
    sys.exit('Signing configuration incomplete')
sys.exit(subprocess.call(sys.argv[1:], env={**os.environ, **values}))
