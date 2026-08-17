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


def _header(console) -> None:
    box, _, Panel, _, _, _, Text = _rich()
    title = Text("CYBER", style="bold bright_cyan")
    title.append(" / WYNN", style="bold bright_magenta")
    console.print(
        Panel(
            Text.assemble(title, ("   "), (f"v{VERSION} • Termux", "dim")),
            border_style="bright_blue",
            box=box.ROUNDED,
            padding=(0, 1),
        )
    )


def _status_line(console) -> None:
    status = engine_status()
    ready = sum(status.values())
    total = len(status)
    api_count = len(configured())
    scans = len(list(SCANS_DIR.glob("*/scan.json"))) if SCANS_DIR.exists() else 0
    resumable = len(list_resumable_scans())
    if ready == total:
        engine = "[green]● READY[/]"
    else:
        engine = f"[yellow]● {ready}/{total} ENGINE[/]"
    tail = f"API [bold]{api_count}[/]   SCAN [bold]{scans}[/]"
    if resumable:
        tail += f"   [yellow]RESUME {resumable}[/]"
    console.print(f"{engine}   {tail}")


def _menu(console) -> str:
    _, _, _, _, Prompt, Table, _ = _rich()
    table = Table.grid(padding=(0, 2))
    table.add_column(style="bold bright_cyan", width=3)
    table.add_column()
    table.add_row("1", "Scan")
    table.add_row("2", "Resume")
    table.add_row("3", "Reports")
    table.add_row("4", "API")
    table.add_row("5", "System")
    table.add_row("q", "Exit")
    console.print(table)
    return Prompt.ask("[bold]Pilih[/]", default="1").strip().lower()


_STAGE = {
    "recon": (10, "Recon"),
    "dns": (25, "DNS"),
    "web": (40, "Web"),
    "crawl": (55, "Crawl"),
    "screen": (78, "Screen"),
    "exposure": (92, "Exposure"),
    "report": (98, "Report"),
    "done": (100, "Done"),
}


def _live_scan(console, title: str, runner):
    from rich.live import Live

    box, _, Panel, _, _, _, _ = _rich()
    state = {"pct": 0, "label": "Start", "detail": ""}

    def render():
        pct = int(state["pct"])
        width = 18
        filled = round(width * pct / 100)
        bar = "█" * filled + "░" * (width - filled)
        detail = str(state["detail"]).strip()
        line = f"[bright_magenta]{bar}[/] [bold]{pct:3d}%[/]  {state['label']}"
        if detail:
            line += f"\n[dim]{detail}[/]"
        return Panel(line, title=title, border_style="blue", box=box.ROUNDED, padding=(0, 1))

    with Live(render(), console=console, refresh_per_second=8, transient=True) as live:
        def progress(stage: str, detail: str):
            if stage == "resume":
                state["label"] = "Resume"
                state["detail"] = detail
            elif stage in _STAGE:
                pct, label = _STAGE[stage]
                state["pct"] = max(int(state["pct"]), pct)
                state["label"] = label
                state["detail"] = detail
            live.update(render(), refresh=True)

        return runner(progress)


def _scope_confirmation(console) -> bool:
    _, _, _, Confirm, _, _, _ = _rich()
    console.print("[yellow]Hanya aset milikmu atau scope bounty/VDP.[/]")
    return Confirm.ask("Target ini berizin?", default=False)


def _scan(console) -> None:
    _, _, Panel, _, Prompt, _, _ = _rich()
    target = Prompt.ask("[bold bright_cyan]Target[/]").strip()
    if not target or not _scope_confirmation(console):
        return
    try:
        result = _live_scan(console, target, lambda cb: scan(target, progress=cb))
    except KeyboardInterrupt:
        console.print("[yellow]Dihentikan • checkpoint tersimpan.[/]")
        _pause(console)
        return
    except Exception as exc:
        console.print(Panel(str(exc), title="GAGAL", border_style="red", padding=(0, 1)))
        _pause(console)
        return
    _show_result(console, result)
    _pause(console)


