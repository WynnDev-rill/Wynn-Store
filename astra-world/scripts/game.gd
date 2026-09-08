extends Node3D
const Data=preload("res://scripts/data.gd")
const State=preload("res://scripts/state.gd")
const World=preload("res://scripts/world.gd")
const Player=preload("res://scripts/player.gd")
const Enemy=preload("res://scripts/enemy.gd")
const UI=preload("res://scripts/ui.gd")
const FX=preload("res://scripts/fx.gd")
const Sound=preload("res://scripts/audio.gd")
var state
var world
var player
var ui
var fx
var audio
var enemies:Array=[]
var playing=false
var has_save=false
var nearby:Dictionary={}
var region_name="Lembah Aeralis"
var autosave=0.0
var scan_clock=0.0
var puzzle_sequence:Array=[]
var elapsed=0.0
var title_camera:Camera3D
var qa_mode=false
var shutting_down=false

func _exit_tree():
 var art=preload("res://scripts/art.gd")
 art.meshes.clear(); art.materials.clear()

func _ready():
 get_tree().auto_accept_quit=false
 _inputs()
 state=State.new()
 qa_mode="--qa" in OS.get_cmdline_user_args()
 if qa_mode: state.path="user://qa_aeralis.json"
 else: has_save=state.load_save()
 world=World.new(); add_child(world); world.build(self)
 player=Player.new(); add_child(player); player.setup(self); _place_player(Vector2(-5,82))
 fx=FX.new(); fx.game=self; add_child(fx)
 audio=Sound.new(); add_child(audio); audio.setup(self)
 ui=UI.new(); add_child(ui); ui.setup(self)
 _spawn_enemies(); world.refresh()
 title_camera=Camera3D.new(); title_camera.fov=63; title_camera.far=700; add_child(title_camera); title_camera.position=Vector3(34,27,98); title_camera.look_at(Vector3(-4,12,26)); title_camera.current=true
 apply_settings()
 print("ASTRA_READY trees=%d static_parts=%d batches=%d"%[world.trees_count,world.static_mesh_count,world.batches.size()])
 if qa_mode:
  var qa=load("res://tests/playthrough.gd").new(); qa.game=self; add_child(qa)
 elif "--startup-check" in OS.get_cmdline_user_args():
  await get_tree().create_timer(1.0).timeout
  quit_cleanly()

func quit_cleanly(code:int=0):
 if shutting_down: return
 shutting_down=true
 if playing: save()
 playing=false; ui.blocking=true; ui.hud.release_all()
 audio.shutdown()
 # The audio mixer releases stopped playback references on its next cycle.
 # Quitting on the same frame races that cleanup in headless and desktop runs.
 await get_tree().create_timer(0.3).timeout
 get_tree().quit(code)

func _inputs():
 var keys={"forward":KEY_W,"back":KEY_S,"left":KEY_A,"right":KEY_D,"jump":KEY_SPACE,"sprint":KEY_SHIFT,"attack":KEY_J,"skill":KEY_Q,"dodge":KEY_K,"interact":KEY_E,"heal":KEY_H,"map":KEY_M,"inventory":KEY_I,"pause":KEY_ESCAPE}
 for action in keys:
  if not InputMap.has_action(action): InputMap.add_action(action)
  var e=InputEventKey.new(); e.physical_keycode=keys[action]; InputMap.action_add_event(action,e)

func _spawn_enemies():
 var specs=[
 ["west_1",Vector2(-55,16),0],["west_2",Vector2(-70,1),0],
 ["east_1",Vector2(54,10),0],["east_2",Vector2(64,-9),0],
 ["north_1",Vector2(-29,-47),0],["north_2",Vector2(-14,-62),0],
 ["path_1",Vector2(-36,21),0],["path_2",Vector2(35,17),0],
 ["path_3",Vector2(9,-32),0],["path_4",Vector2(-43,-27),0],
 ["elite",Vector2(-79,-24),1],["aru",Vector2(27,-92),2]]
 for spec in specs:
  var e=Enemy.new(); add_child(e); e.setup(self,spec[0],world.p(spec[1],0.1),spec[2]); enemies.append(e)

func _process(dt):
 if shutting_down: return
 elapsed+=dt
 if not playing:
  if title_camera:
   title_camera.position.x=34+sin(elapsed*0.055)*4
   title_camera.look_at(Vector3(-4,12,26))
  return
 if ui.blocking: return
 state.s.playtime+=dt; scan_clock-=dt; autosave+=dt
 if scan_clock<=0: _scan(); scan_clock=0.2
 if autosave>20: save(); autosave=0
 if Input.is_action_just_pressed("interact"): interact()
 if Input.is_action_just_pressed("map"): ui.open("map")
 if Input.is_action_just_pressed("inventory"): ui.open("inventory")

