# Spatial revision — 25 Oktober 2026

Lima screenshot pengguna menjadi daftar perbaikan: pembuka, deteksi, transisi gambut, peta, dan dampak. Semuanya kini memiliki renderer Three.js, bukan sekadar simbol CSS. Teks, tanggal, data dan sumber dipertahankan.

| Bagian | Implementasi | Interaksi dan batas |
|---|---|---|
| Pembuka | Fragmen tanah, air, bara dan model kayu Kenney CC0 | Kanvas mengimbangi judul; citra NASA tetap jelas sebagai arsip2023 |
| Deteksi | Satelit dengan panel surya di atas bentang | Pemindaian dapat diperjelas; ilustrasi tidak mengklaim lintasan satelit nyata |
| Gambut | Enam lapisan organik, air dan cahaya bara | Jarak lapisan mengikuti gulir; tidak berskala |
| Peta | Geometri ekstrusi dari batas SVG provinsi yang telah digunakan | Putar/atur ulang; relief seragam, tidak mengode jumlah hotspot/elevasi; peta datar berlabel, timestamp dan angka tetap tersedia |
| Dampak | Lapisan sprite kabut, ribuan titik dan kedalaman perspektif | Gerak udara serta sudut pandang; bukan model molekul atau simulasi dispersi |

Semua renderer baru lazy saat masuk viewport, DPR mobile≤1,25, target penjadwalan24FPS mobile/40FPS desktop dan berhenti di luar layar. Reduced motion menampilkan keadaan statis. Tombol native dan fallback tetap tersedia. Efek tidak memakai framework scroll pihak ketiga; Three.js dan GLTF/SVG loader MIT digunakan, ditambah aset CC0 yang lisensinya dicatat di [licenses/spatial-assets.md](licenses/spatial-assets.md). Unduhan ambientCG tertolak sehingga tidak digunakan.

## Pembanding OHZI

Riset primer diperbarui melalui https://ohzi.io/ dan https://ohzi.dev/ . Prinsip yang diterapkan adalah material, perspektif, respons input dan kinerja, bukan mengambil kode atau aset OHZI yang haknya tidak diberikan. Pembanding visual terdahulu dibatasi loader; tidak mengklaim benchmark frame rate berdampingan. Lihat juga DESIGN-REFERENCES.md untuk10referensi dan batas akses.

Peningkatan ini menghapus lima placeholder visual utama, tetapi geometri satelit dan tanah masih bergaya ilustratif. Detail material, perancangan kamera antaradegan dan model khusus belum dapat disebut setara seluruh karya studio OHZI. Tidak ada skor persentase estetika buatan. Target kualitas dinilai dari screenshot, kontrol, aksesibilitas dan performa yang benar-benar diperiksa.

## Revisi setelah render

Ukuran fragmen pembuka diperkecil; gambar arsip menjadi inset. Lapisan gambut yang menutupi teks diperkecil dan diturunkan. Bola besar dalam visual dampak diganti sprite kabut. Overflow pembuka dipotong dan komposisi mobile diperbaiki. Perbandingan pada viewport390px dan1440px disimpan di screenshots/spatial-*.webp.

Bukti mentah: SPATIAL-QA.json, BROWSER-QA.json dan PERFORMANCE-QA.json. Pengujian Chromium software bukan HP fisik. Audit otomatis tidak merupakan sertifikasi aksesibilitas atau jaminan frame rate pada semua perangkat.
