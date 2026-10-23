# Pemulihan

Pertama: nyalakan kembali Shizuku, beri izin, buka ScreenGuard, lalu **Pengaturan → Pulihkan semua aplikasi**. Tunggu hasil dan periksa aplikasi yang masih gagal. Jangan menghapus data ScreenGuard sebelum pulih: kepemilikan aturan diperlukan untuk menentukan apa yang harus dilepas.

Jika ScreenGuard tidak dapat dibuka atau sudah dihapus, **hentikan server Shizuku dahulu** untuk membatalkan timer pembekuan yang masih hidup. Kemudian gunakan ADB dari perangkat tepercaya. Ganti `com.example.app` dengan paket aplikasi yang memang dikunci dan `0` dengan ID profil yang benar:

```sh
adb shell pm enable --user 0 com.example.app
adb shell pm unsuspend --user 0 com.example.app
adb shell cmd connectivity set-package-networking-enabled true com.example.app
```

Perintah firewall bergantung modul Connectivity/ROM. Jangan mematikan chain global jika ada firewall lain. Restart biasanya menghapus aturan jaringan volatil, tetapi tidak selalu menghapus penonaktifan/penangguhan paket. Menghapus aplikasi tidak otomatis memulihkan status paket yang ditulis lewat hak shell.

Jika data lokal rusak, ScreenGuard menolak menimpa file secara diam-diam dan menampilkan kesalahan. Pulihkan paket melalui sistem/ADB sebelum reset data. Tidak tersedia pemulihan PIN melalui email karena seluruh aplikasi offline.
