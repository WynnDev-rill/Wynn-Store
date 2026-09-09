extends CanvasLayer
const D=preload("res://scripts/data.gd")
var g
var root
var screen
var hud_view
var font=preload("res://assets/fonts/Barlow-Regular.ttf")
var bold=preload("res://assets/fonts/BarlowCondensed-Bold.ttf")
var white=Color("f3f0e5")
var cyan=Color("77e8ed")
var muted=Color("a7b6c9")
var size=Vector2(1280,720)
func setup(game):
	g=game;layer=2;root=Control.new();add_child(root);root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT);root.mouse_filter=Control.MOUSE_FILTER_IGNORE
func reset():
	if screen:screen.queue_free()
	size=g.get_viewport().get_visible_rect().size;screen=Control.new();root.add_child(screen);screen.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT);screen.mouse_filter=Control.MOUSE_FILTER_IGNORE;hud_view=null
func panel(p,s,color=Color("0c1726"),border=Color("31455a")):
	var n=Panel.new();n.position=p;n.size=s;var style=StyleBoxFlat.new();style.bg_color=color;style.border_color=border;style.set_border_width_all(1);style.set_corner_radius_all(5);n.add_theme_stylebox_override("panel",style);screen.add_child(n);n.mouse_filter=Control.MOUSE_FILTER_IGNORE;return n
func text(value,p,s,fs=22,color=Color("f3f0e5"),heading=false):
	var l=Label.new();l.text=value;l.position=p;l.size=s;l.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART;l.add_theme_font_override("font",bold if heading else font);l.add_theme_font_size_override("font_size",fs);l.add_theme_color_override("font_color",color);l.mouse_filter=Control.MOUSE_FILTER_IGNORE;screen.add_child(l);return l
func button(value,p,s,callback,primary=false,disabled=false):
	var b=Button.new();b.text=value;b.position=p;b.size=s;b.disabled=disabled;b.add_theme_font_override("font",bold);b.add_theme_font_size_override("font_size",23);b.add_theme_color_override("font_color",Color("09222c") if primary else white);b.add_theme_color_override("font_hover_color",Color("09222c") if primary else Color.WHITE)
	for state in ["normal","hover","pressed","disabled","focus"]:
		var st=StyleBoxFlat.new();st.bg_color=(cyan if primary else Color("102335"));st.border_color=cyan if primary else Color("456078");st.set_border_width_all(1);st.set_corner_radius_all(5)
		if state=="hover":st.bg_color=st.bg_color.lightened(.14)
		if state=="pressed":st.bg_color=st.bg_color.darkened(.15)
		if state=="disabled":st.bg_color=Color("14202d");st.border_color=Color("293443")
		if state=="focus":st.bg_color=Color.TRANSPARENT;st.border_color=Color("f1dca2");st.set_border_width_all(2)
		b.add_theme_stylebox_override(state,st)
	b.pressed.connect(func():g.sound.play("ui",1,.6);callback.call());screen.add_child(b);return b
