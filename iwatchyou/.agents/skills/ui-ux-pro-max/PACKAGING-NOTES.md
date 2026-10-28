# ui-ux-pro-max — paket sumber asli

Sumber: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/tree/56ea9cf174ed86786187daa9de0e6247cb0747e4/.claude/skills/ui-ux-pro-max

Commit: 56ea9cf174ed86786187daa9de0e6247cb0747e4

Lisensi: MIT. Teks lisensi dan pemberitahuan upstream dipertahankan.

Seluruh 73 file terlacak dalam direktori skill upstream disertakan, identik byte demi byte. SKILL.md tidak diubah, dipotong, diterjemahkan, atau diganti. File tambahan hanya untuk lisensi, provenance, dan catatan kompatibilitas.

Struktur ZIP: satu folder skill di tingkat atas, tepat satu SKILL.md. YAML name/description, CRC ZIP, path aman, jumlah/ukuran file, dan SHA-256 file upstream diperiksa. Ini verifikasi lokal; penerimaan aktual melalui pengunggah chatgpt.com/skills belum diuji. Batas API yang dijadikan pemeriksaan konservatif bukan jaminan batas antarmuka web: ZIP <50 MB, file <=500, setiap file <25 MB.

Keamanan dasar: file executable native, symlink, path traversal, pola kredensial, instruksi pengiriman data, serta pemanggilan proses/jaringan diperiksa. Pemeriksaan statis dasar bukan audit keamanan menyeluruh. Tidak ada kode upstream yang dijalankan pada saat membuka ZIP.

Yang tidak disertakan: riwayat .git, skill lain, workflow CI, serta dependensi pembangunan repositori yang tidak diperlukan oleh skill ini. Tidak ada node_modules, virtualenv, atau Python interpreter yang dibundel.

Seluruh data CSV/JSON, references, scripts, unit tests, dan fixtures dari direktori skill asli dipertahankan. Mesin pencarian memakai Python standard library; tidak memerlukan paket pip. Python tidak dibundel.

KOMPATIBILITAS: SKILL.md asli memakai ${CLAUDE_PLUGIN_ROOT}/.claude/skills/ui-ux-pro-max/scripts/search.py. Variabel CLAUDE_PLUGIN_ROOT tidak dijamin ada di ChatGPT. Saat menggunakan skill, jalankan Python dengan path absolut scripts/search.py di folder skill terpasang. Isi instruksi asli tetap utuh. Contoh portabel: python3 "<folder-skill-terpasang>/scripts/search.py" "keyboard focus modal" --domain ux.

Pencarian lokal tidak memerlukan internet. --persist menulis design-system ke direktori proyek yang ditentukan; --force dapat mengganti file yang sudah ada. Gunakan --output-dir secara eksplisit. Tes yang mengharapkan layout repositori/alat pemutakhiran katalog tidak dijamin berjalan dari paket standalone; tes itu tetap disertakan.

Katalog Google Fonts dan Phosphor adalah data rekomendasi/metadata; bukan seluruh binary font, SVG ikon, GSAP, React, atau 22 framework tersebut. Paket/library aplikasi contoh dipasang pada proyek yang memerlukannya, tidak otomatis lewat unggah skill. Metadata lisensi font dan provenance asli, serta lisensi Phosphor, disertakan.
