from __future__ import annotations

import os
import subprocess
from pathlib import Path

from .config import ROOT

REPO_DIR = ROOT / "repo"
REPO_URL = "https://github.com/WynnDev-rill/Wynn-Store.git"
GO_TOOLS = (
    "github.com/projectdiscovery/subfinder/v2/cmd/subfinder@latest",
    "github.com/projectdiscovery/dnsx/cmd/dnsx@latest",
    "github.com/projectdiscovery/httpx/cmd/httpx@latest",
    "github.com/projectdiscovery/katana/cmd/katana@latest",
    "github.com/projectdiscovery/nuclei/v3/cmd/nuclei@latest",
)


def _run(args: list[str], env: dict[str, str] | None = None) -> None:
    proc = subprocess.run(args, env=env, check=False)
    if proc.returncode != 0:
        raise RuntimeError("Gagal: " + " ".join(args))


def update_app(update_engines: bool = False) -> None:
    if not (REPO_DIR / ".git").exists():
        raise RuntimeError("Repo instalasi tidak ditemukan. Jalankan install.sh lagi.")
    _run(["git", "-C", str(REPO_DIR), "fetch", "origin", "main", "--depth", "1"])
    _run(["git", "-C", str(REPO_DIR), "reset", "--hard", "FETCH_HEAD"])
    _run(["python", "-m", "pip", "install", "-q", "-r", str(REPO_DIR / "cyber-tool-by-wynn" / "requirements.txt")])
    try:
        _run(["nuclei", "-ut"])
    except RuntimeError:
        pass
    if update_engines:
        env = os.environ.copy()
        prefix = env.get("PREFIX", "/data/data/com.termux/files/usr")
        env["GOBIN"] = str(Path(prefix) / "bin")
        for package in GO_TOOLS:
            _run(["go", "install", "-v", package], env=env)
