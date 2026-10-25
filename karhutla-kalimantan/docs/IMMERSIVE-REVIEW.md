# Ruang di balik asap — tinjauan implementasi

Rilis 25 Oktober 2026. Fokus pekerjaan ini: mengubah pengalaman baca menjadi perjalanan spasial yang tetap memiliki sumber, tanggal dan kontrol yang jelas.

## Perubahan yang dapat diperiksa

- Bab **Ruang**: hutan perspektif Three.js dengan tiga keadaan, kamera maju mengikuti gulir, pandangan horizontal sampai sekitar ±66°, input drag/panah dan orientasi pasif. Tombol Jelajahi ruang menyingkirkan judul agar bentang dapat dilihat; Escape mengembalikannya.
- Batang, akar, serasah, 19.200 bilah daun instanced, kabut, berkas cahaya, nyala shader, bara dan genangan berpadu sebagai ilustrasi. Tiga prop Kenney CC0 disimpan lokal; asal dan checksum tersedia di FOREST-ASSETS.json. Ilustrasi tidak mewakili satu lokasi atau kejadian spesifik.
- Transisi kabut tipis pada batas bab mengikuti posisi gulir; koreografi kata dan lapisan kontur tetap terhubung dengan kemajuan baca. Gulir native dipertahankan.
- Kartu Wynn tanpa tulisan di atas gambar, kaca/foil merespons input. Biografi tetap di samping kartu pada layar 320 px maupun desktop. Tombol sensor dan status aktivasi dihapus.
- Suasana api/angin stereo disintesis lokal melalui Web Audio; tidak diunduh dari rekaman bencana. Ikon suara mengendalikan pemutaran. Browser memerlukan gesture untuk memulai audio.
- Renderer baru dimuat saat ruang masuk viewport; DPR mobile dibatasi 1,25, loop dibatasi target 30 FPS dan berhenti di luar layar/tab tersembunyi. Ini batas penjadwalan, bukan klaim 30 FPS pada setiap HP. Reduced motion mematikan gerak otomatis. Fallback SVG dan kontrol HTML tetap tersedia tanpa WebGL.

## Pembanding dan penilaian

Peninjauan sepuluh referensi beserta batas akses dicatat dalam [DESIGN-REFERENCES.md](DESIGN-REFERENCES.md). OHZI sempat tertahan pada loader di lingkungan browser ini; tidak ada dasar untuk mengaku telah melakukan benchmark langsung penuh atau menyatakan parity.

| Aspek | Standar pembanding | Bukti implementasi | Batas penilaian |
|---|---|---|---|
| Ruang dan input | OHZI: ruang menjadi medium eksplorasi | Kamera perspektif, perubahan kondisi, drag/panah, mode jelajah | Hutan bergaya ilustratif; belum setara detail material, aset khusus dan lighting studio OHZI |
| Koreografi | Minh Pham: tipografi dan respons input terarah | Reveal kata, kontur kedalaman, transisi kabut, kartu optik | Tidak meniru identitas atau karya pembanding; selera visual bukan skor objektif |
| Atmosfer | Firestorm: lingkungan, suara dan narasi saling mendukung | Kabut/bara/cahaya dan audio stereo opsional | Audio sintetis, bukan dokumentasi lapangan; tidak ada klaim pemutaran video baru |
| Integritas | The Pudding/NASA: definisi dan sumber dekat isi | Kronologi bertanggal, peta/tabel, sitasi dan sumber terpisah | Kurasi sampai tanggal rilis, bukan feed langsung |
| Ketahanan | Pengalaman tetap dapat dipakai di mobile | Uji responsive, fallback WebGL/no-JS, reduced motion, axe | Pengujian lab, bukan perangkat Android fisik atau audit manusia lengkap |

Hasilnya naik dari halaman editorial dengan objek terpisah menjadi dokumenter dengan satu ruang yang dapat dijelajahi. Target fungsional dan ketahanan dapat diuji; klaim “setara OHZI” dan persentase peningkatan estetika tidak dapat dibuktikan dari pengujian ini.

## Audit visual dan revisi

Screenshot tiga keadaan dan profil mobile diperiksa setelah render Chromium/SwiftShader. Kanopi solid yang tampak seperti payung diganti bilah daun dengan celah. Tepi genangan persegi diganti mask organik. Prop yang terlalu hijau dicocokkan ke arang. Tombol suara mobile diringkas agar tidak menutupi narasi. Biografi diubah menjadi dua kolom pada mobile. Mode jelajah ditambahkan untuk mengurangi teks ketika melihat ruang.

Sensor fisik belum diuji. Event orientasi simulasi membuktikan logika input otomatis tanpa tombol; browser yang meminta izin sensor terpisah tetap menyediakan sentuhan/keyboard. Tidak mencoba melewati aturan izin browser.

Bukti pengujian: [QA.md](QA.md), [BROWSER-QA.json](BROWSER-QA.json), [EXPERIENCE-QA.json](EXPERIENCE-QA.json), [AUDIO-NOTES.md](AUDIO-NOTES.md).
