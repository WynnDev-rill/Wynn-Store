#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$HOME/.cyber-tool-by-wynn"
REPO="$ROOT/repo"
REPO_URL="https://github.com/WynnDev-rill/Wynn-Store.git"
PREFIX_DIR="${PREFIX:-/data/data/com.termux/files/usr}"
LOG="$ROOT/install.log"

if [[ ! -d /data/data/com.termux/files/usr ]]; then
  echo "Jalankan installer ini dari Termux." >&2
  exit 1
fi

mkdir -p "$ROOT"
chmod 700 "$ROOT" 2>/dev/null || true
: > "$LOG"

PROGRESS=0
ui_title() {
  printf '\033[2J\033[H'
  printf '\033[1;36mCYBER\033[0m \033[1;35m/ WYNN\033[0m\n'
  printf '\033[2mTermux · no root\033[0m\n\n'
}
ui_progress() {
  local pct="$1" label="$2" glyph="${3:-›}" width=16 filled empty bar="" i
  (( pct < 0 )) && pct=0
  (( pct > 100 )) && pct=100
  filled=$(( pct * width / 100 ))
  empty=$(( width - filled ))
  for ((i=0; i<filled; i++)); do bar+="█"; done
  for ((i=0; i<empty; i++)); do bar+="░"; done
  printf '\r\033[K\033[35m%s\033[0m \033[2m[%s]\033[0m \033[1m%3d%%\033[0m %s' "$glyph" "$bar" "$pct" "$label"
}
mark() {
  PROGRESS="$1"
  ui_progress "$1" "$2" "✓"
  printf '\n'
}
run_quiet() {
  local label="$1" target="$2"
  shift 2
  local frames='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏' i=0 rc next="$PROGRESS"
  ui_progress "$next" "$label" "${frames:0:1}"
  "$@" >>"$LOG" 2>&1 &
  local pid=$!
  while kill -0 "$pid" >/dev/null 2>&1; do
    (( next < target - 1 )) && next=$((next + 1))
    i=$(((i + 1) % 10))
    ui_progress "$next" "$label" "${frames:$i:1}"
    sleep 0.18
  done
  set +e
  wait "$pid"
  rc=$?
  set -e
  if (( rc != 0 )); then
    printf '\r\033[K\033[31m×\033[0m %s\n' "$label" >&2
    tail -n 20 "$LOG" >&2 || true
    exit "$rc"
  fi
  mark "$target" "$label"
}

sync_source() {
  if [[ -d "$REPO/.git" ]]; then
    git -C "$REPO" fetch origin main --depth 1
    git -C "$REPO" reset --hard FETCH_HEAD
  else
    rm -rf "$REPO"
    git clone --depth 1 "$REPO_URL" "$REPO"
  fi
}

export GOBIN="$PREFIX_DIR/bin"
install_go_tool() {
  local bin="$1" pkg="$2" target="$3"
  if command -v "$bin" >/dev/null 2>&1; then
    mark "$target" "$bin"
  else
    run_quiet "$bin" "$target" go install -v "$pkg"
  fi
}

ui_title
run_quiet "Paket Termux" 8 pkg install -y git python python-pip golang curl
run_quiet "Source" 20 sync_source

APP="$REPO/cyber-tool-by-wynn"
[[ -f "$APP/VERSION" ]] || {
  echo "Cyber Tool tidak ditemukan di branch main." >&2
  exit 1
}
run_quiet "TUI" 28 python -m pip install -q -r "$APP/requirements.txt"

install_go_tool subfinder github.com/projectdiscovery/subfinder/v2/cmd/subfinder@latest 40
install_go_tool dnsx github.com/projectdiscovery/dnsx/cmd/dnsx@latest 52
install_go_tool httpx github.com/projectdiscovery/httpx/cmd/httpx@latest 64
install_go_tool katana github.com/projectdiscovery/katana/cmd/katana@latest 76
install_go_tool nuclei github.com/projectdiscovery/nuclei/v3/cmd/nuclei@latest 88
run_quiet "Templates" 95 nuclei -ut

cat > "$PREFIX_DIR/bin/cyber" <<WRAPPER
#!/data/data/com.termux/files/usr/bin/bash
set -e
ROOT="\${CYBER_HOME:-\$HOME/.cyber-tool-by-wynn}"
APP="\$ROOT/repo/cyber-tool-by-wynn"
if [[ ! -d "\$APP/cyber_tool" ]]; then
  echo "Cyber Tool tidak lengkap. Jalankan installer lagi." >&2
  exit 1
fi
export PYTHONPATH="\$APP\${PYTHONPATH:+:\$PYTHONPATH}"
exec python -m cyber_tool "\$@"
WRAPPER
chmod +x "$PREFIX_DIR/bin/cyber"
mkdir -p "$ROOT/data" "$ROOT/scans" "$ROOT/logs"
chmod 700 "$ROOT" "$ROOT/data" "$ROOT/scans" "$ROOT/logs" 2>/dev/null || true
mark 97 "Command cyber"
run_quiet "Ready" 100 cyber doctor

printf '\n\033[32m✓ Ready\033[0m  \033[1mcyber\033[0m\n'
