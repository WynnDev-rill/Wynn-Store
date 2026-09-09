extends Node3D
const D=preload("res://scripts/data.gd")
const Save=preload("res://scripts/save.gd")
const Inputs=preload("res://scripts/inputs.gd")
const Sound=preload("res://scripts/sound.gd")
const World=preload("res://scripts/world.gd")
const Fx=preload("res://scripts/fx.gd")
const Player=preload("res://scripts/player.gd")
const Enemy=preload("res://scripts/enemy.gd")
const Projectile=preload("res://scripts/projectile.gd")
const UI=preload("res://scripts/ui.gd")
var save
var settings
var inputs=Inputs.new()
var sound
var world
var fx
var player
var camera
var ui
var post
var mode="title"
var previous_mode="title"
var enemies=[]
var projectiles=[]
var tasks=[]
var relics=[]
var offers=[]
var node=0
var wave=0
var route="quiet"
var seed_value=0
var shards=0
var kills=0
var elapsed=0.0
var combo=0
var best_combo=0
var combo_timer=0.0
var hitstop=0.0
var shake=0.0
var flash=0.0
var slow=0.0
var autosave=0.0
var room_clearing=false
var message=""
var message_timer=0.0
var cam_x=12.0
var dialogue=[]
var dialogue_index=0
var dialogue_done=Callable()
var qa=null
func _ready():
	randomize();save=Save.new();settings=save.profile.settings;inputs.resize(get_viewport().get_visible_rect().size)
	sound=Sound.new();add_child(sound);sound.setup(self)
	camera=Camera3D.new();camera.projection=Camera3D.PROJECTION_ORTHOGONAL;camera.size=10.8;camera.far=180;add_child(camera);camera.current=true
	var layer=CanvasLayer.new();layer.layer=1;add_child(layer);post=ColorRect.new();post.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT);post.mouse_filter=Control.MOUSE_FILTER_IGNORE;post.material=ShaderMaterial.new();post.material.shader=load("res://shaders/post.gdshader");layer.add_child(post)
	ui=UI.new();add_child(ui);ui.setup(self);apply_settings();title()
	if "--qa" in OS.get_cmdline_user_args() and OS.has_feature("editor"):
		qa=load("res://scripts/qa.gd").new();add_child(qa);qa.setup(self)
func zone():return D.level_for(node)
func set_mode(next):
	mode=next;inputs.reset();inputs.enabled=mode=="run" and settings.touch
func title():
	clear_arena();set_mode("title");ui.title();sound.track("orbit_home")
func clear_arena():
	tasks.clear();enemies.clear();projectiles.clear()
	for n in [world,player,fx]:
		if is_instance_valid(n):n.queue_free()
	world=null;player=null;fx=null
	for n in get_children():
		if n is Enemy or n is Projectile:n.queue_free()
func stage_scene(z,boss=false,hero=""):
	clear_arena();world=World.new();add_child(world);world.build(self,z,boss);fx=Fx.new();add_child(fx);fx.setup(self);player=Player.new();add_child(player);player.setup(self,save.profile.selected if hero=="" else hero);player.position=Vector3(5,0,0);cam_x=12
func hub():
	set_mode("hub");stage_scene(0,true);player.position=Vector3(11.5,0,.4);player.facing=1;ui.hub();sound.track("orbit_home")
func select_hero(id):
	save.profile.selected=id;save.flush();sound.play("ui");hub()
func buy_meta(id):
	var level=int(save.profile.meta[id]);var price=D.META[id][2]*(level+1)
	if level>=D.META[id][3] or save.profile.shards<price:return
	save.profile.shards-=price;save.profile.meta[id]=level+1;save.flush();sound.play("loot");hub()
func new_run():
	seed_value=randi();node=0;wave=0;relics=[];offers=[];route="quiet";shards=0;kills=0;elapsed=0;combo=0;best_combo=0;save.profile.run={}
	story([["REI","Dulu, cincin itu mengangkat kota kita. Sekarang ia menelan setiap fajar."],["KAEL","Ada tiga simpul yang mengikatnya. Putuskan semua, dan malam ini akan menjadi malam terakhir."],["REI","Kita tidak sedang mengembalikan kemarin. Kita sedang memberi esok sebuah tempat."]],func():enter_room(0))
func enter_room(index):
	set_mode("loading");ui.loading();_load_room.call_deferred(index)
