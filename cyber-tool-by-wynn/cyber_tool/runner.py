from __future__ import annotations

import shutil
import subprocess
from pathlib import Path
from typing import Iterable

REQUIRED_ENGINES = ("subfinder", "dnsx", "httpx", "katana", "nuclei")


def engine_status() -> dict[str, bool]:
    return {name: shutil.which(name) is not None for name in REQUIRED_ENGINES}


def require_engines() -> None:
    missing = [name for name, ok in engine_status().items() if not ok]
    if missing:
        raise RuntimeError("Engine belum terpasang: " + ", ".join(missing) + ". Jalankan: cyber repair")


def run_command(args: Iterable[str], log_file: Path, cwd: Path | None = None, timeout: int | None = None) -> subprocess.CompletedProcess[str]:
    args = [str(x) for x in args]
    log_file.parent.mkdir(parents=True, exist_ok=True)
    with log_file.open("a", encoding="utf-8") as log:
        log.write("\n$ " + " ".join(args) + "\n")
        log.flush()
        proc = subprocess.run(
            args,
            cwd=str(cwd) if cwd else None,
            stdout=log,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=timeout,
            check=False,
        )
    if proc.returncode != 0:
        raise RuntimeError(f"Engine gagal ({args[0]} exit {proc.returncode}). Log: {log_file}")
    return proc
