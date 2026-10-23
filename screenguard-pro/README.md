# ScreenGuard Pro

Versi produksi terbaru: [unduh ScreenGuard Pro 1.2.0](distribution/ScreenGuard-Pro-1.2.0.apk), ditandatangani dengan sertifikat yang sama seperti 1.1.0. [Provenance build dan checksum](distribution/ScreenGuard-Pro-1.2.0.json).

Native Android Kotlin + Jetpack Compose. Android 11+ / user utama (0), tanpa root atau VPN. Proyek ini terpisah dari proyek lain dalam Wynn Store.

## Versi 1.2

Empat menu: **Beranda, Bekukan, Jaringan, Pengaturan**. Tema putih-biru, font Plus Jakarta Sans lokal, dock mengambang, animasi yang mengikuti pengaturan Android, dan ikon launcher adaptive memakai gambar SG asli pengguna.

- **Beranda**: total waktu layar, grafik tujuh hari yang bisa diketuk, daftar aplikasi, statistik aplikasi terperinci (mingguan/per jam/jumlah dibuka/sesi terlama). Semua berasal dari UsageStatsManager; tidak ada data contoh di produksi. Riwayat lama ditandai perkiraan dan detail yang dihapus Android tidak ditebak.
- **Bekukan**: daftar seluruh aplikasi termasuk yang dinonaktifkan, pencarian nama/paket, filter unduhan/sistem/perlindungan. Ketuk aplikasi untuk memilih waktu tunggu, matematika atau kombinasi, tingkat soal, jumlah soal, dan durasi akses sementara. Aplikasi penting/tanpa launcher hanya ditampilkan sebagai informasi.
- **Jaringan**: satu sakelar nyata untuk semua koneksi aplikasi melalui Shizuku/ConnectivityService OEM_DENY_3. API tidak memisahkan Wi-Fi/seluler; ROM yang tidak menyediakan API menunjukkan status tidak didukung. UID bersama ditolak.
- **Pengaturan**: PIN untuk perubahan aturan, izin/Shizuku, tema, target harian, notifikasi, pemulihan semua aturan dan update GitHub terverifikasi. Tidak ada menu Fokus atau tantangan global duplikat.

## Mulai

