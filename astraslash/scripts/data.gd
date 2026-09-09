extends RefCounted
const HEROES = {
"rei": {"name":"REI", "title":"Penjahit Cahaya", "hp":150.0, "speed":7.8, "color":Color("78e7ed"), "desc":"Satu pedang. Seribu garis fajar.\nRei merajut kombo udara dan membelah celah di antara serangan musuh.", "skill":"LINTAS FAJAR  /  tebasan menembus barisan", "ult":"SERIBU PAGI  /  empat sayatan seluruh arena"},
"kael": {"name":"KAEL", "title":"Pemburu Orbit", "hp":125.0, "speed":8.2, "color":Color("eacb82"), "desc":"Bintang selalu meninggalkan jejak.\nKael menjaga jarak dengan busur cahaya, panah penembus, dan hujan meteor.", "skill":"RASI PEMBURU  /  lima panah pencari", "ult":"LANGIT RUNTUH  /  delapan meteor astral"}}
const ZONES = [
{"name":"JEMBATAN ARUNIKA","subtitle":"Di atas kota yang lupa cara terbang","color":Color("73e3ed"),"sky":Color("16304e"),"boss":"warden","boss_name":"VAHL · PENJAGA FAJAR","music":"arunika"},
{"name":"KEBUN KACA","subtitle":"Setiap kelopak menyimpan sebuah ingatan","color":Color("c89ff7"),"sky":Color("302246"),"boss":"cantor","boss_name":"SYRA · PENENUN SENYAP","music":"glass_garden"},
{"name":"INTI GERHANA","subtitle":"Di sinilah malam mulai bernapas","color":Color("ff876c"),"sky":Color("291c31"),"boss":"regent","boss_name":"ORVAN · RAJA TANPA PAGI","music":"eclipse_core"}]
const RELICS = {
"echo":["GEMA KETIGA","Kombo ketiga mengulang sayatannya setelah sesaat, dengan 60% kekuatan.","PEDANG"],
"chain":["BENANG PETIR","Setiap tiga hit menjalar ke dua musuh terdekat.","RANTAI"],
"dash_burst":["JEJAK NOVA","Ujung dodge meledak. Lewati musuh untuk menyerang dari belakang.","GERAK"],
"air_blade":["SAYAP TAJAM","Serangan udara melepaskan gelombang penembus.","UDARA"],
"orbit":["BULAN KEMBAR","Dua satelit mengorbit dan melukai musuh yang mendekat.","ORBIT"],
"frost":["MUSIM DIAM","Launcher membekukan musuh biasa; boss melambat.","KENDALI"],
"vampire":["JANJI MERAH","Hit kombo ketiga memulihkan 3 Vitalitas.","PULIH"],
"perfect":["DETIK ABADI","Perfect dodge memulihkan 8 Vitalitas dan mengisi ulang skill.","DODGE"],
"meteor":["HUJAN KECIL","Skill aktif memanggil tiga meteor tambahan.","SKILL"],
"gravity":["JANTUNG GRAVITASI","Skill aktif menarik musuh biasa sebelum menghantam.","KENDALI"],
"triple":["RASI TERBELAH","Skill aktif menembakkan tiga proyektil tambahan.","SKILL"],
"pierce":["UJUNG CAKRAWALA","Semua proyektil menembus musuh. Tebasan Rei juga mengirim bilah cahaya.","TEMBUS"],
"air_step":["TANGGA LANGIT","Lompatan ketiga; setiap air jump menghasilkan ledakan kecil.","UDARA"],
"thunderfall":["GUNTUR JATUH","Finisher udara mengirim dua gelombang di permukaan tanah.","FINISHER"],
"volatile":["SISA SUPERNOVA","Musuh yang mati meledak dan mencederai musuh di sekitarnya.","LEDAKAN"],
"revive":["API KEDUA","Sekali per run, bangkit dari pukulan fatal dengan 45% Vitalitas.","HIDUP"],
"tempo":["IRAMA TANPA AKHIR","Setiap hit mempercepat isi ulang skill. Kombo 20 hit memberi satu pemulihan.","TEMPO"],
"overdrive":["SETELAH FAJAR","Setelah ultimate, serangan 35% lebih cepat selama 8 detik.","ULTIMATE"}]
const META = {"vitality":["TENUN TUBUH","+12 Vitalitas awal",45,5],"surge":["SUMBU BINTANG","+10 energi awal",50,4],"keepsake":["BEKAL PULANG","+1 pemulihan awal",80,2]}
const SETTINGS = {"preset":2,"scale":0.85,"fps":60,"shadows":true,"post":true,"effects":1.0,"music":0.60,"sfx":0.80,"shake":0.65,"touch":true,"numbers":true}
static func level_for(node):return mini(int(node)/3,2)
static func max_hp(hero,meta):return HEROES[hero].hp+12*int(meta.get("vitality",0))
static func choices(seed_value,node,owned,forge=false):
	var rng=RandomNumberGenerator.new();rng.seed=int(seed_value)+int(node)*797
	var pool=RELICS.keys();var result=[]
	for k in owned:pool.erase(k)
	if forge:
		var offensive=pool.filter(func(k):return RELICS[k][2] in ["PEDANG","SKILL","TEMBUS","LEDAKAN","RANTAI","ULTIMATE"])
		if not offensive.is_empty():
			var first=offensive[rng.randi_range(0,offensive.size()-1)];result.append(first);pool.erase(first)
	while result.size()<3 and not pool.is_empty():
		var i=rng.randi_range(0,pool.size()-1);result.append(pool[i]);pool.remove_at(i)
	return result
