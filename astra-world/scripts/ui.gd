extends CanvasLayer
const Data=preload("res://scripts/data.gd")
const Hud=preload("res://scripts/hud.gd")
const Map=preload("res://scripts/map.gd")
var game
var root:Control
var panel_root:Control
var hud:Control
var blocking=true
var page="title"
var hurt_flash=0.0
var toast_label:Label
var toast_time=0.0
var dialogue_lines:Array=[]
var dialogue_index=0
var dialogue_done:Callable
var title_font:Font
var body_font:Font
var ink=Color("f0ecd9")
var muted=Color("acc1b7")
var gold=Color("e4cb98")
var bg=Color("102c33")

func setup(g):
 game=g; layer=5
 root=Control.new(); root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT); add_child(root); root.mouse_filter=Control.MOUSE_FILTER_IGNORE
 title_font=load("res://assets/branding/display.ttf"); body_font=load("res://assets/branding/body.ttf")
 var theme=Theme.new(); theme.default_font=body_font; theme.default_font_size=17; root.theme=theme
 hud=Hud.new(); root.add_child(hud); hud.setup(game)
 panel_root=Control.new(); root.add_child(panel_root); panel_root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT); panel_root.mouse_filter=Control.MOUSE_FILTER_IGNORE
 toast_label=Label.new(); root.add_child(toast_label); toast_label.set_anchors_and_offsets_preset(Control.PRESET_CENTER_TOP); toast_label.position=Vector2(-310,210); toast_label.size=Vector2(620,62); toast_label.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER; toast_label.vertical_alignment=VERTICAL_ALIGNMENT_CENTER; toast_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART; toast_label.add_theme_stylebox_override("normal",style(Color(0.05,0.15,0.17,0.95),Color(0.7,0.8,0.66,0.4),10)); toast_label.add_theme_color_override("font_color",ink); toast_label.visible=false
 open("title")

func style(color:Color,border:Color=Color(0,0,0,0),radius:int=8) -> StyleBoxFlat:
 var s=StyleBoxFlat.new(); s.bg_color=color; s.border_color=border; s.set_border_width_all(1); s.set_corner_radius_all(radius); s.content_margin_left=18; s.content_margin_right=18; s.content_margin_top=12; s.content_margin_bottom=12
 return s

func label(parent:Node,t:String,size:int=17,color:Color=Color("f0ecd9"),heading:bool=false) -> Label:
 var l=Label.new(); l.text=t; l.add_theme_color_override("font_color",color); l.add_theme_font_size_override("font_size",size)
 if heading: l.add_theme_font_override("font",title_font)
 l.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART; parent.add_child(l); return l

func button(parent:Node,t:String,fn:Callable,primary:bool=false) -> Button:
 var b=Button.new(); b.text=t; b.custom_minimum_size=Vector2(0,49); b.mouse_default_cursor_shape=Control.CURSOR_POINTING_HAND
 b.add_theme_stylebox_override("normal",style(Color("daca9f") if primary else Color("1e4146"),Color("50736d"),8))
 b.add_theme_stylebox_override("hover",style(Color("f0deb0") if primary else Color("305c5c"),gold,8))
 b.add_theme_stylebox_override("pressed",style(Color("b2bea0"),gold,8))
 b.add_theme_stylebox_override("focus",style(Color(0,0,0,0),gold,8))
 b.add_theme_stylebox_override("disabled",style(Color("1b3338"),Color("3a5453"),8))
 b.add_theme_color_override("font_color",Color("153a40") if primary else ink); b.add_theme_color_override("font_hover_color",Color("153a40") if primary else ink)
 b.add_theme_color_override("font_disabled_color",Color("71877e")); b.pressed.connect(func():game.audio.sfx("ui",-12); fn.call()); parent.add_child(b); return b

func spacer(parent:Node,height:float):
 var c=Control.new(); c.custom_minimum_size.y=height; parent.add_child(c)

func clear():
 for n in panel_root.get_children(): panel_root.remove_child(n); n.queue_free()

func close():
 clear(); blocking=false; page=""; hud.release_all(); game.audio.set_mode("aeralis" if game.playing else "home")
 print("ASTRA_UI page=world" if game.playing else "ASTRA_UI page=title")

func _process(dt):
 hurt_flash=maxf(0,hurt_flash-dt)
 if toast_time>0: toast_time-=dt; toast_label.modulate.a=minf(1,toast_time*2)
 else: toast_label.visible=false

