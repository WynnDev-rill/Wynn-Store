# Acuan UI

Referensi pengguna merupakan sumber utama. Nama produk diganti menjadi ScreenGuard Pro, sementara tema putih-biru, kartu membulat, grafik kolom, hierarki informasi, posisi aksi cepat, dan urutan tujuh halaman dipertahankan.

- Permukaan: putih; latar `#F6F9FD`; teks `#142137`; teks sekunder `#58677C`.
- Warna aksi `#1E73DF` dengan teks putih; bidang lembut `#EAF3FF`. Bukan dynamic color agar desain konsisten antarperangkat.
- Kartu 20dp, tombol 12–14dp, jarak 8/12/16/20/24dp. Area sentuh minimum 48dp.
- Font sistem Sans Serif; judul 20/23/30sp, isi 13sp, pendukung 12sp. Sp mendukung pembesaran font.
- Konten maksimal 640dp di tablet; inset sistem, scrolling, pemotongan label panjang, layout IME, dan mode gelap memakai Compose/Material3.
- Grafik dibuat dengan Canvas dari durasi nyata. Semantik membacakan hari beserta nilainya. Ikon aplikasi dari PackageManager; ikon UI dari satu keluarga Material Outlined.
- Transisi halaman fade dan perubahan progress mengikuti skala animasi sistem Compose. Tidak ada animasi dekoratif terus-menerus.

Perubahan fungsional terhadap referensi: satu sakelar firewall seluruh jaringan; onboarding eksplisit; indikator error/pending; tantangan dijalankan melalui ScreenGuard; pengaturan pemulihan. Perubahan ini mengikuti kemampuan Android, bukan simulasi.

UI/UX Pro Max dipakai untuk pedoman Compose, aksesibilitas, sentuhan, dan responsivitas. Hasil rekomendasi generik halaman pemasaran/warna indigo tidak dipakai karena tidak sesuai gambar pengguna.
