extends RefCounted
## Authored content for Region 1. Positions are world-space metres (X, Z).

const TITLE = "Astra World"
const REGION = "Lembah Aeralis"
const ITEMS = {
 "wood": {"name":"Kayu Seruling", "desc":"Seratnya menyimpan nada angin. Bahan gagang dan jimat.", "color":"bd986c"},
 "crystal": {"name":"Giok Embun", "desc":"Kristal yang tumbuh di tempat kabut menyentuh tanah.", "color":"7bddca"},
 "herb": {"name":"Daun Fajar", "desc":"Daun hangat untuk meracik bekal pemulihan.", "color":"b8d773"},
 "echo": {"name":"Serpih Gema", "desc":"Ingatan yang dilepaskan penjaga. Dipakai dalam penempaan.", "color":"f3cd86"},
 "letter": {"name":"Surat yang Tertinggal", "desc":"Surat pengelana. Mira di desa masih menunggu tiga lembar ini.", "color":"eee3c9"},
 "potion": {"name":"Embun Pulih", "desc":"Memulihkan 55 kesehatan. Gunakan lewat tombol bekal.", "color":"e4a8a0"},
 "sword0": {"name":"Bilah Pengelana", "desc":"Pedang perjalanan Nara. Serangan +0.", "color":"a3b5bd"},
 "sword1": {"name":"Bilah Penala", "desc":"Serangan +8. Gioknya bergetar dekat lonceng Aeralis.", "color":"83dfc4"},
 "sword2": {"name":"Bilah Fajar", "desc":"Serangan +18. Ditempa dari gema yang kembali jernih.", "color":"f3cc82"},
 "charm": {"name":"Jimat Akar", "desc":"Kesehatan maksimum +30. Hadiah dari akar yang akhirnya beristirahat.", "color":"b1cf90"},
 "mantle": {"name":"Mantel Angin", "desc":"Pertahanan +3. Kain tebal yang tetap ringan ketika basah.", "color":"a3d9d4"}
}
const RECIPES = [
 {"id":"sword1", "cost":{"wood":3,"crystal":3}, "desc":"Bilah yang dapat menyelaraskan mercusuar."},
 {"id":"potion", "cost":{"herb":2}, "desc":"Satu botol untuk perjalanan berikutnya."},
 {"id":"mantle", "cost":{"wood":5,"herb":4}, "desc":"Kurangi kerusakan yang diterima."},
 {"id":"sword2", "cost":{"wood":5,"crystal":6,"echo":5}, "desc":"Bilah yang kuat untuk menghadapi Aru."}
]
const LANDMARKS = [
 {"id":"village", "name":"Desa Teralun", "pos":Vector2(0,48), "short":"Rumah di antara angin"},
 {"id":"west", "name":"Kebun Seribu Lonceng", "pos":Vector2(-63,6), "short":"Gema pertama · akar"},
 {"id":"east", "name":"Kolam Cermin", "pos":Vector2(61,0), "short":"Gema kedua · hujan"},
 {"id":"north", "name":"Arsip Langit", "pos":Vector2(-23,-58), "short":"Gema ketiga · ingatan"},
 {"id":"boss", "name":"Mahkota Sunyi", "pos":Vector2(27,-92), "short":"Tempat angin berhenti"},
 {"id":"secret", "name":"Taman yang Terlupa", "pos":Vector2(-88,-40), "short":"Dengarkan: akar, hujan, fajar"},
 {"id":"coast", "name":"Tebing Kertas", "pos":Vector2(67,69), "short":"Surat-surat di ujung lembah"}
]
const BEACONS = ["west","east","north"]
const BEACON_TEXT = {
 "west":"Satu nada kembali. Akar-akar teringat cara menumbuhkan daun. Kain layarmu kini dapat menangkap angin. Tahan Lompat saat di udara untuk melayang.",
 "east":"Air kembali memantulkan langit, bukan masa lalu. Di bawah gemericiknya, terdengar seorang penjaga memanggil pulang.",
 "north":"Arsip tidak menyimpan kemenangan. Ia menyimpan nama-nama mereka yang pulang. Aru telah menunggu terlalu lama."
}
const DIALOGUE = {
 "intro":[["Nara","Aku mengikuti suara lonceng ini selama tiga hari. Di sini, bahkan angin terdengar seperti sedang menahan napas."],["Lonceng saku","Tiga nada hilang. Satu penjaga lupa mengapa ia berjaga."],["Nara","Kalau begitu, kita mulai dari rumah-rumah yang masih menyalakan lampu."]],
 "ilya_start":[["Ilya · penjaga desa","Aeralis tidak sedang tenggelam. Ia sedang dilupakan. Setiap mercusuar yang padam membawa pergi sedikit ingatan kami."],["Nara","Loncengku berbunyi sejak aku menginjak pantai. Apa yang dicari suara ini?"],["Ilya · penjaga desa","Tiga nada: akar, hujan, dan ingatan. Temui Sava. Buat Bilah Penala dari tiga Kayu Seruling dan tiga Giok Embun. Lalu bangunkan mercusuar kami."],["Ilya · penjaga desa","Aru menjaga Mahkota Sunyi di utara. Ia bukan musuh kami. Tetapi kabut telah membuatnya lupa."]],
 "sava":[["Sava · penempa","Bilah yang baik tidak hanya memotong. Ia tahu kapan harus berhenti."],["Sava · penempa","Kayu Seruling tumbuh di sekitar jalan desa. Giok Embun bersinar di batu-batu. Bawa masing-masing tiga ke tungku di sampingku."],["Sava · penempa","Penjaga menunjukkan serangannya sebelum menerjang. Hindari lingkaran merah; serang setelah mereka kehilangan keseimbangan."]],
 "mira":[["Mira · pengirim surat","Kabut menjatuhkan surat-surat ayahku di seluruh lembah. Tiga lembar. Aku tak butuh harta di petinya—hanya kabar bahwa ia pernah sampai di sana."],["Mira · pengirim surat","Carilah di Tebing Kertas, dekat Kolam Cermin, dan di balik reruntuhan Arsip Langit."]],
 "mira_done":[["Mira · pengirim surat","Ini tulisannya. Ia tidak tersesat. Ia memilih menetap untuk merawat lonceng terakhir."],["Mira · pengirim surat","Terima kasih sudah membawa pulang sesuatu yang tak bisa ditempa. Ambil giok ini. Ayah pasti akan menyukaimu."]],
 "lore":[["Catatan penala","Tiga batu bernyanyi. Akar meminum hujan; hujan menyambut fajar. Sentuh mereka mengikuti urutan kehidupan."],["Nara","Bukan kekuatan. Mendengarkan."]],
 "ending":[["Ilya · penjaga desa","Aku ingat namanya sekarang. Aru. Ia menahan kabut agar kami bisa pergi, lalu lupa bahwa kami sudah pulang."],["Nara","Ia tidak meminta dikalahkan. Hanya ingin mendengar lonceng desa sekali lagi."],["Ilya · penjaga desa","Maka malam ini kita bunyikan semuanya. Untuk orang yang menunggu. Untuk orang yang akhirnya pulang."],["Nara","Besok angin akan bergerak lagi. Hari ini, aku ingin mendengarkannya dari sini."]]
}

