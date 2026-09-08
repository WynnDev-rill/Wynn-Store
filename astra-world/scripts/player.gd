extends CharacterBody3D
const Art=preload("res://scripts/art.gd")
const World=preload("res://scripts/world.gd")
var game
var model:Node3D
var pivot:Node3D
var camera:Camera3D
var arm:SpringArm3D
var move_touch=Vector2.ZERO
var yaw=0.0
var pitch=-0.22
var stamina=100.0
var invulnerable=0.0
var dodge_time=0.0
var attack_time=0.0
var attack_cooldown=0.0
var skill_cooldown=0.0
var dash_cooldown=0.0
var combo=0
var gait=0.0
var step_clock=0.0
var dodge_dir=Vector3.ZERO
var jumping_touch=false
var gliding=false
var mouse_look=false
var heal_cooldown=0.0

func setup(g):
 game=g; name="Nara"; collision_layer=2; collision_mask=1
 var cs=CollisionShape3D.new(); var cap=CapsuleShape3D.new(); cap.radius=0.3; cap.height=1.75; cs.shape=cap; cs.position.y=0.9; add_child(cs)
 model=Art.person(self)
 pivot=Node3D.new(); add_child(pivot); pivot.position.y=1.55; pivot.top_level=true
 arm=SpringArm3D.new(); arm.spring_length=6.3; arm.margin=0.25; arm.collision_mask=1; pivot.add_child(arm); arm.add_excluded_object(get_rid())
 var sphere=SphereShape3D.new(); sphere.radius=0.25; arm.shape=sphere
 camera=Camera3D.new(); camera.fov=65; camera.near=0.12; camera.far=650; arm.add_child(camera); camera.current=true
 floor_snap_length=0.7; floor_max_angle=deg_to_rad(50)

func _physics_process(dt):
 if game==null or not game.playing or game.ui.blocking: return
 attack_cooldown=maxf(0,attack_cooldown-dt); skill_cooldown=maxf(0,skill_cooldown-dt); dash_cooldown=maxf(0,dash_cooldown-dt); invulnerable=maxf(0,invulnerable-dt); heal_cooldown=maxf(0,heal_cooldown-dt)
 attack_time=maxf(0,attack_time-dt); dodge_time=maxf(0,dodge_time-dt)
 var input=Input.get_vector("left","right","forward","back")+move_touch
 input=input.limit_length()
 var dir=Basis(Vector3.UP,yaw)*Vector3(input.x,0,input.y)
 var sprint=Input.is_action_pressed("sprint") and stamina>2 and input.length()>0.1
 var speed=8.2 if sprint else 5.1
 if attack_time>0: speed*=0.55
 if dodge_time>0: dir=dodge_dir; speed=14.0
 velocity.x=move_toward(velocity.x,dir.x*speed,dt*34)
 velocity.z=move_toward(velocity.z,dir.z*speed,dt*34)
 var jump_held=Input.is_action_pressed("jump") or jumping_touch
 gliding=not is_on_floor() and velocity.y<0 and jump_held and game.state.s.beacons.size()>0 and stamina>0
 if gliding:
  velocity.y=move_toward(velocity.y,-1.4,dt*20); stamina=maxf(0,stamina-dt*12)
 else: velocity.y-=19.0*dt
 if is_on_floor() and Input.is_action_just_pressed("jump"): jump()
 if sprint: stamina=maxf(0,stamina-dt*11)
 elif not gliding and dodge_time<=0: stamina=minf(100,stamina+dt*19)
 move_and_slide()
 if dir.length()>0.1: model.rotation.y=lerp_angle(model.rotation.y,atan2(-dir.x,-dir.z),dt*12)
 if position.y<-3 or Vector2(position.x,position.z).length()>137: game.rescue("Arus membawamu ke tempat aman.")
 gait+=dt*speed*1.75*input.length()
 var stride=sin(gait)*0.55*input.length()
 model.get_node("LegL").rotation.x=stride; model.get_node("LegR").rotation.x=-stride
 model.get_node("ArmL").rotation.x=-stride*0.6
 var right=model.get_node("ArmR")
 if attack_time>0:
  right.rotation=Vector3(-1.0+sin(attack_time*13)*1.8,0,-0.8)
 else: right.rotation=Vector3(stride*0.55,0,-0.04)
 model.get_node("Cape").rotation.x=0.1+sin(gait*0.5)*0.05+input.length()*0.12
 model.get_node("Head").rotation.z=sin(Time.get_ticks_msec()*0.0013)*0.025
 model.get_node("Glider").visible=gliding
 model.position.y=absf(sin(gait))*0.045*input.length()
 if input.length()>0.2 and is_on_floor():
  step_clock-=dt
  if step_clock<=0: game.audio.sfx("step",-17); step_clock=0.30 if not sprint else 0.22
 _camera(dt)
 if Input.is_action_just_pressed("attack"): attack()
 if Input.is_action_just_pressed("skill"): skill()
 if Input.is_action_just_pressed("dodge"): dodge()
 if Input.is_action_just_pressed("heal"): heal()

