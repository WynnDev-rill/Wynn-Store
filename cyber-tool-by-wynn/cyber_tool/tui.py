from __future__ import annotations

import json

from . import VERSION
from .api_keys import PROVIDERS, configured, masked, provider_status, remove_key, set_key
from .config import SCANS_DIR, ensure_layout
from .runner import engine_status
from .scanner import list_resumable_scans, resume_scan, scan
from .updater import update_app


def _rich():
    try:
        from rich import box
        from rich.console import Console
        from rich.panel import Panel
        from rich.prompt import Confirm, Prompt
        from rich.table import Table
        from rich.text import Text
        return box, Console, Panel, Confirm, Prompt, Table, Text
    except ImportError as exc:
        raise SystemExit("UI membutuhkan Rich. Jalankan installer Cyber Tool lagi.") from exc


def _header(console):
    box, _, Panel, _, _, _, Text = _rich()
    title = Text("CYBER TOOL", style="bold bright_cyan")
    title.append("  BY WYNN", style="bold bright_magenta")
    subtitle = f"v{VERSION}  •  bounty automation  •  Termux  •  authorized scope only"
    console.print(Panel(Text.assemble(title, "\n", (subtitle, "dim")), border_style="bright_blue", box=box.ROUNDED, padding=(1, 2)))


def _dashboard(console):
    box, _, Panel, _, _, Table, _ = _rich()
    status = engine_status()
    ready = sum(status.values())
    api_count = len(configured())
    scans = list(SCANS_DIR.glob("*/scan.json")) if SCANS_DIR.exists() else []
    resumable = len(list_resumable_scans())
    table = Table.grid(expand=True, padding=(0, 2))
    table.add_column(ratio=1)
    table.add_column(ratio=1)
    engine_state = "[bold green]READY[/]" if ready == len(status) else f"[yellow]{ready}/{len(status)}[/]"
    table.add_row(
        f"[dim]ENGINE[/]\n{engine_state}\n[dim]recon • web • crawl • screening[/]",
        f"[dim]API SOURCES[/]\n[bold cyan]{api_count}[/] configured\n[dim]disaring sesuai Subfinder terpasang[/]",
    )
    table.add_row(
        f"[dim]HISTORY[/]\n[bold cyan]{len(scans)}[/] scan • [yellow]{resumable} resumable[/]",
        "[dim]POLICY[/]\n[bold green]SCOPE GUARD[/]\n[dim]secret disamarkan • no login abuse • no OAST[/]",
    )
    console.print(Panel(table, title="SYSTEM", border_style="blue", box=box.ROUNDED))


def _menu(console) -> str:
    box, _, Panel, _, Prompt, Table, _ = _rich()
    table = Table.grid(padding=(0, 2))
    table.add_column(style="bold bright_cyan", width=5)
    table.add_column()
    table.add_row("1", "Mulai Bounty Scan")
    table.add_row("2", "Lanjutkan scan terhenti")
    table.add_row("3", "Hasil sebelumnya")
    table.add_row("4", "API Sources")
    table.add_row("5", "Health / repair")
    table.add_row("6", "Update")
    table.add_row("r", "Refresh")
    table.add_row("q", "Keluar")
    console.print(Panel(table, title="ACTIONS", border_style="magenta", box=box.ROUNDED))
    return Prompt.ask("[bold]Pilih[/]", default="1").strip().lower()


def _labels() -> dict[str, str]:
    return {
        "recon": "Mencari aset publik",
        "dns": "Memastikan aset aktif",
        "web": "Mengenali layanan web",
        "crawl": "Mempelajari endpoint",
        "screen": "Screening kerentanan",
        "exposure": "Mencari exposure sensitif",
        "resume": "Melewati tahap selesai",
        "report": "Menyusun hasil",
        "done": "Selesai",
    }


def _progress(console):
    labels = _labels()

    def progress(stage: str, detail: str):
        console.print(f"[bright_magenta]›[/] [bold]{labels.get(stage, stage)}[/] [dim]{detail}[/]")
    return progress