static func landmark(id:String) -> Dictionary:
 for l in LANDMARKS:
  if l.id == id: return l
 return LANDMARKS[0]

static func quest(s:Dictionary) -> Dictionary:
 if s.get("ending",false): return {"title":"Angin yang pulang", "body":"Aeralis kembali bernyanyi. Jelajahi rahasia yang tersisa.", "pos":Vector2(0,48)}
 if s.get("boss",false): return {"title":"Pulang ke Teralun", "body":"Sampaikan kepada Ilya bahwa Aru telah beristirahat.", "pos":Vector2(0,48)}
 if not s.get("met_ilya",false): return {"title":"Suara di antara kabut", "body":"Temui Ilya di pusat Desa Teralun.", "pos":Vector2(0,48)}
 if not s.get("forged",false): return {"title":"Bilah yang mendengarkan", "body":"Kumpulkan 3 kayu dan 3 giok, lalu tempa Bilah Penala di desa.", "pos":Vector2(10,47)}
 for id in BEACONS:
  if not id in s.get("beacons",[]): return {"title":"Tiga nada Aeralis · %d/3" % s.get("beacons",[]).size(), "body":"Bersihkan penjaga dan selaraskan " + landmark(id).name + ".", "pos":landmark(id).pos}
 return {"title":"Penjaga yang terlupa", "body":"Hadapi Aru di Mahkota Sunyi. Hindari gelombangnya, lalu balas saat ia berhenti.", "pos":Vector2(27,-92)}
