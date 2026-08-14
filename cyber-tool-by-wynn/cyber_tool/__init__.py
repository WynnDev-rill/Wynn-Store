"""Cyber Tool By Wynn - authorized bug bounty automation for Termux."""

from pathlib import Path

VERSION = (Path(__file__).resolve().parent.parent / "VERSION").read_text(encoding="utf-8").strip()

__all__ = ["VERSION"]
