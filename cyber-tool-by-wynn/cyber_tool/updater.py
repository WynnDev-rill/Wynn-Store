from __future__ import annotations

import os
import subprocess
from pathlib import Path

from .config import LOGS_DIR, ROOT

REPO_DIR = ROOT / "repo"
REPO_URL = "https://github.com/WynnDev-rill/Wynn-Store.git"
GO_TOOLS = (
    "github.com/projectdiscovery/subfinder/v2/cmd/subfinder@latest",
    "github.com/projectdiscovery/dnsx/cmd/dnsx@latest",
    "github.com/projectdiscovery/httpx/cmd/httpx@latest",
    "github.com/projectdiscovery/katana/cmd/katana@latest",
    "github.com/projectdiscovery/nuclei/v3/cmd/nuclei@latest",
)


def _run(args: list[str], env: dict[str, str] | None = None, log_file: Path | None = None) -> None:
    log_file = log_file or (LOGS_DIR / "update.log")
    log_file.parent.mkdir(parents=True, exist_ok=True)
    with log_file.open("a", encoding="utf-8") as log:
        log.write("\n$ " + " ".join(args) + "\n")
        log.flush()
        proc = subprocess.run(
            args,
            env=env,
            stdout=log,
            stderr=subprocess.STDOUT,
            text=True,
            check=False,
        )
    if proc.returncode != 0:
        raise RuntimeError(f"Update gagal: {args[0]} (exit {proc.returncode}). Log: {log_file}")


def update_app(update_engines: bool = False) -> None:
    if not (REPO_DIR / ".git").exists():
        raise RuntimeError("Repo instalasi tidak ditemukan. Jalankan installer lagi.")

    log_file = LOGS_DIR / "update.log"
    log_file.parent.mkdir(parents=True, exist_ok=True)
    log_file.write_text("", encoding="utf-8")

    _run(["git", "-C", str(REPO_DIR), "fetch", "origin", "main", "--depth", "1"], log_file=log_file)
    _run(["git", "-C", str(REPO_DIR), "reset", "--hard", "FETCH_HEAD"], log_file=log_file)
    _run(
        ["python", "-m", "pip", "install", "-q", "-r", str(REPO_DIR / "cyber-tool-by-wynn" / "requirements.txt")],
        log_file=log_file,
    )
    try:
        _run(["nuclei", "-ut"], log_file=log_file)
    except RuntimeError:
        pass

    if update_engines:
        env = os.environ.copy()
        prefix = env.get("PREFIX", "/data/data/com.termux/files/usr")
        env["GOBIN"] = str(Path(prefix) / "bin")
        for package in GO_TOOLS:
            _run(["go", "install", "-v", package], env=env, log_file=log_file)
