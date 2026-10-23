# ScreenGuard Pro

Versi produksi terbaru: [unduh APK 1.1.0](distribution/ScreenGuard-Pro-1.1.0.apk). Pulihkan aturan di APK preview lama sebelum beralih; lihat [panduan migrasi dan update](docs/UPDATES.md).

Aplikasi Android native Kotlin + Jetpack Compose untuk waktu layar, kontrol internet tanpa VPN, penangguhan aplikasi, jadwal, dan PIN orang tua. Proyek berdiri sendiri; situs Wynn Store, Road to Immortal, dan Lunabloom tidak diubah.

## Instalasi dan penggunaan

1. Buka workflow **ScreenGuard Pro Android** pada tab Actions repository, pilih run terbaru yang berhasil, lalu unduh artifact **ScreenGuard-Pro-test**. Ekstrak `ScreenGuard-Pro-test.apk` dan instal. APK tes dioptimalkan dengan R8 dan memakai ID `id.wynn.screenguard.preview`, terpisah dari paket produksi. Kunci tes dapat berubah antar-run; pulihkan aturan sebelum mengganti instalasi bila Android menolak update karena sertifikat berbeda.
2. Android minimum: **11 / API 30**. Izinkan akses penggunaan. Instal [Shizuku](https://shizuku.rikka.app/download/), pairing melalui Debugging Nirkabel, mulai Shizuku, lalu berikan izin ScreenGuard.
3. Pada HyperOS, izinkan autostart dan pilih baterai tanpa batasan untuk ScreenGuard dan Shizuku. UI menyediakan pintasan dan panduan; menu dapat berbeda pada tiap versi HyperOS.
4. Tambahkan batas harian/jadwal/aplikasi fokus, lalu aktifkan **Pemantauan otomatis**. Notifikasi menunjukkan ketika Shizuku atau izin tidak siap. Membuat aturan saja tidak mengaktifkan pemantauan diam-diam.
5. Untuk membuka aplikasi yang dikunci, gunakan **Bekukan Aplikasi → Dibekukan → Buka**. Tantangan matematika atau waktu tunggu melepas pembekuan manual dan memberikan jeda 5 menit untuk batas/jadwal. PIN tetap diperlukan jika diaktifkan.
6. Sebelum uninstall, gunakan **Pengaturan → Pulihkan semua aplikasi** dan periksa hasilnya.

## Fitur dan batas teknis

| Fitur | Implementasi dan batas |
| --- | --- |
| Dashboard, penggunaan, internet, pembekuan, tantangan, otomatisasi, pengaturan | Tujuh halaman Compose asli; tidak ada WebView, gambar halaman statis, atau data contoh dalam aplikasi. |
| Waktu layar | `UsageStatsManager.queryEvents` untuk hari ini/dua hari terakhir; sesi dipotong pada batas tanggal/zona waktu, penguncian layar, dan restart. Waktu per aplikasi adalah gabungan aktivitas paket, sehingga split-screen dapat membuat jumlah durasi aplikasi melebihi waktu fisik layar. Launcher/SystemUI/ScreenGuard tidak dijumlahkan. |
| Riwayat 7/30 hari | Snapshot lokal harian; data lama yang belum sempat disimpan memakai agregat `queryUsageStats` Android dan ditandai perkiraan. Android dapat sudah menghapus sebagian riwayat. Tidak ada angka pengisi. Snapshot dipertahankan 90 hari. |
| Firewall tanpa VPN/root | Shizuku UserService memanggil ConnectivityService `setUidFirewallRule`, OEM_DENY_3 (9), dan mengaktifkan chain tersebut. Dukungan diperiksa saat tersambung, dan kegagalan sistem dilaporkan. Beberapa ROM/modul Connectivity tidak menyediakan API ini meski versi Android memenuhi minimum. |
| Wi-Fi dan seluler | Satu sakelar memblokir seluruh jaringan paket. API ini tidak menawarkan pemisahan Wi-Fi/seluler yang setara pada semua perangkat. UI sengaja menjelaskan satu kontrol tersebut. VPN milik aplikasi lain, profil kerja/kloning, shared UID, dan penerusan jaringan dapat memengaruhi hasil; shared UID ditolak. Kontrol Shizuku hanya mendukung profil utama/user 0; profil kerja dan aplikasi kloning tidak didukung. |
| Pembekuan | `cmd package suspend --user …`; ikon tetap ada dan Android menolak peluncuran. Penonaktifan memakai `disable-user` dan biasanya menghilangkan ikon. Tidak menghapus data aplikasi. Hanya operasi milik ScreenGuard yang dipulihkan. Aplikasi yang sudah dikunci alat lain tidak diambil alih. |
| Tantangan membuka | Dari ScreenGuard, bukan intersepsi universal setiap ketukan ikon peluncur. Android menampilkan dialog sistem untuk aplikasi ditangguhkan; aplikasi nonaktif tidak mempunyai ikon. Pengguna memilih matematika atau 30/60/120 detik. Pembekuan manual dilepas; batas/jadwal mendapat jeda 5 menit sebelum berlaku lagi. |
| Batas/jadwal/fokus | Foreground service pilihan pengguna; memeriksa setiap ~15 detik saat layar hidup, ~60 detik saat mati tanpa wakelock. Jadwal mendukung lintas tengah malam dan hari mulai. WorkManager ~15 menit menjadi pemulihan tambahan. Doze/force-stop/optimisasi vendor dapat menunda; ini bukan alarm tepat waktu. |
| Restart | Aturan dan kepemilikan kunci disimpan atomik. Receiver/WorkManager memulihkan saat memungkinkan. Pada perangkat tanpa root, Shizuku harus dimulai lagi setelah boot. Aturan belum diklaim aktif ketika koneksi tidak siap. |
| PIN | 6 digit, salt acak, PBKDF2-HMAC-SHA256 120.000 iterasi, pembandingan waktu konstan, lockout setelah 5 kegagalan, sesi izin 2 menit di memori dan dicabut saat app ke latar. PIN melindungi perubahan/pembukaan dalam ScreenGuard; bukan pengganti Device Owner/MDM. Pemilik perangkat bisa mencabut izin, force-stop, menghapus data/uninstall, atau memakai ADB. |
| Privasi | Tidak ada server pribadi, akun, SDK analitik, iklan atau API berbayar. Izin `INTERNET` hanya untuk update resmi GitHub, tanpa unggahan data penggunaan. Backup Android dimatikan. Aturan dan PIN hash di sandbox internal; statistik harian di SQLite lokal. |

Jangan gunakan bersama firewall lain yang menulis OEM_DENY_3 pada UID yang sama. ScreenGuard tidak mematikan atau mengosongkan chain global karena itu dapat membuka blokir milik alat lain. Pemulihan internet hanya membersihkan UID aplikasi yang dikelolanya.

## Build lokal

JDK **17**, Android SDK **36**, Build Tools **36.0.0**, koneksi ke Google Maven/Maven Central, dan Gradle wrapper **8.13**.

```bash
cd screenguard-pro
./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest
```

`assembleDebug` menghasilkan APK debug untuk pengujian instrumentasi. `assemblePreview` menghasilkan APK tes kecil yang dioptimalkan dengan signing debug (bukan kunci produksi). `assembleRelease` tanpa konfigurasi signing menghasilkan APK **unsigned**, bukan APK rilis siap instal. Wrapper memverifikasi checksum distribusi Gradle. Versi dependensi dipatok di Gradle.

## Release signing aman

Workflow menyediakan job signing khusus `workflow_dispatch` dari `main`, setelah pengujian berhasil, pada environment `screenguard-release`. Tetapkan empat **GitHub environment secrets**:

- `SG_KEYSTORE_BASE64`: isi keystore rilis yang di-base64.
- `SG_STORE_PASSWORD`
- `SG_KEY_ALIAS`
- `SG_KEY_PASSWORD`

Buat kunci privat sendiri di perangkat tepercaya; simpan cadangan terenkripsi di luar repository. Jangan memakai kunci debug sebagai kunci rilis. Jangan mengganti kunci setelah distribusi jika ingin update tanpa uninstall. Keystore hanya didekode ke `RUNNER_TEMP` dengan izin 0600 dan dibuang pada langkah `always()`. Workflow fork/PR tidak menerima kunci. Jika secrets belum tersedia, job melewati signing dengan pesan yang jelas; APK tes tetap dihasilkan.

Untuk signing lokal gunakan environment `SG_KEYSTORE_PATH`, `SG_STORE_PASSWORD`, `SG_KEY_ALIAS`, `SG_KEY_PASSWORD`. `.jks`, `.keystore`, `local.properties`, dan build output diabaikan Git. Kunci privat tidak disediakan atau dimasukkan dalam proyek.

## Struktur

- `domain/`: aturan murni, jadwal, penghitungan sesi, PIN, validasi paket.
- `data/`: AtomicFile, SQLite snapshot, katalog aplikasi, rekonsiliasi aturan berseri.
- `privilege/`: kontrak AIDL bertipe dan Shizuku UserService; tidak ada endpoint shell bebas.
- `service/`: foreground service, pemulihan boot, WorkManager.
- `ui/`: tujuh halaman, onboarding perizinan, komponen grafik, dialog PIN dan aturan.
- `app/src/test`: kasus batas jadwal/usage/security/storage.
- `app/src/androidTest`: tes interaksi Compose dan screenshot tujuh halaman. Fixture hanya berada dalam source set tes.

Lihat [panduan pengujian perangkat](docs/DEVICE-TESTING.md), [pemulihan darurat](docs/RECOVERY.md), [acuan desain](docs/DESIGN.md), dan [sumber/lisensi](THIRD_PARTY_NOTICES.md).

## Versi 1.1 — navigasi, izin, dan update

UI UX Pro Max terpasang pada `.agents/skills/ui-ux-pro-max`; sistem desain yang telah disesuaikan berada di `design-system/screenguard-pro/MASTER.md`. Seluruh halaman memakai font Plus Jakarta Sans lokal, tema putih-biru/gelap, dock mengambang dengan state tiap tab yang dipertahankan, dan animasi native yang mengikuti pengaturan animasi Android.

Panduan baru menjelaskan Setelan Terbatas Android dan membuka Info aplikasi. Pembaruan dideteksi ketika aplikasi dibuka; unduhan diperiksa checksum, versi, paket, dan sertifikat sebelum pemasangan melalui Android. Internet dipakai hanya untuk update resmi GitHub, tanpa mengirim data pemakaian. Lihat [UPDATES.md](docs/UPDATES.md) untuk signing, penerbitan feed, serta migrasi dari APK preview lama.