1. Instal APK produksi terbaru dari `distribution/` atau artifact rilis Actions. Gunakan sertifikat produksi yang sama untuk update dari 1.1.0 tanpa uninstall. APK `preview` memakai ID terpisah dan bukan pembaruan produksi.
2. Instal [Shizuku](https://shizuku.rikka.app/download/), pairing Debugging Nirkabel dan mulai layanan. Berikan izin ScreenGuard. Pada HyperOS izinkan autostart dan baterai tanpa batasan untuk kedua aplikasi.
3. Aktifkan akses penggunaan untuk statistik/batas harian. Bila Android memblokir izin karena **Setelan Terbatas**, gunakan panduan Info aplikasi di onboarding. Akses penggunaan melalui Shizuku hanya menargetkan paket ScreenGuard sendiri dan hasilnya diperiksa.
4. Buka **Bekukan**, ketuk aplikasi, pilih tantangan lalu **Bekukan aplikasi**. `disable-user` menyimpan data aplikasi dan menghilangkan ikon launcher (sebagian launcher OEM perlu menyegarkan cache). Ketuk aplikasi yang dibekukan di ScreenGuard untuk menyelesaikan tantangan dan membuka sementara.
5. Selama akses sementara aplikasi harus diaktifkan, sehingga ikon dapat muncul lagi. Sesudah durasi berakhir timer di daemon Shizuku membekukannya kembali, termasuk saat klien ScreenGuard di-force-stop. CPU tidur/Doze dapat menunda eksekusi. Timer tidak memakai wake lock.
6. Batas harian/jadwal opsional berada di editor aplikasi → **Batas & jadwal otomatis**. Menyimpan aturan mengaktifkan notifikasi pemantauan ±15 detik saat layar hidup/60 detik saat mati. Tantangan memberi jeda sementara sebelum aturan berlaku lagi. Ini bukan alarm tepat waktu.
7. Sebelum uninstall, buka **Pengaturan → Pulihkan semua aplikasi**, periksa hasilnya. Jangan hapus data saat ada aplikasi yang masih dibekukan.

## Batas platform dan keamanan

Shizuku harus tetap aktif. Daemon bertahan dari penghentian klien ScreenGuard, **bukan** dari berhentinya server Shizuku atau reboot. Pada perangkat tanpa root Shizuku harus dimulai ulang setelah boot. Aplikasi yang sudah dibekukan tetap nonaktif; sesi yang sedang terbuka memerlukan koneksi kembali untuk pemulihan pembekuan. Restart/perubahan jam membatalkan validitas sesi; aturan dan kepemilikan tersimpan atomik. Fallback WorkManager membantu pemulihan ketika Android mengizinkan.

ScreenGuard tidak mengganti peluncur atau mengintersepsi semua ikon Android. Ia tidak menghapus aplikasi dan tidak mengambil alih pembekuan milik alat lain. Komponen penting (launcher, input, telepon/SMS default, admin perangkat, izin, penginstal, Shizuku dan komponen vendor kritis) dilindungi. Profil kerja/kloning tidak dikelola. Jangan gunakan firewall lain pada UID/OEM_DENY_3 yang sama.

PIN enam digit melindungi perubahan/pemulihan, bukan menyembunyikan jalan keluar sistem. Hash PBKDF2-HMAC-SHA256 120.000 iterasi dengan salt acak, lockout percobaan, sesi dua menit yang dicabut saat aplikasi ke latar. Pemilik perangkat dapat mencabut izin, mengubah jam, menghentikan Shizuku, memakai ADB atau menghapus aplikasi; ini bukan Device Owner/MDM. Tantangan biasa tidak memerlukan PIN pengaturan.

Data hanya di perangkat: AtomicFile aturan, SQLite snapshot harian hingga 90 hari. Internet hanya untuk metadata dan APK resmi GitHub, tanpa unggahan statistik, akun, iklan, analitik atau API berbayar. Split-screen dapat membuat jumlah durasi per aplikasi melebihi waktu fisik layar. Android dapat menghapus riwayat lama; agregat hari lama belum tersimpan ditandai perkiraan.

## Build dan signing

JDK 17, SDK 36, Build Tools 36.0.0, Gradle wrapper 8.13. Dependensi dipatok.

```bash
cd screenguard-pro
./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease :app:assemblePreview :app:assembleDebugAndroidTest :device-fixture:assembleDebug
bash qa/run-device.sh
```

Actions **ScreenGuard Pro Android** menjalankan unit/lint, emulator API35 dengan Shizuku UID2000, interaksi Compose, font besar/gelap/landscape, screenshot, pembekuan/restore/firewall asli, timer setelah force-stop, serta pemasangan APK produksi dan pemeriksaan feed live. Fixture hanya di source set tes.

`assembleRelease` tanpa signing menghasilkan APK unsigned. Job signing manual dari `main` menggunakan environment `screenguard-release` dan secrets `SG_KEYSTORE_BASE64`, `SG_STORE_PASSWORD`, `SG_KEY_ALIAS`, `SG_KEY_PASSWORD`. Kunci hanya didekode ke RUNNER_TEMP dan dibersihkan. Tidak ada fallback debug untuk paket produksi. Signing lokal memakai environment `SG_KEYSTORE_PATH`, `SG_STORE_PASSWORD`, `SG_KEY_ALIAS`, `SG_KEY_PASSWORD`. Pertahankan kunci produksi yang sama; jangan membuat pengganti setelah distribusi. Tidak ada kunci privat/password dalam git.

## Struktur dan referensi

`domain/` aturan/sesi/jadwal/PIN murni; `data/` penyimpanan/katalog/statistik/rekonsiliasi; `privilege/` AIDL bertipe tanpa shell bebas; `service/` pemantauan/pemulihan; `ui/` empat destinasi dan alur detail; `qa/` pengujian perangkat.

[Keputusan teknis dan riset Hail/ShizuWall/Canta](docs/ARCHITECTURE-1.2.md) · [Pemulihan darurat](docs/RECOVERY.md) · [Update/signing](docs/UPDATES.md) · [Desain UI UX Pro Max](design-system/screenguard-pro/MASTER.md) · [Lisensi](THIRD_PARTY_NOTICES.md)

Kode GPL-3.0; referensi Hail/ShizuWall GPL-3.0, AndroidX/Kotlin/Shizuku API Apache-2.0, font SIL OFL. UI UX Pro Max disertakan dengan lisensi MIT. Aset SG diberikan pengguna. Pengujian HyperOS fisik tetap diperlukan untuk perilaku launcher dan penghemat baterai vendor.
