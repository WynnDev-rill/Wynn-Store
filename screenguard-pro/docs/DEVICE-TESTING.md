# Pengujian perangkat nyata

Build/tes JVM dan screenshot emulator tidak membuktikan kompatibilitas firewall pada setiap ROM. Jalankan matriks berikut pada Poco/HyperOS dengan Shizuku melalui wireless ADB, serta satu perangkat Android standar. Pakai aplikasi nonkritis; jangan menguji dengan launcher, IME, aplikasi telepon, atau layanan keamanan.

1. Instal APK; periksa onboarding saat izin belum ada. Statistik kosong harus dijelaskan, bukan diisi contoh. Grant/revoke akses penggunaan dan kembali ke ScreenGuard.
2. Mulai Shizuku; izinkan ScreenGuard. Status harus terhubung. Hentikan Shizuku dan coba kontrol: tidak boleh menampilkan keberhasilan palsu.
3. Blokir aplikasi pengujian; tutup dan buka lagi, periksa internet melalui Wi-Fi lalu seluler, IPv4/IPv6 jika tersedia. Lepas blokir dan ulangi. Uji juga setelah pergantian jaringan. Jangan menyimpulkan dari cache konten.
4. Tangguhkan paket; periksa Android menolak launch. Buka dari ScreenGuard dengan tantangan salah lalu benar. Sesudah jeda lima menit, batas otomatis harus berlaku lagi jika sudah terlampaui.
5. Nonaktifkan paket; pastikan ikon hilang dan paket masih mempunyai datanya. Buka dari daftar ScreenGuard; pastikan ikon dan peluncuran kembali berfungsi.
6. Tetapkan batas satu menit; aktifkan pemantauan, gunakan aplikasi, lalu periksa penangguhan pada pemeriksaan berikutnya. Periksa jam lintas tengah malam, pilihan hari mulai, fokus 25 menit, dan penghentian pemantauan.
7. Pasang PIN, tutup/rotasi aplikasi, coba mengubah aturan/tema/PIN dan membuka kunci. Lima PIN salah harus memicu waktu tunggu. Matematika tidak menggantikan PIN orang tua.
8. Restart, mulai kembali Shizuku, buka ScreenGuard, dan periksa pemulihan aturan. Uji juga setelah force-stop, pembaruan APK, revoke permission, dan mode baterai HyperOS. Force-stop tidak bisa dipulihkan diam-diam oleh aplikasi biasa.
9. Uji ‘Pulihkan semua aplikasi’; pastikan aplikasi yang dikunci alat lain tidak disentuh. Baru kemudian uninstall.
10. Periksa 320/360/411dp, landscape/tablet, ukuran font 1.5×/2×, mode gelap, TalkBack, skala animasi 0×, keyboard terbuka, serta label aplikasi panjang.

Foreground service sengaja tidak memakai wake lock dan tidak menjanjikan jadwal tepat waktu selama Doze. Catat model, Android/HyperOS, Shizuku, kondisi izin, dan hasil setiap langkah. Kegagalan satu ROM harus dilaporkan tanpa mengubahnya menjadi indikator sukses.
