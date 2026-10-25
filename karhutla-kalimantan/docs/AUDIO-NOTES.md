# Suara suasana

Suara api dan angin dibuat secara prosedural melalui Web Audio API: noise cokelat terfilter, lapisan udara, modulasi gain lambat, dan crackle pendek dengan posisi stereo. Ini sound design sintetis orisinal, bukan rekaman kejadian karhutla atau dokumentasi lapangan.

Graf audio baru dibuat setelah pengunjung menekan kontrol suara. Volume naik/turun bertahap; kompresor membatasi transien. Saat kontrol mati atau tab tersembunyi, penjadwal crackle dihentikan dan AudioContext disuspend. Tab kembali terdengar hanya bila pengunjung sebelumnya memilih suara aktif. Tidak ada aset audio eksternal, unggahan, analitik atau API berbayar.

Pemeriksaan otomatis memverifikasi tipe dan state kontrol. Kesesuaian keras-lembut di speaker HP fisik belum diukur; gain awal sengaja rendah dan suara dapat dimatikan kapan pun.

Validation: strict standalone TypeScript check passed. An isolated Chromium 133 fixture verified zero AudioContext constructions before the control gesture, a running context and nonzero waveform after activation (one sample peak0.0367/RMS0.0154), and suspended context after switching off. No JavaScript errors were recorded. These amplitude samples are implementation evidence, not calibrated loudness or a listening test on phone speakers.
