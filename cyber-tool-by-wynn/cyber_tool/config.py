from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any

ROOT = Path(os.environ.get("CYBER_HOME", str(Path.home() / ".cyber-tool-by-wynn"))).expanduser()
DATA_DIR = ROOT / "data"
SCANS_DIR = ROOT / "scans"
LOGS_DIR = ROOT / "logs"
CONFIG_FILE = ROOT / "config.json"
KEYS_FILE = ROOT / "api-keys.json"
SUBFINDER_PROVIDER_FILE = ROOT / "subfinder-provider.yaml"

DEFAULT_CONFIG: dict[str, Any] = {
    "schema": 1,
    "rate_limit": 5,
    "crawl_depth": 2,
    "subfinder_minutes": 4,
    "nuclei_concurrency": 10,
    "auto_update_check": True,
}


def ensure_layout() -> None:
    for path in (ROOT, DATA_DIR, SCANS_DIR, LOGS_DIR):
        path.mkdir(parents=True, exist_ok=True)
        try:
            path.chmod(0o700)
        except OSError:
            pass
    if not CONFIG_FILE.exists():
        save_config(DEFAULT_CONFIG.copy())
    if not KEYS_FILE.exists():
        KEYS_FILE.write_text("{}\n", encoding="utf-8")
        _chmod_private(KEYS_FILE)


def _chmod_private(path: Path) -> None:
    try:
        path.chmod(0o600)
    except OSError:
        pass


def load_config() -> dict[str, Any]:
    ensure_layout()
    try:
        loaded = json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        loaded = {}
    cfg = DEFAULT_CONFIG.copy()
    if isinstance(loaded, dict):
        cfg.update(loaded)
    return cfg


def save_config(cfg: dict[str, Any]) -> None:
    ROOT.mkdir(parents=True, exist_ok=True)
    tmp = CONFIG_FILE.with_suffix(".tmp")
    tmp.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    tmp.replace(CONFIG_FILE)
    _chmod_private(CONFIG_FILE)
