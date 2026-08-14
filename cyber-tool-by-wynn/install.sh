#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$HOME/.cyber-tool-by-wynn"
REPO="$ROOT/repo"
REPO_URL="https://github.com/WynnDev-rill/Wynn-Store.git"
PREFIX_DIR="${PREFIX:-/data/data/com.termux/files/usr}"
LOG="$ROOT/install.log"

if [[ ! -d /data/data/com.termux/files/usr ]]; then
  echo "Installer ini harus dijalankan dari Termux." >&2
  exit 1
fi

mkdir -p "$ROOT"
chmod 700 "$ROOT" 2>/dev/null || true
: > "$LOG"

header() {
  printf '\033[2J\033[H'
  printf '\033[1;36mCYBER TOOL\033[0m  \033[1;35mBY WYNN\033[0m\n'
  printf '\033[2mBounty Automation · Termux · no root\033[0m\n\n'
}
step() { printf '\033[36m›\033[0m %s\n' "$1"; }
run() {
  local label="$1"; shift
  step "$label"
  if ! "$@" >>"$LOG" 2>&1; then
    printf '\033[31m× %s gagal\033[0m\n' "$label" >&2
    tail -n 35 "$LOG" >&2 || true
    exit 1
  fi
  printf '\033[32m✓\033[0m %s\n' "$label"
}

header
run "Menyiapkan paket dasar" pkg install -y git python python-pip golang curl

if [[ -d "$REPO/.git" ]]; then
  run "Memperbarui source Cyber Tool" git -C "$REPO" fetch origin main --depth 1
  run "Menyinkronkan versi terbaru" git -C "$REPO" reset --hard FETCH_HEAD
else
  rm -rf "$REPO"
  run "Mengambil source Cyber Tool" git clone --depth 1 "$REPO_URL" "$REPO"
fi

APP="$REPO/cyber-tool-by-wynn"
[[ -f "$APP/VERSION" ]] || { echo "Folder cyber-tool-by-wynn belum tersedia di branch main." >&2; exit 1; }
run "Memasang tampilan terminal" python -m pip install -q -r "$APP/requirements.txt"

export GOBIN="$PREFIX_DIR/bin"
install_go_tool() {
  local bin="$1" pkg="$2"
  if command -v "$bin" >/dev/null 2>&1; then
    printf '\033[32m✓\033[0m Engine %s sudah ada\n' "$bin"
    return
  fi
  run "Memasang engine $bin" go install -v "$pkg"
}
install_go_tool subfinder github.com/projectdiscovery/subfinder/v2/cmd/subfinder@latest
install_go_tool dnsx github.com/projectdiscovery/dnsx/cmd/dnsx@latest
install_go_tool httpx github.com/projectdiscovery/httpx/cmd/httpx@latest
install_go_tool katana github.com/projectdiscovery/katana/cmd/katana@latest
install_go_tool nuclei github.com/projectdiscovery/nuclei/v3/cmd/nuclei@latest
run "Memperbarui template screening" nuclei -ut

cat > "$PREFIX_DIR/bin/cyber" <<WRAPPER
#!/data/data/com.termux/files/usr/bin/bash
set -e
ROOT="\${CYBER_HOME:-\$HOME/.cyber-tool-by-wynn}"
APP="\$ROOT/repo/cyber-tool-by-wynn"
if [[ ! -d "\$APP/cyber_tool" ]]; then
  echo "Cyber Tool rusak/tidak lengkap. Jalankan installer lagi." >&2
  exit 1
fi
export PYTHONPATH="\$APP\${PYTHONPATH:+:\$PYTHONPATH}"
exec python -m cyber_tool "\$@"
WRAPPER
chmod +x "$PREFIX_DIR/bin/cyber"
mkdir -p "$ROOT/data" "$ROOT/scans" "$ROOT/logs"
chmod 700 "$ROOT" "$ROOT/data" "$ROOT/scans" "$ROOT/logs" 2>/dev/null || true
run "Memeriksa instalasi" cyber doctor

printf '\n\033[32m✓ Cyber Tool By Wynn siap.\033[0m\n'
printf '  Jalankan: \033[1mcyber\033[0m\n'
printf '  Update:   \033[1mcyber update\033[0m\n'
printf '  Repair:   \033[1mcyber repair\033[0m\n'
printf '\033[2m  Data lokal: %s\n  Log installer: %s\033[0m\n' "$ROOT" "$LOG"
