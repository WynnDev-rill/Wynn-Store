# Acuan UI

Referensi pengguna merupakan sumber utama. Nama produk diganti menjadi ScreenGuard Pro, sementara tema putih-biru, kartu membulat, grafik kolom, hierarki informasi, posisi aksi cepat, dipertahankan, dengan struktur empat menu sesuai revisi pengguna.

- Permukaan: putih; latar `#F6F9FD`; teks `#142137`; teks sekunder `#58677C`.
- Warna aksi `#2366D1` dengan teks putih; bidang lembut `#EAF3FF`. Bukan dynamic color agar desain konsisten antarperangkat.
- Kartu 24dp, list/input 20dp, tombol 12–20dp, jarak 8/12/16/20/24dp. Area sentuh minimum 48dp.
- Plus Jakarta Sans lokal (400/500/600/700); judul 20/24/32sp, isi 14/16sp, pendukung 12sp. Sp mendukung pembesaran font.
- Konten maksimal 640dp di tablet; inset sistem, scrolling, pemotongan label panjang, layout IME, dan mode gelap memakai Compose/Material3.
- Grafik dibuat dengan Canvas dari durasi nyata. Hari dapat diketuk; semantik membacakan tanggal, durasi dan status dipilih. Halaman statistik aplikasi menampilkan data per jam, jumlah pembukaan dan sesi terlama bila kejadian Android tersedia. Ikon aplikasi dari PackageManager; ikon UI dari satu keluarga Material Outlined.
- Dock mengambang empat destinasi (Beranda, Bekukan, Jaringan, Pengaturan), kapsul aktif, state tiap tab dipertahankan. Dock tetap di atas keyboard. Transisi halaman fade/geser pendek dan perubahan progress mengikuti skala animasi sistem Compose. Tidak ada animasi dekoratif terus-menerus.

Perubahan fungsional terhadap referensi: satu sakelar firewall seluruh jaringan; onboarding eksplisit; indikator error/pending; tantangan dijalankan melalui ScreenGuard; pengaturan pemulihan. Perubahan ini mengikuti kemampuan Android, bukan simulasi.

UI/UX Pro Max dipakai untuk pedoman Compose, aksesibilitas, sentuhan, dan responsivitas. Hasil rekomendasi generik halaman pemasaran/warna indigo tidak dipakai karena tidak sesuai gambar pengguna.

Pedoman lengkap terbaru: [design system](../design-system/screenguard-pro/MASTER.md). Skill dapat dijalankan dari folder proyek: `python3 .agents/skills/ui-ux-pro-max/scripts/search.py "navigation animation typography" --stack jetpack-compose`.