func toast(t:String):
 if toast_label==null: return
 toast_label.text=t; toast_label.visible=true; toast_time=4.0; toast_label.modulate.a=1

func open(which:String):
 clear(); blocking=true; page=which; hud.release_all()
 print("ASTRA_UI page="+which)
 var shade=ColorRect.new(); shade.color=Color(0.025,0.075,0.09,0.82 if which!="title" else 0.23); panel_root.add_child(shade); shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
 if which=="title": _title(); return
 if which=="dialogue": _dialogue_view(); return
 if which=="ending": _ending(); return
 var outer=MarginContainer.new(); panel_root.add_child(outer); outer.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
 for side in ["left","right"]: outer.add_theme_constant_override("margin_"+side,64)
 for side in ["top","bottom"]: outer.add_theme_constant_override("margin_"+side,34)
 var all=VBoxContainer.new(); all.add_theme_constant_override("separation",18); outer.add_child(all)
 var bar=HBoxContainer.new(); all.add_child(bar); bar.add_theme_constant_override("separation",16)
 var title={"map":"Peta Aeralis","journal":"Jurnal perjalanan","inventory":"Bekal perjalanan","character":"Nara","craft":"Penempaan","pause":"Perjalanan dijeda","settings":"Pengaturan","credits":"Tentang Astra World","confirm":"Perjalanan baru"}.get(which,"Astra World")
 var heading=label(bar,title,32,gold,true); heading.size_flags_horizontal=Control.SIZE_EXPAND_FILL
 button(bar,"Kembali",func():
  if which in ["settings","credits"]: open("pause" if game.playing else "title")
  else: close())
 if which in ["map","journal","inventory","character","craft"]:
  var tabs=HBoxContainer.new(); tabs.add_theme_constant_override("separation",10); all.add_child(tabs)
  for tab in [["map","Peta"],["journal","Jurnal"],["inventory","Tas"],["character","Karakter"],["craft","Tempa"]]:
   var b=button(tabs,tab[1],func():open(tab[0]),which==tab[0]); b.size_flags_horizontal=Control.SIZE_EXPAND_FILL
 var scroll=ScrollContainer.new(); scroll.size_flags_vertical=Control.SIZE_EXPAND_FILL; scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED; all.add_child(scroll)
 var content=VBoxContainer.new(); content.size_flags_horizontal=Control.SIZE_EXPAND_FILL; content.add_theme_constant_override("separation",13); scroll.add_child(content)
 match which:
  "map": _map(content)
  "journal": _journal(content)
  "inventory": _inventory(content)
  "character": _character(content)
  "craft": _craft(content)
  "pause":
   label(content,"Lembah tetap menunggumu. Perjalanan tersimpan otomatis.",18,muted)
   button(content,"Lanjutkan",close,true)
   button(content,"Simpan perjalanan",func():game.save(); toast("Perjalanan tersimpan." if game.state.last_error=="" else game.state.last_error))
   button(content,"Pengaturan",func():open("settings"))
   button(content,"Bantuan kontrol",func():_controls_dialogue())
   button(content,"Kembali ke layar judul",func():game.save(); game.playing=false; game.audio.set_mode("home"); open("title"))
  "settings": _settings(content)
  "credits":
   label(content,"ASTRA WORLD",32,gold,true)
   label(content,"Region I · Lembah Aeralis\nSebuah petualangan tentang ingatan, penantian, dan jalan pulang.\n\nDunia, cerita, model prosedural, antarmuka, serta musik original dibuat untuk proyek ini. Ikon dibuat dengan image generation dan diadaptasi menjadi lambang vektor.\n\nDibuat dengan Godot Engine · lisensi MIT\nTipografi DejaVu · lisensi Bitstream Vera / DejaVu\n\nVersi 1.0.0 · WynnDev\nTanpa iklan, tanpa pembelian, dapat dimainkan sepenuhnya offline.",18,muted)
  "confirm":
   label(content,"Memulai perjalanan baru akan mengganti save yang sekarang. Salinan terakhir tetap disimpan sebagai cadangan.",21)
   button(content,"Mulai perjalanan baru",func():game.start_new(),true)
   button(content,"Batalkan",func():open("title"))

