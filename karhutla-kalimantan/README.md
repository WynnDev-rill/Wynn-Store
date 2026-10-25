# Kalimantan: Di balik asap

Dokumenter interaktif berbahasa Indonesia tentang karhutla Kalimantan. Proyek terisolasi di `karhutla-kalimantan/`; tidak mengubah aplikasi lain di monorepo.

## Jalankan

Node 24 LTS, npm. Dari direktori ini:

```sh
npm ci
npm run dev
npm test
npm run build
npx playwright install chromium
npm run test:browser
npm run test:experience
```

Hasil static build: `dist/`. Tidak memerlukan backend, API key, atau paket berbayar.

## Cloudflare Pages

- Project: `karhutla-kalimantan`
- Repository: `WynnDev-rill/Wynn-Store`
- Production branch: `main`
- Root: `karhutla-kalimantan`
- Build: `npm test && npm run build`
- Output (relatif root): `dist`
- Node: `24` (`.node-version`)
- Build watch paths: `karhutla-kalimantan/*`

Git integration mengerjakan build/deploy otomatis. Proyek Pages baru terpisah; konfigurasi Pages proyek Wynn-Store yang sudah ada tidak diubah.

## Struktur & keputusan

Astro static HTML + TypeScript; CSS orisinal; SVG choropleth geometri nyata; satu Three.js cutaway yang dimuat lazy dan dirender hanya ketika perlu. Tidak ada scroll hijacking, preloader wajib, audio/video autoplay, akun, cookies atau analitik pengunjung. Aset dan font dilayani lokal.

`src/data/burned-area.json` adalah transkripsi satu edisi KLHK. `public/data/` menyertakan CSV dan geometri untuk pemeriksaan. Jangan campur angka dari edisi lain tanpa memperbarui metodologi serta tes.

- [Riset lingkungan](docs/RESEARCH.md)
- [Referensi desain (10 utama)](docs/DESIGN-REFERENCES.md)
- [Tinjauan musim & kartu optik terbaru](docs/SEASON-DESIGN-REVIEW.md)
- [Revisi pengalaman visual & profil Wynn](docs/EXPERIENCE-REDESIGN.md)
- [Audit terbaru & screenshot](docs/QA.md)
- [Art direction awal](docs/DESIGN.md)
- [Audit isi](docs/CONTENT-QA.md)
- [Batas wilayah & sumber lisensi](docs/boundary-metadata.json)

Kredit media dan metode tersedia bagi pembaca di `/sumber/`. Jejak Januari–Oktober 2026; pembaruan rilis sampai 25 Oktober dan cuplikan sampai 24 Oktober 21.00 WIB, bukan feed langsung. Data luas historis 2019–2023 tersedia sebagai konteks terpisah. Citra dokumenter bukan generatif. Avatar profil dipilih dan disediakan Wynn. Penampang gambut adalah ilustrasi edukatif tidak berskala, bukan alat prediksi.

## Hak pihak ketiga

Lisensi kode repositori tidak menghapus hak media. Citra NASA/USGS dan NASA SVS: pemakaian informasi/edukasi sesuai pedoman NASA, dengan kredit, tanpa endorsement. Foto Sungai Sebangau Mankdhay rahman: CC BY-SA 4.0 (diperkecil/dikompresi, tampilan dapat dipotong; adaptasi foto dibagikan di bawah lisensi sama). geoBoundaries CC BY 4.0; sumber geometri OpenStreetMap/Wambacher ODbL 1.0; geometri turunan dibagikan di bawah ODbL. Font Manrope dan Newsreader SIL OFL; teks lisensi di `docs/licenses/`.

`test:browser` menguji layout 320–1440px, keyboard/menu, 25 kombinasi data, grafik kronologi, profil/avatar, motion teks/kontur, WebGL, reduced motion, fallback tanpa JavaScript dan audit axe. Hasil dan screenshot di `test-results/` (tidak dikomit). Untuk memakai Chromium sistem: `BROWSER_EXECUTABLE_PATH=/path/to/chromium npm run test:browser`. Tes mobile adalah emulasi viewport/touch, bukan pengukuran di HP fisik.

`test:experience` memeriksa perubahan transform kontur saat gulir, fallback ketika WebGL sengaja dimatikan, serta reduced motion. Galeri arsip/pemutar telah diganti profil Wynn; bibliografi dan kredit media yang masih digunakan tetap tersedia. Runtime produksi tetap gratis dan mandiri; studi Spline tidak menjadi dependensi frontend.

## Edisi ruang interaktif

Bab Ruang menambahkan hutan Three.js dengan tiga keadaan, drag/panah/orientasi otomatis ketika tersedia, serta audio sintetis opsional. Wynn tampil berdampingan dengan kartu optik tanpa tombol sensor. Detail implementasi dan perbandingan premium: [docs/IMMERSIVE-REVIEW.md](docs/IMMERSIVE-REVIEW.md).
