extends Control
var game
var font:Font
var serif:Font
var joy_id=-1
var look_id=-1
var joy_origin=Vector2.ZERO
var joy_pos=Vector2.ZERO
var jump_id=-1
var mouse_down=false
var hint_time=0.0
var ink=Color("f1ecd9")
var gold=Color("e6cc97")
var panel=Color(0.035,0.09,0.10,0.70)

func setup(g):
 game=g; set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT); mouse_filter=Control.MOUSE_FILTER_IGNORE
 font=load("res://assets/branding/body.ttf"); serif=load("res://assets/branding/display.ttf")

func pt(x:float,y:float) -> Vector2: return Vector2(x/1280*size.x,y/720*size.y)
func scaled(v:Vector2) -> Vector2: return Vector2(v.x/size.x*1280,v.y/size.y*720)

func _process(_dt): queue_redraw()

func text(t:String,p:Vector2,s:int=16,c:Color=Color("f1ecd9"),display:bool=false):
 draw_string(serif if display else font,pt(p.x,p.y),t,HORIZONTAL_ALIGNMENT_LEFT,-1,s*size.y/720,c)

func circle_button(c:Vector2,r:float,label:String,kind:String,cool:float=0):
 var center=pt(c.x,c.y); var radius=r*size.y/720
 draw_circle(center,radius,panel,true,-1,true); draw_arc(center,radius,0,TAU,48,Color(0.9,0.84,0.66,0.65),1.3,true)
 var w=19*size.y/720
 match kind:
  "sword":
   draw_line(center+Vector2(-w,w),center+Vector2(w,-w),ink,2.8,true)
   draw_line(center+Vector2(-w,w*0.3),center+Vector2(-w*0.2,w),gold,2.8,true)
   draw_line(center+Vector2(w,-w),center+Vector2(w*0.5,-w),ink,2,true)
  "skill":
   for i in range(3): draw_arc(center,5+i*6,0.2,5.1,30,Color("9ce3d4"),1.5,true)
  "jump":
   draw_polyline(PackedVector2Array([center+Vector2(-w*0.6,2),center+Vector2(0,-w*0.6),center+Vector2(w*0.6,2)]),ink,2,true)
   draw_line(center+Vector2(0,-w*0.6),center+Vector2(0,w*0.65),ink,2,true)
  "dodge":
   for i in range(2): draw_polyline(PackedVector2Array([center+Vector2(-w+i*13,-8),center+Vector2(-w+10+i*13,0),center+Vector2(-w+i*13,8)]),ink,2,true)
  "heal":
   draw_line(center-Vector2(9,0),center+Vector2(9,0),ink,2.2,true); draw_line(center-Vector2(0,9),center+Vector2(0,9),ink,2.2,true)
 if cool>0:
  draw_circle(center,radius,Color(0.04,0.10,0.11,0.78)); text(str(ceili(cool)),c+Vector2(-7,6),20)
 text(label,c+Vector2(-label.length()*3.4,r+21),12,ink)

