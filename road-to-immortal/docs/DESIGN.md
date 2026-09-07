# Road to Immortal

Produk native Indonesia untuk tiga keputusan: berapa bintang bersih hari ini,
hero mana yang layak dipilih, dan bagaimana menghadapi draft lawan.

## Sistem visual

UI UX Pro Max digunakan pada 2026-09-05 dari repo
nextlevelbuilder/ui-ux-pro-max-skill. Pencarian pertama gaming menghasilkan 3D
berat yang tidak cocok untuk utilitas ini; pencarian terarah mobile utility
memilih Minimalism & Swiss Style. Panduan Compose, semantics, 48dp targets,
reduced motion, safe areas, dan pro-rules diterapkan. Palet/typography generik
hasil pencarian tidak disalin.

- Identitas: jalur menanjak mengapit bintang empat sisi, ikon vektor asli.
- Indigo #4E51D8; gold #916615 pada light dan #E7BD70 pada dark.
- Permukaan light #F8F8FC / white; dark #11121C / #1D1E2C.
- Roboto platform, angka tabular, headline 30sp, daily target 64sp.
- 4/8dp rhythm; screen gutter 20dp; cards 24dp radius; controls 48dp minimum.
- Lima tujuan navigasi: Perjalanan, Jelajah, Meta, Tier List, Tim. Pengaturan di app bar. Rancangan Hero dibuka dari Perjalanan; detail hero menyediakan Rancang hero dan Cari partner.
- State: data cache tampil seketika, refresh tidak mengosongkan layar; sumber
  dan tanggal tersedia melalui indikator data, tidak memenuhi layar utama.
- Angka berubah via AnimatedContent; sheet memilih rank, haptic saat pilihan
  berubah; Compose mengikuti skala animasi OS. Tidak ada confetti berulang,
  streak, histori pertandingan, atau progress diary yang tidak diminta.
- Semua hero asli menggunakan artwork publik sumber; identitas aplikasi tidak
  menggunakan logo MLBB atau meniru ikon aplikasi lain.

## Batas makna data

Meta ialah statistik agregat per rank dan jendela, bukan real-time. Rekomendasi
draft adalah skor transparan berbasis statistik + cakupan lane, bukan peluang
menang tim. Win rate tidak digandakan menjadi matchup rate jika sumber hanya
memberikan perubahan terhadap baseline. Ketiadaan data selalu dibedakan dari 0.
Tanggal season yang hanya diketahui komunitas ditampilkan sebagai perkiraan.
