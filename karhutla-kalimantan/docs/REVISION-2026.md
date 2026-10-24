# Revisi dokumenter — fokus Oktober 2026

## Standar penilaian yang dapat diperiksa

Tidak ada skor selera 9/10 atau klaim setara Awwwards tanpa penilai eksternal. Pembanding dipilih menurut fungsi: OHZI untuk kedalaman/lighting dan koordinasi motion; Minh Pham untuk transisi dan komposisi; The Pudding untuk interaksi data; NASA Earth Observatory untuk integritas dokumentasi. Peninjauan awal dan batas akses situs dicatat di DESIGN-REFERENCES.md.

| Dimensi | Sebelum | Revisi | Batas pembandingan |
|---|---|---|---|
| Relevansi berita | Fokus sejarah hingga 2023 | Pembuka dan kronologi Oktober 2026; rilis sampai 24 Oktober, snapshot 23 Oktober 21.00 WIB | Bukan feed realtime; arsip citra 2023 diberi label, tidak disamarkan sebagai kejadian 2026 |
| Storytelling data (The Pudding) | Peta luas tahunan saja | Dua snapshot hotspot 22/23 Oktober, waktu sumber dan CSV; luas tahunan dipisahkan dalam arsip | Tidak ada koordinat hotspot mentah sehingga tidak membuat titik api acak |
| Kedalaman 3D (OHZI) | Pohon geometris sederhana, lapisan datar, kamera tetap | Kanopi bervariasi, akar bercabang, tekstur gambut, kontak bayangan, air dan bara bawah tanah; kamera turun/mengorbit serta pemisahan lapisan dikendalikan gulir | Diorama prosedural ilustratif; bukan aset photoreal hasil pemodelan studio atau simulasi fisika api |
| Motion (Minh Pham) | Parallax foto dan pergantian kondisi | Kamera gulir, ruang transisi perspektif, framing teks/foto yang terkoordinasi | Tidak meniru mask-cursor dan tidak memaksa navigasi lewat 3D |
| Bukti (NASA) | Foto/citra arsip bertanggal | Arsip dipisahkan tegas dari laporan 2026; sumber primer terkini dekat setiap klaim | Belum ada foto/video 2026 berlisensi ulang yang diverifikasi; rilis resmi menjadi bukti kontemporer |
| Keterpakaian | Keyboard, tabel, reduced motion | Jalur yang sama dipertahankan, fallback SVG, tanpa scroll hijack; diuji 320–1440px | Emulasi browser, bukan pengujian HP fisik |

## Ambang penerimaan rilis

- Semua fungsi lama dan snapshot baru lulus pengujian, tidak ada error JavaScript.
- Nol temuan otomatis axe pada beranda/sumber; label waktu/unit benar dan metadata tanggal 2026 terlihat di pembuka.
- Model utuh terbaca pada mobile; empat kondisi dan progres kamera berfungsi. Kamera/lapisan tetap stabil pada reduced motion; tidak ada loop GPU tanpa interaksi.
- Screenshot desktop/mobile diperiksa dan dibandingkan dengan baseline docs/screenshots/gambut.webp. Bukti baru di docs/screenshots/revision-*.webp.
- Build dan pemeriksaan aset/CSP lulus; perubahan dibatasi pada direktori proyek.

Penilaian visual tetap memiliki unsur subjektif. Kenaikan kualitas dijelaskan melalui perubahan yang terlihat dan tes yang dapat diulang, bukan angka peringkat situs yang tidak memiliki dataset pembanding.

## Dasar faktual revisi

- [Kemenhut, 24 Oktober 2026](https://www.kehutanan.go.id/news/menhut-pimpin-operasi-di-kalimantan-barat-omc-diperkuat): cuplikan SiPongi 23 Oktober pukul 21.00 WIB, operasi Pulang Pisau/Ketapang dan dukungan OMC.
- [Kemenhut, 23 Oktober 2026](https://www.kehutanan.go.id/news/hotspot-naik-operasi-dan-perlindungan-satwa-diperkuat): cuplikan 22 Oktober, definisi hotspot, serta 51 orangutan yang diselamatkan di Ketapang dan Kayong Utara pada 6 Agustus–pertengahan Oktober.
- [Kemenhut, 22 Oktober 2026](https://www.kehutanan.go.id/news/hotspot-naik-operasi-dan-penegakan-hukum-diperkuat): cuplikan 21 Oktober dan operasi di lahan gambut.

Angka hotspot tidak dijumlahkan antarhari dan tidak dikonversi menjadi hektare. Kalimantan Timur/Utara tidak dirinci dalam rilis yang dipakai, sehingga tidak diberi angka nol. Diorama merupakan ilustrasi edukatif, bukan rekonstruksi satu lokasi atau model prediksi.
