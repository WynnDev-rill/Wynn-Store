# Cyber Tool By Wynn

Automation bug-bounty untuk Termux tanpa root: recon, validasi DNS/web, crawl, screening, prioritization, checkpoint/resume, dan report.

> Gunakan hanya pada aset milikmu atau yang secara eksplisit termasuk scope bug bounty/VDP. Tool tidak memakai credential yang ditemukan untuk login dan profile bawaan mengecualikan DoS, fuzz/intrusive, credential stuffing, token spraying, serta OAST.

## Instalasi Termux

```bash
pkg install -y curl && curl -fsSL https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/cyber-tool-by-wynn/install.sh | bash
```

Lalu:

```bash
cyber
```

Installer v0.3 memakai progress bar ringkas ala Furina Agent dan menyimpan detail proses ke log, jadi terminal tidak dipenuhi output dependency.

## TUI v0.3

TUI tetap memakai **Rich** seperti Furina Agent eksperimen, tetapi dibuat lebih minimal untuk scanner:

- home: satu status line + lima menu;
- progress scan: live, tidak menumpuk baris;
- report: ringkasan + kandidat prioritas saja;
- API: hanya source yang sudah dikonfigurasi ditampilkan;
- update: output dependency masuk log sehingga spinner tetap bersih.

## Update

```bash
cyber update
```

Update semua engine juga:

```bash
cyber update --engines
```

Jika engine hilang:

```bash
cyber repair
```

## Resume / checkpoint

Setiap tahap scan disimpan. Scan yang terputus dapat dilanjutkan tanpa mengulang tahap yang sudah selesai.

```bash
cyber resume --yes-i-am-authorized
cyber resume SCAN_ID --yes-i-am-authorized
```

TUI menyediakan menu `Resume`. Authorization diminta lagi karena scope program dapat berubah.

## API Sources

API key opsional. Tanpa key, source publik Subfinder tetap dipakai. Key disimpan lokal dengan permission ketat dan input CLI disembunyikan dari shell history.

```bash
cyber api list
cyber api set virustotal
cyber api set github
cyber api set chaos
cyber api check
```

Format gabungan yang didukung antara lain Censys `API_ID:API_SECRET`, FOFA `EMAIL:API_KEY`, Intelligence X `HOST:API_KEY`, dan ZoomEye `HOST:API_KEY`.

Cyber Tool membaca `subfinder -ls` dari engine yang benar-benar terpasang. Key source yang tidak didukung versi itu tetap tersimpan lokal tetapi tidak diteruskan ke scanner, sehingga konfigurasi lama tidak merusak scan.

Registry v0.3 mencakup GitHub, VirusTotal, SecurityTrails, ProjectDiscovery Chaos, Censys, Shodan, BinaryEdge, BeVigil, FullHunt, Cert Spotter, URLScan.io, Netlas, LeakIX, HackerTarget, WhoisXML API, Hunter, BuiltWith, C99, FOFA, Intelligence X, Quake, Robtex, ThreatBook, ZoomEye, DNSRepo, dan Chinaz. Ketersediaan aktual mengikuti `subfinder -ls`.

## Pipeline

1. Scope guard + authorization.
2. Passive discovery dengan Subfinder.
3. DNS validation dengan dnsx.
4. HTTP probing/fingerprint dengan httpx.
5. Crawl dengan Katana dalam root-domain scope.
6. Nuclei screening dengan rate limit konservatif.
7. Exposure/configuration screening.
8. Scope filter, secret redaction, ranking.
9. Report JSON + Markdown dan checkpoint.

Semua hasil otomatis berstatus **candidate** sampai diverifikasi manual. Aturan program bounty/VDP tetap menjadi otoritas.

## Command cepat

```bash
cyber
cyber doctor
cyber history
cyber resume --yes-i-am-authorized
cyber api check
cyber update
cyber update --engines
```

Mode CLI scan membutuhkan konfirmasi eksplisit:

```bash
cyber scan example.com --yes-i-am-authorized
```