func _unhandled_input(e):
 if e is InputEventKey and e.pressed and not e.echo:
  if e.physical_keycode==KEY_ESCAPE:
   if ui.page=="dialogue": ui.advance_dialogue()
   elif ui.blocking and playing: ui.close()
   elif playing: ui.open("pause")
  elif e.physical_keycode==KEY_ENTER and ui.page=="dialogue": ui.advance_dialogue()

func _notification(what):
 if state==null or ui==null: return
 if what==NOTIFICATION_APPLICATION_PAUSED or what==NOTIFICATION_APPLICATION_FOCUS_OUT:
  if playing: save(); ui.hud.release_all()
 if what==NOTIFICATION_WM_GO_BACK_REQUEST:
  if playing and not ui.blocking: ui.open("pause")
  elif playing: ui.close()
 if what==NOTIFICATION_WM_CLOSE_REQUEST:
  quit_cleanly()

func _place_player(pos:Vector2):
 player.position=world.p(pos,0.7); player.velocity=Vector3.ZERO; player.camera_snap()

func start_new():
 if has_save: state.save()
 state.reset(); has_save=true; puzzle_sequence.clear()
 for e in enemies:
  e.dead=false; e.hp=e.maximum; e.visible=true; e.position=e.home; e.attack_timer=1.2; e.telegraph=0; e.telegraph_node.visible=false
  e.model.scale=Vector3.ONE*(2.15 if e.boss else (1.35 if e.elite else 1.0))
 for d in world.interactables: d.node.visible=true
 world.refresh(); player.stamina=100; player.yaw=0; player.pitch=-0.22; _place_player(Vector2(-5,82))
 playing=true; player.camera.current=true; ui.close(); audio.set_mode("aeralis"); save()
 ui.dialogue(Data.DIALOGUE.intro,func():ui.toast("Berjalan ke desa. Geser sisi kanan untuk melihat sekitar."))

func resume_game():
 playing=true; _place_player(Vector2(state.s.position[0],state.s.position[1])); player.camera.current=true; ui.close(); world.refresh(); audio.set_mode("aeralis")

func quest() -> Dictionary: return Data.quest(state.s)

func objective_short() -> String:
 if not state.s.met_ilya: return "Temui Ilya di Desa Teralun"
 if not state.s.forged: return "Bilah Penala · Kayu %d/3 · Giok %d/3"%[mini(3,state.amount("wood")),mini(3,state.amount("crystal"))]
 if state.s.boss and not state.s.ending: return "Kembali kepada Ilya"
 if state.s.ending: return "Temukan surat, taman, dan peti yang tersisa"
 if state.s.beacons.size()==3: return "Pulihkan Aru di Mahkota Sunyi"
 return "Bersihkan penjaga · selaraskan mercusuar"

func _scan():
 nearby={}; var best=3.6
 for d in world.interactables:
  if not d.node.visible: continue
  var distance=player.global_position.distance_to(d.node.global_position)
  if distance<best:
   best=distance; nearby=d
 for l in Data.LANDMARKS:
  if Vector2(player.position.x,player.position.z).distance_to(l.pos)<22:
   region_name=l.name
   if not l.id in state.s.discovered:
    state.s.discovered.append(l.id); ui.toast("Ditemukan · "+l.name); state.s.xp+=12; save()
 var e=nearest_enemy(player.position,14)
 audio.set_mode("combat" if e!=null else "aeralis")

func nearest_enemy(pos:Vector3,range_m:float,boss_only:bool=false):
 var best=null; var dist=range_m
 for e in enemies:
  if not is_instance_valid(e) or e.dead or (boss_only and not e.boss): continue
  if e.boss and state.s.beacons.size()<3: continue
  var d=e.position.distance_to(pos)
  if d<dist: best=e; dist=d
 return best