func _load_room(index):
	await get_tree().process_frame
	var previous=player.snapshot() if is_instance_valid(player) else {};var hero=player.hero if is_instance_valid(player) else save.profile.selected
	node=index;wave=0;room_clearing=false;hitstop=0;slow=0;combo=0;stage_scene(zone(),node%3==2,hero)
	if index>0 and not previous.is_empty():player.restore(previous);player.position=Vector3(5,0,0);player.hp=minf(player.max_hp,player.hp+player.max_hp*.12)
	sound.track(D.ZONES[zone()].music)
	if node%3==2:
		story([[D.ZONES[zone()].boss_name,["Tidak ada yang melewati gerbang ini tanpa meninggalkan cahayanya.","Aku menyimpan suara mereka dalam kaca. Jangan paksa aku memecahkannya.","Kau menyebutnya penjara. Aku menyebutnya kota yang tak akan kehilangan siapa pun lagi."][zone()]],[hero.to_upper(),["Cahaya yang disimpan terlalu lama akan padam. Minggir, Vahl.","Ingatan bukan sangkar, Syra. Biarkan mereka bernyanyi lagi.","Kota yang berhenti berubah sudah kehilangan semuanya."][zone()]]],func():begin_encounter())
	else:begin_encounter()
func begin_encounter():
	set_mode("run");ui.hud();spawn_wave();toast(D.ZONES[zone()].name,2.8);snapshot()
func spawn(kind,x,elite=false):
	var e=Enemy.new();add_child(e);e.setup(self,kind,x,elite);enemies.append(e);fx.ring(e.position+Vector3.UP,Color("ff9e78"),1,.5);return e
func spawn_wave():
	if node%3==2:spawn(D.ZONES[zone()].boss,30);sound.track("boss");return
	var count=3+(1 if route=="forge" and node%3==1 else 0)
	if route=="quiet" and node%3==1:count=2
	for i in range(count):
		var roster=["hollow","hollow","shield","oracle"]
		var kind=roster[(i+wave+zone())%4];spawn(kind,22+i*5,node%3==1 and i==count-1)
func nearest(p):
	var best=null;var distance=999.0
	for e in enemies:
		if not is_instance_valid(e) or e.dead:continue
		var d=e.position.distance_to(p)
		if d<distance:distance=d;best=e
	return best
func shoot(p,v,damage,friendly=true,pierce=false,homing=false,radius=.25):
	if projectiles.size()>=70:return
	var shot=Projectile.new();add_child(shot);shot.setup(self,p,v,damage,friendly,pierce,homing,radius);projectiles.append(shot)
func area(p,radius,damage,launch=0.0,heavy=false):
	for e in enemies.duplicate():
		if is_instance_valid(e) and not e.dead and (e.position+Vector3.UP-p).length()<radius+e.height*.2:
			e.hurt(damage,signf(e.position.x-p.x),launch,heavy);on_hit(e,damage,heavy)
func meteor(x,damage,friendly):
	var color=player.color if friendly else Color("ff755b");fx.beam(Vector3(x+2,9,-.1),Vector3(x,.1,0),color,.19,.26);fx.death(Vector3(x,.3,0),color);fx.ring(Vector3(x,.1,0),color,1.6,.4,true);sound.play("break",.8,.5)
	if friendly:area(Vector3(x,1,0),2.7,damage,5,true);impact(.025,.08)
	elif absf(player.position.x-x)<1.5 and player.position.y<2.4:player.hurt(damage,signf(player.position.x-x))
func on_hit(e,damage,heavy=false):
	if mode!="run":return
	combo+=1;best_combo=maxi(combo,best_combo);combo_timer=3.4;player.energy=minf(100,player.energy+3.6);impact(.047 if heavy else .026,.11 if heavy else .045);sound.play("hit",.8 if heavy else 1.15,.5)
	if player.has("vampire") and combo%3==0:player.hp=minf(player.max_hp,player.hp+3)
	if player.has("tempo"):
		player.skill_cd=maxf(0,player.skill_cd-.18)
		if combo%20==0:player.potions=mini(5,player.potions+1);toast("IRAMA · +1 PEMULIHAN",1.4)
	if player.has("chain") and combo%3==0:
		var count=0
		for other in enemies.duplicate():
			if other!=e and not other.dead and other.position.distance_to(e.position)<7:
				fx.beam(e.position+Vector3.UP,other.position+Vector3.UP,Color("c3ddff"),.05,.19);other.hurt(14,player.facing,0,false);count+=1
				if count==2:break
