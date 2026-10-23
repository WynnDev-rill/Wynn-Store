# Pembaruan dan signing

Saat aplikasi dibuka, updater membaca JSON publik `updates/stable.json` (atau `preview.json` untuk paket preview). Pengecekan tidak memblokir UI, dibatasi satu percobaan per 2 menit per proses, dan dapat dimatikan di Pengaturan. Tidak ada statistik penggunaan, daftar aplikasi, akun atau ID perangkat yang dikirim.

APK harus berasal dari path distribusi resmi repository ini atau GitHub Releases ScreenGuard. HTTPS/redirect dibatasi. Metadata dibatasi 32 KiB, APK 100 MiB. Sebelum instalasi, ukuran, SHA-256, package name, versionCode, minimum Android dan sertifikat penandatangan harus sesuai. APK dengan kunci lain ditolak. Android meminta izin pemasangan dari sumber ini dan konfirmasi akhir; tidak ada pemasangan diam-diam.

## Saluran produksi

Paket produksi `id.wynn.screenguard` memakai kunci signing khusus yang disimpan di luar repository. APK preview lama `id.wynn.screenguard.preview` memakai kunci pengujian sementara. Android tidak dapat memperbarui APK dengan kunci berbeda. Untuk pindah dari preview lama: pilih **Pulihkan semua aplikasi** di preview, hentikan pemantauan, lalu pasang APK produksi dan berikan izin baru. Pengaturan preview tidak dipindahkan otomatis.

Backup signing privat berisi JKS dan kredensial disampaikan secara privat kepada pemilik. Jangan commit/upload sebagai artifact publik. Untuk rilis berikutnya, masukkan empat GitHub Actions Secrets pada environment `screenguard-release`:

- `SG_KEYSTORE_BASE64`: base64 JKS dari backup
- `SG_STORE_PASSWORD`
- `SG_KEY_ALIAS`
- `SG_KEY_PASSWORD`

Setelah menaikkan versionCode/versionName, jalankan workflow ScreenGuard Pro Android di main. Job signing hanya berjalan setelah semua tes lolos. Job tersebut mengunggah APK bertanda tangan ke GitHub Releases dan memperbarui feed stable. Gunakan kunci yang sama selamanya. Tanpa secrets, workflow hanya membuat APK pengujian; tidak mengklaim rilis produksi berhasil.

Distribusi APK publik pertama tersedia di `distribution/` sebagai bootstrap channel, sedangkan kunci privat tidak pernah masuk git. Rilis workflow berikutnya memakai GitHub Releases, sehingga binary berikutnya tidak perlu ditambahkan ke git. Feed stable selalu menunjuk APK yang telah ditandatangani dan diuji. Jangan menerbitkan checksum/kunci berbeda untuk memaksa update.

## Izin penggunaan yang dibatasi Android

Pesan “Aplikasi tidak diberi akses / Dikontrol oleh Setelan Terbatas” memerlukan tindakan pengguna: Info aplikasi ScreenGuard Pro → menu ⋮ → Izinkan setelan terbatas → konfirmasi kunci layar → kembali ke Akses penggunaan. Lokasi menu dapat berbeda di HyperOS. Aplikasi tidak dapat menjamin atau mengubah kebijakan vendor secara otomatis. Jika Shizuku sudah terhubung, tombol **Aktifkan akses melalui Shizuku** menyediakan alternatif dengan konfirmasi pengguna: `cmd appops set --user 0 <paket-ScreenGuard-sendiri> GET_USAGE_STATS allow`. Endpoint tidak menerima nama paket atau perintah dari pemanggil; hanya izin milik ScreenGuard sendiri yang dapat diubah. Hasil dicek melalui AppOps dan UsageRepository; kegagalan vendor ditampilkan sebagai error.

Panduan resmi: https://support.google.com/android/answer/12623953?hl=id
