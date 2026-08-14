from __future__ import annotations

import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

from .scope import url_in_scope

_SECRET_WORDS = re.compile(r"(?i)(password|passwd|pwd|token|api[_-]?key|secret|authorization|bearer|session|cookie)")
_ASSIGNMENT = re.compile(r"(?i)\b(password|passwd|pwd|token|api[_-]?key|secret|authorization|bearer)\b(\s*[:=]\s*)[^\s,;&]+")
_JWT = re.compile(r"\beyJ[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{8,}\b")
_GH_TOKEN = re.compile(r"\bgh[pousr]_[A-Za-z0-9_]{20,}\b")

SEVERITY_SCORE = {"info": 10, "low": 25, "medium": 50, "high": 75, "critical": 95, "unknown": 15}


@dataclass
class Finding:
    severity: str
    score: int
    title: str
    template_id: str
    target: str
    tags: list[str]
    evidence: list[str]
    confidence: str = "candidate"


def redact_url(value: str) -> str:
    try:
        p = urlsplit(value)
    except ValueError:
        return redact_text(value)
    if not p.scheme or not p.netloc:
        return redact_text(value)
    query = []
    for key, val in parse_qsl(p.query, keep_blank_values=True):
        query.append((key, "<redacted>" if _SECRET_WORDS.search(key) else redact_text(val)))
    return urlunsplit((p.scheme, p.netloc, p.path, urlencode(query), p.fragment))


def redact_text(value: object) -> str:
    text = str(value)
    text = _ASSIGNMENT.sub(lambda m: f"{m.group(1)}{m.group(2)}<redacted>", text)
    text = _JWT.sub("<redacted-jwt>", text)
    text = _GH_TOKEN.sub("<redacted-token>", text)
    return text


def _read_jsonl(path: Path):
    if not path.exists():
        return
    with path.open(encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue
            if isinstance(obj, dict):
                yield obj


def parse_httpx(path: Path, roots: list[str]) -> list[dict]:
    assets: list[dict] = []
    seen: set[str] = set()
    for row in _read_jsonl(path) or ():
        url = str(row.get("url") or row.get("input") or "")
        if not url.startswith(("http://", "https://")):
            continue
        if not url_in_scope(url, roots) or url in seen:
            continue
        seen.add(url)
        tech = row.get("tech") or []
        if isinstance(tech, str):
            tech = [tech]
        assets.append({
            "url": url,
            "status": row.get("status_code") or row.get("status-code"),
            "title": redact_text(row.get("title") or ""),
            "tech": [redact_text(x) for x in tech],
            "server": redact_text(row.get("webserver") or row.get("server") or ""),
            "ip": redact_text(row.get("host_ip") or row.get("ip") or ""),
            "cname": redact_text(row.get("cname") or ""),
        })
    return assets


def _tags(info: dict) -> list[str]:
    tags = info.get("tags") or []
    if isinstance(tags, str):
        tags = [x.strip() for x in tags.split(",")]
    return [str(x).lower() for x in tags if str(x).strip()]


def parse_nuclei(paths: list[Path], roots: list[str]) -> list[Finding]:
    findings: list[Finding] = []
    seen: set[tuple[str, str]] = set()
    for path in paths:
        for row in _read_jsonl(path) or ():
            info = row.get("info") if isinstance(row.get("info"), dict) else {}
            severity = str(info.get("severity") or row.get("severity") or "unknown").lower()
            tags = _tags(info)
            target = str(row.get("matched-at") or row.get("host") or row.get("url") or "")
            host_target = target if "://" in target else str(row.get("host") or target)
            if not host_target or not url_in_scope(host_target, roots):
                continue
            template_id = str(row.get("template-id") or row.get("template_id") or row.get("template") or "unknown")
            dedupe = (template_id, target)
            if dedupe in seen:
                continue
            seen.add(dedupe)
            title = str(info.get("name") or template_id)
            score = SEVERITY_SCORE.get(severity, 15)
            tagset = set(tags)
            if tagset.intersection({"exposure", "exposures", "config", "token", "credentials"}):
                score += 8
            if "cve" in tagset:
                score += 4
            score = min(score, 100)
            evidence_raw = row.get("extracted-results") or row.get("extracted_results") or []
            if not isinstance(evidence_raw, list):
                evidence_raw = [evidence_raw]
            evidence = [redact_text(v) for v in evidence_raw[:5] if str(v).strip()]
            findings.append(Finding(
                severity=severity,
                score=score,
                title=redact_text(title),
                template_id=template_id,
                target=redact_url(target),
                tags=tags,
                evidence=evidence,
            ))
    findings.sort(key=lambda f: (f.score, SEVERITY_SCORE.get(f.severity, 0)), reverse=True)
    return findings


def write_report(scan_dir: Path, domain: str, assets: list[dict], findings: list[Finding], meta: dict) -> tuple[Path, Path]:
    payload = {
        "project": "Cyber Tool By Wynn",
        "target": domain,
        "meta": meta,
        "assets": assets,
        "findings": [asdict(f) for f in findings],
    }
    json_path = scan_dir / "report.json"
    json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    counts = {s: sum(1 for f in findings if f.severity == s) for s in ("critical", "high", "medium", "low", "info", "unknown")}
    lines = [
        "# Cyber Tool By Wynn — Bounty Report",
        "",
        f"Target: `{domain}`",
        f"Aset web aktif: **{len(assets)}**",
        f"Kandidat temuan: **{len(findings)}**",
        "",
        "> Semua hasil adalah kandidat otomatis. Verifikasi manual dan aturan program bounty tetap menjadi otoritas sebelum dilaporkan.",
        "",
        "## Ringkasan",
        "",
        f"- Critical: {counts['critical']}",
        f"- High: {counts['high']}",
        f"- Medium: {counts['medium']}",
        f"- Low: {counts['low']}",
        f"- Info: {counts['info']}",
        "",
        "## Kandidat prioritas",
        "",
    ]
    if not findings:
        lines.append("Tidak ada kandidat vulnerability yang terdeteksi pada scan ini.")
    for idx, f in enumerate(findings[:100], 1):
        lines.extend([
            f"### {idx}. [{f.severity.upper()}] {f.title}",
            "",
            f"- Score: {f.score}/100",
            f"- Template: `{f.template_id}`",
            f"- Target: `{f.target}`",
            f"- Status: {f.confidence}; perlu verifikasi manual",
        ])
        if f.tags:
            lines.append("- Tags: " + ", ".join(f.tags))
        if f.evidence:
            lines.append("- Evidence (secret disamarkan): " + "; ".join(f.evidence))
        lines.append("")
    md_path = scan_dir / "REPORT.md"
    md_path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
    return json_path, md_path
