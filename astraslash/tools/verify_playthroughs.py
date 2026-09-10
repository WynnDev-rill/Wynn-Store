"""Full input-driven simulation; no health, damage, enemies or progress are changed.
The fixed 60 Hz clock runs without rendering. This is not a GPU benchmark.
"""
import concurrent.futures
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'evidence'
EVIDENCE.mkdir(exist_ok=True)
GODOT = os.environ.get('GODOT', 'godot')

def verify(hero):
    with tempfile.TemporaryDirectory(prefix='astraslash-' + hero + '-') as temp:
        directory = Path(temp)
        env = {**os.environ, 'XDG_DATA_HOME': str(directory / 'profile')}
        log = EVIDENCE / ('playthrough-' + hero + '.log')
        with log.open('w') as stream:
            result = subprocess.run([
                GODOT, '--headless', '--resolution', '1280x720', '--fixed-fps', '60',
                '--path', str(ROOT), '--', '--qa', '--fast-qa',
                '--qa-dir=' + str(directory), '--autoplay=' + hero,
            ], env=env, stdout=stream, stderr=stream, timeout=300)
        if result.returncode or 'SCRIPT ERROR:' in log.read_text():
            raise RuntimeError(f'{hero}: simulation failed; see {log}')
        result = json.loads((directory / ('playthrough-' + hero + '.json')).read_text())
        assert result['result'] == 'victory' and result['node'] == 8, result
        assert {row['node'] for row in result['transitions']} == set(range(9)), result
        result['method'] = 'State-assisted ordinary input events, headless fixed 60 Hz; no gameplay overrides'
        (EVIDENCE / ('playthrough-' + hero + '.json')).write_text(json.dumps(result, indent=2) + '\n')
        print(hero, 'VICTORY', result['kills'], 'kills;', round(result['elapsed'], 1), 'simulated seconds', flush=True)

with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    list(pool.map(verify, ['rei', 'kael']))
