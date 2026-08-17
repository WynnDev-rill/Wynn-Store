# Cyber Tool By Wynn

Tool pertama dalam proyek **Cyber Tool By Wynn**: automation bug-bounty untuk Termux tanpa root. Antarmuka dibuat sederhana; engine internal menjalankan passive recon, DNS validation, HTTP fingerprinting, crawling, screening vulnerability, deduplication, redaction, prioritization, checkpoint/resume, dan report secara otomatis.

> Gunakan hanya pada aset yang kamu miliki atau yang secara eksplisit termasuk scope program bug bounty/VDP. Tool tidak dirancang untuk memakai credential yang ditemukan, mengambil data pengguna, persistence, DoS, credential stuffing, token spraying, atau eksploitasi destruktif.

## Instalasi Termux

Cara paling sederhana, satu baris:

```bash
pkg install -y curl && curl -fsSL https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/cyber-tool-by-wynn/install.sh | bash
```

Installer mengambil source terbaru dan memasang dependency/engine yang diperlukan. Setelah selesai cukup jalankan:

```bash
cyber
```

Tidak perlu masuk ke folder repository lagi.

Alternatif instalasi manual:

```bash
pkg install -y git
git clone --depth 1 https://github.com/WynnDev-rill/Wynn-Store.git
bash Wynn-Store/cyber-tool-by-wynn/install.sh
```

## Update

```bash
cyber update
```

Untuk sekaligus membangun ulang engine security versi terbaru:

```bash
cyber update --engines
```

Jika engine hilang/rusak:

```bash
cyber repair
```

## Resume / checkpoint

Mulai v0.2, setiap tahap scan disimpan sebagai checkpoint. Jika Termux ditutup, proses dihentikan, koneksi putus, atau satu engine gagal, tahap yang sudah selesai tidak perlu dijalankan ulang.

Dari TUI pilih **Lanjutkan scan terhenti**, atau:

```bash
cyber resume --yes-i-am-authorized
cyber resume SCAN_ID --yes-i-am-authorized
```

Konfirmasi authorization diminta kembali saat resume karena scope program dapat berubah.

## API Sources

API key bersifat **opsional**. Tanpa key, sumber publik yang didukung engine tetap dipakai. Gunakan hanya key resmi/legal milikmu sendiri. Key paling mudah ditambahkan dari menu `API Sources` di TUI. CLI meminta nilainya secara tersembunyi agar tidak masuk shell history:

```bash
cyber api list
cyber api set virustotal
cyber api set github
cyber api set chaos
cyber api check
```

Contoh format gabungan:

- Censys: `API_ID:API_SECRET`
- FOFA: `EMAIL:API_KEY`
- Intelligence X: `HOST:API_KEY`
- ZoomEye: `HOST:API_KEY`

Key disimpan di `$HOME/.cyber-tool-by-wynn/api-keys.json` dengan permission lokal ketat dan tidak pernah ditulis ke repository. Cyber Tool membaca `subfinder -ls` dari engine yang benar-benar terpasang. Key provider yang tidak didukung versi Subfinder tersebut **tetap tersimpan lokal tetapi tidak diteruskan ke scanner**, sehingga konfigurasi lama tidak merusak scan.

Registry v0.2 mencakup GitHub, VirusTotal, SecurityTrails, ProjectDiscovery Chaos, Censys, Shodan, BinaryEdge, BeVigil, FullHunt, Cert Spotter, URLScan.io, Netlas, LeakIX, HackerTarget, WhoisXML API, Hunter, BuiltWith, C99, FOFA, Intelligence X, Quake, Robtex, ThreatBook, ZoomEye, DNSRepo, dan Chinaz. Ketersediaan aktual mengikuti output `subfinder -ls` pada versi engine yang terpasang.

## Cara kerja otomatis

1. Scope guard + konfirmasi authorization.
2. Passive asset discovery dengan Subfinder (+ API sources kompatibel jika tersedia).
3. DNS validation dengan dnsx.
4. HTTP probing/fingerprint dengan httpx, redirect dibatasi pada host yang sama.
5. Standard crawling dengan Katana dan scope `rdn`, termasuk JavaScript endpoint parsing, `robots.txt`, dan sitemap.
6. Nuclei screening dengan rate limit konservatif.
7. Fuzz/DoS/intrusive/credential-stuffing/token-spray dikecualikan; OAST/Interactsh dimatikan; unsigned template ditolak.
8. Pass tambahan untuk exposure/configuration findings.
9. Scope filter diterapkan lagi sebelum kandidat masuk report.
10. Secret/token pada evidence dan URL disamarkan sebelum report.
11. Report JSON + Markdown dan status checkpoint disimpan per scan.

Semua hasil scanner diberi status **candidate** sampai diverifikasi manual. Program bounty/VDP dan scope resminya selalu lebih berwenang daripada keputusan tool.

## Command cepat

```bash
cyber                         # buka TUI
cyber doctor                  # cek engine + kompatibilitas API
cyber history                 # hasil sebelumnya
cyber resume --yes-i-am-authorized
cyber api list                # API tersimpan
cyber api check               # cek cocok dengan Subfinder terpasang
cyber update                  # update app + template
cyber update --engines        # update app + semua engine
```

Mode CLI scan sengaja meminta konfirmasi eksplisit:

```bash
cyber scan example.com --yes-i-am-authorized
```
