from pathlib import Path
import json

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
