extends Node3D
const Rig=preload("res://scripts/rig.gd")
var g
var kind="hollow"
var rig
var hp=58.0
var max_hp=58.0
var height=2.2
var boss=false
var elite=false
var dead=false
var velocity=Vector3.ZERO
var facing=-1
var state="idle"
var timer=0.0
var duration=1.0
var cooldown=1.0
var stun=0.0
var phase=1
var attack_index=0
var target_x=0.0
var guard=0.0
var clock=0.0
var label
func setup(game,id,x,is_elite=false):
	g=game;kind=id;elite=is_elite;position.x=x;boss=id in ["warden","cantor","regent"]
	max_hp={"hollow":58.0,"shield":100.0,"oracle":62.0,"warden":650.0,"cantor":760.0,"regent":1000.0}[id]*(1.5 if elite else 1.0)*(1.0 if boss else 1.0+g.zone()*.22);hp=max_hp
	rig=Rig.new();add_child(rig);rig.setup(id);var size=1.48 if boss else (1.12 if elite else .93);rig.scale=Vector3.ONE*size;height=2.25*size
	cooldown=1.3+randf()*.7;guard=1.0 if kind in ["shield","warden"] else 0.0
func tick(dt):
	if dead:return
	clock+=dt;stun=maxf(0,stun-dt);cooldown-=dt;timer+=dt
	if boss and phase==1 and hp<max_hp*.5:
		phase=2;stun=1.15;state="idle";cooldown=1.6;g.fx.death(position+Vector3.UP*1.5,Color("ff9777"),true);g.sound.play("ultimate",.8,.65);g.toast("ORBIT RETAK  ·  FASE II",2.2);g.impact(.08,.19)
	var dx=g.player.position.x-position.x;facing=1 if dx>0 else -1
	if stun<=0:
		if state=="windup":
			velocity.x=0
			if timer>=duration:attack();state="attack";timer=0;duration=.4
		elif state=="attack":
			velocity.x=0
			if timer>=duration:state="idle";cooldown=(1.0 if phase==2 else 1.45) if boss else randf_range(.7,1.15)
		elif cooldown<=0 and (absf(dx)<(5.2 if boss else 2.6) or kind in ["oracle","cantor"]):
			state="windup";timer=0;duration=(.95 if boss else (.9 if kind=="shield" else .7))*(.84 if phase==2 else 1.0);target_x=g.player.position.x
			var width=7.0 if boss else 3.1
			g.fx.danger(position.x+facing*width*.35,width,duration);g.fx.ring(position+Vector3.UP*height,Color("ff8260"),.22,duration);g.sound.play("telegraph",1.1 if boss else 1.4,.25)
		else:
			var desired=0.0
			if kind in ["oracle","cantor"]:
				if absf(dx)<6:desired=-facing*2.2
				elif absf(dx)>10:desired=facing*2.2
			elif absf(dx)>(3.6 if boss else 1.65):desired=facing*(3.2 if boss else (2.1 if kind=="shield" else 3.7))
			velocity.x=move_toward(velocity.x,desired,dt*16)
	else:velocity.x=move_toward(velocity.x,0,dt*10)
	velocity.y-=dt*22;var old=position.y;position+=velocity*dt;position.x=clampf(position.x,1.3,41.5)
	var floor_y=g.world.floor_at(position.x,old,position.y)
	if position.y<=floor_y:position.y=floor_y;velocity.y=0
	if kind in ["oracle","cantor"] and stun<=0:position.y=lerpf(position.y,.75+sin(clock*2)*.2,minf(1,dt*3))
	var pose="heavy" if state=="windup" else ("attack" if state=="attack" else ("hurt" if stun>0 else "idle"))
	rig.pose(dt,pose,.05+timer/maxf(.01,duration)*(.13 if state=="windup" else .8),velocity.x,position.y<.1,facing)
func attack():
	attack_index+=1
	if kind=="hollow" or kind=="shield":sweep(3.1,14 if kind=="shield" else 10)
	elif kind=="oracle":fan(3,12,11)
	elif kind=="warden":
		match attack_index%3:
			0:sweep(5.7,22);wave()
			1:wave();g.after(.30,func():if is_instance_valid(self) and not dead:wave())
			2:pillars(3)
	elif kind=="cantor":
		match attack_index%3:
			0:fan(5 if phase==1 else 7,13,16)
			1:pillars(4)
			2:wave();fan(3,11,14)
	elif kind=="regent":
		match attack_index%4:
			0:sweep(6.0,24);wave()
			1:pillars(5 if phase==2 else 3)
			2:fan(7,16,17)
			3:wave();g.after(.30,func():if is_instance_valid(self) and not dead:wave());g.after(.6,func():if is_instance_valid(self) and not dead:fan(3,14,15))
	if boss and phase==2 and attack_index%5==0 and g.enemies.size()<4:g.spawn("hollow",clampf(position.x-facing*5,3,39),false)
func sweep(reach,damage):
	g.fx.slash(position+Vector3.UP*1.3,Color("ff7458"),facing,reach*.55,true);g.sound.play("heavy",.8,.5)
	var dx=g.player.position.x-position.x
	if dx*facing>-.6 and absf(dx)<reach and absf(g.player.position.y-position.y)<2.0:g.player.hurt(damage,facing)
func fan(count,speed,damage):
	var origin=position+Vector3.UP*1.25;var angle=atan2(g.player.position.y+1.0-origin.y,g.player.position.x-origin.x)
	for i in range(count):
		var a=angle+(i-(count-1)*.5)*.16;g.shoot(origin,Vector3(cos(a),sin(a),0)*speed,damage,false,false,false,.22)
	g.sound.play("shot",.65,.55)
func wave():
	g.shoot(position+Vector3.UP*.45,Vector3(facing*13,0,0),18,false,true,false,.55);g.fx.ring(position+Vector3.UP*.1,Color("ff7659"),1.6,.3,true)
func pillars(count):
	for i in range(count):
		var x=clampf(target_x+(i-(count-1)*.5)*2.8,2,41);g.fx.danger(x,1.3,.65+i*.11)
		g.after(.65+i*.11,func():if is_instance_valid(self) and not dead:g.meteor(x,20,false))
func hurt(amount,direction,launch=0.0,heavy=false):
	if dead:return
	var blocked=guard>0 and direction==-facing and not heavy and stun<=0
	if blocked:amount*=.3;g.fx.ring(position+Vector3.UP,Color("e8d098"),.7,.15);g.sound.play("parry",.6,.3)
	if heavy:guard=0;stun=maxf(stun,.35 if boss else .65)
	elif not boss:stun=maxf(stun,.16)
	hp=maxf(0,hp-amount);rig.flash();g.fx.impact(position+Vector3.UP*(height*.5),g.player.color,heavy);g.fx.number(position+Vector3.UP*height,amount,heavy)
	if not boss:
		velocity.x=direction*(4.2 if heavy else 1.6);velocity.y=maxf(velocity.y,launch)
		if state=="windup" and heavy:state="idle";cooldown=.6
	if hp<=0:
		dead=true;g.enemy_defeated(self)
func snapshot():return {"kind":kind,"x":position.x,"y":position.y,"hp":hp,"elite":elite,"phase":phase,"attack_index":attack_index}
