extends Node3D
var parts={}
var rests={}
var targets={}
var mats=[]
var clock=0.0
var flash_timer=0.0
var kind="rei"
func setup(id):
	kind=id
	var model=load("res://assets/models/"+id+".glb").instantiate();add_child(model);scan(model)
func scan(n):
	if n is Node3D:
		parts[str(n.name)]=n;rests[str(n.name)]={"p":n.position,"r":n.rotation}
	if n is MeshInstance3D:
		for i in n.mesh.get_surface_count():
			var src=n.mesh.surface_get_material(i)
			if src is StandardMaterial3D:
				var m=src.duplicate();m.diffuse_mode=BaseMaterial3D.DIFFUSE_TOON;m.specular_mode=BaseMaterial3D.SPECULAR_TOON;m.rim_enabled=true;m.rim=.32;m.rim_tint=.7;n.set_surface_override_material(i,m);mats.append([m,m.emission,m.emission_energy_multiplier,m.emission_enabled])
	for c in n.get_children():scan(c)
func angle(id,degrees):
	if parts.has(id):targets[id]=Vector3(deg_to_rad(degrees.x),deg_to_rad(degrees.y),deg_to_rad(degrees.z))
func flash():
	flash_timer=.065
	for m in mats:m[0].emission_enabled=true;m[0].emission=Color("d9fcff");m[0].emission_energy_multiplier=1.6
func pose(dt,state,phase,speed,ground,facing):
	clock+=dt;targets.clear()
	if flash_timer>0:
		flash_timer-=dt
		if flash_timer<=0:
			for m in mats:m[0].emission=m[1];m[0].emission_energy_multiplier=m[2];m[0].emission_enabled=m[3]
	var stride=sin(clock*13)*minf(absf(speed)/6.0,1.2)
	angle("Torso",Vector3(6+absf(speed),0,0));angle("UpperArm_L",Vector3(-stride*27,0,-8));angle("UpperArm_R",Vector3(stride*27,0,10))
	angle("Thigh_L",Vector3(stride*38,0,0));angle("Thigh_R",Vector3(-stride*38,0,0));angle("Shin_L",Vector3(maxf(0,-stride)*57,0,0));angle("Shin_R",Vector3(maxf(0,stride)*57,0,0))
	if not ground:
		angle("Thigh_L",Vector3(-42,0,0));angle("Shin_L",Vector3(65,0,0));angle("Thigh_R",Vector3(22,0,0));angle("Shin_R",Vector3(25,0,0))
	if state in ["attack","heavy","launcher","skill"]:
		var sweep=lerpf(-108,84,smoothstep(.16,.43,phase))*(1-smoothstep(.64,1.0,phase)*.72)
		angle("Torso",Vector3(10,sweep*.55,0));angle("UpperArm_L",Vector3(25,0,-30));angle("UpperArm_R",Vector3(-72,sweep,-58));angle("Forearm_R",Vector3(-42+phase*32,0,0))
		if state=="launcher":angle("UpperArm_R",Vector3(lerpf(45,-174,smoothstep(.13,.5,phase)),0,-20));angle("Torso",Vector3(-14,sweep*.2,0))
		if kind in ["kael","oracle","cantor"] and state!="launcher":
			angle("Torso",Vector3(4,-20,0));angle("UpperArm_R",Vector3(-82,0,-12));angle("Forearm_R",Vector3(-4,0,0));angle("UpperArm_L",Vector3(-65,-25,lerpf(46,-24,smoothstep(.1,.48,phase))));angle("Forearm_L",Vector3(lerpf(-82,-10,phase),0,0));angle("Weapon",Vector3(0,0,90))
	if state=="dash":
		angle("Torso",Vector3(59,0,0));angle("UpperArm_R",Vector3(65,0,16));angle("UpperArm_L",Vector3(62,0,-20));angle("Thigh_R",Vector3(-73,0,0));angle("Shin_R",Vector3(90,0,0))
	if state=="slam":angle("Torso",Vector3(35,0,0));angle("UpperArm_R",Vector3(-155,0,-20));angle("Thigh_L",Vector3(-50,0,0));angle("Shin_L",Vector3(80,0,0))
	if state=="ult":angle("Torso",Vector3(-16,0,0));angle("UpperArm_R",Vector3(-166,0,0));angle("UpperArm_L",Vector3(-50,0,-65))
	if state=="hurt":angle("Torso",Vector3(-24,0,-15))
	angle("Coat_L",Vector3(-absf(speed)*3-8+sin(clock*5)*7,0,-4));angle("Coat_R",Vector3(-absf(speed)*3-8+cos(clock*5)*7,0,4));angle("HairTip",Vector3(-absf(speed)*2+sin(clock*4)*9,0,0))
	for id in parts:
		parts[id].rotation=parts[id].rotation.lerp(rests[id].r+targets.get(id,Vector3.ZERO),1-exp(-dt*25))
	if parts.has("Hip"):parts.Hip.position=rests.Hip.p+Vector3(0,absf(stride)*.035+sin(clock*2.8)*.01,0)
	rotation.y=lerp_angle(rotation.y,float(facing)*1.10,1-exp(-dt*18))