func interact():
 if ui.blocking or nearby.is_empty(): return
 var d=nearby
 match d.kind:
  "npc":
   match d.id:
    "ilya":
     if state.s.boss and not state.s.ending:
      ui.dialogue(Data.DIALOGUE.ending,func():state.s.ending=true; save(); audio.set_mode("home"); ui.open("ending"))
     elif not state.s.met_ilya:
      ui.dialogue(Data.DIALOGUE.ilya_start,func():state.s.met_ilya=true; save())
     elif state.s.ending: ui.dialogue([["Ilya","Untuk pertama kalinya, aku ingin hari ini berlangsung lebih lama. Duduklah. Lonceng masih berbunyi."]])
     else: ui.dialogue([["Ilya",quest().body]])
    "sava": ui.dialogue(Data.DIALOGUE.sava)
    "mira":
     if state.amount("letter")>=3 and not state.s.mira_done:
      ui.dialogue(Data.DIALOGUE.mira_done,func():state.s.mira_done=true; state.add("crystal",6); state.s.xp+=70; save())
     elif state.s.mira_done: ui.dialogue([["Mira","Aku sudah membalas suratnya. Akan kubiarkan angin yang mengantar."]])
     else: state.s.mira=true; save(); ui.dialogue(Data.DIALOGUE.mira)
  "gather":
   if d.id in state.s.collected: return
   state.add(d.item,1); state.s.collected.append(d.id); d.node.visible=false; audio.sfx("gather",-8); fx.burst(d.node.position+Vector3.UP*0.6,Data.ITEMS[d.item].color,5); ui.toast("+1 "+Data.ITEMS[d.item].name); save()
  "chest":
   if d.get("secret",false) and not state.s.puzzle: ui.toast("Tiga batu di taman belum bernyanyi dalam urutan yang tepat."); return
   if d.id in state.s.collected: return
   state.s.collected.append(d.id); d.node.visible=false; state.add("crystal",2); state.add("wood",2); state.add("potion",1); state.s.xp+=25
   if d.letter: state.add("letter",1)
   ui.toast("Peti terbuka · +2 Giok · +2 Kayu · +1 Bekal"+(" · +1 Surat" if d.letter else "")); audio.sfx("beacon",-10); save()
  "forge": ui.open("craft")
  "rest":
   if nearest_enemy(player.position,13)!=null: ui.toast("Penjaga masih terlalu dekat."); return
   state.s.hp=state.max_hp(); state.add("potion",maxi(0,3-state.amount("potion"))); player.stamina=100; save(); audio.sfx("heal",-6); ui.toast("Beristirahat · kesehatan dan bekal dipulihkan.")
  "beacon":
   if d.id in state.s.beacons: ui.toast("Mercusuar telah menyala. Perjalanan cepat tersedia di peta."); return
   if not state.s.forged: ui.toast("Buat Bilah Penala di Tungku Sava terlebih dahulu."); return
   var blocked=false
   for e in enemies:
    if not e.dead and e.id.begins_with(d.id+"_"): blocked=true
   if blocked: ui.toast("Tenangkan kedua penjaga di sekitar mercusuar."); return
   state.s.beacons.append(d.id); state.s.xp+=60; state.s.hp=state.max_hp(); state.add("potion",1); world.refresh(); audio.sfx("beacon",-3); fx.wave(d.node.position+Vector3.UP,12,"a2e5cf",1.0); save()
   ui.dialogue([["Nada yang kembali",Data.BEACON_TEXT[d.id]]],func():
    if state.s.beacons.size()==3: ui.toast("Jalan menuju Aru kini terbuka di Mahkota Sunyi."))
  "note": ui.dialogue(Data.DIALOGUE.lore)
  "rune":
   if state.s.puzzle: ui.toast("Taman telah mengingat lagunya."); return
   if d.order==puzzle_sequence.size():
    puzzle_sequence.append(d.order); audio.sfx("gather",-7); fx.burst(d.node.position+Vector3.UP,"d5e2b5",8)
    if puzzle_sequence.size()==3: state.s.puzzle=true; state.s.xp+=60; save(); audio.sfx("beacon",-5); ui.toast("Akar · Hujan · Fajar. Peti taman kini terbuka.")
    else: ui.toast(d.label+" bergema. Dengarkan batu berikutnya.")
   else: puzzle_sequence.clear(); audio.sfx("warning",-14); ui.toast("Nadanya terputus. Mulai kembali dari akar.")
 _scan()

func craft(recipe:Dictionary) -> bool:
 if Vector2(player.position.x,player.position.z).distance_to(Vector2(10,47))>5: return false
 if not state.craft(recipe): return false
 audio.sfx("beacon",-7); ui.toast(Data.ITEMS[recipe.id].name+" berhasil ditempa."); save(); return true

func save():
 state.s.position=[player.position.x,player.position.z]
 if not state.save() and ui: ui.toast(state.last_error)

func rescue(message:String):
 state.s.hp=state.max_hp(); player.stamina=100; player.invulnerable=3.0; _place_player(Vector2(4,55)); save(); ui.toast(message.replace("te ram","temaram"))

func fast_travel(id:String):
 if nearest_enemy(player.position,17)!=null: ui.toast("Perjalanan cepat tidak tersedia saat penjaga berada di dekatmu."); return
 if id!="village" and not id in state.s.beacons: return
 var l=Data.landmark(id); _place_player(l.pos+Vector2(0,6)); player.invulnerable=2; ui.close(); save(); ui.toast(l.name)

func chests_opened() -> int:
 var n=0
 for id in state.s.collected:
  if str(id).begins_with("chest_"): n+=1
 return n

func set_preset(i:int):
 var st=state.settings; st.preset=i
 st.resolution=[0.60,0.75,0.9,1.0][i]; st.foliage=[0.25,0.5,0.75,1.0][i]
 st.shadows=i>0; st.effects=i>0; st.fps=[30,45,60,60][i]
 apply_settings()

func apply_settings():
 Engine.max_fps=int(state.settings.fps)
 get_viewport().scaling_3d_scale=state.settings.resolution
 get_viewport().msaa_3d=Viewport.MSAA_2X if state.settings.preset>=2 else Viewport.MSAA_DISABLED
 if world: world.apply_settings()
 if audio: audio.apply()
 state.save_settings()