def _show_result(console, result) -> None:
    box, _, Panel, _, _, _, _ = _rich()
    sev = {}
    for finding in result.findings:
        sev[finding.severity] = sev.get(finding.severity, 0) + 1
    summary = (
        f"[bold green]SCAN SELESAI[/]\n\nTarget: [cyan]{result.domain}[/]\n"
        f"Web aktif: [bold]{len(result.assets)}[/]\nKandidat: [bold]{len(result.findings)}[/]\n"
        f"Critical/High: [bold]{sev.get('critical', 0) + sev.get('high', 0)}[/]\n\n"
        f"Report: [dim]{result.report_md}[/]"
    )
    console.print(Panel(summary, title="RESULT", border_style="green", box=box.ROUNDED))
    for finding in result.findings[:10]:
        console.print(f"[{_severity_color(finding.severity)}]{finding.severity.upper():8}[/] {finding.title} [dim]{finding.target}[/]")


def _scope_confirmation(console) -> bool:
    box, _, Panel, Confirm, _, _, _ = _rich()
    console.print(Panel(
        "Cyber Tool hanya boleh digunakan pada aset yang memang termasuk scope program bounty/VDP atau yang kamu miliki.\n"
        "Automation tidak menggunakan credential yang ditemukan untuk login, tidak menjalankan DoS/fuzzing berisiko, "
        "dan OAST dinonaktifkan pada profile bawaan.",
        title="SCOPE", border_style="yellow", box=box.ROUNDED,
    ))
    return Confirm.ask("Saya memiliki izin untuk menguji target ini", default=False)


def _scan(console):
    box, _, Panel, _, Prompt, _, _ = _rich()
    target = Prompt.ask("[bold bright_cyan]Domain target[/]").strip()
    if not _scope_confirmation(console):
        console.print("[yellow]Scan dibatalkan.[/]")
        return
    try:
        result = scan(target, progress=_progress(console))
    except KeyboardInterrupt:
        console.print("\n[yellow]Scan dihentikan. Jalankan menu Resume; tahap yang selesai tidak diulang.[/]")
        return
    except Exception as exc:
        console.print(Panel(str(exc), title="SCAN GAGAL", border_style="red"))
        console.print("[dim]Scan dapat dicoba dilanjutkan dari menu Resume jika metadata sudah dibuat.[/]")
        return
    _show_result(console, result)


def _resume(console):
    box, _, Panel, _, Prompt, Table, _ = _rich()
    rows = list_resumable_scans()
    if not rows:
        console.print("[dim]Tidak ada scan terhenti/gagal yang bisa dilanjutkan.[/]")
        return
    table = Table(box=box.SIMPLE, expand=True)
    table.add_column("#", width=3, style="cyan")
    table.add_column("Target")
    table.add_column("Status")
    table.add_column("Tahap selesai", justify="right")
    table.add_column("Scan ID", overflow="fold")
    for idx, (meta, directory) in enumerate(rows, 1):
        scope = (meta.get("authorized_scope") or ["?"])[0]
        stages = meta.get("stages") or {}
        table.add_row(str(idx), str(scope), str(meta.get("status", "?")), str(sum(v == "complete" for v in stages.values())), directory.name)
    console.print(Panel(table, title="RESUME", border_style="yellow"))
    raw = Prompt.ask("Nomor scan", default="1").strip()
    try:
        _, directory = rows[int(raw) - 1]
    except (ValueError, IndexError):
        console.print("[red]Pilihan tidak valid.[/]")
        return
    if not _scope_confirmation(console):
        console.print("[yellow]Resume dibatalkan.[/]")
        return
    try:
        result = resume_scan(directory.name, progress=_progress(console))
    except KeyboardInterrupt:
        console.print("\n[yellow]Scan dihentikan lagi; checkpoint tetap tersimpan.[/]")
        return
    except Exception as exc:
        console.print(Panel(str(exc), title="RESUME GAGAL", border_style="red"))
        return
    _show_result(console, result)


def _severity_color(sev: str) -> str:
    return {"critical": "bold red", "high": "red", "medium": "yellow", "low": "cyan", "info": "dim"}.get(sev, "white")


def _history(console):
    box, _, Panel, _, _, Table, _ = _rich()
    rows = []
    for path in sorted(SCANS_DIR.glob("*/scan.json"), reverse=True)[:20] if SCANS_DIR.exists() else []:
        try:
            meta = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        rows.append((meta, path.parent))
    if not rows:
        console.print("[dim]Belum ada scan.[/]")
        return
    table = Table(box=box.SIMPLE, expand=True)
    table.add_column("Target", style="cyan")
    table.add_column("Status")
    table.add_column("Web", justify="right")
    table.add_column("Findings", justify="right")
    table.add_column("Report", overflow="fold")
    for meta, directory in rows:
        table.add_row(
            str((meta.get("authorized_scope") or ["?"])[0]),
            str(meta.get("status", "?")),
            str(meta.get("live_web", "-")),
            str(meta.get("findings", "-")),
            str(directory / "REPORT.md"),
        )
    console.print(Panel(table, title="HISTORY", border_style="blue"))


