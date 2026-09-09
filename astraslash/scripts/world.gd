extends Node3D
const D=preload("res://scripts/data.gd")
var g
var zone=0
var platforms=[]
var groups={}
var ornaments=[]
var sun
var environment
var clock=0.0
var rng=RandomNumberGenerator.new()
func mat(color,glow=0.0,metallic=0.0):
	var m=StandardMaterial3D.new();m.albedo_color=color;m.roughness=.7;m.metallic=metallic
	if glow>0:m.emission_enabled=true;m.emission=color;m.emission_energy_multiplier=glow
	return m
func batch(key,mesh,material,p,s=Vector3.ONE,rotation=Vector3.ZERO):
	if not groups.has(key):mesh.material=material;groups[key]={"mesh":mesh,"transforms":[]}
	groups[key].transforms.append(Transform3D(Basis.from_euler(rotation).scaled(s),p))
func box(key,m,p,s):batch(key,BoxMesh.new(),m,p,s)
func cylinder(key,m,p,r,h,vertices=12):
	var mesh=CylinderMesh.new();mesh.top_radius=r;mesh.bottom_radius=r;mesh.height=h;mesh.radial_segments=vertices;batch(key,mesh,m,p)
func ring(key,m,p,r,t,rot=Vector3.ZERO):
	var mesh=TorusMesh.new();mesh.inner_radius=r-t;mesh.outer_radius=r+t;mesh.rings=48;mesh.ring_segments=6;batch(key,mesh,m,p,Vector3.ONE,rot)
