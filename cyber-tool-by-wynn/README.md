# Cyber Tool By Wynn

Tool pertama dalam proyek **Cyber Tool By Wynn**: automation bug-bounty untuk Termux tanpa root. Antarmuka dibuat sederhana; engine internal menjalankan passive recon, DNS validation, HTTP fingerprinting, crawling, screening vulnerability, deduplication, redaction, prioritization, dan report secara otomatis.

> Gunakan hanya pada aset yang kamu miliki atau yang secara eksplisit termasuk scope program bug bounty/VDP. Tool tidak dirancang untuk memakai credential yang ditemukan, mengambil data pengguna, persistence, DoS, atau eksploitasi destruktif.

## Instalasi Termux

Cara paling sederhana:

```bash
pkg install -y curl && curl -fsSL https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/cyber-tool-by-wynn/install.sh | bash
```

Installer akan mengambil source terbaru dan memasang dependency/engine yang diperlukan. Setelah selesai cukup jalankan:

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

## API Sources

API key bersifat **opsional**. Tanpa key, sumber publik yang didukung engine tetap dipakai. Key legal/free-tier milik pengguna paling mudah ditambahkan dari menu `API Sources` di TUI. CLI juga tersedia dan meminta key secara tersembunyi agar nilainya tidak masuk shell history:

```bash
cyber api list
cyber api set virustotal
cyber api set github
cyber api set urlscan
cyber api set netlas
```

Untuk Censys, masukkan nilai dengan format `API_ID:API_SECRET` ketika prompt muncul.

Key disimpan di `$HOME/.cyber-tool-by-wynn/api-keys.json` dengan permission lokal yang ketat dan tidak pernah ditulis ke repository. Konfigurasi provider Subfinder dibuat otomatis. Tool hanya menyimpan provider yang masih didukung registry Cyber Tool agar key provider lama/tidak dikenal tidak ikut masuk konfigurasi scanner.

Provider v0.1.1: GitHub, VirusTotal, SecurityTrails, ProjectDiscovery Chaos, URLScan.io, Netlas, LeakIX, HackerTarget, WhoisXML API, Censys, Shodan, BinaryEdge, BeVigil, FullHunt, dan Cert Spotter.

## Cara kerja otomatis

1. Scope guard + konfirmasi authorization.
2. Passive asset discovery dengan Subfinder (+ API sources jika tersedia).
3. DNS validation dengan dnsx.
4. HTTP probing/fingerprint dengan httpx.
5. Standard crawling dengan Katana, termasuk JavaScript endpoint parsing, `robots.txt`, dan sitemap.
6. Nuclei screening dengan rate limit konservatif; template fuzz/DoS/intrusive dikecualikan eksplisit.
7. Pass tambahan untuk exposure/configuration findings.
8. Scope filter kedua setelah discovery/crawl dan sebelum kandidat masuk report.
9. Secret/token pada evidence dan URL disamarkan sebelum report.
10. Report JSON + Markdown disimpan per scan.

Semua hasil scanner diberi status **candidate** sampai diverifikasi manual. Program bounty/VDP dan scope resminya selalu lebih berwenang daripada keputusan tool.

## Command cepat

```bash
cyber                         # buka TUI
cyber doctor                  # cek instalasi
cyber history                 # hasil sebelumnya
cyber api list                # status API
cyber update                  # update app + template
cyber update --engines        # update app + semua engine
```

Mode CLI scan sengaja meminta konfirmasi eksplisit:

```bash
cyber scan example.com --yes-i-am-authorized
```
