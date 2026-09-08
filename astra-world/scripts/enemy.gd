extends Node3D
const Art=preload("res://scripts/art.gd")
const World=preload("res://scripts/world.gd")
var game
var id:String
var home:Vector3
var boss=false
var elite=false
var dead=false
var hp=60.0
var maximum=60.0
var model:Node3D
var clock=0.0
var attack_timer=1.2
var telegraph=0.0
var mode=0
var stagger=0.0
var telegraph_node:MeshInstance3D
var target_point=Vector3.ZERO
var radius=3.5
var phase=1

func setup(g,key:String,pos:Vector3,type:int=0):
 game=g; id=key; position=pos; home=pos; elite=type==1; boss=type==2
 maximum=660.0 if boss else (145.0 if elite else 66.0); hp=maximum
 model=Art.construct(self,elite,boss)
 telegraph_node=Art.part(self,"ring",Vector3(0,0.08,0),Vector3.ONE,"df7965",Vector3.ZERO,0.7)
 telegraph_node.visible=false; radius=6.8 if boss else (4.2 if elite else 3.1)
 if id in game.state.s.defeated: dead=true; visible=false

func _physics_process(dt):
 if game==null or dead or not game.playing or game.ui.blocking: return
 if boss and game.state.s.beacons.size()<3: return
 var pp=game.player.global_position; var distance=global_position.distance_to(pp)
 clock+=dt; stagger=maxf(0,stagger-dt)
 if distance>48: return
 var active=distance<(26 if boss else 15)
 if stagger>0: return
 if telegraph>0:
  telegraph-=dt
  telegraph_node.visible=true
  var progress=1-telegraph/(1.2 if boss else 0.85)
  telegraph_node.scale=Vector3(radius,0.25,radius)*(0.6+0.4*progress)
  telegraph_node.rotation.y+=dt
  model.get_node("ArmL").rotation.x=-progress*2.2
  model.get_node("ArmR").rotation.x=-progress*2.2
  if telegraph<=0: _strike()
 elif active:
  attack_timer-=dt
  var d=pp-global_position; d.y=0
  if distance>radius*0.68:
   var step=d.normalized()*(2.6 if boss else 2.1)*dt
   if (position+step).distance_to(home)<(28 if boss else 18): position+=step
  if d.length()>0.1: model.rotation.y=lerp_angle(model.rotation.y,atan2(-d.x,-d.z),dt*5)
  if distance<radius+0.8 and attack_timer<=0:
   telegraph=1.2 if boss else 0.85; target_point=pp
   game.audio.sfx("warning",-11)
  model.get_node("ArmL").rotation.x=sin(clock*3)*0.25
  model.get_node("ArmR").rotation.x=-sin(clock*3)*0.25
 else:
  var patrol=home+Vector3(sin(clock*0.25)*2,0,cos(clock*0.25)*2)
  position=position.move_toward(patrol,dt*1.0)
  if hp<maximum and position.distance_to(home)<3: hp=minf(maximum,hp+dt*4)
 position.y=World.height_at(position.x,position.z)+(0.2 if boss else 0)
 model.get_node("LegL").rotation.x=sin(clock*4)*0.3 if active else sin(clock)*0.06
 model.get_node("LegR").rotation.x=-model.get_node("LegL").rotation.x

func _strike():
 telegraph_node.visible=false; attack_timer=2.1 if boss else 2.4
 game.audio.sfx("slam",-4 if boss else -9)
 var dist=game.player.global_position.distance_to(global_position)
 game.fx.wave(global_position+Vector3.UP*0.25,radius,"e7997e",0.45)
 if dist<radius and not (game.player.global_position.y-position.y>1.4): game.player.hurt(25 if boss else (18 if elite else 12))
 if boss:
  mode=(mode+1)%3
  if mode==1: game.fx.hazard(target_point,3.5,1.1)
  if hp<maximum*0.5:
   phase=2; attack_timer=1.5
   if mode==2:
    for a in [0.0,2.1,4.2]: game.fx.hazard(global_position+Vector3(sin(a)*6,0,cos(a)*6),3.0,1.25)

func hurt(amount:float):
 if dead or (boss and game.state.s.beacons.size()<3): return
 hp=maxf(0,hp-amount)
 game.fx.number(global_position+Vector3.UP*(4.5 if boss else 2.6),str(int(amount)),"ffe1a5")
 if not boss: stagger=0.22
 if hp<=0:
  dead=true; telegraph_node.visible=false
  game.state.s.defeated.append(id); game.state.add("echo",5 if boss else (3 if elite else 1))
  var old_level=game.state.level(); game.state.s.xp+=150 if boss else (75 if elite else 28)
  if game.state.level()>old_level:
   game.state.s.hp=game.state.max_hp(); game.ui.toast("Nara mencapai level %d · kesehatan dipulihkan"%game.state.level()); game.audio.sfx("beacon",-8)
  game.fx.burst(global_position+Vector3.UP,"dfc898",22)
  var tw=create_tween(); tw.tween_property(model,"scale",Vector3.ZERO,0.45); tw.tween_callback(func(): visible=false)
  if elite:
   game.state.add("charm",1); game.state.s.charm="charm"; game.ui.toast("Jimat Akar diperoleh · kesehatan maksimum +30")
  if boss:
   game.state.s.boss=true
   game.ui.dialogue([["Aru · pemikul sunyi","Lonceng itu... mereka sudah pulang?"],["Nara","Sudah. Kau boleh beristirahat."],["Aru · pemikul sunyi","Kalau begitu... biarkan angin lewat."]],func(): game.ui.toast("Kembali ke Ilya di Desa Teralun."))
  game.save()