def _show_result(console, result) -> None:
    box, _, Panel, _, _, Table, _ = _rich()
    high = sum(f.severity in {"critical", "high"} for f in result.findings)
    body = (
        f"[bold cyan]{result.domain}[/]  •  {len(result.assets)} web  •  "
        f"{len(result.findings)} kandidat  •  [bold]{high} high+[/]\n"
        f"[dim]{result.scan_id}/REPORT.md[/]"
    )
    console.print(Panel(body, title="RESULT", border_style="green", box=box.ROUNDED, padding=(0, 1)))
    if not result.findings:
        return
    table = Table(box=box.SIMPLE, expand=True, show_header=False, padding=(0, 1))
    table.add_column(width=8)
    table.add_column(overflow="fold")
    table.add_column(overflow="fold")
    for finding in result.findings[:6]:
        table.add_row(
            f"[{_severity_color(finding.severity)}]{finding.severity.upper()}[/]",
            finding.title,
            f"[dim]{finding.target}[/]",
        )
    console.print(table)


def _resume(console) -> None:
    box, _, Panel, _, Prompt, Table, _ = _rich()
    rows = list_resumable_scans(limit=8)
    if not rows:
        console.print("[dim]Tidak ada scan untuk dilanjutkan.[/]")
        _pause(console)
        return

    table = Table(box=box.SIMPLE, expand=True)
    table.add_column("#", width=3, style="cyan")
    table.add_column("Target")
    table.add_column("Status", width=11)
    table.add_column("Step", justify="right", width=5)
    for idx, (meta, _) in enumerate(rows, 1):
        scope = str((meta.get("authorized_scope") or ["?"])[0])
        stages = meta.get("stages") or {}
        table.add_row(str(idx), scope, str(meta.get("status", "?")), str(sum(v == "complete" for v in stages.values())))
    console.print(Panel(table, title="RESUME", border_style="yellow", box=box.ROUNDED, padding=(0, 1)))

    try:
        idx = int(Prompt.ask("Pilih", default="1")) - 1
        meta, directory = rows[idx]
    except (ValueError, IndexError):
        console.print("[red]Pilihan tidak valid.[/]")
        _pause(console)
        return

    if not _scope_confirmation(console):
        return

    target = str((meta.get("authorized_scope") or [directory.name])[0])
    try:
        result = _live_scan(console, target, lambda cb: resume_scan(directory.name, progress=cb))
    except KeyboardInterrupt:
        console.print("[yellow]Dihentikan • checkpoint tetap tersimpan.[/]")
        _pause(console)
        return
    except Exception as exc:
        console.print(Panel(str(exc), title="GAGAL", border_style="red", padding=(0, 1)))
        _pause(console)
        return
    _show_result(console, result)
    _pause(console)


def _severity_color(sev: str) -> str:
    return {
        "critical": "bold red",
        "high": "red",
        "medium": "yellow",
        "low": "cyan",
        "info": "dim",
    }.get(sev, "white")


def _history(console) -> None:
    box, _, Panel, _, _, Table, _ = _rich()
    rows = []
    if SCANS_DIR.exists():
        for path in sorted(SCANS_DIR.glob("*/scan.json"), reverse=True)[:10]:
            try:
                meta = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            rows.append(meta)
    if not rows:
        console.print("[dim]Belum ada report.[/]")
        _pause(console)
        return

    table = Table(box=box.SIMPLE, expand=True)
    table.add_column("Target", style="cyan")
    table.add_column("Status", width=11)
    table.add_column("Web", justify="right", width=5)
    table.add_column("Find", justify="right", width=5)
    for meta in rows:
        table.add_row(
            str((meta.get("authorized_scope") or ["?"])[0]),
            str(meta.get("status", "?")),
            str(meta.get("live_web", "-")),
            str(meta.get("findings", "-")),
        )
    console.print(Panel(table, title="REPORTS", border_style="blue", box=box.ROUNDED, padding=(0, 1)))
    _pause(console)


def _provider_picker(console, only_configured: bool = False):
    _, _, _, _, Prompt, Table, _ = _rich()
    active = set(configured())
    providers = [p for p in PROVIDERS if not only_configured or p.key in active]
    if not providers:
        console.print("[dim]Belum ada API tersimpan.[/]")
        return None

    grid = Table.grid(padding=(0, 2))
    grid.add_column()
    grid.add_column()
    cells = [f"[cyan]{i}[/] {p.label}" for i, p in enumerate(providers, 1)]
    if len(cells) % 2:
        cells.append("")
    for i in range(0, len(cells), 2):
        grid.add_row(cells[i], cells[i + 1])
    console.print(grid)
    try:
        return providers[int(Prompt.ask("Source")) - 1]
    except (ValueError, IndexError):
        console.print("[red]Pilihan tidak valid.[/]")
        return None