func _title():
 var margin=MarginContainer.new(); panel_root.add_child(margin); margin.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
 margin.add_theme_constant_override("margin_left",86); margin.add_theme_constant_override("margin_top",70); margin.add_theme_constant_override("margin_bottom",40)
 var h=HBoxContainer.new(); margin.add_child(h)
 var v=VBoxContainer.new(); v.custom_minimum_size.x=400; v.add_theme_constant_override("separation",10); h.add_child(v)
 var crest=TextureRect.new(); crest.texture=load("res://assets/branding/crest.svg"); crest.custom_minimum_size=Vector2(88,95); crest.expand_mode=TextureRect.EXPAND_IGNORE_SIZE; crest.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT; crest.size_flags_horizontal=Control.SIZE_SHRINK_BEGIN; v.add_child(crest)
 label(v,"A S T R A",52,ink,true); label(v,"W O R L D",35,gold,true)
 spacer(v,4); label(v,"REGION I   /   LEMBAH AERALIS",13,gold)
 label(v,"Ke mana angin membawa ingatan?",18,ink,true); spacer(v,28)
 var resume=button(v,"Lanjutkan perjalanan",func():game.resume_game(),true); resume.disabled=not game.has_save
 button(v,"Perjalanan baru",func():
  if game.has_save: open("confirm")
  else: game.start_new(),not game.has_save)
 var small=HBoxContainer.new(); v.add_child(small)
 button(small,"Pengaturan",func():open("settings")); button(small,"Tentang",func():open("credits"))
 var fill=Control.new(); fill.size_flags_vertical=Control.SIZE_EXPAND_FILL; v.add_child(fill)
 label(v,"W Y N N D E V     ·     1.0.0",11,Color("bed2c5"))

func _card(parent:Node) -> VBoxContainer:
 var p=PanelContainer.new(); p.add_theme_stylebox_override("panel",style(Color("17383e"),Color("34554f"),10)); parent.add_child(p)
 var v=VBoxContainer.new(); v.add_theme_constant_override("separation",9); p.add_child(v); return v

func _journal(parent:Node):
 var q=game.quest(); var v=_card(parent)
 label(v,"PERJALANAN UTAMA",12,gold); label(v,q.title,27,ink,true); label(v,q.body,18,muted)
 for id in Data.BEACONS:
  label(v,("✓  " if id in game.state.s.beacons else "○  ")+Data.landmark(id).name,17)
 var side=_card(parent); label(side,"SURAT YANG PULANG",13,gold)
 label(side,"Mira menunggu tiga surat di desa. Ditemukan: %d/3.\nPetunjuk: Tebing Kertas · Kolam Cermin · belakang Arsip Langit."%mini(3,game.state.amount("letter")),17,muted)
 if game.state.s.mira_done: label(side,"Selesai · Mira telah membaca surat ayahnya.",17,gold)
 var mystery=_card(parent); label(mystery,"TAMAN YANG TERLUPA",13,gold)
 label(mystery,"Selesai · tiga batu kembali bernyanyi." if game.state.s.puzzle else "Di barat Arsip Langit, tiga batu menunggu urutan kehidupan. Prasasti di pintu taman menyimpan petunjuk.",17,muted)
 label(parent,"Catatan penjelajahan · %d/%d landmark ditemukan · %d peti terbuka"%[game.state.s.discovered.size(),Data.LANDMARKS.size(),game.chests_opened()],15,gold)

func _inventory(parent:Node):
 label(parent,"Bahan dikumpulkan di dunia. Senjata dan perlengkapan dapat dipasang di layar Karakter.",17,muted)
 var grid=GridContainer.new(); grid.columns=3; grid.add_theme_constant_override("h_separation",12); grid.add_theme_constant_override("v_separation",12); parent.add_child(grid)
 for id in Data.ITEMS:
  if game.state.amount(id)<1: continue
  var v=_card(grid); v.custom_minimum_size.x=315
  label(v,Data.ITEMS[id].name+"   ×%d"%game.state.amount(id),19,Color(Data.ITEMS[id].color),true)
  label(v,Data.ITEMS[id].desc,15,muted)

