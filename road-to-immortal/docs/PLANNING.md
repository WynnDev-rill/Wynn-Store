# Tier List, Rancangan Hero dan Tim — 1.1.0

## Data dan tier

MLBB.GG ditinjau sebagai referensi pada 7 September 2026. UI tidak disalin, dan
aplikasi tidak bergantung pada endpoint privat atau prediksi AI situs itu.
Tier menggunakan MetaSlice tujuh hari yang sudah diperbarui pipeline gratis
enam jam sekali: MLBB GMS, rank all/epic/legend/mythic/honor/glory. Tidak ada
fallback ke rank lain secara diam-diam. Katalog terbaru otomatis menghasilkan
tier dan pilihan hero baru tanpa update APK.

Bobot editorial: 50 + (win−50)×pick/(pick+0.5) + 1.4×ln(1+pick) + 0.025×ban.
Pick rendah diredam karena sumber tidak menyediakan jumlah pertandingan.
Urutan per lane dibagi kuantil SS 10%, S 20%, A 30%, B 25%, C 15%.
Lane adalah filter metadata hero; statistik bukan statistik terpisah per lane.
UI menyebut Tier olahan, menampilkan usia revisi sumber, dan menjelaskan batas
ini pada tombol informasi. Tidak ada daftar hero/tier yang di-hardcode.

## Penyimpanan

DataStore `immortal` tetap sama. Satu key tambahan `hero_pools_v1` menyimpan
JSON map lane → ordered hero IDs. Tidak mengubah key rank, bintang, season,
tema atau favorit. Semua mutasi berada dalam transaksi DataStore edit; maksimal
10 ID unik positif per lane. Move ke lane penuh tidak menghapus pilihan asal.
Hero yang sementara hilang dari katalog tetap tersimpan sebagai ID dan bisa
dihapus/dipindahkan pengguna. Backup Android sudah mencakup direktori DataStore.
Kegagalan decode pool tidak mengosongkan pengaturan lain. Format v1 harus tetap
dibaca pada update selanjutnya; perubahan schema memerlukan migrasi eksplisit.

## Rekomendasi Tim

Bounded beam mencari susunan hero unik dengan assignment lane yang valid.
Pilihan Duo/Trio/Squad, hero awal opsional, satu lawan opsional. Pencarian
berjalan pada Dispatchers.Default, mempertahankan seed, mengecualikan lawan,
dan menawarkan variasi susunan. Bobot meta di atas ditambah rata-rata delta
sinergi ×0.5, advantage matchup ×0.5, dan bonus/penalti cakupan role untuk squad.
Delta lawan pada sumber bertanda berlawanan dengan advantage hero sendiri.
Skor tampilan dibatasi 0–100; bukan probabilitas kemenangan atau combo win rate.
Ketiadaan edge sinergi disebut singkat; tidak dibuat angka win rate pengganti.
Role hanya proksi komposisi (frontline/mage/sustained), bukan pengukuran damage.

## Desain dan verifikasi

Panduan design-system UI UX Pro Max digunakan: token semantik Material,
target sentuh 48dp, portrait, state lokal saveable, lazy list keyed, animasi
perpindahan pool, dan pemisahan perhitungan dari UI. Counter item dipindah ke
Jelajah; layar Draft lama dihapus. SourceLine dibuat singkat dan rincian teknis
dipusatkan pada dokumen ini. Evidence final dicatat di QA.md setelah CI selesai.
