# Road to Immortal

Companion MLBB native berbahasa Indonesia. Proyek ini sepenuhnya terpisah dari Wynn Store dan cyber-tool; hanya workflow Android/data tambahan berada di `.github/workflows/`.

## Menggunakan aplikasi

- **Perjalanan**: pilih logo rank, divisi, dan bintang. Target net harian langsung berubah menuju Mythical Immortal 100★ berdasarkan jadwal season online. Tidak ada riwayat pertandingan atau progress tracking buatan.
- **Jelajah**: hero dengan artwork, skill, cerita, matchup, sinergi, serta build item inti, spell, emblem dan talent. Item dapat dicari, difilter, dan diperiksa atribut serta komponennya. Bookmark hero favorit.
- **Meta**: statistik win/pick/ban pada enam cakupan rank, filter hero berdasarkan lane/role, pencarian, dan pengurutan.
- **Tier List**: SS/S/A/B/C per rank dan lane, dihitung dari win/pick/ban sumber online. Aksi tambah langsung menyimpan hero ke lane tersebut.
- **Rancangan Hero**: hingga 10 hero per lane, tampil di Perjalanan. Tambah, hapus, naik/turun urutan, dan pindah lane; pilihan tersimpan lintas restart/update tanpa mengubah rank atau favorit.
- **Tim**: kombinasi Duo, Trio dan Squad dengan hero awal dan lawan opsional. Skor rekomendasi memakai meta, sinergi yang tersedia, lane dan komposisi; bukan win rate combo. Counter item tersedia di Jelajah.
- **Pengaturan → Data & sumber**: revisi sumber, waktu pemeriksaan, jadwal reset, patch komunitas, dan pembaruan manual. Tema mengikuti sistem atau dipilih terang/gelap.

## Build

Android 8.0+ (API 26); compile/target SDK 36. JDK 17, Gradle 8.13, Android SDK 36, Build Tools 35.0.0.

```bash
cd road-to-immortal
./gradlew testDebugUnitTest lintRelease assembleRelease
```

Snapshot di `data/catalog.json` dibundel otomatis oleh Gradle. Pembaruan katalog berikutnya diambil melalui HTTPS, tanpa update APK. Tidak memerlukan API key atau server berbayar.

APK optimized berada di `app/build/outputs/apk/release/app-release.apk`. Nama aplikasi dan application ID tetap `Road to Immortal` / `id.wynn.roadtoimmortal`.

Versi **1.0.1** telah lolos build, verifikasi signature dan tes emulator: [hasil QA](docs/QA.md). Artifact pada [run terverifikasi](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/34064341346) memuat APK siap pasang di `dist/Road-to-Immortal.apk`.

**Signing:** build ini menggunakan kunci development yang sengaja tersedia di `signing/development.jks`, alias `road-to-immortal`, password `android`. Ini memungkinkan APK development yang konsisten dan bisa diinstal/diupdate. Kunci publik ini **bukan production signing**, tidak memberikan keaslian distribusi yang aman. Untuk distribusi publik resmi, gunakan kunci privat milik penerbit dan ubah signing config; pengguna APK lama mungkin perlu memasang ulang karena sertifikat berubah.

## Sumber dan batas data

Lihat [SOURCES.md](docs/SOURCES.md) untuk kontrak, provenance dan asumsi. Aplikasi tidak menyebut statistik sebagai real-time dan tidak membuat peluang menang tim. Snapshot pertama diambil dari sumber nyata: 133 hero, 187 identitas item termasuk variasi jungle/roam, 33 emblem/talent, 12 spell, 6 cakupan rank, dan 399 kombinasi item inti. Jumlah akan berubah bersama sumber online.

Sumber build memberikan **tiga item inti**, bukan enam item atau urutan pembelian. Statistik build saat ini mencakup Mythic pada lane yang disebutkan. Rekomendasi item situasional adalah aturan efek item, bukan statistik counter item. Efek item terbaru dari komunitas dapat memakai bahasa Inggris; antarmuka dan metadata hero tersedia dalam bahasa Indonesia.

Jadwal season memakai sumber komunitas yang menyatakan memeriksa countdown game. Jadwal dapat berubah. Aplikasi menghentikan target yang memerlukan tanggal ketika jadwal tidak tersedia atau sudah lewat; tidak menebak season berikutnya.

## Pembaruan otomatis

`Road to Immortal data` berjalan setiap enam jam pada default branch, dan dapat dijalankan manual. Respons di-cache; normalisasi/validasi dilakukan sebelum snapshot dipublikasikan. Data item/build dibatasi sekitar sekali sehari. GitHub dapat menunda jadwal; repository publik yang lama tidak aktif juga dapat mengalami penonaktifan scheduled workflow. Periksa workflow dan `data/health.json` bila timestamp tidak bergerak.

```bash
python3 -m unittest discover -s pipeline -p 'test_*.py'
python3 pipeline/sync.py
# Untuk memeriksa respons yang sudah tersimpan tanpa jaringan:
python3 pipeline/sync.py --offline
```

Jangan mengubah timestamp lama menjadi waktu sekarang saat mempertahankan fallback. Jangan memasukkan placeholder atau data hasil generasi sebagai fakta game.

## Verifikasi

Workflow Android menjalankan unit tests, lint, build APK optimized dan tes UIAutomator pada emulator Android 15. Artifact workflow menyimpan APK, laporan pengujian, screenshot, hierarchy aksesibilitas, logcat, memory dan frame metrics. Status run adalah bukti aktual; daftar kemampuan pengujian ini tidak berarti setiap run sudah lolos. Hasil yang ditinjau dicatat dalam laporan QA setelah pengujian selesai.

Run 1.0.1: 10 tes pipeline, 19 unit test Android dan 3 perjalanan emulator lolos. Lint selesai dengan 0 error dan 20 warning. Hasil, identitas APK, screenshot yang diperiksa dan batas pengujian tersedia dalam [QA.md](docs/QA.md).

## Privasi dan hak

Tanpa akun, iklan, atau analitik. Rank/favorit tersimpan lewat DataStore di perangkat. Katalog dan gambar memerlukan permintaan ke GitHub/CDN serta penyedia gambar; alamat IP dan metadata jaringan mengikuti kebijakan penyedia. Pencadangan Android mengikuti pengaturan perangkat. Cache gambar hanya membantu gambar yang sudah diunduh.

Aplikasi komunitas independen; tidak berafiliasi dengan atau didukung Moonton. Artwork, nama, dan materi MLBB tetap milik pemegang haknya. Logo Road to Immortal adalah identitas original, tersedia sebagai vector Android dan SVG. Lisensi source aplikasi tidak melisensikan ulang materi game; lihat [NOTICE.md](NOTICE.md).