func shade(alpha=.88):panel(Vector2.ZERO,size,Color(.025,.047,.082,alpha),Color.TRANSPARENT)
func eyebrow(value,p):text(value,p,Vector2(760,28),17,cyan,true)
func back(callback):button("‹  KEMBALI",Vector2(34,27),Vector2(154,43),callback)
func title():
	reset();var image=TextureRect.new();image.texture=load("res://assets/art/title.png");image.expand_mode=TextureRect.EXPAND_IGNORE_SIZE;image.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_COVERED;image.size=size;image.mouse_filter=Control.MOUSE_FILTER_IGNORE;screen.add_child(image)
	var grad=Gradient.new();grad.colors=PackedColorArray([Color(.02,.04,.075,.96),Color(.02,.04,.075,.76),Color(.02,.04,.075,0)]);grad.offsets=PackedFloat32Array([0,.36,.86]);var tex=GradientTexture2D.new();tex.gradient=grad;tex.fill_from=Vector2(0,0);tex.fill_to=Vector2(1,0);var veil=TextureRect.new();veil.texture=tex;veil.size=size;veil.mouse_filter=Control.MOUSE_FILTER_IGNORE;screen.add_child(veil)
	var icon=TextureRect.new();icon.texture=load("res://assets/art/icon_foreground.png");icon.expand_mode=TextureRect.EXPAND_IGNORE_SIZE;icon.position=Vector2(48,24);icon.size=Vector2(112,112);screen.add_child(icon)
	eyebrow("MALAM ORBIT PECAH",Vector2(67,size.y*.24))
	text("ASTRA",Vector2(61,size.y*.285),Vector2(450,120),102,white,true);text("SLASH",Vector2(61,size.y*.415),Vector2(450,120),102,cyan,true)
	text("Beri esok sebuah tempat.",Vector2(68,size.y*.585),Vector2(420,36),23,white)
	button("MASUK KE OBSERVATORIUM  →",Vector2(67,size.y-.29*size.y),Vector2(355,57),g.hub,true)
	button("PENGATURAN",Vector2(67,size.y-118),Vector2(171,44),g.settings_screen);button("KREDIT",Vector2(251,size.y-118),Vector2(171,44),credits)
	text("WYNNDEV  /  1.0.0",Vector2(67,size.y-39),Vector2(400,24),14,muted,true)
func hub():
	reset();panel(Vector2(size.x*.53,0),Vector2(size.x*.47,size.y),Color(.035,.065,.105,.93),Color.TRANSPARENT);var x=size.x*.57;var w=size.x*.39;var h=D.HEROES[g.save.profile.selected]
	back(g.title);eyebrow("OBSERVATORIUM  /  PANGKALAN",Vector2(x,34));text("✦  "+str(g.save.profile.shards)+"  SERPIHAN",Vector2(x+270,34),Vector2(260,26),18,Color("e4c98c"),true)
	button("01  REI",Vector2(x,92),Vector2(w/2-7,44),func():g.select_hero("rei"),g.save.profile.selected=="rei");button("02  KAEL",Vector2(x+w/2+7,92),Vector2(w/2-7,44),func():g.select_hero("kael"),g.save.profile.selected=="kael")
	text(h.name,Vector2(x,146),Vector2(w,81),68,white,true);eyebrow(h.title.to_upper(),Vector2(x,223));text(h.desc,Vector2(x,271),Vector2(w,100),21,muted)
	panel(Vector2(x,392),Vector2(w,91));text(h.skill+"\n"+h.ult,Vector2(x+16,408),Vector2(w-30,65),18,white,true)
	button("MULAI PERJALANAN  →" if g.save.profile.run.is_empty() else "LANJUTKAN PERJALANAN  →",Vector2(x,size.y-125),Vector2(w,58),g.new_run if g.save.profile.run.is_empty() else g.resume_run,true)
	button("PANDUAN",Vector2(x,size.y-57),Vector2(w/2-6,36),guide);button("PENGATURAN",Vector2(x+w/2+6,size.y-57),Vector2(w/2-6,36),g.settings_screen)
	eyebrow("TENUNAN PERMANEN",Vector2(34,size.y-150));var i=0
	for id in D.META:
		var data=D.META[id];var level=int(g.save.profile.meta[id]);var cost=data[2]*(level+1);var p=Vector2(34+i*(size.x*.49/3),size.y-111);var w2=size.x*.49/3-10
		var b=button(data[0]+"  "+str(level)+"/"+str(data[3])+"\n"+("LENGKAP" if level>=data[3] else "✦ "+str(cost)),p,Vector2(w2,65),func():g.buy_meta(id),false,level>=data[3] or g.save.profile.shards<cost);b.add_theme_font_size_override("font_size",17);text(data[1],p+Vector2(0,72),Vector2(w2,22),13,muted);i+=1
func hud():
	reset();hud_view=load("res://scripts/hud.gd").new();hud_view.g=g;hud_view.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT);hud_view.mouse_filter=Control.MOUSE_FILTER_IGNORE;screen.add_child(hud_view);button("Ⅱ",Vector2(size.x-67,24),Vector2(42,40),g.pause)
