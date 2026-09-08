# Verifikasi Road to Immortal 1.1.0

[Run 34173311581](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/34173311581)
selesai sukses pada 8 September 2026. APK optimized diuji sebagai aplikasi
terpasang, dengan UIAutomator berjalan dalam host terpisah dan animasi aktif.

## Identitas APK

- Source: `709f8e776a99a1469cbc742d7e3c20c3f125d5c3`.
- Paket `id.wynn.roadtoimmortal`, versi `1.1.0`, versionCode `3`.
- Minimum Android 8.0/API 26; target API 36.
- Unduhan `Road-to-Immortal-1.1.0.apk`, 2.410.202 byte.
- SHA-256 `0a88d2c2fe4c83e85bb8aed0c932aade70f779cbc9153f0391011c4f44c7219c`.
- Sertifikat SHA-256 `c73f2354fa53afac08783feec25d6a19da14703b59fbfb82c381177458bb681d`.
- Signature v2 diverifikasi oleh apksigner. Kunci development sama dengan 1.0.1;
  ini bukan production signing. File unduhan identik byte dengan APK build asli.

Artifact **Road-to-Immortal**, ID `10036520154`, memuat APK di
`dist/Road-to-Immortal.apk`, laporan unit/lint, screenshot, hierarchy UI,
logcat, signature, hash, source commit, dan metrik emulator. Retensi Actions
30 hari; APK juga diserahkan sebagai berkas unduhan dalam percakapan.

## Hasil

| Pemeriksaan | Hasil |
| --- | --- |
| Kontrak pipeline Python | 10 tes lolos |
| Unit Android | 26 tes lolos; 0 gagal, 0 dilewati |
| Lint release | 0 error, 20 warning |
| Build, signature, instalasi dan startup | Lolos |
| UIAutomator APK optimized | 5 skenario lolos, 217,882 detik |
| Crash fatal atau ANR aplikasi dalam logcat | Tidak ditemukan |

Unit mencakup Road (9), repository/cache (6), draft/search dasar (4), tier/pool/
Tim (6), dan migrasi Preferences (1). Migrasi memakai file DataStore dengan key
versi lama, lalu menguji penambahan bersamaan: kapasitas tetap 10, rank 37★,
tema gelap dan favorit lama tetap utuh. Unit lain menguji perpindahan ke lane
penuh tanpa kehilangan hero asal, urutan tersimpan, input rusak, cakupan rank
yang tepat, seed/lawan, lane unik, dan arah delta counter.

Warning lint yang tersisa mencakup versi dependency/API lebih baru, urutan
parameter Modifier, pembacaan ukuran jendela, dan resource qualifier. Tidak
disembunyikan; laporan lengkap disertakan pada artifact.

## Alur pengguna

1. **Pool penuh dan partner:** mengisi EXP hingga 10 hero melalui tombol nyata,
   melihat keadaan penuh, lalu membuka detail Miya dan Cari partner.
2. **Jelajah:** mencari hero di landscape, memeriksa Build/Skill/Matchup,
   detail Antique Cuirass, emblem dan spell.
3. **Tier, pool dan Tim:** Gold/Mythic, pencarian Miya, tambah langsung dari tier,
   tambah Layla dari Jelajah, naikkan urutan, pindah ke Mid, buka detail dan
   kembali dengan konteks lane utuh, instalasi `pm install -r`, restart, hapus,
   pilih seed Miya, buka Trio/Squad, lalu Counter item.
4. **Road:** pilih Mythic, tambah lima bintang, periksa 95 bintang tersisa,
   tema gelap, font 150%, rotasi, dan persistensi rank setelah restart.
5. **Meta dan jaringan:** ubah rank/sort, pilih hero dan lawan tanpa duplikasi,
   pindah tab, refresh online, putuskan default network, refresh offline,
   katalog offline setelah restart, dan pemulihan online.

Screenshot diperiksa langsung selama iterasi: Tier List penuh dan hasil search,
pool setelah reorder/pindah/penuh, hero dan build, Trio, Squad, dan jalur
partner. Portrait, label lane, status pilihan, urutan, kapasitas dan navigasi
terbaca. Bukti utama: `18a`, `18`, `19`, `20`, `21`, `22`, `24`, `25` di
`qa/output`. Tema, teks besar, landscape serta jaringan juga diuji dan direkam.

Kegagalan tes sebelumnya diperbaiki pada penggerak UI: Back yang tidak diperlukan,
teks terpotong 1 piksel yang dianggap terlihat, dan node aksesibilitas lama saat
animasi/rekomposisi. Retry dibatasi pada StaleObjectException sebelum aksi;
assertion produk tetap ketat. Animasi aplikasi tidak dimatikan. Perbaikan produk
yang ikut diverifikasi: konteks pool dipertahankan saat kembali dari detail,
lane Home diteruskan ke editor, dan hero awal Tim tidak boleh menjadi lawan.

## Data dan batas verifikasi

Pipeline otomatis [34163610191](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/34163610191)
sukses. Bundel `2026-09-07T21:34:19Z` berisi 133 hero, 187 identitas item termasuk
variasi/metadata, 33 emblem/talent, 12 spell, 399 build tiga item inti, dan enam
cakupan rank. Statistik direvisi sumber `2026-09-07T14:15:01Z`; refresh online
nyata juga lolos. Tier merupakan olahan win/pick/ban per rank dengan filter lane;
angka sumber bukan statistik terpisah per lane. Skor Tim bukan combo win rate.
Metode dan migrasi dijelaskan di [PLANNING.md](PLANNING.md).

Emulator Android 15/API 35, Pixel 6 x86_64, software GPU SwiftShader. Cold launch
2,445 detik; PSS setelah alur pool/Tim sekitar 89,3 MiB. Laporan gfxinfo pada
alur tersebut menunjukkan 92,06% janky frames di emulator software. Angka ini
tidak membuktikan performa ponsel fisik; klaim 60 FPS tidak dibuat. Poco F6,
matriks Android 8–16, TalkBack lengkap, dan penggunaan jangka panjang belum
diuji langsung. Pemeriksaan aksesibilitas mencakup hierarchy kontrol, teks besar,
rotasi dan target sentuh, bukan sertifikasi menyeluruh.
