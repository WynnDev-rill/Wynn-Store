extends Node
## Input-driven rendered integration run. Never grants XP, inventory, kills,
## teleports, invulnerability or quest flags. QA saves are isolated from users.
var game
var steps:Array=[]
var failed=false
var frame_times:Array=[]
var started=0
var fighting=false

func _ready():
 started=Time.get_ticks_msec(); run.call_deferred()

func _process(dt):
 if game.playing and not game.ui.blocking: frame_times.append(dt)

func tick(seconds:float): await get_tree().create_timer(seconds).timeout

func log_step(t:String):
 print("QA_STEP "+t); steps.append({"step":t,"seconds":(Time.get_ticks_msec()-started)/1000.0,"position":[game.player.position.x,game.player.position.y,game.player.position.z],"hp":game.state.s.hp,"xp":game.state.s.xp})

func check(condition:bool,t:String):
 if not condition:
  failed=true; log_step("FAIL "+t); await shot("FAIL"); finish()
  await tick(30)

func shot(name:String):
 await RenderingServer.frame_post_draw
 var img=get_viewport().get_texture().get_image()
 img.save_png("res://evidence/"+name+".png")

func press(action:String):
 Input.action_press(action); await tick(0.065); Input.action_release(action); await tick(0.09)

func click_text(t:String) -> bool:
 for b in game.ui.panel_root.find_children("*","Button",true,false):
  if b.text==t and b.is_visible_in_tree() and not b.disabled:
   var event=InputEventMouseButton.new(); event.position=b.get_global_rect().get_center(); event.button_index=MOUSE_BUTTON_LEFT; event.pressed=true; Input.parse_input_event(event)
   await tick(0.08); event=event.duplicate(); event.pressed=false; Input.parse_input_event(event); await tick(0.18); return true
 return false

func dialogue():
 var guard=0
 while game.ui.page=="dialogue" and guard<12:
  await tick(0.12)
  await click_text("Lanjut  ›" if game.ui.dialogue_index<game.ui.dialogue_lines.size()-1 else "Kembali ke perjalanan")
  guard+=1

func stop_move():
 for action in ["left","right","forward","back","sprint"]: Input.action_release(action)

func go(v:Vector2,seconds:float=36.0):
 var begin=Time.get_ticks_msec(); var previous=game.player.position; var stuck=0.0
 while Vector2(game.player.position.x,game.player.position.z).distance_to(v)>1.45:
  if (Time.get_ticks_msec()-begin)/1000.0>seconds:
   stop_move(); await check(false,"walk timeout to "+str(v)); return
  if game.ui.blocking: await dialogue()
  var enemy=game.nearest_enemy(game.player.position,7.5)
  if enemy!=null:
   stop_move(); await fight(enemy); begin=Time.get_ticks_msec()
  var direction=v-Vector2(game.player.position.x,game.player.position.z)
  var local=direction.normalized().rotated(game.player.yaw)
  for pair in [["left",maxf(0,-local.x)],["right",maxf(0,local.x)],["forward",maxf(0,-local.y)],["back",maxf(0,local.y)]]:
   if pair[1]>0.03: Input.action_press(pair[0],pair[1])
   else: Input.action_release(pair[0])
  if game.player.position.distance_to(previous)<0.03: stuck+=0.12
  else: stuck=0
  if stuck>1.0: await press("jump"); stuck=0
  previous=game.player.position
  await tick(0.12)
 stop_move(); await tick(0.3)

func use(id:String):
 var d={}
 for item in game.world.interactables:
  if item.id==id: d=item
 await check(not d.is_empty(),"interaction exists "+id)
 var v=Vector2(d.node.position.x,d.node.position.z)
 await go(v)
 await tick(0.3)
 await check(not game.nearby.is_empty() and game.nearby.id==id,"context target "+id)
 await press("interact"); await tick(0.25)

func fight(enemy):
 if enemy.dead: return
 var begin=Time.get_ticks_msec()
 while not enemy.dead:
  if Time.get_ticks_msec()-begin>95000: await check(false,"combat timeout "+enemy.id); return
  if game.ui.blocking: await dialogue()
  var delta=enemy.position-game.player.position
  game.player.yaw=atan2(-delta.x,-delta.z) # Camera input equivalent; never moves the actor.
  if delta.length()>3.0:
   Input.action_press("forward")
  else: stop_move()
  if enemy.telegraph>0 and enemy.telegraph<0.32:
   Input.action_release("forward"); Input.action_press("back"); await press("dodge"); Input.action_release("back")
  if game.state.s.hp<game.state.max_hp()*0.6: await press("heal")
  await press("skill"); await press("attack")
  await tick(0.12)
 stop_move(); log_step("combat "+enemy.id)

