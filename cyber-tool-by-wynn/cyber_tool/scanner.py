from __future__ import annotations

import json
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

from .api_keys import configured, write_subfinder_provider_config
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


def scan(target: str, progress: Progress | None = None) -> ScanResult:
    ensure_layout()
    require_engines()
    cfg = load_config()
    domain = normalize_domain(target)
    roots = [domain]
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    scan_id = f"{stamp}-{domain.replace('.', '_')}"
    scan_dir = SCANS_DIR / scan_id
    scan_dir.mkdir(parents=True, exist_ok=False)
    log_file = LOGS_DIR / f"{scan_id}.log"
    started = time.monotonic()
    rate = max(1, min(int(cfg.get("rate_limit", 5)), 30))

    meta = {
        "scan_id": scan_id,
        "started_utc": datetime.now(timezone.utc).isoformat(),
        "authorized_scope": roots,
        "rate_limit": rate,
        "api_sources_configured": configured(),
        "policy": "authorized-bounty-only; no credential use; no destructive exploitation",
    }
    (scan_dir / "scan.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    _notify(progress, "recon", "Mencari aset publik")
    write_subfinder_provider_config()
    subs_raw = scan_dir / "subdomains-raw.txt"
    sub_args = [
        "subfinder", "-d", domain, "-silent", "-all",
        "-max-time", str(max(1, min(int(cfg.get("subfinder_minutes", 4)), 10))),
        "-rl", str(rate), "-duc", "-o", str(subs_raw),
    ]
    if configured() and SUBFINDER_PROVIDER_FILE.exists():
        sub_args.extend(["-pc", str(SUBFINDER_PROVIDER_FILE)])
    run_command(sub_args, log_file, timeout=900)
    subs = filter_domains(_read_lines(subs_raw) + [domain], roots)
    subs_file = scan_dir / "subdomains.txt"
    _write_lines(subs_file, subs)

    _notify(progress, "dns", f"Memeriksa {len(subs)} aset")
    resolved_raw = scan_dir / "resolved-raw.txt"
    run_command([
        "dnsx", "-l", str(subs_file), "-silent", "-retry", "2",
        "-rl", str(max(5, rate * 4)), "-duc", "-o", str(resolved_raw),
    ], log_file, timeout=600)
    resolved = filter_domains(_read_lines(resolved_raw) + [domain], roots)
    resolved_file = scan_dir / "resolved.txt"
    _write_lines(resolved_file, resolved)

    _notify(progress, "web", "Mengecek layanan web aktif")
    httpx_file = scan_dir / "httpx.jsonl"
    run_command([
        "httpx", "-l", str(resolved_file), "-silent", "-j", "-sc", "-title", "-td",
        "-server", "-ip", "-cname", "-fhr", "-rl", str(rate), "-t", "20",
        "-timeout", "10", "-retries", "1", "-duc", "-o", str(httpx_file),
    ], log_file, timeout=900)
    assets = parse_httpx(httpx_file, roots)
    live_urls = sorted({a["url"] for a in assets})
    live_file = scan_dir / "live-urls.txt"
    _write_lines(live_file, live_urls)

    crawled: list[str] = []
    if live_urls:
        _notify(progress, "crawl", f"Mempelajari endpoint dari {len(live_urls)} web")
        crawl_raw = scan_dir / "crawl-raw.txt"
        run_command([
            "katana", "-list", str(live_file), "-silent",
            "-d", str(max(1, min(int(cfg.get("crawl_depth", 2)), 3))),
            "-jc", "-kf", "robotstxt,sitemapxml", "-iqp",
            "-rl", str(rate), "-c", "5", "-p", "3", "-timeout", "10",
            "-duc", "-o", str(crawl_raw),
        ], log_file, timeout=1200)
        crawled = filter_urls(_read_lines(crawl_raw), roots)
    crawl_file = scan_dir / "crawl.txt"
    _write_lines(crawl_file, crawled)

    nuclei_targets = filter_urls(live_urls + crawled, roots)
    nuclei_targets = nuclei_targets[:2500]
    targets_file = scan_dir / "targets.txt"
    _write_lines(targets_file, nuclei_targets)

    nuclei_files: list[Path] = []
    if nuclei_targets:
        _notify(progress, "screen", f"Screening {len(nuclei_targets)} endpoint")
        main_out = scan_dir / "nuclei-main.jsonl"
        run_command([
            "nuclei", "-l", str(targets_file),
            "-s", "low,medium,high,critical",
            "-pt", "http,ssl",
            "-etags", "fuzz,dos,intrusive",
            "-rl", str(rate), "-c", str(max(2, min(int(cfg.get("nuclei_concurrency", 10)), 15))),
            "-bs", "10", "-timeout", "10", "-retries", "1",
            "-jle", str(main_out), "-or", "-ot", "-duc",
        ], log_file, timeout=3600)
        nuclei_files.append(main_out)

        _notify(progress, "exposure", "Mencari exposure dan konfigurasi sensitif")
        exposure_out = scan_dir / "nuclei-exposure.jsonl"
        run_command([
            "nuclei", "-l", str(targets_file),
            "-tags", "exposure,exposures,config",
            "-s", "info,low,medium,high,critical",
            "-pt", "http",
            "-etags", "fuzz,dos,intrusive",
            "-rl", str(rate), "-c", "8", "-bs", "8", "-timeout", "10", "-retries", "1",
            "-jle", str(exposure_out), "-or", "-ot", "-duc",
        ], log_file, timeout=2400)
        nuclei_files.append(exposure_out)

    _notify(progress, "report", "Mengurutkan kandidat temuan")
    findings = parse_nuclei(nuclei_files, roots)
    meta.update({
        "finished_utc": datetime.now(timezone.utc).isoformat(),
        "subdomains": len(subs),
        "resolved": len(resolved),
        "live_web": len(assets),
        "crawled_urls": len(crawled),
        "screened_urls": len(nuclei_targets),
        "findings": len(findings),
    })
    (scan_dir / "scan.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report_json, report_md = write_report(scan_dir, domain, assets, findings, meta)
    elapsed = time.monotonic() - started
    _notify(progress, "done", f"Selesai: {len(findings)} kandidat")
    return ScanResult(scan_id, domain, scan_dir, assets, findings, report_md, report_json, elapsed)