func _draw():
 if game==null or not game.playing or game.ui.blocking: return
 var s=game.state; var player=game.player
 # Only relevant HUD data is shown; there is no debug or engine text by default.
 draw_style_box(game.ui.style(Color(0.035,0.09,0.1,0.55),Color(0.8,0.85,0.73,0.15),12),Rect2(pt(24,20),pt(294,81)))
 text("N A R A",Vector2(41,47),18,gold)
 text("Lv. %d"%s.level(),Vector2(253,46),14)
 draw_rect(Rect2(pt(41,60),pt(259,7)),Color("405850"))
 draw_rect(Rect2(pt(41,60),pt(259*s.s.hp/s.max_hp(),7)),Color("9bd1ac"))
 draw_rect(Rect2(pt(41,75),pt(259*player.stamina/100,3)),Color("e5c28a"))
 var q=game.quest()
 text(q.title,Vector2(30,134),18,ink,true)
 text(game.objective_short(),Vector2(30,160),13,Color("d1d9c8"))
 var dist=Vector2(player.position.x,player.position.z).distance_to(q.pos)
 text("%d m"%dist,Vector2(30,182),13,gold)
 # Compass line and live map, both based on the real player position.
 text(game.region_name,Vector2(530,37),15,ink,true)
 var mapc=pt(1186,92); var r=58*size.y/720
 draw_circle(mapc,r,panel,true,-1,true); draw_arc(mapc,r,0,TAU,64,gold,1,true)
 draw_line(mapc-Vector2(r,0),mapc+Vector2(r,0),Color(0.8,0.87,0.72,0.15),1,true)
 draw_line(mapc-Vector2(0,r),mapc+Vector2(0,r),Color(0.8,0.87,0.72,0.15),1,true)
 for l in game.Data.LANDMARKS:
  var delta=l.pos-Vector2(player.position.x,player.position.z)
  var pos=mapc+delta.limit_length(85)*r/90
  draw_circle(pos,2.5,Color("eed098") if l.id in s.s.discovered else Color("8aa69a"),true,-1,true)
 var d=q.pos-Vector2(player.position.x,player.position.z)
 draw_circle(mapc+d.limit_length(83)*r/90,4.2,gold,false,1.5,true)
 var forward=Vector2(-sin(player.yaw),-cos(player.yaw))
 draw_colored_polygon(PackedVector2Array([mapc+forward*8,mapc+forward.rotated(2.35)*6,mapc+forward.rotated(-2.35)*6]),ink)
 text("U",Vector2(1181,24),12,gold)
 text("PETA",Vector2(1168,172),11)
 for b in [["JURNAL",996],["TAS",1079],["II",1242]]: text(b[0],Vector2(b[1],38),12)
 if game.nearby.size()>0:
  var label=game.nearby.label
  var w=clampf(label.length()*9+76,220,420)
  draw_style_box(game.ui.style(Color(0.08,0.18,0.19,0.9),Color("aebda5"),24),Rect2(pt(640-w/2,512),pt(w,48)))
  text("E  ·  "+label,Vector2(640-w/2+22,543),16)
 # Floating joystick zone and separate pointer ownership allow move + camera + attack.
 var jp=Vector2(131,591) if joy_id<0 else joy_origin
 var knob=Vector2.ZERO if joy_id<0 else (joy_pos-joy_origin).limit_length(52)
 draw_circle(pt(jp.x,jp.y),63*size.y/720,Color(0.1,0.2,0.18,0.28),true,-1,true)
 draw_arc(pt(jp.x,jp.y),63*size.y/720,0,TAU,48,Color(0.86,0.90,0.81,0.30),1.4,true)
 draw_circle(pt(jp.x+knob.x,jp.y+knob.y),24*size.y/720,Color(0.92,0.93,0.82,0.36),true,-1,true)
 circle_button(Vector2(1134,587),48,"SERANG","sword")
 circle_button(Vector2(1019,581),32,"GEMA","skill",player.skill_cooldown)
 circle_button(Vector2(1226,504),30,"LOMPAT","jump")
 circle_button(Vector2(1219,657),30,"HINDAR","dodge",player.dash_cooldown)
 circle_button(Vector2(896,632),27,"BEKAL · %d"%s.amount("potion"),"heal",player.heal_cooldown)
 if game.state.s.beacons.size()>0 and not player.is_on_floor(): text("Tahan Lompat untuk melayang",Vector2(1010,454),12,gold)
 var boss=game.nearest_enemy(player.position,30,true)
 if boss!=null:
  text("A R U   ·   P E M I K U L   S U N Y I",Vector2(431,71),15,gold)
  draw_rect(Rect2(pt(430,83),pt(418,6)),Color("485656")); draw_rect(Rect2(pt(430,83),pt(418*boss.hp/boss.maximum,6)),Color("d4977c"))
 elif game.nearest_enemy(player.position,9)!=null:
  var enemy=game.nearest_enemy(player.position,9)
  text("Penjaga Akar" if enemy.elite else "Penjaga kabut",Vector2(546,78),14,gold)
  draw_rect(Rect2(pt(530,89),pt(220*enemy.hp/enemy.maximum,4)),Color("d49d80"))
 if s.settings.show_fps: text("%d FPS"%Engine.get_frames_per_second(),Vector2(29,704),12)
 if game.ui.hurt_flash>0: draw_rect(Rect2(Vector2.ZERO,size),Color(0.65,0.15,0.08,game.ui.hurt_flash*0.45))

func _input(event):
 if game==null or not game.playing or game.ui.blocking: return
 if event is InputEventScreenTouch:
  var pos=scaled(event.position)
  if event.pressed:
   if _press(pos,event.index): get_viewport().set_input_as_handled(); return
   if pos.x<450 and pos.y>310 and joy_id<0:
    joy_id=event.index; joy_origin=pos; joy_pos=pos
   elif look_id<0: look_id=event.index
  else:
   if event.index==joy_id: joy_id=-1; game.player.move_touch=Vector2.ZERO
   if event.index==look_id: look_id=-1
   if event.index==jump_id: jump_id=-1; game.player.jumping_touch=false
 elif event is InputEventScreenDrag:
  if event.index==joy_id:
   joy_pos=scaled(event.position); game.player.move_touch=(joy_pos-joy_origin).limit_length(52)/52
  elif event.index==look_id: game.player.look(event.relative*Vector2(1280/size.x,720/size.y))
 elif event is InputEventMouseButton:
  if event.button_index==MOUSE_BUTTON_RIGHT: mouse_down=event.pressed
  if event.button_index==MOUSE_BUTTON_LEFT and event.pressed:
   if _press(scaled(event.position),-2): get_viewport().set_input_as_handled()
 elif event is InputEventMouseMotion and mouse_down: game.player.look(event.relative)

func _press(pos:Vector2,index:int) -> bool:
 if pos.distance_to(Vector2(1186,92))<65: game.ui.open("map"); return true
 if pos.y<60:
  if pos.x>1220: game.ui.open("pause"); return true
  if pos.x>1060: game.ui.open("inventory"); return true
  if pos.x>980: game.ui.open("journal"); return true
 if pos.distance_to(Vector2(1134,587))<55: game.player.attack(); return true
 if pos.distance_to(Vector2(1019,581))<40: game.player.skill(); return true
 if pos.distance_to(Vector2(1226,504))<40:
  game.player.jump(); game.player.jumping_touch=true; jump_id=index; return true
 if pos.distance_to(Vector2(1219,657))<40: game.player.dodge(); return true
 if pos.distance_to(Vector2(896,632))<35: game.player.heal(); return true
 if game.nearby.size()>0 and Rect2(430,505,430,62).has_point(pos): game.interact(); return true
 return false

func release_all():
 joy_id=-1; look_id=-1; jump_id=-1; mouse_down=false
 if game.player: game.player.move_touch=Vector2.ZERO; game.player.jumping_touch=false
