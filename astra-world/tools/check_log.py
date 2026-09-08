#!/usr/bin/env python3
"""Fail closed on engine diagnostics, independently of Godot's exit status."""
import re,sys
from pathlib import Path
text=re.sub(r'\x1b\[[0-?]*[ -/]*[@-~]','',Path(sys.argv[1]).read_text(errors='replace'))
errors=[line for line in text.splitlines() if any(x in line for x in ['SCRIPT ERROR','Parse Error','ERROR:','Shader compilation failed'])]
if errors:
    print('\n'.join(errors)); sys.exit(1)
print('Engine log clean:',sys.argv[1])