func loading():
	reset();shade(1);eyebrow("MENGHUBUNGKAN ORBIT",Vector2(size.x*.12,size.y*.45));text("Fajar sedang dirajut…",Vector2(size.x*.12,size.y*.50),Vector2(650,62),46,white,true)
func upgrade():
	reset();shade(.92);eyebrow("RESONANSI DITEMUKAN  /  "+str(g.node+1)+" DARI 9",Vector2(55,43));text("PILIH BENTUK CAHAYAMU",Vector2(53,77),Vector2(size.x-100,70),52,white,true);text("Satu tenunan baru untuk sisa perjalanan.",Vector2(56,151),Vector2(750,32),21,muted)
	var w=(size.x-144)/3
	for i in range(g.offers.size()):
		var id=g.offers[i];var d=D.RELICS[id];var x=52+i*(w+20);panel(Vector2(x,216),Vector2(w,size.y-285),Color("102034"),Color("496476"));text("0"+str(i+1)+"   /   "+d[2],Vector2(x+23,240),Vector2(w-46,25),16,cyan,true);text("✧",Vector2(x+24,278),Vector2(w-48,90),78,Color("dcc68f"),true);text(d[0],Vector2(x+23,382),Vector2(w-46,80),32,white,true);text(d[1],Vector2(x+23,466),Vector2(w-46,110),20,muted);button("TENUN  →",Vector2(x+23,size.y-137),Vector2(w-46,47),func():g.choose_relic(id),true)
func route():
	reset();shade();eyebrow("SIMPANG ORBIT",Vector2(55,45));text("DUA JALAN MENUJU FAJAR",Vector2(53,82),Vector2(size.x-100,70),53,white,true)
	var w=(size.x-144)/2
	var content=[["forge","TUNGKU RETAK","Risiko mengasah cahaya.","Lebih banyak penjaga dan musuh elite.\n+25 serpihan. Pilihan resonansi berikutnya menjamin satu tenunan ofensif."],["quiet","MONUMEN SUNYI","Sebelum berlari, bernapaslah.","Pulih 25% Vitalitas dan +1 pemulihan.\nLebih sedikit penjaga, dengan musuh elite di ujung lintasan."]]
	for i in range(2):
		var d=content[i];var x=52+i*(w+40);panel(Vector2(x,220),Vector2(w,365));eyebrow("0"+str(i+1)+"  /  RUTE",Vector2(x+25,248));text(d[1],Vector2(x+25,294),Vector2(w-50,56),41,white,true);text(d[2],Vector2(x+25,353),Vector2(w-50,36),23,cyan);text(d[3],Vector2(x+25,401),Vector2(w-50,100),20,muted);button("TEMPUH JALAN INI  →",Vector2(x+25,520),Vector2(w-50,44),func():g.choose_route(d[0]),true)
func dialog():
	reset();panel(Vector2.ZERO,Vector2(size.x,62),Color(.025,.04,.07,.93),Color.TRANSPARENT);panel(Vector2(0,size.y-241),Vector2(size.x,241),Color(.025,.04,.07,.96),Color("567784"));var line=g.dialogue[g.dialogue_index];eyebrow(line[0],Vector2(61,size.y-215));text(line[1],Vector2(61,size.y-175),Vector2(size.x-160,105),25,white);button("LANJUT  →",Vector2(size.x-239,size.y-64),Vector2(184,42),g.next_dialog,true);button("LEWATI",Vector2(size.x-163,14),Vector2(119,36),g.skip_dialog)
