from __future__ import annotations

import re
from urllib.parse import urlsplit

_DOMAIN_RE = re.compile(r"^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$", re.I)


def normalize_domain(value: str) -> str:
    value = value.strip()
    if not value:
        raise ValueError("Target kosong")
    candidate = value if "://" in value else "https://" + value
    parsed = urlsplit(candidate)
    host = (parsed.hostname or "").rstrip(".").lower()
    if host.startswith("*."):
        host = host[2:]
    try:
        host = host.encode("idna").decode("ascii")
    except UnicodeError as exc:
        raise ValueError("Domain tidak valid") from exc
    if not _DOMAIN_RE.fullmatch(host):
        raise ValueError("Versi awal menerima domain, misalnya example.com")
    return host


def host_in_scope(host: str, roots: list[str] | tuple[str, ...]) -> bool:
    host = (host or "").strip().rstrip(".").lower()
    return any(host == root or host.endswith("." + root) for root in roots)


def url_in_scope(url: str, roots: list[str] | tuple[str, ...]) -> bool:
    try:
        parsed = urlsplit(url if "://" in url else "https://" + url)
    except ValueError:
        return False
    return host_in_scope(parsed.hostname or "", roots)


def filter_domains(values: list[str], roots: list[str] | tuple[str, ...]) -> list[str]:
    out: set[str] = set()
    for value in values:
        value = value.strip().lower().rstrip(".")
        if host_in_scope(value, roots):
            out.add(value)
    return sorted(out)


def filter_urls(values: list[str], roots: list[str] | tuple[str, ...]) -> list[str]:
    out: set[str] = set()
    for value in values:
        value = value.strip()
        if value and url_in_scope(value, roots):
            out.add(value)
    return sorted(out)
