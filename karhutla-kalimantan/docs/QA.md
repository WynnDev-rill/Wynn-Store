# Pemeriksaan rilis — 24 Oktober 2026

## Hasil

- Build Astro + pemeriksaan TypeScript: nol error, warning tipe, atau hint.
- Empat tes data: lulus. Seluruh 25 nilai, geometri tiap provinsi, presisi edisi sumber, CSV, dan sitasi diperiksa.
- Empat belas skenario browser: lulus. Termasuk keyboard/menu, 25 kombinasi peta, perbandingan citra, empat keadaan Three.js, filter, dialog/focus/Escape, video NASA benar-benar diputar, dan isi tanpa JavaScript.
- Audit axe WCAG 2 A/AA dan 2.1 AA: nol temuan otomatis pada halaman utama dan sumber.
- Lebar viewport: 320, 360, 390, 768, 1024, 1440 px; tidak ada overflow horizontal halaman. Tabel memiliki scroll lokal. Mobile mencakup emulasi touch dan reduced motion, bukan pengujian pada HP fisik.
- Screenshot desktop dan mobile ditinjau. Teks gambut yang semula redup diperbaiki; model dibuat sticky pada mobile; label peta mobile disederhanakan dan outline terpilih diperjelas; kontras label diperbaiki hingga lolos audit otomatis.
- Citra, foto, video, dan bibliografi dicek. Foto Sebangau dipastikan bukan bukti keberhasilan restorasi; citra Landsat ditandai warna semu. Tidak ada kesaksian rekaan atau statistik kematian tanpa dasar.

## Temuan rilis yang diperbaiki

Preview semula tidak menyertakan header produksi. Saat uji Cloud Browser, script peta kecil yang otomatis di-inline oleh Astro diblokir CSP. Solusi: `assetsInlineLimit: 0`, menjaga script eksternal tanpa melonggarkan CSP. Pengujian browser kini melayani header CSP yang sama; build juga memeriksa semua script HTML, tautan lokal, dan kandidat gambar responsif agar masalah ini tidak lolos lagi.

Dialog arsip kini menghentikan dan membersihkan video secara sinkron saat ditutup, termasuk Escape, serta mengembalikan fokus ke pemicu.

## Performa

Satu pengukuran lab Chromium 133, viewport 390×844, CPU 4× lebih lambat, jaringan 200.000 byte/detik dan latensi 150 ms: LCP 1,48 detik, CLS 0,0122. Server lab tidak mengompresi aset; hasil ini bukan angka lapangan Android atau skor Lighthouse.

Pengukuran menemukan browser memuat gambar perbandingan lebih awal meskipun `loading=lazy`. Kandidat gambar responsif mengurangi dua gambar tersebut dari sekitar 1,19 MB menjadi 215 KB pada viewport uji. Halaman siap muat pada pengukuran lab turun dari sekitar 8,70 menjadi 3,85 detik. Three.js dan video tidak dimuat pada layar pembuka. Video tetap 4,13 MB, hanya diminta saat pengguna membuka/memutarnya.

Bundle awal JavaScript sekitar 14,4 KB sebelum gzip; Three.js sekitar 568 KB / 142 KB gzip, dimuat terpisah ketika mendekati bab gambut. Renderer memakai DPR maksimal 1,5 dan hanya menggambar transisi/perubahan; fallback SVG dan narasi tetap ada bila WebGL gagal. Ada peringatan ukuran chunk Vite untuk bundle 3D yang sengaja dimuat lazy; ini bukan error build.

Data dan bukti mentah: BROWSER-QA.json, PERFORMANCE-QA.json, BUNDLE-SIZES.json. Beberapa screenshot ada di screenshots/. Jalankan `npm run test:browser` untuk membuat ulang seluruh screenshot dan pemeriksaan fungsional.
