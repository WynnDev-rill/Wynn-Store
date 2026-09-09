extends Node3D
const D=preload("res://scripts/data.gd")
const Rig=preload("res://scripts/rig.gd")
var g
var rig
var hero="rei"
var color=Color.WHITE
var hp=150.0
var max_hp=150.0
var energy=0.0
var potions=2
var facing=1
var velocity=Vector3.ZERO
var ground=true
var jumps=0
var state="idle"
var timer=0.0
var duration=1.0
var fired=false
var hit_time=0.08
var combo=0
var combo_window=0.0
var skill_cd=0.0
var dash_cd=0.0
var heal_cd=0.0
var inv=0.0
var perfect_used=false
var revive_used=false
var haste=0.0
var orbit_time=0.0
var ghost_time=0.0
var slam_done=false
func setup(game,id):
	g=game;hero=id;color=D.HEROES[id].color;max_hp=D.max_hp(id,g.save.profile.meta);hp=max_hp;energy=10*int(g.save.profile.meta.surge);potions=2+int(g.save.profile.meta.keepsake)
	rig=Rig.new();add_child(rig);rig.setup(id)
func has(id):return id in g.relics
func begin(action,length,at=.08):
	state=action;timer=0;duration=length/(1.35 if haste>0 and action in ["attack","heavy","launcher"] else 1.0);hit_time=at;fired=false
func tick(dt):
	var old_state=state;timer+=dt;inv=maxf(0,inv-dt);skill_cd=maxf(0,skill_cd-dt);dash_cd=maxf(0,dash_cd-dt);heal_cd=maxf(0,heal_cd-dt);combo_window-=dt;haste=maxf(0,haste-dt);ghost_time-=dt
	if combo_window<=0:combo=0
	var move=g.inputs.movement()
	var free=state=="idle" or (state in ["attack","heavy","launcher"] and timer>duration*.70)
	if g.inputs.take("dash") and dash_cd<=0 and state!="ult":
		if absf(move.x)>.1:facing=1 if move.x>0 else -1
		begin("dash",.255);dash_cd=.67;inv=.28;perfect_used=false;g.sound.play("dash");g.fx.ring(position+Vector3.UP,color,.55,.21)
	elif g.inputs.take("ult") and energy>=100 and state!="ult":ultimate()
	elif g.inputs.take("heal") and potions>0 and heal_cd<=0:
		potions-=1;heal_cd=4;hp=minf(max_hp,hp+max_hp*.38);inv=maxf(inv,.32);g.fx.ring(position+Vector3.UP,Color("93ecc0"),1.2,.55);g.sound.play("loot");g.toast("Tenun pulih",1.5)
	elif g.inputs.take("jump") and jumps<(3 if has("air_step") else 2) and state not in ["ult","hurt","slam"]:
		velocity.y=10.2 if ground else 9.0;jumps+=1;ground=false;begin("idle",1);g.sound.play("jump",1+jumps*.08)
		g.fx.ring(position+Vector3.UP*.1,color,.65,.24,true)
		if jumps>1 and has("air_step"):g.area(position+Vector3.UP,2.4,16,0,false)
	elif free and g.inputs.take("skill") and skill_cd<=0:skill()
	elif free and g.inputs.take("heavy"):
		if not ground:begin("slam",1.2);velocity.y=-15;slam_done=false;g.sound.play("heavy")
		elif move.y<-.45:begin("launcher",.46,.13);g.sound.play("heavy",1.2)
		else:begin("heavy",.51,.16);g.sound.play("heavy")
	elif free and (g.inputs.take("attack") or g.inputs.held.get("attack",false)):
		combo=combo%3+1;combo_window=1.0;begin("attack",[.29,.32,.40][combo-1]);g.sound.play("shot" if hero=="kael" else "slash"+str(combo),1.0)
	if state in ["attack","heavy","launcher"] and timer>=hit_time and not fired:
		fired=true;strike()
	if state=="dash":
		velocity.x=facing*18;velocity.y=maxf(velocity.y,0)*.92
		if ghost_time<=0:g.fx.ghost(rig);ghost_time=.065
	elif state=="slam":velocity.x=move.x*2.8
	elif state=="hurt":velocity.x=move_toward(velocity.x,0,dt*15)
	else:
		var speed=D.HEROES[hero].speed
		var control=.42 if state in ["attack","heavy","launcher"] else (0.0 if state in ["ult","skill"] else 1.0)
		velocity.x=move_toward(velocity.x,move.x*speed*control,dt*65)
		if absf(move.x)>.18 and free:facing=1 if move.x>0 else -1
	if state in ["attack","heavy","launcher"] and timer<.05:
		var target=g.nearest(position)
		if target and absf(target.position.x-position.x)<7 and absf(move.x)<.15:facing=1 if target.position.x>position.x else -1
	if state!="dash" and state!="ult":velocity.y-=dt*(9.0 if state=="attack" and not ground else 27.0)
	var old_y=position.y;position+=velocity*dt;position.x=clampf(position.x,1,42)
	var floor_y=g.world.floor_at(position.x,old_y,position.y)
	if velocity.y<=0 and position.y<=floor_y:
		position.y=floor_y;velocity.y=0;ground=true;jumps=0
		if state=="slam" and not slam_done:
			slam_done=true;g.area(position+Vector3.UP*.6,3.6,38,6,true);g.fx.ring(position+Vector3.UP*.07,color,2,.4,true);g.impact(.08,.16);g.sound.play("break");begin("heavy",.26,10)
			if has("thunderfall"):
				for side in [-1,1]:g.shoot(position+Vector3.UP*.5,Vector3(side*18,0,0),26,true,true,false,.55)
	else:ground=false
	if state!="idle" and state!="slam" and timer>=duration:
		if state=="dash" and has("dash_burst"):g.area(position+Vector3.UP*.7,2.7,26,3,true);g.fx.ring(position+Vector3.UP*.8,color,1.3,.3)
		if state=="ult" and has("overdrive"):haste=8
		begin("idle",1)
	orbit_time+=dt
	if has("orbit"):
		for i in range(2):
			var p=position+Vector3(cos(orbit_time*3+i*PI)*1.7,1.15+sin(orbit_time*3+i*PI)*.3,sin(orbit_time*3+i*PI)*.65)
			if int(orbit_time*14)!=int((orbit_time-dt)*14):g.fx.ring(p,color,.09,.12)
		if int(orbit_time)!=int(orbit_time-dt):g.area(position+Vector3.UP,2.1,10,0,false)
	rig.pose(dt,state,timer/maxf(.01,duration),velocity.x,ground,facing)