func pause():
	reset();shade(.94);eyebrow("ORBIT DITAHAN",Vector2(55,42));text("TARIK NAPAS",Vector2(53,80),Vector2(700,74),60,white,true)
	button("LANJUTKAN",Vector2(55,217),Vector2(300,57),g.pause,true);button("PENGATURAN",Vector2(55,291),Vector2(300,52),g.settings_screen);button("PANDUAN",Vector2(55,360),Vector2(300,52),guide);button("SIMPAN & KE PANGKALAN",Vector2(55,429),Vector2(300,52),g.abandon)
	text("Perjalanan tersimpan otomatis.\nKembali kapan pun cahayamu siap.",Vector2(55,518),Vector2(320,86),20,muted)
	var x=size.x*.36;eyebrow("TENUNAN PERJALANAN",Vector2(x,217))
	if g.relics.is_empty():text("Resonansi pertamamu menunggu di ujung lintasan.",Vector2(x,270),Vector2(size.x-x-60,80),22,muted)
	for i in range(g.relics.size()):
		var d=D.RELICS[g.relics[i]];var w=(size.x-x-70)/2;var p=Vector2(x+(i%2)*(w+15),269+(i/2)*91);panel(p,Vector2(w,77));text(d[0],p+Vector2(13,10),Vector2(w-26,26),22,white,true);text(d[2],p+Vector2(13,43),Vector2(w-26,22),14,cyan,true)
func guide():
	var return_to=g.mode;g.set_mode("guide");reset();shade(.96);back(func():g.set_mode(return_to);g.ui.pause() if return_to=="pause" else g.ui.hub());eyebrow("CATATAN PETARUNG",Vector2(55,105));text("GERAK ADALAH SENJATA",Vector2(53,145),Vector2(1000,76),57,white,true)
	var rows=[["KOMBO","Ketuk atau tahan serang. Tiga pukulan; hit terakhir membuka pertahanan.","J  /  X"],["UDARA","Lompat dua kali. Arah atas + berat meluncurkan musuh. Berat di udara menghantam tanah.","SPASI · W+K"],["DODGE","Dodge dapat membatalkan serangan. Hindari pukulan tepat pada awal dodge untuk perfect dodge.","SHIFT  /  B"],["SKILL & ULTIMATE","Skill pulih setiap 6,5 detik. Hit mengisi energi ultimate; lepaskan saat 100.","L · U  /  RB · LB"],["PULIH","Gunakan persediaan untuk memulihkan 38% Vitalitas. Sisa hitungan terlihat di HUD.","H  /  D-PAD ↑"],["BACA MUSUH","Merah menandai serangan. Lompati gelombang tanah; berat membongkar perisai.","A · D / STIK KIRI"]]
	var w=(size.x-135)/2
	for i in range(rows.size()):
		var row=rows[i];var p=Vector2(55+(i%2)*(w+25),246+(i/2)*142);eyebrow(row[0]+"   /   "+row[2],p);text(row[1],p+Vector2(0,35),Vector2(w-15,88),20,muted)
func settings_screen():
	reset();shade(.97);back(g.settings_back);eyebrow("PENGATURAN",Vector2(55,96));text("SESUAIKAN RITMEMU",Vector2(53,130),Vector2(1100,70),52,white,true)
	var w=(size.x-140)/2;var x2=80+w
	for i in range(4):button(["LOW","MEDIUM","HIGH","ULTRA"][i],Vector2(55+i*(w/4),231),Vector2(w/4-9,43),func():g.preset(i),int(g.settings.preset)==i)
	slider("Skala render",Vector2(55,306),w,g.settings.scale,.5,1.2,.01,func(v):g.settings.scale=v;g.apply_settings())
	slider("Kepadatan efek",Vector2(55,388),w,g.settings.effects,.2,1.25,.05,func(v):g.settings.effects=v)
	toggle("Bayangan",Vector2(55,474),g.settings.shadows,func(v):g.settings.shadows=v;g.apply_settings());toggle("Bloom & grading",Vector2(55+w/2,474),g.settings.post,func(v):g.settings.post=v;g.apply_settings())
	text("BATAS FPS",Vector2(55,548),Vector2(w,28),17,cyan,true)
	for i in range(4):button(str([30,60,90,120][i]),Vector2(55+i*w/4,589),Vector2(w/4-9,40),func():g.settings.fps=[30,60,90,120][i];g.apply_settings();g.ui.settings_screen(),int(g.settings.fps)==[30,60,90,120][i])
	slider("Musik",Vector2(x2,231),w,g.settings.music,0,1,.01,func(v):g.settings.music=v)
	slider("Efek suara",Vector2(x2,313),w,g.settings.sfx,0,1,.01,func(v):g.settings.sfx=v)
	slider("Guncangan kamera",Vector2(x2,395),w,g.settings.shake,0,1,.01,func(v):g.settings.shake=v)
	toggle("Kontrol sentuh",Vector2(x2,491),g.settings.touch,func(v):g.settings.touch=v;g.apply_settings());toggle("Angka damage",Vector2(x2,553),g.settings.numbers,func(v):g.settings.numbers=v)
	text("Pengaturan disimpan saat kembali.",Vector2(x2,size.y-44),Vector2(w,24),16,muted)