func _character(parent:Node):
 var v=_card(parent)
 label(v,"Nara · pembawa lonceng",28,gold,true)
 label(v,"Seorang pengelana yang membuat peta dari suara. Lonceng saku peninggalan ibunya menuntunnya ke Aeralis.",18,muted)
 label(v,"Level %d  ·  XP %d / %d  ·  Kesehatan %d  ·  Serangan %d"%[game.state.level(),game.state.s.xp,game.state.level()*100,game.state.max_hp(),game.state.damage()],19)
 label(v,"Bilah: "+Data.ITEMS[game.state.s.weapon].name+"\nMantel: "+("Mantel Angin" if game.state.s.armor!="" else "Mantel pengelana")+"\nJimat: "+("Jimat Akar" if game.state.s.charm!="" else "Belum ditemukan"),18,muted)
 for id in ["sword0","sword1","sword2","mantle","charm"]:
  if game.state.amount(id)>0:
   button(parent,"Pasang "+Data.ITEMS[id].name,func():
    if id.begins_with("sword"): game.state.s.weapon=id
    elif id=="mantle": game.state.s.armor=id
    else: game.state.s.charm=id
    game.save(); open("character"))
 label(parent,"Gema (Q) · gelombang area, 7 detik pemulihan. Hindar (K) · kebal singkat. Melayang · tahan Lompat di udara setelah mercusuar pertama menyala.",17,muted)

func _craft(parent:Node):
 var at_forge=Vector2(game.player.position.x,game.player.position.z).distance_to(Vector2(10,47))<5
 label(parent,"Tungku Sava siap digunakan." if at_forge else "Kunjungi Tungku Sava di Desa Teralun untuk menempa.",18,gold)
 for recipe in Data.RECIPES:
  var v=_card(parent); var row=HBoxContainer.new(); v.add_child(row)
  var name_l=label(row,Data.ITEMS[recipe.id].name,22,ink,true); name_l.size_flags_horizontal=Control.SIZE_EXPAND_FILL
  var b=button(row,"Tempa",func():
   if game.craft(recipe): open("craft"))
  b.disabled=not at_forge or not game.state.can_craft(recipe)
  label(v,recipe.desc,16,muted)
  var costs=[]
  for id in recipe.cost: costs.append("%s %d/%d"%[Data.ITEMS[id].name,game.state.amount(id),recipe.cost[id]])
  label(v,"  ·  ".join(costs),15,gold)

func _map(parent:Node):
 var row=HBoxContainer.new(); row.add_theme_constant_override("separation",22); parent.add_child(row)
 var map=Map.new(); map.game=game; row.add_child(map)
 var v=VBoxContainer.new(); v.custom_minimum_size.x=310; v.add_theme_constant_override("separation",9); row.add_child(v)
 label(v,"JALAN PULANG",13,gold); label(v,"Mercusuar yang menyala menjadi titik perjalanan cepat. Tidak tersedia saat bertarung.",16,muted)
 for l in Data.LANDMARKS:
  if l.id in game.state.s.discovered:
   var b=button(v,l.name,func():game.fast_travel(l.id))
   b.disabled=not (l.id=="village" or l.id in game.state.s.beacons)

func _settings(parent:Node):
 var presets=HBoxContainer.new(); presets.add_theme_constant_override("separation",10); parent.add_child(presets)
 for i in range(4):
  var b=button(presets,["Low","Medium","High","Ultra"][i],func():game.set_preset(i); open("settings"),game.state.settings.preset==i); b.size_flags_horizontal=Control.SIZE_EXPAND_FILL
 label(parent,"Pilih preset sesuai kelancaran perangkat. Resolusi mengubah detail render dunia; teks tetap tajam.",16,muted)
 _option(parent,"Batas frame",["30 FPS","45 FPS","60 FPS"],[30,45,60],"fps")
 _slider(parent,"Resolusi dunia","resolution",0.5,1.0,0.05)
 _toggle(parent,"Bayangan matahari","shadows")
 _toggle(parent,"Partikel dan efek tambahan","effects")
 _slider(parent,"Kepadatan rumput","foliage",0.0,1.0,0.25)
 _toggle(parent,"Tampilkan FPS","show_fps")
 _slider(parent,"Musik","music",0.0,1.0,0.05)
 _slider(parent,"Suara lingkungan dan aksi","sfx",0.0,1.0,0.05)
 _slider(parent,"Sensitivitas kamera","sensitivity",0.5,2.0,0.1)
 _toggle(parent,"Balik kamera vertikal","invert_y")

