from __future__ import annotations

import json
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

from .api_keys import configured, usable_configured, write_subfinder_provider_config
from .config import LOGS_DIR, SCANS_DIR, SUBFINDER_PROVIDER_FILE, ensure_layout, load_config
from .report import parse_httpx, parse_nuclei, write_report
from .runner import require_engines, run_command
from .scope import filter_domains, filter_urls, normalize_domain

Progress = Callable[[str, str], None]


@dataclass
class ScanResult:
    scan_id: str
    domain: str
    scan_dir: Path
    assets: list[dict]
    findings: list
    report_md: Path
    report_json: Path
    elapsed: float


def _notify(cb: Progress | None, stage: str, detail: str) -> None:
    if cb:
        cb(stage, detail)


def _read_lines(path: Path) -> list[str]:
    if not path.exists():
        return []
    return [x.strip() for x in path.read_text(encoding="utf-8", errors="replace").splitlines() if x.strip()]


def _write_lines(path: Path, values: list[str]) -> None:
    path.write_text("\n".join(values) + ("\n" if values else ""), encoding="utf-8")


def _meta_path(scan_dir: Path) -> Path:
    return scan_dir / "scan.json"


def _save_meta(scan_dir: Path, meta: dict) -> None:
    tmp = scan_dir / "scan.json.tmp"
    tmp.write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    tmp.replace(_meta_path(scan_dir))


def _mark_stage(scan_dir: Path, meta: dict, stage: str) -> None:
    meta.setdefault("stages", {})[stage] = "complete"
    meta["updated_utc"] = datetime.now(timezone.utc).isoformat()
    _save_meta(scan_dir, meta)


def _stage_done(meta: dict, stage: str, *required: Path) -> bool:
    if (meta.get("stages") or {}).get(stage) != "complete":
        return False
    return all(path.exists() for path in required)


def list_resumable_scans(limit: int = 20) -> list[tuple[dict, Path]]:
    ensure_layout()
    rows: list[tuple[dict, Path]] = []
    if not SCANS_DIR.exists():
        return rows
    for path in sorted(SCANS_DIR.glob("*/scan.json"), reverse=True):
        try:
            meta = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if meta.get("status") in {"running", "interrupted", "failed"}:
            rows.append((meta, path.parent))
        if len(rows) >= limit:
            break
    return rows


def _load_resume(scan_id: str | None) -> tuple[dict, Path]:
    if scan_id:
        scan_dir = SCANS_DIR / scan_id
        path = _meta_path(scan_dir)
        if not path.exists():
            raise ValueError(f"Scan tidak ditemukan: {scan_id}")
        meta = json.loads(path.read_text(encoding="utf-8"))
    else:
        rows = list_resumable_scans(limit=1)
        if not rows:
            raise ValueError("Tidak ada scan yang bisa dilanjutkan.")
        meta, scan_dir = rows[0]
    if meta.get("status") == "complete":
        raise ValueError("Scan tersebut sudah selesai.")
    scope = meta.get("authorized_scope") or []
    if not scope:
        raise ValueError("Metadata scope scan tidak valid.")
    return meta, scan_dir


def scan(target: str, progress: Progress | None = None) -> ScanResult:
    ensure_layout()
    domain = normalize_domain(target)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    scan_id = f"{stamp}-{domain.replace('.', '_')}"
    scan_dir = SCANS_DIR / scan_id
    scan_dir.mkdir(parents=True, exist_ok=False)
    meta = {
        "scan_id": scan_id,
        "started_utc": datetime.now(timezone.utc).isoformat(),
        "updated_utc": datetime.now(timezone.utc).isoformat(),
        "authorized_scope": [domain],
        "api_sources_configured": configured(),
        "policy": "authorized-bounty-only; no credential use; no destructive exploitation",
        "status": "running",
        "stages": {},
    }
    _save_meta(scan_dir, meta)
    return _run_scan(domain, scan_dir, meta, progress, resumed=False)


def resume_scan(scan_id: str | None = None, progress: Progress | None = None) -> ScanResult:
    ensure_layout()
    meta, scan_dir = _load_resume(scan_id)
    domain = normalize_domain((meta.get("authorized_scope") or [""])[0])
    meta["status"] = "running"
    meta["resumed_utc"] = datetime.now(timezone.utc).isoformat()
    meta.pop("last_error", None)
    _save_meta(scan_dir, meta)
    return _run_scan(domain, scan_dir, meta, progress, resumed=True)