func strike():
	var launch=state=="launcher";var heavy=state=="heavy";var damage=23.0 if launch else (34.0 if heavy else [13.0,16.0,23.0][combo-1])
	var up=10.5 if launch else (4.5 if not ground else (3.0 if combo==3 else 0.0))
	if hero=="kael" and not launch:
		g.shoot(position+Vector3(0,1.18,0),Vector3(facing*(23 if heavy else 25),0,0),36 if heavy else damage,true,heavy or has("pierce"),false,.50 if heavy else .20)
		g.fx.beam(position+Vector3.UP*1.15,position+Vector3(facing*1.8,1.15,0),color,.055)
	else:
		var range_x=3.05 if heavy else 2.6
		g.fx.slash(position+Vector3.UP*1.1,color,facing,2.2 if heavy else 1.65,heavy or launch)
		for e in g.enemies.duplicate():
			if not is_instance_valid(e) or e.dead:continue
			var diff=e.position-position
			if diff.x*facing>-.7 and absf(diff.x)<range_x+e.height*.18 and absf(diff.y)<2.0:
				e.hurt(damage,facing,up,heavy or launch or combo==3);g.on_hit(e,damage,heavy or launch)
				if launch and has("frost"):e.stun=maxf(e.stun,2.0 if not e.boss else .65);g.fx.ring(e.position+Vector3.UP,Color("a9e8ff"),1,.5)
		if hero=="rei" and has("pierce"):g.shoot(position+Vector3.UP,Vector3(facing*20,0,0),damage*.6,true,true)
	if launch:velocity.y=8.5;ground=false;jumps=maxi(1,jumps)
	if not ground and has("air_blade"):g.shoot(position+Vector3.UP,Vector3(facing*19,0,0),16,true,true)
	if state=="attack" and combo==3:
		if has("echo"):
			var p=position+Vector3(facing*1.2,1.1,0);g.after(.17,func():g.area(p,2.6,damage*.6,2,false);g.fx.slash(p,color,facing,1.8))