func enemy_defeated(e):
	var p=e.position+Vector3.UP*e.height*.5;var boss=e.boss;enemies.erase(e);kills+=1;shards+=30 if boss else (10 if e.elite else 4);player.energy=minf(100,player.energy+5);fx.death(p,Color("ffd5a2"),boss);sound.play("break",.7 if boss else 1.1,.6);e.queue_free()
	if boss:
		impact(.12,.25);clear_projectiles()
		for other in enemies.duplicate():
			fx.death(other.position+Vector3.UP,Color("ffd5a2"));other.dead=true;other.queue_free()
		enemies.clear()
	if player.has("volatile"):
		after(.03,func():area(p,3.2,24,4,false);fx.ring(p,Color("ffb18a"),1.6,.3))
	if enemies.is_empty() and not room_clearing:
		room_clearing=true
		after(1.05,func():
			if node%3!=2 and wave<1:wave+=1;room_clearing=false;spawn_wave();toast("GELOMBANG II",1.5)
			else:room_complete())
func clear_projectiles():
	for p in projectiles:
		if is_instance_valid(p):p.queue_free()
	projectiles.clear()
func room_complete():
	clear_projectiles();tasks.clear();shards+=10
	if node==8:
		story([["ORVAN","Jika fajar kembali... siapa yang akan mengingat mereka?"],["KAEL","Kami. Dan orang-orang yang belum sempat mereka temui."],["REI","Lihat ke timur, Orvan. Mereka tidak meminta kita berhenti."],["NARASI","Cincin terakhir terbuka. Untuk pertama kalinya sejak Orbit Pecah, bayangan kota bergerak.\nDan bersama cahaya, datang sesuatu yang belum mereka kenal: sebuah hari baru."]],func():victory());return
	offers=D.choices(seed_value,node,relics,route=="forge" and node%3==1);set_mode("upgrade");ui.upgrade();sound.play("loot");snapshot()
func choose_relic(id):
	if id not in offers:return
	relics.append(id);sound.play("loot");offers=[]
	if node%3==0:set_mode("route");ui.route();snapshot()
	else:enter_room(node+1)
func choose_route(id):
	route=id
	if id=="quiet":player.hp=minf(player.max_hp,player.hp+player.max_hp*.25);player.potions=mini(5,player.potions+1)
	else:shards+=25
	enter_room(node+1)
func after(delay,callback):tasks.append({"time":delay,"call":callback})
func impact(stop,power):hitstop=maxf(hitstop,stop);shake=maxf(shake,power);flash=maxf(flash,power*2)
func toast(text,seconds=2.0):message=text;message_timer=seconds
func story(lines,done):
	dialogue=lines;dialogue_index=0;dialogue_done=done;set_mode("dialog");ui.dialog()
func next_dialog():
	dialogue_index+=1;sound.play("ui",1,.4)
	if dialogue_index>=dialogue.size():
		var cb=dialogue_done;dialogue_done=Callable();if cb.is_valid():cb.call()
	else:ui.dialog()
func skip_dialog():
	dialogue_index=dialogue.size()-1;next_dialog()
func pause():
	if mode=="run":snapshot();set_mode("pause");ui.pause()
	elif mode=="pause":set_mode("run");ui.hud()
func settings_screen():previous_mode=mode;set_mode("settings");ui.settings_screen()
func settings_back():
	save.flush();set_mode(previous_mode)
	match mode:
		"pause":ui.pause()
		"hub":ui.hub()
		_:title()
func preset(index):
	settings.preset=index;settings.scale=[.58,.72,.85,1.0][index];settings.effects=[.35,.65,1.0,1.25][index];settings.shadows=index>=2;settings.post=index>=1;apply_settings();ui.settings_screen()
func apply_settings():
	Engine.max_fps=int(settings.fps);get_viewport().scaling_3d_scale=float(settings.scale);get_viewport().msaa_3d=[Viewport.MSAA_DISABLED,Viewport.MSAA_DISABLED,Viewport.MSAA_2X,Viewport.MSAA_4X][int(settings.preset)]
	if is_instance_valid(world):world.configure()
	if post:post.material.set_shader_parameter("enabled",settings.post)
	inputs.enabled=mode=="run" and settings.touch
func snapshot():
	if not is_instance_valid(player) or mode not in ["run","upgrade","route","pause"]:return
	var rows=[]
	for e in enemies:
		if is_instance_valid(e) and not e.dead:rows.append(e.snapshot())
	save.profile.run={"hero":player.hero,"node":node,"wave":wave,"route":route,"seed":seed_value,"shards":shards,"kills":kills,"elapsed":elapsed,"best_combo":best_combo,"relics":relics.duplicate(),"offers":offers.duplicate(),"player":player.snapshot(),"enemies":rows,"phase":"run" if mode=="pause" else mode,"clearing":room_clearing};save.flush()