def _run_scan(
    domain: str,
    scan_dir: Path,
    meta: dict,
    progress: Progress | None,
    resumed: bool,
) -> ScanResult:
    require_engines()
    cfg = load_config()
    roots = [domain]
    scan_id = str(meta.get("scan_id") or scan_dir.name)
    log_file = LOGS_DIR / f"{scan_id}.log"
    started = time.monotonic()
    rate = max(1, min(int(cfg.get("rate_limit", 5)), 30))
    meta["rate_limit"] = rate
    meta["api_sources_configured"] = configured()
    _save_meta(scan_dir, meta)

    try:
        subs_raw = scan_dir / "subdomains-raw.txt"
        subs_file = scan_dir / "subdomains.txt"
        if not _stage_done(meta, "recon", subs_file):
            _notify(progress, "recon", "Mencari aset publik" + (" (resume)" if resumed else ""))
            write_subfinder_provider_config()
            sub_args = [
                "subfinder", "-d", domain, "-silent", "-all",
                "-max-time", str(max(1, min(int(cfg.get("subfinder_minutes", 4)), 10))),
                "-rl", str(rate), "-duc", "-o", str(subs_raw),
            ]
            if usable_configured() and SUBFINDER_PROVIDER_FILE.exists():
                sub_args.extend(["-pc", str(SUBFINDER_PROVIDER_FILE)])
            run_command(sub_args, log_file, timeout=900)
            subs = filter_domains(_read_lines(subs_raw) + [domain], roots)
            _write_lines(subs_file, subs)
            _mark_stage(scan_dir, meta, "recon")
        else:
            _notify(progress, "resume", "Recon sudah selesai, dilewati")
        subs = filter_domains(_read_lines(subs_file) + [domain], roots)

        resolved_raw = scan_dir / "resolved-raw.txt"
        resolved_file = scan_dir / "resolved.txt"
        if not _stage_done(meta, "dns", resolved_file):
            _notify(progress, "dns", f"Memeriksa {len(subs)} aset")
            run_command([
                "dnsx", "-l", str(subs_file), "-silent", "-retry", "2",
                "-rl", str(max(5, rate * 4)), "-duc", "-o", str(resolved_raw),
            ], log_file, timeout=600)
            resolved = filter_domains(_read_lines(resolved_raw) + [domain], roots)
            _write_lines(resolved_file, resolved)
            _mark_stage(scan_dir, meta, "dns")
        else:
            _notify(progress, "resume", "DNS validation sudah selesai, dilewati")
        resolved = filter_domains(_read_lines(resolved_file) + [domain], roots)

        httpx_file = scan_dir / "httpx.jsonl"
        if not _stage_done(meta, "web", httpx_file):
            _notify(progress, "web", "Mengecek layanan web aktif")
            run_command([
                "httpx", "-l", str(resolved_file), "-silent", "-j", "-sc", "-title", "-td",
                "-server", "-ip", "-cname", "-fhr", "-maxr", "3",
                "-rstr", "2097152", "-rl", str(rate), "-t", "20",
                "-timeout", "10", "-retries", "1", "-duc", "-o", str(httpx_file),
            ], log_file, timeout=900)
            _mark_stage(scan_dir, meta, "web")
        else:
            _notify(progress, "resume", "HTTP probing sudah selesai, dilewati")
        assets = parse_httpx(httpx_file, roots)
        live_urls = sorted({a["url"] for a in assets})
        live_file = scan_dir / "live-urls.txt"
        _write_lines(live_file, live_urls)

        crawl_file = scan_dir / "crawl.txt"
        if not _stage_done(meta, "crawl", crawl_file):
            crawled: list[str] = []
            if live_urls:
                _notify(progress, "crawl", f"Mempelajari endpoint dari {len(live_urls)} web")
                crawl_raw = scan_dir / "crawl-raw.txt"
                run_command([
                    "katana", "-list", str(live_file), "-silent",
                    "-d", str(max(1, min(int(cfg.get("crawl_depth", 2)), 3))),
                    "-jc", "-kf", "robotstxt,sitemapxml", "-iqp", "-fs", "rdn",
                    "-rl", str(rate), "-c", "5", "-p", "3", "-timeout", "10",
                    "-duc", "-o", str(crawl_raw),
                ], log_file, timeout=1200)
                crawled = filter_urls(_read_lines(crawl_raw), roots)
            _write_lines(crawl_file, crawled)
            _mark_stage(scan_dir, meta, "crawl")
        else:
            _notify(progress, "resume", "Crawl sudah selesai, dilewati")
        crawled = filter_urls(_read_lines(crawl_file), roots)

        nuclei_targets = filter_urls(live_urls + crawled, roots)[:2500]
        targets_file = scan_dir / "targets.txt"
        _write_lines(targets_file, nuclei_targets)

        main_out = scan_dir / "nuclei-main.jsonl"
        if not _stage_done(meta, "screen", main_out):
            if nuclei_targets:
                _notify(progress, "screen", f"Screening {len(nuclei_targets)} endpoint")
                run_command([
                    "nuclei", "-l", str(targets_file),
                    "-s", "low,medium,high,critical",
                    "-pt", "http,ssl",
                    "-etags", "fuzz,dos,intrusive,creds-stuffing,token-spray",
                    "-ni", "-dut",
                    "-rl", str(rate), "-c", str(max(2, min(int(cfg.get("nuclei_concurrency", 10)), 15))),
                    "-bs", "10", "-timeout", "10", "-retries", "1",
                    "-jle", str(main_out), "-or", "-ot", "-duc",
                ], log_file, timeout=3600)
            else:
                main_out.write_text("", encoding="utf-8")
            _mark_stage(scan_dir, meta, "screen")
        else:
            _notify(progress, "resume", "Vulnerability screening sudah selesai, dilewati")

        exposure_out = scan_dir / "nuclei-exposure.jsonl"
        if not _stage_done(meta, "exposure", exposure_out):
            if nuclei_targets:
                _notify(progress, "exposure", "Mencari exposure dan konfigurasi sensitif")
                run_command([
                    "nuclei", "-l", str(targets_file),
                    "-tags", "exposure,exposures,config",
                    "-s", "info,low,medium,high,critical",
                    "-pt", "http",
                    "-etags", "fuzz,dos,intrusive,creds-stuffing,token-spray",
                    "-ni", "-dut",
                    "-rl", str(rate), "-c", "8", "-bs", "8", "-timeout", "10", "-retries", "1",
                    "-jle", str(exposure_out), "-or", "-ot", "-duc",
                ], log_file, timeout=2400)
            else:
                exposure_out.write_text("", encoding="utf-8")
            _mark_stage(scan_dir, meta, "exposure")
        else:
            _notify(progress, "resume", "Exposure screening sudah selesai, dilewati")

        _notify(progress, "report", "Mengurutkan kandidat temuan")
        findings = parse_nuclei([main_out, exposure_out], roots)
        meta.update({
            "finished_utc": datetime.now(timezone.utc).isoformat(),
            "updated_utc": datetime.now(timezone.utc).isoformat(),
            "status": "complete",
            "subdomains": len(subs),
            "resolved": len(resolved),
            "live_web": len(assets),
            "crawled_urls": len(crawled),
            "screened_urls": len(nuclei_targets),
            "findings": len(findings),
        })
        meta.setdefault("stages", {})["report"] = "complete"
        _save_meta(scan_dir, meta)
        report_json, report_md = write_report(scan_dir, domain, assets, findings, meta)
        elapsed = time.monotonic() - started
        _notify(progress, "done", f"Selesai: {len(findings)} kandidat")
        return ScanResult(scan_id, domain, scan_dir, assets, findings, report_md, report_json, elapsed)
    except KeyboardInterrupt:
        meta["status"] = "interrupted"
        meta["updated_utc"] = datetime.now(timezone.utc).isoformat()
        _save_meta(scan_dir, meta)
        raise
    except Exception as exc:
        meta["status"] = "failed"
        meta["last_error"] = str(exc)[:500]
        meta["updated_utc"] = datetime.now(timezone.utc).isoformat()
        _save_meta(scan_dir, meta)
        raise
