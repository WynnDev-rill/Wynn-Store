from __future__ import annotations

import argparse
import getpass
import os
import subprocess
from pathlib import Path

from . import VERSION
from .api_keys import PROVIDERS, configured, masked, remove_key, set_key
from .config import ROOT, ensure_layout
from .runner import engine_status
from .scanner import scan
from .tui import run_tui
from .updater import GO_TOOLS, update_app


def _repair() -> int:
    prefix = os.environ.get("PREFIX", "/data/data/com.termux/files/usr")
    if not Path(prefix).exists():
        print("Repair engine ditujukan untuk Termux.")
        return 2
    env = os.environ.copy()
    env["GOBIN"] = str(Path(prefix) / "bin")
    for package in GO_TOOLS:
        print(f"[engine] {package.split('/')[-1].split('@')[0]}")
        if subprocess.run(["go", "install", "-v", package], env=env).returncode != 0:
            return 1
    subprocess.run(["nuclei", "-ut"], check=False)
    return 0


def _api_cli(args) -> int:
    if args.action == "list":
        for p in PROVIDERS:
            print(f"{p.key:16} {masked(p.key)}")
        return 0
    if args.action == "set":
        if not args.provider:
            raise SystemExit("Gunakan: cyber api set PROVIDER")
        value = getpass.getpass(f"API key {args.provider}: ").strip()
        if not value:
            raise SystemExit("API key kosong")
        set_key(args.provider, value)
        print(f"API {args.provider} tersimpan lokal.")
        return 0
    if args.action == "remove":
        if not args.provider:
            raise SystemExit("Gunakan: cyber api remove PROVIDER")
        remove_key(args.provider)
        print(f"API {args.provider} dihapus.")
        return 0
    return 0


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="cyber", description="Cyber Tool By Wynn — authorized bounty automation")
    p.add_argument("--version", action="version", version=f"Cyber Tool By Wynn {VERSION}")
    sub = p.add_subparsers(dest="cmd")
    s = sub.add_parser("scan", help="jalankan scan pada scope yang berizin")
    s.add_argument("target")
    s.add_argument("--yes-i-am-authorized", action="store_true", help="konfirmasi eksplisit untuk mode CLI")
    sub.add_parser("history")
    u = sub.add_parser("update")
    u.add_argument("--engines", action="store_true")
    sub.add_parser("repair")
    sub.add_parser("doctor")
    a = sub.add_parser("api")
    a.add_argument("action", choices=["list", "set", "remove"], nargs="?", default="list")
    a.add_argument("provider", nargs="?")
    return p


def main(argv: list[str] | None = None) -> int:
    ensure_layout()
    args = build_parser().parse_args(argv)
    if not args.cmd:
        return run_tui()
    if args.cmd == "scan":
        if not args.yes_i_am_authorized:
            print("Scan CLI membutuhkan konfirmasi izin. Gunakan TUI `cyber`, atau tambahkan --yes-i-am-authorized jika target memang berizin.")
            return 2
        result = scan(args.target, progress=lambda stage, detail: print(f"[{stage}] {detail}"))
        print(result.report_md)
        return 0
    if args.cmd == "update":
        update_app(update_engines=args.engines)
        print("Cyber Tool sudah diperbarui.")
        return 0
    if args.cmd == "repair":
        return _repair()
    if args.cmd == "doctor":
        for name, ok in engine_status().items():
            print(f"{name:10} {'READY' if ok else 'MISSING'}")
        print(f"api        {len(configured())} configured")
        print(f"data       {ROOT}")
        return 0
    if args.cmd == "api":
        return _api_cli(args)
    if args.cmd == "history":
        from .tui import _history, _rich
        _, Console, _, _, _, _, _ = _rich()
        _history(Console())
        return 0
    return 0