func slider(label,p,w,value,low,high,step,cb):
	text(label,p,Vector2(w-100,27),21,white);var number=text(str(int(value*100))+"%",p+Vector2(w-79,0),Vector2(79,27),21,cyan,true);var s=HSlider.new();s.position=p+Vector2(0,39);s.size=Vector2(w-15,27);s.min_value=low;s.max_value=high;s.step=step;s.value=value;s.value_changed.connect(func(v):number.text=str(int(v*100))+"%";cb.call(v));screen.add_child(s)
func toggle(label,p,value,cb):
	var c=CheckButton.new();c.text=label;c.position=p;c.size=Vector2(250,40);c.button_pressed=value;c.add_theme_font_override("font",font);c.add_theme_font_size_override("font_size",20);c.toggled.connect(cb);screen.add_child(c)
func result(won):
	reset();shade(.88);eyebrow("PERJALANAN SELESAI" if won else "CAHAYA MASIH ADA",Vector2(65,72));text("FAJAR PERTAMA" if won else "TENUN KEMBALI",Vector2(60,123),Vector2(size.x-120,100),80,white,true);text("Cincin terbuka. Kota akhirnya memiliki hari esok." if won else "Malam ini belum berakhir. Bawa serpihannya pulang.",Vector2(65,241),Vector2(size.x-140,65),25,muted)
	var values=[["WAKTU",time_text(g.elapsed)],["PENJAGA DIKALAHKAN",str(g.kills)],["KOMBO TERBAIK",str(g.best_combo)],["SERPIHAN DIBAWA",str(g.shards)]];var w=(size.x-130)/4
	for i in range(4):
		var p=Vector2(65+i*w,359);eyebrow(values[i][0],p);text(values[i][1],p+Vector2(0,36),Vector2(w,78),57,cyan,true)
	button("KEMBALI KE PANGKALAN",Vector2(65,size.y-138),Vector2(343,57),g.hub,true);button("MAIN LAGI",Vector2(430,size.y-138),Vector2(245,57),g.new_run);button("KREDIT",Vector2(697,size.y-138),Vector2(180,57),credits)
func credits():
	reset();g.set_mode("credits");shade(.98);back(g.title);eyebrow("ASTRASLASH  /  MALAM ORBIT PECAH",Vector2(55,106));text("UNTUK HARI YANG BELUM ADA",Vector2(53,148),Vector2(size.x-106,76),52,white,true)
	text("DUNIA, GAMEPLAY & PRODUKSI\nWynnDev · dibuat bersama Codex\n\nKARAKTER, LINGKUNGAN & EFEK\nDesain original AstraSlash · pipeline Blender dan Godot\n\nMUSIK & SUARA\nKomposisi dan sintesis original AstraSlash",Vector2(55,276),Vector2(size.x*.5-65,342),23,white)
	text("TEKNOLOGI & LISENSI\nGodot Engine — MIT\nBlender — GPL, digunakan sebagai alat produksi\nBarlow / Barlow Condensed — Jeremy Tribby, SIL OFL\nIlustrasi judul — OpenAI Image Generation\n\nTanpa iklan. Tanpa pembelian di dalam game.\nTerima kasih telah memberi esok sebuah tempat.",Vector2(size.x*.53,276),Vector2(size.x*.43,330),22,muted)
static func time_text(seconds):return "%02d:%02d" % [int(seconds)/60,int(seconds)%60]
func tick(dt):
	if hud_view and is_instance_valid(hud_view):hud_view.queue_redraw()