func resume_run():
	var r=save.profile.run.duplicate(true)
	if r.is_empty():new_run();return
	node=int(r.node);wave=int(r.get("wave",0));route=r.get("route","quiet");seed_value=int(r.seed);shards=int(r.shards);kills=int(r.kills);elapsed=float(r.elapsed);best_combo=int(r.best_combo);relics=r.relics.filter(func(k):return D.RELICS.has(k));offers=r.get("offers",[]);room_clearing=bool(r.get("clearing",false));stage_scene(zone(),node%3==2,r.hero);player.restore(r.player)
	for row in r.enemies:
		var e=spawn(row.kind,float(row.x),bool(row.elite));e.hp=clampf(float(row.hp),1,e.max_hp);e.position.y=float(row.y);e.phase=int(row.phase);e.attack_index=int(row.attack_index)
	set_mode(r.phase);sound.track("boss" if node%3==2 else D.ZONES[zone()].music)
	match mode:
		"upgrade":ui.upgrade()
		"route":ui.route()
		_:
			set_mode("run");ui.hud()
			if enemies.is_empty():
				if room_clearing:after(.8,func():
					if node%3!=2 and wave<1:wave+=1;room_clearing=false;spawn_wave()
					else:room_complete())
				else:spawn_wave()
func bank():save.profile.shards+=shards;save.profile.run={};save.flush()
func abandon():snapshot();hub()
func defeat():
	bank();set_mode("defeat");clear_projectiles();tasks.clear();ui.result(false);sound.track("orbit_home")
func victory():
	save.profile.wins+=1
	if save.profile.best<=0 or elapsed<save.profile.best:save.profile.best=elapsed
	bank();set_mode("victory");ui.result(true);sound.play("victory");sound.track("orbit_home")
func _input(event):
	inputs.event(event)
	if event is InputEventKey and event.pressed and event.physical_keycode==KEY_F12 and OS.has_feature("editor"):capture()
func _notification(what):
	if what in [NOTIFICATION_APPLICATION_FOCUS_OUT,NOTIFICATION_APPLICATION_PAUSED] and mode=="run":pause()
	if what==NOTIFICATION_WM_GO_BACK_REQUEST:
		if mode=="run":pause()
		elif mode=="settings":settings_back()
func capture(path="res://evidence/gameplay.png"):
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png(path)
func _process(real_dt):
	var dt=minf(real_dt,.05);inputs.tick(dt);sound.tick(dt);message_timer=maxf(0,message_timer-dt);flash=move_toward(flash,0,dt*2.5);shake=move_toward(shake,0,dt*.7)
	post.material.set_shader_parameter("pulse",flash)
	if get_viewport().get_visible_rect().size!=inputs.viewport:inputs.resize(get_viewport().get_visible_rect().size)
	if inputs.take("pause"):pause()
	if world:world.tick(dt)
	if mode=="run":
		elapsed+=real_dt;autosave+=dt;combo_timer-=dt
		if combo_timer<=0:combo=0
		if autosave>5:autosave=0;snapshot()
		if hitstop>0:hitstop-=dt
		else:
			if slow>0:slow-=dt;dt*=.35
			player.tick(dt)
			if mode=="run":
				for e in enemies.duplicate():
					if is_instance_valid(e) and not e.dead:e.tick(dt)
					if mode!="run":break
				for p in projectiles.duplicate():
					if is_instance_valid(p) and p in projectiles and not p.tick(dt):projectiles.erase(p);p.queue_free()
				var due=[]
				for task in tasks:task.time-=dt;if task.time<=0:due.append(task)
				for task in due:tasks.erase(task)
				for task in due:
					if task.call.is_valid() and mode=="run":task.call.call()
	elif mode=="hub" and player:player.rig.pose(dt,"idle",0,0,true,1)
	if fx:fx.tick(dt)
	if player:
		if mode=="hub":camera.size=4.9;camera.position=Vector3(14,2.65,10);camera.look_at(Vector3(14,1.35,0));player.rig.rotation.y=.22
		else:
			cam_x=lerpf(cam_x,clampf(player.position.x+player.facing*.8,10.5,33.5),1-exp(-dt*5.5));var focus=Vector3(cam_x,2.4+clampf(player.position.y*.18,0,.8),0)
			camera.size=lerpf(camera.size,9.8 if player.state=="ult" else 10.8,1-exp(-dt*5));camera.position=focus+Vector3(randf_range(-1,1)*shake*settings.shake,2.7+randf_range(-1,1)*shake*settings.shake,13);camera.look_at(focus)
	ui.tick(dt)
