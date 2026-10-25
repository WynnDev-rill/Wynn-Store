# Tinjauan rilis 25 Oktober 2026

Fokus baru: dokumentasi jejak kejadian Januari–Oktober 2026, hingga rilis yang diperiksa pada 25 Oktober. Bukan hanya tiga cuplikan Oktober. Sumber dan cakupan masing-masing peristiwa tersedia di [SEASON-RESEARCH.md](SEASON-RESEARCH.md).

## Sebelum dan sesudah

- Jejak musim menjadi bagian tersendiri dengan lima tahap bertanggal, navigasi bulan, ringkasan dan detail sumber. Tanggal 17 Januari hanya berlaku pada catatan Kalimantan Barat, tidak diklaim sebagai satu tanggal penyalaan seluruh pulau.
- Peta diperbarui sampai cuplikan 24 Oktober 21.00 WIB, rilis 25 Oktober: Tengah 60, Barat 55, Selatan nihil deteksi high confidence. Timur/Utara tidak dirinci; nol deteksi tidak berarti seluruh kebakaran telah padam.
- Teks yang bertumpuk di atas avatar dihapus. Lapisan kaca, foil dan kilau mengikuti pointer/sentuhan/keyboard; sensor orientasi digunakan otomatis ketika browser telah mengizinkannya. Tidak ada tombol sensor maupun permintaan izin otomatis.
- Komposisi hero, medan sinyal, tekstur bidang, grafik, penampang, dampak dan pemulihan diselaraskan. Navigasi mobile dipadatkan menjadi header 64 px dan rail bab di bawah; kontrol model tetap terjangkau.
- Pengurangan gerak menghentikan animasi dan transisi, mempertahankan isi dan kontrol. Tidak ada audio otomatis atau gulir yang diambil alih.

## Pembanding premium

Peninjauan sumber primer OHZI, Minh Pham dan The Pudding diperbarui; konteks visual peninjauan sebelumnya dan keterbatasan akses tercatat di [DESIGN-REFERENCES.md](DESIGN-REFERENCES.md). Screenshot implementasi diperiksa oleh pelaksana dan peninjau kedua sebelum publikasi.

| Pembanding | Standar yang diterapkan | Batas yang tetap ada |
|---|---|---|
| OHZI | Bahasa ruang yang menyambungkan kontur, citra, lapisan gambut dan kartu optik; gerak dibatasi untuk tetap dapat dipakai di mobile | Tidak mengklaim aset fotorealistis, kualitas lighting atau frame rate yang sama dengan OHZI |
| Minh Pham | Skala tipografi, ruang negatif, reveal kata, perpindahan fokus dan respons input | Struktur dokumenter tetap berbeda; tidak menyalin karya, cursor atau identitas situs |
| The Pudding | Narasi bertanggal, unit data jelas, detail bertingkat dan sumber yang dapat dibuka | Kurasi editorial, bukan semua kejadian atau feed langsung |

Tidak ada dasar untuk memberi peringkat objektif “di atas seluruh situs rata-rata” atau angka persentase peningkatan estetika. Ambang rilis menggunakan pemeriksaan visual, fungsi, aksesibilitas otomatis, data dan loading; bukan skor selera buatan.

## Temuan dan perbaikan

1. Kredit footer kurang kontras terhadap latar terang: warna teks digelapkan; audit axe diulang.
2. Rail bab mempertahankan status Peta ketika kembali ke pembuka: pemilihan bab memakai posisi scroll, pembuka tidak memilih bab lama.
3. Transisi tersisa pada reduced motion: animasi dan transisi dimatikan secara eksplisit.
4. Screenshot mobile memakai offset header lama: diperbarui mengikuti header ringkas agar tidak memasukkan sisa bab sebelumnya.
5. Pengujian Escape kartu sempat membaca frame lama pada tab latar: tab diaktifkan sebelum memeriksa hasil input. Input sentuh dan keyboard tetap tersedia bila sensor tidak mengirim data.

Giroskop fisik belum diuji pada HP. Uji orientasi menggunakan event simulasi tanpa tombol aktivasi, sehingga membuktikan jalur logika, bukan kompatibilitas semua sensor/browser. Semua gambar dokumenter tetap bertanggal; avatar adalah ilustrasi pilihan Wynn, bukan dokumentasi karhutla.
