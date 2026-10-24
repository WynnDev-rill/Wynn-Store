# Dari langit ke tanah — revisi pengalaman

Permintaan: dokumenter tidak terasa seperti artikel biasa; animasi meliputi teks dan elemen visual, informasi dipadatkan menjadi infografis, galeri pengamatan diganti profil Wynn dengan avatar yang disediakan. Riset dan bibliografi tetap dapat diperiksa.

## Matriks pengalaman

| Bagian | Tujuan | Visual dan interaksi | Alternatif akses |
|---|---|---|---|
| Pembuka | Menetapkan tempat dan subjek | Judul dua skala, citra satelit sebagai lembar pengamatan, kontur dan kedalaman mengikuti gulir | Judul semantik, tautan masuk langsung, tanggal citra arsip |
| Sinyal | Membedakan deteksi dari kejadian | Deteksi → verifikasi → tangani, diagram singkat | Tiga uraian HTML dan sitasi |
| Kronologi | Membaca tiga cuplikan | Grafik 9 titik dari angka Kemenhut, tab tanggal dan nilai provinsi | Deskripsi SVG, kontrol keyboard, fallback tanpa JS |
| Lapisan | Mengantar ke mekanisme | Bidang kontur dalam perspektif, pembukaan lapisan mengikuti gulir | Teks penjelas tetap terlihat, susunan statis pada reduced motion |
| Gambut | Menjelaskan proses di bawah tanah | Empat kondisi Three.js, daun individual, air, akar, bara dan kamera penampang | Tombol kondisi dan fallback SVG |
| Peta | Membaca tempat dan waktu | Cuplikan 22/23 Oktober; data luas tahunan tersimpan dalam detail konteks | Angka HTML, tabel, CSV |
| Dampak | Menghubungkan manusia/habitat/iklim | Orbit partikel konseptual, tiga penjelasan singkat | Uraian dan sitasi, bukan data ukuran/risiko |
| Pulih | Menghubungkan respons dan pemulihan | 51 penyelamatan dengan konteks waktu/tempat, tiga langkah | Semua angka dan keterangan dalam HTML |
| Wynn | Memperkenalkan pembuat | Avatar pilihan Wynn, tipografi nama besar, kemampuan dan tautan karya | Profil singkat tanpa klaim klien, penghargaan, senioritas atau jumlah pengguna |

## Plugin, skill dan implementasi

- **3D Website Creator**, **3D Scene Architecture**, **3D Motion & Interaction**, **Responsive Accessibility**, **Visual QA Handoff** menjadi panduan struktur, gerak dan pemeriksaan.
- **Spline** dipakai untuk studi editable tujuh lapisan dan sudut cahaya. Kedua pratinjau mengembalikan frame kosong meskipun pemeriksaan geometri menyatakan objek berada dalam frame; studi tidak diterbitkan atau dimasukkan sebagai embed. Perbaikan shader/pencahayaan satu kali tidak memulihkan preview. Penyebab renderer tidak dipastikan.
- Visual lapisan final memakai CSS perspective dan SVG agar dapat diperiksa di runtime website, tanpa ketergantungan Spline. Adegan hutan nyata memakai Three.js yang sudah ada.
- Motion teks menggunakan Web Animations API: reveal kata bertahap, transform dengan easing dan clip; sumber teks tetap di DOM. Kamera dan kedalaman gambar dikendalikan satu penjadwal RAF saat gulir/resize, tanpa mengganti scroll bawaan.
- Kanopi faceted diganti ribuan bilah daun melengkung yang digabung per pohon; palet dan material lebih alami. Model, akar, air dan bara tetap ilustrasi edukatif.
- Blender tidak tersedia. Tidak ada generasi model berbayar, klaim aset Blender, atau foto kejadian buatan AI.

## Ketepatan konten

Grafik menggunakan Tengah 198→874→785, Barat 38→39→184 dan Selatan 52→99→30, masing-masing cuplikan 21/22/23 Oktober 2026 pukul 21.00 WIB, berdasarkan rilis Kemenhut 22–24 Oktober. Tidak menjumlahkan deteksi antarhari, mengubahnya menjadi hektare, atau menyamakan provinsi tak dirinci dengan nol. Data historis dan sumber ilmiah tetap tersedia.

51 orangutan merujuk penyelamatan Ketapang/Kayong Utara, 6 Agustus–pertengahan Oktober 2026. Avatar dikirim oleh Wynn untuk identitas profil; tidak diklaim sebagai foto wajah atau karya ilustrasi Wynn. Profil membatasi pencapaian pada web dan aplikasi Android yang dapat digunakan serta proses desain sampai publikasi.

## Perbandingan dan batas

Dibanding versi sebelumnya, perjalanan kini menghubungkan tipografi kinetik, foto sebagai elemen spasial, diagram proses, grafik waktu, lapisan perspektif, diorama, partikel dan identitas pembuat. Hubungan desain–motion–engineering dari OHZI menjadi pembanding, bukan sumber aset atau klaim kesetaraan. Ini masih dokumenter dengan satu adegan 3D utama, bukan dunia 3D fotorealistis yang bisa dijelajahi penuh.

Lihat [QA.md](QA.md) untuk hasil aktual dan screenshot; angka peningkatan persentase sebelumnya adalah perkiraan artistik, bukan hasil ukur rilis.