def _api(console) -> None:
    box, _, Panel, _, Prompt, Table, _ = _rich()
    while True:
        console.clear()
        _header(console)
        active = set(configured())
        statuses = {row["key"]: row["supported"] for row in provider_status()}

        if active:
            table = Table(box=box.SIMPLE, expand=True)
            table.add_column("Source", style="cyan")
            table.add_column("Key")
            table.add_column("Engine", justify="right")
            by_key = {p.key: p for p in PROVIDERS}
            for key in sorted(active):
                provider = by_key.get(key)
                if provider is None:
                    continue
                supported = statuses.get(key)
                engine = "[dim]?[/]" if supported is None else ("[green]READY[/]" if supported else "[yellow]SKIP[/]")
                table.add_row(provider.label, masked(key), engine)
            console.print(Panel(table, title=f"API • {len(active)}", border_style="blue", box=box.ROUNDED, padding=(0, 1)))
        else:
            console.print("[dim]Belum ada API key.[/]")

        console.print("[dim]1 Add/update   2 Remove   3 Check   b Back[/]")
        action = Prompt.ask("Pilih", default="b").strip().lower()
        if action in {"b", "back", "q"}:
            return
        if action == "1":
            provider = _provider_picker(console)
            if provider:
                value = Prompt.ask(f"{provider.label} • {provider.hint}", password=True).strip()
                if value:
                    try:
                        set_key(provider.key, value)
                        console.print("[green]✓ Tersimpan lokal.[/]")
                    except ValueError as exc:
                        console.print(f"[red]{exc}[/]")
                    _pause(console)
        elif action == "2":
            provider = _provider_picker(console, only_configured=True)
            if provider:
                remove_key(provider.key)
                console.print("[green]✓ Dihapus.[/]")
                _pause(console)
        elif action == "3":
            configured_rows = [row for row in provider_status() if row["configured"]]
            if not configured_rows:
                console.print("[dim]Belum ada API key.[/]")
            for row in configured_rows:
                supported = row["supported"]
                state = "?" if supported is None else ("READY" if supported else "SKIP")
                style = "green" if supported else ("yellow" if supported is False else "dim")
                console.print(f"[{style}]{state:5}[/] {row['label']}")
            _pause(console)


def _system(console) -> None:
    box, _, Panel, _, Prompt, Table, _ = _rich()
    while True:
        console.clear()
        _header(console)
        status = engine_status()
        table = Table(box=box.SIMPLE, expand=True, show_header=False)
        table.add_column()
        table.add_column(justify="right")
        for name, ok in status.items():
            table.add_row(name, "[green]READY[/]" if ok else "[red]MISSING[/]")
        table.add_row("API", str(len(configured())))
        console.print(Panel(table, title="SYSTEM", border_style="blue", box=box.ROUNDED, padding=(0, 1)))
        if not all(status.values()):
            console.print("[yellow]Engine hilang → cyber repair[/]")
        console.print("[dim]u Update   e Update + engines   b Back[/]")
        action = Prompt.ask("Pilih", default="b").strip().lower()
        if action in {"b", "back", "q"}:
            return
        if action in {"u", "e"}:
            try:
                with console.status("[bright_magenta]Updating…[/]", spinner="dots"):
                    update_app(update_engines=action == "e")
                console.print("[green]✓ Updated.[/]")
            except Exception as exc:
                console.print(Panel(str(exc), title="GAGAL", border_style="red", padding=(0, 1)))
            _pause(console)


def _pause(console) -> None:
    try:
        console.input("\n[dim]Enter[/]")
    except (EOFError, KeyboardInterrupt):
        pass


def run_tui() -> int:
    _, Console, _, _, _, _, _ = _rich()
    ensure_layout()
    console = Console()
    try:
        while True:
            console.clear()
            _header(console)
            _status_line(console)
            console.print()
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
                _system(console)
            elif choice in {"q", "quit", "exit"}:
                return 0
    except (EOFError, KeyboardInterrupt):
        console.print()
        return 0