func run():
 await tick(2); await shot("01-title")
 await click_text("Perjalanan baru"); await dialogue()
 log_step("new game through title"); await shot("02-opening")
 # Touch movement and independent camera ownership are exercised through events.
 var touch=InputEventScreenTouch.new(); touch.index=0; touch.position=Vector2(140,580); touch.pressed=true; Input.parse_input_event(touch)
 var drag=InputEventScreenDrag.new(); drag.index=0; drag.position=Vector2(140,530); drag.relative=Vector2(0,-50); Input.parse_input_event(drag)
 var look=InputEventScreenTouch.new(); look.index=1; look.position=Vector2(700,300); look.pressed=true; Input.parse_input_event(look)
 var ld=InputEventScreenDrag.new(); ld.index=1; ld.position=Vector2(720,300); ld.relative=Vector2(20,0); Input.parse_input_event(ld)
 var before=game.player.position; await tick(0.5)
 await check(game.player.position.distance_to(before)>0.4,"touch joystick moves player with camera drag")
 touch.pressed=false; look.pressed=false; Input.parse_input_event(touch); Input.parse_input_event(look); game.player.yaw=0
 await tick(0.2); await check(game.player.move_touch==Vector2.ZERO,"touch release clears movement"); log_step("multitouch")
 # Gather on the introductory trail before entering the village.
 for id in ["gather_7","gather_6","gather_5","gather_3","gather_2","gather_0"]: await use(id)
 await go(Vector2(-3,59)); await use("ilya"); await dialogue(); log_step("Ilya introduction")
 await go(Vector2(-3,55)); await go(Vector2(10,53)); await use("forge"); await shot("03-crafting")
 await click_text("Tempa"); await check(game.state.s.forged,"forged through crafting UI"); await click_text("Kembali")
 await use("rest"); await go(Vector2(-5,57)); await use("mira"); await dialogue()
 await go(Vector2(-6,47)); await go(Vector2(-27,26)); await go(Vector2(-51,16));
 for e in game.enemies:
  if e.id.begins_with("west_"): await fight(e)
 await use("west"); await dialogue(); log_step("west beacon"); await shot("04-bell-garden")
 await go(Vector2(-41,2)); await go(Vector2(3,10)); await go(Vector2(49,13))
 for e in game.enemies:
  if e.id.begins_with("east_"): await fight(e)
 await use("east"); await dialogue(); log_step("east beacon"); await shot("05-mirror-pool")
 await use("chest_1"); await go(Vector2(61,-15)); await go(Vector2(15,-31)); await go(Vector2(-22,-43))
 for e in game.enemies:
  if e.id.begins_with("north_"): await fight(e)
 await use("north"); await dialogue(); log_step("north beacon"); await shot("06-sky-archive")
 await use("chest_2")
 await go(Vector2(-30,-41)); await go(Vector2(-63,-28))
 for e in game.enemies:
  if e.elite: await fight(e)
 await use("note"); await dialogue()
 for id in ["rune_0","rune_1","rune_2"]: await use(id)
 await use("chest_3"); await check(game.state.s.puzzle,"secret puzzle solved"); log_step("secret and elite"); await shot("07-secret")
 await press("map"); await shot("08-map"); await click_text("Desa Teralun")
 await go(Vector2(5,61)); await go(Vector2(36,69)); await go(Vector2(67,69)); await use("chest_0")
 await press("map"); await click_text("Desa Teralun"); await go(Vector2(-5,57)); await use("mira"); await dialogue(); await check(game.state.s.mira_done,"letter side quest complete")
 await go(Vector2(4,55)); await use("rest"); await press("map"); await click_text("Arsip Langit"); await go(Vector2(-8,-70)); await go(Vector2(22,-78)); await shot("09-boss-approach")
 for e in game.enemies:
  if e.boss: await fight(e)
 await dialogue(); await check(game.state.s.boss,"boss defeated"); log_step("boss complete")
 await press("map"); await click_text("Desa Teralun"); await go(Vector2(-3,55)); await use("ilya"); await dialogue()
 await check(game.state.s.ending,"Region 1 ending reached"); await shot("10-ending"); log_step("Region 1 complete")
 await click_text("Tetap menjelajahi Aeralis"); await press("inventory"); await shot("11-inventory"); await click_text("Karakter"); await shot("12-character"); await click_text("Kembali")
 game.save()
 var verify=game.State.new(); verify.path=game.state.path
 await check(verify.load_save() and verify.s.ending and verify.s.beacons.size()==3,"save reload preserves complete journey")
 game.ui.open("settings"); await shot("13-settings")
 for p in ["Low","Medium","High","Ultra"]:
  await click_text(p); await check(game.state.settings.preset==["Low","Medium","High","Ultra"].find(p),"preset "+p)
 log_step("save and presets verified"); finish()

func finish():
 stop_move(); frame_times.sort()
 var result={"passed":not failed,"duration_seconds":(Time.get_ticks_msec()-started)/1000.0,"steps":steps,"frames":frame_times.size(),"renderer":RenderingServer.get_video_adapter_name(),"p50_frame_ms":frame_times[int(frame_times.size()*0.5)]*1000 if frame_times.size()>0 else 0,"p95_frame_ms":frame_times[int(frame_times.size()*0.95)]*1000 if frame_times.size()>0 else 0,"note":"Software-rendered CI measurement; not a physical Android FPS claim."}
 var f=FileAccess.open("res://evidence/playthrough.json",FileAccess.WRITE); f.store_string(JSON.stringify(result,"  ")); f.close()
 get_tree().quit(1 if failed else 0)
