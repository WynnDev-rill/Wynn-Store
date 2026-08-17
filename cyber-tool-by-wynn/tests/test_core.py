from pathlib import Path
from types import SimpleNamespace
import json

import cyber_tool.api_keys as api_keys
import cyber_tool.scanner as scanner
import cyber_tool.updater as updater
from cyber_tool.api_keys import PROVIDERS
from cyber_tool.report import parse_nuclei, redact_text, redact_url
from cyber_tool.scope import filter_domains, filter_urls, normalize_domain


def test_scope():
    assert normalize_domain("https://Example.com/path") == "example.com"
    assert filter_domains(["a.example.com", "evil-example.com", "example.com"], ["example.com"]) == ["a.example.com", "example.com"]
    assert filter_urls(["https://a.example.com/x", "https://example.com/y", "https://evil.com"], ["example.com"]) == ["https://a.example.com/x", "https://example.com/y"]


def test_redaction():
    assert "hunter2" not in redact_text("password=hunter2")
    redacted = redact_url("https://example.com/reset?token=abc123&next=/home")
    assert "abc123" not in redacted
    assert "%3Credacted%3E" in redacted


def test_nuclei_parse(tmp_path: Path):
    p = tmp_path / "n.jsonl"
    row = {"template-id": "demo-exposure", "matched-at": "https://app.example.com/.env?token=supersecret", "info": {"name": "Demo Exposure", "severity": "high", "tags": ["exposure"]}, "extracted-results": ["password=secret123"]}
    p.write_text(json.dumps(row) + "\n", encoding="utf-8")
    findings = parse_nuclei([p], ["example.com"])
    assert len(findings) == 1
    assert findings[0].score >= 75
    assert "supersecret" not in findings[0].target
    assert "secret123" not in findings[0].evidence[0]


def test_nuclei_out_of_scope_is_dropped(tmp_path: Path):
    p = tmp_path / "outside.jsonl"
    row = {"template-id": "outside", "host": "evil.example.net:443", "matched-at": "evil.example.net:443", "info": {"name": "Outside", "severity": "high"}}
    p.write_text(json.dumps(row) + "\n", encoding="utf-8")
    assert parse_nuclei([p], ["example.com"]) == []


def test_api_registry_tracks_current_provider_family():
    keys = {provider.key for provider in PROVIDERS}
    assert "facebook" not in keys
    assert {"github", "virustotal", "urlscan", "netlas", "leakix", "hackertarget", "hunter", "builtwith", "fofa", "intelx"}.issubset(keys)


def test_runtime_provider_detection(monkeypatch):
    monkeypatch.setattr(api_keys.shutil, "which", lambda _: "/bin/subfinder")
    fake = SimpleNamespace(stdout="github\nvirustotal\nhunter\n", stderr="", returncode=0)
    monkeypatch.setattr(api_keys.subprocess, "run", lambda *a, **k: fake)
    assert api_keys.runtime_subfinder_sources() == {"github", "virustotal", "hunter"}


def test_provider_config_skips_unsupported_runtime_source(tmp_path: Path, monkeypatch):
    keys_file = tmp_path / "keys.json"
    provider_file = tmp_path / "provider.yaml"
    monkeypatch.setattr(api_keys, "KEYS_FILE", keys_file)
    monkeypatch.setattr(api_keys, "SUBFINDER_PROVIDER_FILE", provider_file)
    monkeypatch.setattr(api_keys, "ensure_layout", lambda: None)
    monkeypatch.setattr(api_keys, "_chmod_private", lambda _: None)
    monkeypatch.setattr(api_keys, "runtime_subfinder_sources", lambda: {"github"})
    api_keys.save_keys({"github": ["ghp_demo"], "hunter": ["hunter_demo"]})
    saved = json.loads(keys_file.read_text(encoding="utf-8"))
    generated = provider_file.read_text(encoding="utf-8")
    assert set(saved) == {"github", "hunter"}
    assert "github:" in generated
    assert "hunter:" not in generated


def test_resumable_scan_listing(tmp_path: Path, monkeypatch):
    monkeypatch.setattr(scanner, "SCANS_DIR", tmp_path)
    monkeypatch.setattr(scanner, "ensure_layout", lambda: None)
    for name, status in [("a", "complete"), ("b", "interrupted"), ("c", "failed")]:
        d = tmp_path / name
        d.mkdir()
        (d / "scan.json").write_text(json.dumps({"scan_id": name, "status": status, "authorized_scope": ["example.com"]}), encoding="utf-8")
    rows = scanner.list_resumable_scans()
    assert [directory.name for _, directory in rows] == ["c", "b"]


def test_scan_preflight_does_not_create_ghost_scan(tmp_path: Path, monkeypatch):
    monkeypatch.setattr(scanner, "SCANS_DIR", tmp_path)
    monkeypatch.setattr(scanner, "ensure_layout", lambda: None)

    def fail_preflight():
        raise RuntimeError("missing engine")

    monkeypatch.setattr(scanner, "require_engines", fail_preflight)
    try:
        scanner.scan("example.com")
    except RuntimeError:
        pass
    else:
        raise AssertionError("scan should fail preflight")
    assert list(tmp_path.iterdir()) == []


def test_resume_preflight_keeps_previous_state(tmp_path: Path, monkeypatch):
    monkeypatch.setattr(scanner, "SCANS_DIR", tmp_path)
    monkeypatch.setattr(scanner, "ensure_layout", lambda: None)
    d = tmp_path / "demo"
    d.mkdir()
    meta_path = d / "scan.json"
    meta_path.write_text(
        json.dumps({"scan_id": "demo", "status": "failed", "authorized_scope": ["example.com"], "stages": {"recon": "complete"}}),
        encoding="utf-8",
    )

    def fail_preflight():
        raise RuntimeError("missing engine")

    monkeypatch.setattr(scanner, "require_engines", fail_preflight)
    try:
        scanner.resume_scan("demo")
    except RuntimeError:
        pass
    else:
        raise AssertionError("resume should fail preflight")

    meta = json.loads(meta_path.read_text(encoding="utf-8"))
    assert meta["status"] == "failed"
    assert "resumed_utc" not in meta


def test_updater_writes_subprocess_output_to_log(tmp_path: Path, monkeypatch):
    log_file = tmp_path / "update.log"

    def fake_run(args, **kwargs):
        kwargs["stdout"].write("hidden dependency output\n")
        return SimpleNamespace(returncode=0)

    monkeypatch.setattr(updater.subprocess, "run", fake_run)
    updater._run(["demo"], log_file=log_file)
    assert "hidden dependency output" in log_file.read_text(encoding="utf-8")
