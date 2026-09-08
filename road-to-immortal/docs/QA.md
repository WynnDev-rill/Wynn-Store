# Verifikasi Road to Immortal 1.0.1

Laporan versi terbaru: [Verifikasi 1.1.0](QA-1.1.0.md). Di bawah ini adalah arsip hasil 1.0.1.

Run yang diperiksa: [GitHub Actions 34064341346](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/34064341346), selesai sukses pada 6 September 2026, 22:40 UTC (7 September, 05:40 WIB).

## Identitas APK

- Source: `d7a7cd9f2ac5429b97d8e9d176dfefc3791b6982`.
- Paket: `id.wynn.roadtoimmortal`, versi `1.0.1`, versionCode `2`.
- Android minimum: API 26 / Android 8.0. Target API 36.
- APK optimized: 2.357.754 byte. Nama unduhan: `Road-to-Immortal-1.0.1.apk`.
- SHA-256: `54d48cb4090c1391b5b96156bc8635cc2f66f484465e2e82616ab0e9c4ec69d3`.
- Sertifikat SHA-256: `c73f2354fa53afac08783feec25d6a19da14703b59fbfb82c381177458bb681d`.
- `apksigner verify` berhasil, APK Signature Scheme v2 valid. Ini kunci development yang sama dengan build sebelumnya, bukan production signing.

Artifact **Road-to-Immortal** di run tersebut berisi `dist/Road-to-Immortal.apk`, APK build asli, laporan unit/lint, screenshot, hierarchy UI, logcat, informasi signature/paket, serta metrik emulator. Hash APK unduhan identik dengan APK build asli. Retensi artifact Actions 30 hari; APK juga diserahkan sebagai berkas unduhan dalam percakapan.

## Hasil pengujian

| Pemeriksaan | Hasil |
| --- | --- |
| Kontrak pipeline Python | 10 tes lolos |
| Unit Android: rank, draft, pencarian, repository/cache | 19 tes lolos, 0 gagal, 0 dilewati |
| Lint release | 0 error, 20 warning |
| Build optimized dan signature | Lolos |
| Instalasi dan startup APK melalui ADB | Lolos |
| UIAutomator pada APK optimized, host tes terpisah | 3 perjalanan lolos, 0 gagal; 132,504 detik |
| Pencarian crash/ANR aplikasi pada logcat run | Tidak ditemukan |

Warning lint mencakup versi dependency/target yang lebih baru, pembacaan ukuran jendela, urutan parameter Modifier, dan qualifier resource yang redundan. Warning tidak disembunyikan dan laporannya tersedia dalam artifact.

## Perjalanan yang diverifikasi

1. **Rank dan tampilan — lolos.** Mulai perjalanan, pilih Mythic, tambah lima bintang, periksa sisa 95 bintang, tutup picker, ubah dark mode, gunakan font 150%, rotasi, dan buka ulang proses. Nilai rank bertahan. Screenshot `01`, `02`, `03`, `11`, `12`, `13`.
2. **Katalog dan detail — lolos.** Jelajah hero, cari Miya pada landscape dengan header yang bisa digulir, buka build/skill/matchup, cari Antique Cuirass dan baca efeknya, lalu buka emblem dan spell. Screenshot `04a`, `04b`, `04`, `05`, `06`, `07`, `07a`, `07b`.
3. **Meta, draft dan jaringan — lolos.** Ganti cakupan rank/pengurutan, pilih Miya dan Eudora pada tim berbeda, pastikan hero yang sudah dipilih tidak muncul lagi dan draft bertahan saat pindah tab. Refresh katalog online berhasil. Setelah default network benar-benar putus, refresh menampilkan error tanpa menghapus katalog. Katalog tetap dapat dibuka sesudah proses dimulai ulang dalam kondisi offline; refresh berhasil lagi setelah jaringan aktif. Screenshot `08`, `09`, `10`, `14a`, `14`, `15`, `16`, `17`.

Screenshot utama ditinjau langsung: angka target, logo dan kontrol rank, artwork hero, detail item, meta, draft, tema terang/gelap, teks besar, hasil pencarian landscape, pesan offline dan pemulihan. Gambar mengikuti unduhan CDN; pada screenshot awal build ada satu indikator gambar yang masih dimuat. Ikon talent yang sama terlihat termuat pada halaman emblem berikutnya. Tes tidak menyatakan semua aset eksternal selalu tersedia.

## Perbaikan yang mengakhiri kegagalan sebelumnya

Run sebelumnya sudah berhasil mengompilasi APK; kegagalan terakhir terdapat pada tes offline. Perintah menonaktifkan radio selesai sebelum Android menyelesaikan pemutusan default network. Tes sekarang menunggu kondisi jaringan yang dimaksud dan merekam `dumpsys connectivity` sebagai bukti. Repository langsung melaporkan koneksi tidak tersedia ketika tidak ada default network yang mampu mengakses internet, mempertahankan data terakhir, dan tidak mengubah waktu pemeriksaan sukses. Unit test tambahan mencakup cache HTTP yang masih fresh, startup offline, dan recovery.

Perubahan UI yang tertunda ikut masuk: artwork memakai AsyncImage dengan fallback portrait ke icon hero, dan katalog pada viewport pendek menempatkan header di area gulir agar hasil pencarian tetap dapat dicapai.

## Data dan batas verifikasi

Pipeline data otomatis terakhir yang diperiksa [34058092601](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/34058092601) sukses. Snapshot bundel `2026-09-06T20:28:28Z` berisi 133 hero, 187 identitas item (termasuk variasi dan metadata), 33 emblem/talent, 12 spell, 399 kombinasi tiga item inti, dan enam cakupan rank. Refresh online nyata juga diuji dari APK. Statistik agregat tujuh hari, pembatasan build/item, dan sumber season komunitas dijelaskan dalam [SOURCES.md](SOURCES.md).

Emulator Android 15/API 35 menggunakan profil Pixel 6, x86_64 dan software GPU SwiftShader. Cold launch terukur sekitar 2,27 detik; PSS akhir sekitar 78 MiB. Angka frame/jank emulator ini tidak menjadi bukti FPS pada ponsel fisik. Poco F6, perangkat Android 8–16 lainnya, TalkBack lengkap, dan performa penggunaan jangka panjang belum diuji secara langsung. Aksesibilitas yang diperiksa terbatas pada hierarchy kontrol, ukuran teks besar dan rotasi; tidak ada klaim kepatuhan menyeluruh.