def _api(console):
    box, _, Panel, _, Prompt, Table, _ = _rich()
    while True:
        runtime = {row["key"]: row["supported"] for row in provider_status()}
        table = Table(box=box.SIMPLE, expand=True)
        table.add_column("#", width=3, style="cyan")
        table.add_column("Source")
        table.add_column("API")
        table.add_column("Engine", width=11)
        table.add_column("Format", style="dim")
        for i, p in enumerate(PROVIDERS, 1):
            supported = runtime.get(p.key)
            engine = "[dim]?[/]" if supported is None else ("[green]READY[/]" if supported else "[yellow]SKIP[/]")
            table.add_row(str(i), p.label, masked(p.key), engine, p.hint)
        console.print(Panel(table, title="API SOURCES", border_style="bright_blue"))
        console.print("[dim]a Tambah/update  d Hapus  c Cek kompatibilitas  b Kembali. Key tetap lokal.[/]")
        action = Prompt.ask("Pilih", default="b").strip().lower()
        if action in {"b", "q", "back"}:
            return
        if action == "c":
            active = [row for row in provider_status() if row["configured"]]
            if not active:
                console.print("[dim]Belum ada API key.[/]")
            for row in active:
                state = row["supported"]
                label = "UNKNOWN" if state is None else ("READY" if state else "SKIPPED")
                console.print(f"{row['label']}: {label}")
            continue
        if action not in {"a", "d"}:
            continue
        raw = Prompt.ask("Nomor source").strip()
        try:
            provider = PROVIDERS[int(raw) - 1]
        except (ValueError, IndexError):
            console.print("[red]Pilihan tidak valid.[/]")
            continue
        if action == "d":
            remove_key(provider.key)
            console.print(f"[green]✓[/] {provider.label} dihapus")
        else:
            value = Prompt.ask(f"{provider.label} ({provider.hint})", password=True).strip()
            try:
                set_key(provider.key, value)
                console.print(f"[green]✓[/] {provider.label} tersimpan")
            except ValueError as exc:
                console.print(f"[red]{exc}[/]")


def _health(console):
    box, _, Panel, _, _, Table, _ = _rich()
    table = Table(box=box.SIMPLE, expand=True)
    table.add_column("Engine")
    table.add_column("Status")
    status = engine_status()
    for name, ok in status.items():
        table.add_row(name, "[green]READY[/]" if ok else "[red]MISSING[/]")
    api_rows = [row for row in provider_status() if row["configured"]]
    api_ready = sum(row["supported"] is not False for row in api_rows)
    table.add_row("API sources", f"{api_ready}/{len(api_rows)} usable/unknown")
    console.print(Panel(table, title="HEALTH", border_style="blue"))
    if not all(status.values()):
        console.print("[yellow]Ada engine yang hilang. Jalankan `cyber repair` dari Termux.[/]")
    unsupported = [str(row["label"]) for row in api_rows if row["supported"] is False]
    if unsupported:
        console.print("[yellow]Key disimpan tetapi dilewati oleh Subfinder terpasang: " + ", ".join(unsupported) + "[/]")


def _update(console):
    _, _, _, Confirm, _, _, _ = _rich()
    engines = Confirm.ask("Sekalian perbarui engine security? (lebih lama)", default=False)
    with console.status("[bright_magenta]Memperbarui Cyber Tool…[/]", spinner="dots"):
        update_app(update_engines=engines)
    console.print("[green]✓ Cyber Tool sudah diperbarui.[/]")


def run_tui() -> int:
    _, Console, _, _, _, _, _ = _rich()
    ensure_layout()
    console = Console()
    while True:
        console.clear()
        _header(console)
        _dashboard(console)
        choice = _menu(console)
        if choice == "1":
            _scan(console)
        elif choice == "2":
            _resume(console)
        elif choice == "3":
            _history(console)
        elif choice == "4":
            _api(console)
        elif choice == "5":
            _health(console)
        elif choice == "6":
            _update(console)
        elif choice in {"q", "quit", "exit"}:
            return 0
        elif choice == "r":
            continue
        input("\nEnter untuk kembali…")