func skill():
	begin("skill",.52);skill_cd=6.5;inv=maxf(inv,.30);g.sound.play("heavy",1.3);var target=g.nearest(position)
	if target:facing=1 if target.position.x>position.x else -1
	if has("gravity"):
		for e in g.enemies:
			if not e.boss and e.position.distance_to(position)<10:e.velocity.x=(position.x+facing*2-e.position.x)*9;e.stun=.4
	if hero=="rei":
		var old=position+Vector3.UP;g.area(position+Vector3(facing*2.5,1,0),4.0,44,5,true);position.x=clampf(position.x+facing*3.8,1,42);g.fx.beam(old,position+Vector3.UP,color,.24,.25);g.fx.slash(position+Vector3.UP,color,facing,2.6,true)
	else:
		for i in range(5):g.shoot(position+Vector3.UP*1.2,Vector3(facing*19,(i-2)*2.5,0),17,true,has("pierce"),true)
	if has("triple"):
		for i in range(3):g.shoot(position+Vector3.UP*1.2,Vector3(facing*20,(i-1)*3,0),18,true,has("pierce"))
	if has("meteor"):
		for i in range(3):
			var x=position.x+facing*(i+1)*2.0;g.after(.15+i*.14,func():g.meteor(x,22,true))
func ultimate():
	energy=0;begin("ult",1.65);inv=1.8;velocity=Vector3.ZERO;g.sound.play("ultimate");g.flash=1;g.impact(.08,.22);g.toast("SERIBU PAGI" if hero=="rei" else "LANGIT RUNTUH",1.8)
	for e in g.enemies:e.stun=maxf(e.stun,1.25)
	for i in range(4 if hero=="rei" else 8):
		g.after(.22+i*(.28 if hero=="rei" else .15),func():
			if hero=="rei":
				for e in g.enemies.duplicate():
					if is_instance_valid(e) and not e.dead:e.hurt(35,facing,3,true);g.fx.slash(e.position+Vector3.UP,color,facing,3,true)
				g.fx.beam(Vector3(1,randf_range(.5,3),.3),Vector3(43,randf_range(.5,3),.3),color,.09,.21);g.impact(.035,.12)
			else:
				var e=g.nearest(position);g.meteor(e.position.x if e else position.x+randf_range(-5,5),35,true))
func hurt(amount,direction):
	if hp<=0:return
	if inv>0:
		if state=="dash" and timer<.145 and not perfect_used:
			perfect_used=true;energy=minf(100,energy+20);g.slow=.48;g.sound.play("parry");g.fx.ring(position+Vector3.UP,Color.WHITE,1.6,.45);g.toast("PERFECT DODGE",1.3)
			if has("perfect"):hp=minf(max_hp,hp+8);skill_cd=0
		return
	hp=maxf(0,hp-amount);g.combo=0;g.combo_timer=0;inv=.72;velocity=Vector3(direction*5.5,3,0);begin("hurt",.22);rig.flash();g.sound.play("hurt");g.impact(.07,.16);g.fx.number(position+Vector3.UP,amount,true,Color("ff9984"))
	if hp<=0:
		if has("revive") and not revive_used:revive_used=true;hp=max_hp*.45;inv=2;g.fx.death(position+Vector3.UP,color,true);g.toast("API KEDUA",2);g.area(position+Vector3.UP,6,50,6,true)
		else:g.defeat()
func snapshot():return {"x":position.x,"y":position.y,"hp":hp,"energy":energy,"potions":potions,"skill_cd":skill_cd,"revive_used":revive_used}
func restore(d):
	position=Vector3(clampf(float(d.get("x",5)),1,42),clampf(float(d.get("y",0)),0,7),0);hp=clampf(float(d.get("hp",max_hp)),1,max_hp);energy=clampf(float(d.get("energy",0)),0,100);potions=clampi(int(d.get("potions",2)),0,5);skill_cd=maxf(0,float(d.get("skill_cd",0)));revive_used=bool(d.get("revive_used",false));inv=1.0
