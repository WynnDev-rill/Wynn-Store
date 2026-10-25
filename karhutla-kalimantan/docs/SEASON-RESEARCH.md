# Jejak penanganan karhutla Kalimantan 2026

Pemeriksaan: **25 Oktober 2026**. Catatan ini mendukung `SeasonJourney.astro` dan `src/data/season.ts`. Seluruh sumber di bawah adalah rilis primer pemerintah yang dibaca langsung. Tidak ada tanggal penyalaan tunggal yang ditetapkan untuk seluruh Kalimantan. Rangkaian Januari–Oktober mencakup kejadian serta operasi di lokasi berbeda.

## Sumber dan batas penggunaan

| Tahap | Sumber primer | Fakta yang dipakai | Batas cakupan |
|---|---|---|---|
| Januari | [Gakkum, 28 Januari 2026](https://gakkum.kehutanan.go.id/siaran-pers/detail/cegah-karhut-meluas-manggala-agni-bergerak-cepat-di-kalimantan-barat) | Laporan menyebut kebakaran Kalbar terjadi sejak 17 Januari; sebagian lokasi dipadamkan pada 25 Januari; lokasi lain masih ditangani. | Tanggal 17 Januari berlaku bagi catatan Kalbar pada laporan ini. Tidak dianggap awal penyalaan seluruh pulau. |
| Juli | [Kemenhut, 17 Juli 2026](https://www.kehutanan.go.id/pers/kementerian-kehutanan-perkuat-pengendalian-karhutla-di-kalimantan-selatan-dan-seluruh-wilayah-rawan-hadapi-puncak-musim-kemarau-2026) | Kalsel siaga sejak 6 Juli; 180 personel di tiga Daops; posko aktif 15 Juli; 107 operasi hingga 16 Juli. | Angka operasi bukan hotspot atau jumlah kejadian unik. Tidak memakai angka nasional sebagai angka Kalimantan. |
| Agustus | [Kemenhut, 20 Agustus 2026](https://www.kehutanan.go.id/news/pemerintah-perkuat-operasi-terpadu-karhutla-di-kalimantan-tambah-dukungan-udara-dan-pemadaman-darat) | Operasi terpadu, dukungan udara/darat dan sumber air; laporan asap di beberapa kota; kondisi jarak pandang berbeda menurut tempat dan waktu. | Tidak menyamakan pengamatan satu bandara dengan seluruh Kalimantan. Pengamatan hotspot pada rilis ini memakai MODIS, jam 07.00 WIB; tidak dicampur menjadi seri tren dengan laporan multi-satelit Oktober. |
| September | [Kemenhut, 21 September 2026](https://www.kehutanan.go.id/news/pengendalian-karhutla-kalteng-sumsel-diperkuat-64-armada-udara-dikerahkan) | Pada 20 September, jarak pandang Tjilik Riwut sempat sekitar 600 m; 14 pergerakan pesawat terganggu (7 datang, 7 berangkat). | Angka 64 armada dan 54.394 personel dalam laporan mencakup berbagai wilayah nasional, sehingga tidak dijadikan metrik Kalimantan. Jam data nasional dan rincian provinsi juga berbeda. |
| Oktober | [Kemenhut, 25 Oktober 2026](https://www.kehutanan.go.id/news/hotspot-turun-1-019-titik-operasi-karhutla-dan-perlindungan-satwa-tetap-diperkuat-2) | Cuplikan 24 Oktober 21.00 WIB: Kalteng 60, Kalbar 55, Kalsel nihil high confidence; pemadaman/pendinginan tetap berlangsung. Dua orangutan dilepasliarkan di Ketapang pada 24 Oktober setelah evakuasi dan pemeriksaan. | Nasional 317 hotspot. Kalimantan Timur dan Utara tidak dirinci. Nol Kalsel berarti nihil deteksi high confidence pada cuplikan itu; tidak berarti tidak ada kebakaran atau asap. |

URL `/pers/` untuk rilis 25 Oktober juga berhasil melalui HTTP biasa, tetapi alat web lebih konsisten membaca tautan `/news/` yang ditautkan oleh daftar resmi. Komponen memakai URL `/news/` yang diverifikasi. Nomor rilis terbaru **SP.642/HKLN/10/2026**.

## Nilai pengamatan terbaru yang diekspor

`latestSeasonSnapshot` menyimpan tanggal observasi dan tanggal publikasi secara terpisah. Satuan: **hotspot high confidence**.

| Provinsi | 24 Oktober 2026, 21.00 WIB |
|---|---:|
| Kalimantan Tengah | 60 |
| Kalimantan Barat | 55 |
| Kalimantan Selatan | 0 |
| Kalimantan Timur | tidak dirinci (`null`) |
| Kalimantan Utara | tidak dirinci (`null`) |

Tidak menjumlahkan snapshot antartanggal atau mengubah hotspot menjadi hektare. Penurunan deteksi tidak digunakan sebagai bukti seluruh api, bara, atau polusi telah hilang. Rangkaian komponen bukan laporan waktu nyata maupun rekap akhir tahun.

## Implementasi dan akses

- Lima tautan bulan memakai anchor HTML asli dan menuju artikel yang dapat difokuskan.
- Rangkuman tiap tahap selalu terlihat. Konteks tambahan memakai `details/summary` asli dan tetap bekerja tanpa JavaScript.
- Tanggal kejadian/pemantauan dan tanggal rilis ditampilkan terpisah; tiap tahap menaut langsung ke sumber primer.
- Tidak memakai foto generatif, rekonstruksi saksi, angka korban rekaan, atau prediksi risiko dari ilustrasi.
- Validasi kompilasi dilakukan melalui pemeriksaan Astro proyek; parent mengintegrasikan komponen ke halaman dan memeriksa hasil browser.