func _camera(dt):
 pivot.global_position=pivot.global_position.lerp(global_position+Vector3(0,1.55,0),1-exp(-dt*14))
 pivot.rotation=Vector3(pitch,yaw,0)

func camera_snap():
 pivot.global_position=global_position+Vector3(0,1.55,0); pivot.rotation=Vector3(pitch,yaw,0)

func look(delta:Vector2):
 yaw-=delta.x*0.004*game.state.settings.sensitivity
 pitch=clampf(pitch-delta.y*0.003*game.state.settings.sensitivity*(-1 if game.state.settings.invert_y else 1),-0.95,0.38)

func jump():
 if is_on_floor() and stamina>=8: velocity.y=7.8; stamina-=8; game.audio.sfx("dash",-14)

func attack():
 if game.ui.blocking or attack_cooldown>0: return
 attack_cooldown=0.48; attack_time=0.38; combo=(combo+1)%3
 var target=game.nearest_enemy(global_position,4.0)
 if target!=null:
  var d=target.global_position-global_position; model.rotation.y=atan2(-d.x,-d.z)
  target.hurt(game.state.damage()*(1.35 if combo==0 else 1.0))
  game.fx.burst(target.global_position+Vector3.UP,"f3d493",8)
  game.audio.sfx("hit",-3)
 else: game.audio.sfx("swing",-8)
 game.fx.slash(global_position+Vector3.UP,model.rotation.y,combo)

func skill():
 if game.ui.blocking or skill_cooldown>0 or stamina<22: return
 stamina-=22; skill_cooldown=7.0; attack_time=0.5
 game.audio.sfx("skill",-4); game.fx.wave(global_position+Vector3.UP*0.25,7.0,"85e3d0",0.65)
 for enemy in game.enemies:
  if is_instance_valid(enemy) and not enemy.dead and enemy.global_position.distance_to(global_position)<7:
   enemy.hurt(game.state.damage()*1.8); enemy.stagger=0.8

func dodge():
 if game.ui.blocking or stamina<23 or dash_cooldown>0: return
 stamina-=23; dodge_time=0.25; invulnerable=0.43; dash_cooldown=0.7
 var d=Input.get_vector("left","right","forward","back")+move_touch
 dodge_dir=(Basis(Vector3.UP,yaw)*Vector3(d.x,0,d.y)).normalized() if d.length()>0.1 else -model.global_basis.z
 game.audio.sfx("dash",-7); game.fx.burst(global_position+Vector3.UP*0.3,"b3e4d8",6)

func hurt(n:float):
 if invulnerable>0 or game.ui.blocking: return
 var amount=maxf(1,n-(3 if game.state.s.armor=="mantle" else 0))
 game.state.s.hp=maxf(0,game.state.s.hp-amount); invulnerable=0.65
 game.ui.hurt_flash=0.35; game.audio.sfx("hurt",-4)
 if game.state.s.hp<=0: game.rescue("Loncengmu te ram membawa pulang.")

func heal():
 if game.ui.blocking or heal_cooldown>0 or game.state.amount("potion")==0: return
 if game.state.s.hp>=game.state.max_hp(): game.ui.toast("Kesehatan masih penuh."); return
 game.state.add("potion",-1); game.state.s.hp=minf(game.state.max_hp(),game.state.s.hp+55); heal_cooldown=1.0
 game.fx.burst(global_position+Vector3.UP,"a4e6ad",14); game.audio.sfx("heal",-6); game.save()