func build(game,index,boss=false):
	g=game;zone=index;rng.seed=7341+index*9;var z=D.ZONES[zone];var accent=z.color
	var stone=mat(Color("26374b").lerp(accent,.1));var dark=mat(Color("111b2a"));var marble=mat(Color("79818a"));var gold=mat(Color("ad9260"),.0,.55);var glow=mat(accent,1.8);var back=mat(z.sky.lightened(.13))
	for i in range(12):
		var x=i*4.0
		box("foundation",dark,Vector3(x,-.78,0),Vector3(3.95,1.5,6.6))
		box("paving",stone,Vector3(x,-.045,0),Vector3(3.91,.09,6.5))
		box("seam",gold,Vector3(x-1.95,.008,0),Vector3(.025,.014,6.4))
		box("edge",gold,Vector3(x,-.08,3.28),Vector3(3.94,.11,.05))
		box("inlay",glow,Vector3(x,.012,-1.6),Vector3(3.4,.022,.035))
		box("facade",marble,Vector3(x,-.74,3.34),Vector3(3.2,.76,.12))
		batch("facade-gem",PrismMesh.new(),gold,Vector3(x,-.74,3.43),Vector3(.28,.6,.09),Vector3(0,0,PI))
	for x in [1.0,10.0,19.0,28.0,37.0,46.0]:
		cylinder("column",marble,Vector3(x,2,-3.6),.24,4)
		cylinder("column-foot",gold,Vector3(x,.18,-3.6),.4,.36)
		cylinder("capital",gold,Vector3(x,3.85,-3.6),.37,.22)
		ring("arch",stone,Vector3(x+4.5,3.7,-3.65),4.5,.17,Vector3(PI/2,0,0))
		ring("arch-inlay",gold,Vector3(x+4.5,3.7,-3.48),4.23,.027,Vector3(PI/2,0,0))
		batch("lantern",SphereMesh.new(),glow,Vector3(x,3.6,-3.5),Vector3(.14,.24,.14))
	for layer in range(3):
		var city=mat(z.sky.lerp(Color("071329"),.22).lightened(.015*layer))
		for i in range(19):
			var x=i*5.3-24+rng.randf_range(-1,1);var h=rng.randf_range(4,17);var depth=-18-layer*17
			box("city"+str(layer),city,Vector3(x,h/2-3,depth),Vector3(rng.randf_range(1.5,3.7),h,2))
			var roof=PrismMesh.new();batch("roofs"+str(layer),roof,city,Vector3(x,h-2,depth),Vector3(2.4,2.4,2.5))
			for k in range(3):box("windows",glow,Vector3(x+.25,k*1.1+h-7,depth+1.02),Vector3(.035,.45,.025))
	for i in range(3):
		var o=MeshInstance3D.new();var mesh=TorusMesh.new();mesh.inner_radius=4.4+i*.55;mesh.outer_radius=4.47+i*.55;mesh.rings=64;mesh.ring_segments=6;o.mesh=mesh;o.material_override=gold;o.position=Vector3(24,8.3,-11);o.rotation=Vector3(PI/2+.12*i,.15*i,.2*i);add_child(o);ornaments.append([o,i+1])
	var orb=SphereMesh.new();orb.radial_segments=32;orb.rings=16;batch("moon",orb,mat(accent,1.2),Vector3(24,8.3,-11),Vector3(2.7,2.7,2.7))
	if zone==1:
		for i in range(7):
			var x=i*7.0
			cylinder("tree",gold,Vector3(x,1.8,-6),.11,3.6)
			for j in range(5):
				var p=Vector3(x+sin(j*2.1)*1.1,3+cos(j*1.6)*.7,-6+sin(j*1.8));batch("petals",SphereMesh.new(),mat(Color("9e81b6")),p,Vector3(1.5,.42,1.0))
		for i in range(24):
			var p=Vector3(i*1.8,.3,-2.8);batch("flowers",PrismMesh.new(),glow,p,Vector3(.17,.6,.18))
	if zone==2:
		for i in range(10):
			var x=i*5.0
			batch("spires",PrismMesh.new(),dark,Vector3(x,3.1,-7.5),Vector3(1.2,8.4,1.5))
			box("fractures",glow,Vector3(x,2.5,-6.72),Vector3(.04,3.0,.04))
	platforms=[] if boss else [[13.0,1.8,4.2],[27.0,2.65,3.6]]
	for p in platforms:
		box("ledge",stone,Vector3(p[0],p[1]-.15,0),Vector3(p[2],.3,2.7));box("ledge-light",glow,Vector3(p[0],p[1]-.18,1.37),Vector3(p[2],.04,.04))
		batch("ledge-bracket",PrismMesh.new(),gold,Vector3(p[0],p[1]-.6,0),Vector3(.6,.8,1.5),Vector3(0,0,PI))
	ring("portal",glow,Vector3(40,1.6,-.4),1.5,.045,Vector3(PI/2,0,0));ring("portal-gold",gold,Vector3(40,1.6,-.4),1.7,.035,Vector3(PI/2,0,0))
	for key in groups:
		var v=groups[key];var mm=MultiMesh.new();mm.transform_format=MultiMesh.TRANSFORM_3D;mm.mesh=v.mesh;mm.instance_count=v.transforms.size()
		for i in v.transforms.size():mm.set_instance_transform(i,v.transforms[i])
		var n=MultiMeshInstance3D.new();n.multimesh=mm;add_child(n)
	groups.clear()
	var env=WorldEnvironment.new();environment=Environment.new();env.environment=environment;add_child(env)
	environment.background_mode=Environment.BG_SKY;environment.sky=Sky.new();var sky_mat=ShaderMaterial.new();sky_mat.shader=load("res://shaders/sky.gdshader");sky_mat.set_shader_parameter("horizon",z.sky.lightened(.18));sky_mat.set_shader_parameter("zenith",z.sky.darkened(.63));environment.sky.sky_material=sky_mat
	environment.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR;environment.ambient_light_color=accent.lightened(.45);environment.ambient_light_energy=.48;environment.reflected_light_source=Environment.REFLECTION_SOURCE_SKY
	environment.tonemap_mode=Environment.TONE_MAPPER_FILMIC;environment.tonemap_exposure=1.05;environment.fog_enabled=true;environment.fog_light_color=z.sky.lightened(.16);environment.fog_density=.006
	environment.glow_enabled=true;environment.glow_intensity=.7;environment.glow_strength=.9
	sun=DirectionalLight3D.new();sun.rotation_degrees=Vector3(-42,-28,-12);sun.light_color=Color("ffe1b1");sun.light_energy=1.55;sun.directional_shadow_max_distance=42;add_child(sun)
	var fill=DirectionalLight3D.new();fill.rotation_degrees=Vector3(-12,154,0);fill.light_color=accent;fill.light_energy=.65;add_child(fill);configure()
func configure():
	if sun:sun.shadow_enabled=g.settings.shadows
	if environment:environment.glow_enabled=g.settings.post
func floor_at(x,old_y,new_y):
	var floor_y=0.0
	for p in platforms:
		if absf(x-p[0])<p[2]/2 and old_y>=p[1]-.04 and new_y<=p[1]:floor_y=maxf(floor_y,p[1])
	return floor_y
func tick(dt):
	clock+=dt
	for pair in ornaments:pair[0].rotation.z+=dt*.018*pair[1]