func _slider(parent:Node,title:String,key:String,low:float,high:float,step:float):
 var row=HBoxContainer.new(); row.add_theme_constant_override("separation",24); parent.add_child(row)
 var l=label(row,title,17); l.custom_minimum_size=Vector2(340,42)
 var slider=HSlider.new(); slider.min_value=low; slider.max_value=high; slider.step=step; slider.value=game.state.settings[key]; slider.custom_minimum_size=Vector2(390,42); slider.size_flags_horizontal=Control.SIZE_EXPAND_FILL; row.add_child(slider)
 var value=label(row,"%d%%"%int(slider.value*100),17,gold); value.custom_minimum_size.x=70
 slider.value_changed.connect(func(n):game.state.settings[key]=n; value.text="%d%%"%int(n*100); game.apply_settings())

func _toggle(parent:Node,title:String,key:String):
 var c=CheckButton.new(); c.text=title; c.button_pressed=game.state.settings[key]; c.custom_minimum_size.y=47; parent.add_child(c)
 c.toggled.connect(func(b):game.state.settings[key]=b; game.apply_settings())

func _option(parent:Node,title:String,choices:Array,values:Array,key:String):
 var row=HBoxContainer.new(); parent.add_child(row); var l=label(row,title,17); l.custom_minimum_size.x=340
 var o=OptionButton.new(); o.custom_minimum_size=Vector2(240,44); row.add_child(o)
 for i in choices.size():
  o.add_item(choices[i]); if game.state.settings[key]==values[i]: o.select(i)
 o.item_selected.connect(func(i):game.state.settings[key]=values[i]; game.apply_settings())

func dialogue(lines:Array,done:Callable=Callable()):
 dialogue_lines=lines; dialogue_index=0; dialogue_done=done; open("dialogue")

func _dialogue_view():
 var p=PanelContainer.new(); panel_root.add_child(p); p.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_WIDE); p.offset_left=92; p.offset_right=-92; p.offset_top=-241; p.offset_bottom=-32
 p.add_theme_stylebox_override("panel",style(Color(0.04,0.13,0.15,0.97),Color("718c79"),12))
 var v=VBoxContainer.new(); v.add_theme_constant_override("separation",12); p.add_child(v)
 label(v,dialogue_lines[dialogue_index][0],21,gold,true)
 var body=label(v,dialogue_lines[dialogue_index][1],20,ink); body.custom_minimum_size.y=74
 var row=HBoxContainer.new(); v.add_child(row)
 var idx=label(row,"%d / %d"%[dialogue_index+1,dialogue_lines.size()],13,muted); idx.size_flags_horizontal=Control.SIZE_EXPAND_FILL
 button(row,"Lanjut  ›" if dialogue_index<dialogue_lines.size()-1 else "Kembali ke perjalanan",advance_dialogue,true)

func advance_dialogue():
 dialogue_index+=1
 if dialogue_index>=dialogue_lines.size():
  var cb=dialogue_done; close()
  if cb.is_valid(): cb.call()
 else: open("dialogue")

func _controls_dialogue():
 dialogue([["Menjelajahi Aeralis","Sentuh dan geser sisi kiri untuk berjalan. Geser sisi kanan untuk memutar kamera. Tombol kanan: Serang, Gema, Lompat, dan Hindar. Sentuh nama objek untuk berinteraksi."],["Bertarung","Lihat lingkaran merah sebelum penjaga menyerang. Hindar memberi kekebalan singkat. Gema menghantam semua musuh di sekitarmu. Bekal memulihkan kesehatan; istirahat di desa menambah bekal."],["Keyboard dan mouse","WASD bergerak · klik kanan dan geser untuk kamera · J serang · Q gema · K hindar · Spasi lompat/melayang · Shift lari · E interaksi · H bekal · M peta · I tas · Esc jeda."]])

func _ending():
 var center=CenterContainer.new(); panel_root.add_child(center); center.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
 var v=VBoxContainer.new(); v.custom_minimum_size.x=660; v.add_theme_constant_override("separation",18); center.add_child(v)
 label(v,"A S T R A   W O R L D",22,gold,true)
 label(v,"Angin yang pulang",46,ink,true)
 label(v,"REGION I SELESAI",14,gold)
 label(v,"Tiga mercusuar bernyanyi lagi. Desa Teralun mengingat namanya.\nDan seorang penjaga, akhirnya, dapat beristirahat.",20,muted)
 label(v,"%d landmark ditemukan  ·  %d peti terbuka  ·  Nara level %d"%[game.state.s.discovered.size(),game.chests_opened(),game.state.level()],16,gold)
 button(v,"Tetap menjelajahi Aeralis",close,true)
 button(v,"Layar judul",func():game.save(); game.playing=false; open("title"))
