# Pengujian perangkat nyata

Build/tes JVM dan screenshot emulator tidak membuktikan kompatibilitas firewall pada setiap ROM. Jalankan matriks berikut pada Poco/HyperOS dengan Shizuku melalui wireless ADB, serta satu perangkat Android standar. Pakai aplikasi nonkritis; jangan menguji dengan launcher, IME, aplikasi telepon, atau layanan keamanan.

1. Instal APK; periksa onboarding saat izin belum ada. Statistik kosong harus dijelaskan, bukan diisi contoh. Grant/revoke akses penggunaan dan kembali ke ScreenGuard.
2. Mulai Shizuku; izinkan ScreenGuard. Status harus terhubung. Hentikan Shizuku dan coba kontrol: tidak boleh menampilkan keberhasilan palsu.
3. Blokir aplikasi pengujian; tutup dan buka lagi, periksa internet melalui Wi-Fi lalu seluler, IPv4/IPv6 jika tersedia. Lepas blokir dan ulangi. Uji juga setelah pergantian jaringan. Jangan menyimpulkan dari cache konten.
4. Bekukan paket dari daftar Bekukan; pastikan ikon hilang dan data tetap ada. Cari paket yang nonaktif melalui filter/pencarian ScreenGuard.
5. Uji tunggu, soal Ringan/Sedang/Menantang dan kombinasi 1–5 soal. Jawaban salah dan tunggu belum selesai tidak boleh membuka paket. Set akses 1 menit; setelah tantangan aplikasi terbuka, lalu harus dibekukan kembali. Ikon boleh muncul selama sesi aktif.
6. Tetapkan batas satu menit/jadwal pada editor aplikasi, gunakan aplikasi dan periksa disable-user pada pemeriksaan berikutnya. Periksa lintas tengah malam, pilihan hari mulai, dan perubahan dari Selalu menjadi Sesuai batas & jadwal. Mengosongkan semua aturan otomatis menghentikan pemantauan bila tidak ada sesi aktif.
7. Pasang PIN, tutup/rotasi aplikasi, coba mengubah aturan/tema/PIN dan memulihkan. Lima PIN salah harus memicu waktu tunggu. Tantangan biasa dapat diselesaikan tanpa PIN pengaturan. Perubahan jam/reboot tidak boleh memperpanjang sesi.
8. Force-stop klien ScreenGuard saat akses sementara berjalan: daemon Shizuku harus membekukan ulang. Ulangi dengan menghentikan Shizuku: jangan mengklaim timer tetap hidup; mulai Shizuku kembali dan periksa rekonsiliasi. Restart, mulai Shizuku, periksa aturan. Uji pembaruan APK tanpa uninstall, revoke izin dan penghemat baterai HyperOS. Klien biasa tidak dapat memulihkan dirinya dari force-stop; timer terpisah yang diuji.
9. Uji ‘Pulihkan semua aplikasi’; pastikan aplikasi yang dikunci alat lain tidak disentuh. Baru kemudian uninstall.
10. Periksa 320/360/411dp, landscape/tablet, ukuran font 1.5×/2×, mode gelap, TalkBack, skala animasi 0×, keyboard terbuka, serta label aplikasi panjang.

Foreground service sengaja tidak memakai wake lock dan tidak menjanjikan jadwal tepat waktu selama Doze. Catat model, Android/HyperOS, Shizuku, kondisi izin, dan hasil setiap langkah. Kegagalan satu ROM harus dilaporkan tanpa mengubahnya menjadi indikator sukses.
