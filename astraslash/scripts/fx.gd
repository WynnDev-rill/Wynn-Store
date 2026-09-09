extends Node3D
var g
var particles=[]
var visuals=[]
var multimesh
var pool
var clock=0.0
const CAP=420
func material(c):
	var m=StandardMaterial3D.new();m.shading_mode=BaseMaterial3D.SHADING_MODE_UNSHADED;m.transparency=BaseMaterial3D.TRANSPARENCY_ALPHA;m.blend_mode=BaseMaterial3D.BLEND_MODE_ADD;m.albedo_color=c;m.cull_mode=BaseMaterial3D.CULL_DISABLED;m.no_depth_test=false;return m
func setup(game):
	g=game;multimesh=MultiMesh.new();multimesh.transform_format=MultiMesh.TRANSFORM_3D;multimesh.use_colors=true
	var mesh=PrismMesh.new();mesh.size=Vector3(.035,.12,.025);var m=material(Color.WHITE);m.vertex_color_use_as_albedo=true;mesh.material=m;multimesh.mesh=mesh;multimesh.instance_count=CAP;multimesh.visible_instance_count=0;pool=MultiMeshInstance3D.new();pool.multimesh=multimesh;add_child(pool)
func sparks(p,color,count=18,power=5.0):
	for i in range(int(count*g.settings.effects)):
		if particles.size()>=CAP:break
		var v=Vector3(randf_range(-1,1),randf_range(-.35,1.1),randf_range(-.3,.3)).normalized()*randf_range(power*.35,power)
		particles.append({"p":p,"v":v,"life":randf_range(.18,.52),"color":color,"size":randf_range(.65,1.8)})
func transient(mesh,m,p,life,growth=0.0,rotation=Vector3.ZERO):
	if visuals.size()>110:return null
	var n=MeshInstance3D.new();n.mesh=mesh;n.material_override=m;n.position=p;n.rotation=rotation;add_child(n);visuals.append({"n":n,"life":life,"max":life,"grow":growth,"mat":m,"p":p});return n
func ring(p,color,radius=1.0,life=.28,ground=false):
	var mesh=TorusMesh.new();mesh.inner_radius=radius*.94;mesh.outer_radius=radius;mesh.rings=40;mesh.ring_segments=5
	return transient(mesh,material(color),p,life,1.8,Vector3.ZERO if ground else Vector3(PI/2,0,0))
func slash(p,color,facing=1,radius=1.7,heavy=false):
	var st=SurfaceTool.new();st.begin(Mesh.PRIMITIVE_TRIANGLES)
	for i in range(25):
		var a=-1.6+3.0*i/25;var b=-1.6+3.0*(i+1)/25;var taper=sin(PI*(float(i)+.5)/25)
		var w=.10+taper*(.30 if heavy else .17)
		var pts=[Vector3(cos(a)*radius,sin(a)*radius,0),Vector3(cos(b)*radius,sin(b)*radius,0),Vector3(cos(a)*(radius-w),sin(a)*(radius-w),0),Vector3(cos(b)*(radius-w),sin(b)*(radius-w),0)]
		for j in [0,1,2,1,3,2]:st.add_vertex(pts[j]*Vector3(facing,1,1))
	var n=transient(st.commit(),material(color),p,.18 if heavy else .13,.35,Vector3(.1,0,randf_range(-.45,.45)))
	if n:n.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
func beam(a,b,color,width=.045,life=.13):
	var mesh=BoxMesh.new();mesh.size=Vector3(width,width,maxf(.03,a.distance_to(b)));var n=transient(mesh,material(color),(a+b)/2,life,.0)
	if n and a.distance_to(b)>.001:n.look_at(b)
func impact(p,color,heavy=false):
	sparks(p,color,32 if heavy else 16,7 if heavy else 4.8);ring(p,Color("fff3d9"),.3 if heavy else .18,.17)
	beam(p-Vector3(.5,.5,0),p+Vector3(.5,.5,0),Color.WHITE,.035,.055);beam(p-Vector3(.2,-.4,0),p+Vector3(.2,-.4,0),color,.045,.07)
	if g.settings.preset>=2 and visuals.size()<65:
		var n=OmniLight3D.new();n.position=p+Vector3(0,0,.4);n.light_color=color;n.light_energy=2.0;n.omni_range=3;add_child(n);visuals.append({"n":n,"life":.08,"max":.08,"grow":0,"mat":null,"p":p})
func danger(x,width,life):
	var mesh=BoxMesh.new();mesh.size=Vector3(width,.025,2.8);var m=material(Color(1,.10,.045,.23));transient(mesh,m,Vector3(x,.035,0),life,0)
	beam(Vector3(x-width/2,.07,1.45),Vector3(x+width/2,.07,1.45),Color("ff6a4c"),.04,life)
func death(p,color,boss=false):
	sparks(p,color,95 if boss else 35,11 if boss else 6);ring(p,color,1.5 if boss else .55,.6)
	for i in range(3 if boss else 1):ring(p,Color("ffe4b4"),.6+i*.7,.45+i*.1)
func number(p,amount,heavy=false,color=Color("f9efce")):
	if not g.settings.numbers or visuals.size()>85:return
	var n=Label3D.new();n.text=str(int(amount));n.font=load("res://assets/fonts/BarlowCondensed-Bold.ttf");n.font_size=46 if heavy else 35;n.pixel_size=.011;n.modulate=color;n.outline_modulate=Color("172335");n.outline_size=7;n.billboard=BaseMaterial3D.BILLBOARD_ENABLED;n.no_depth_test=true;n.position=p+Vector3(randf_range(-.2,.2),.35,.3);add_child(n);visuals.append({"n":n,"life":.55,"max":.55,"grow":0,"mat":null,"p":n.position,"text":true})
func ghost(rig):
	if g.settings.preset<2 or visuals.size()>65:return
	var m=material(Color(.3,.9,1,.14))
	for child in rig.find_children("*","MeshInstance3D",true,false):
		if visuals.size()>95:break
		var n=MeshInstance3D.new();n.mesh=child.mesh;n.material_override=m;n.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF;add_child(n);n.global_transform=child.global_transform;visuals.append({"n":n,"life":.11,"max":.11,"grow":0,"mat":m,"p":n.position})
func tick(dt):
	clock+=dt
	for i in range(particles.size()-1,-1,-1):
		var p=particles[i];p.life-=dt
		if p.life<=0:particles.remove_at(i);continue
		p.v.y-=dt*9;p.p+=p.v*dt
	for i in particles.size():
		var p=particles[i];var basis=Basis.from_euler(Vector3(0,0,atan2(-p.v.x,p.v.y))).scaled(Vector3.ONE*p.size)
		multimesh.set_instance_transform(i,Transform3D(basis,p.p));var c=p.color;c.a=clampf(p.life*4,0,1);multimesh.set_instance_color(i,c)
	multimesh.visible_instance_count=particles.size()
	for i in range(visuals.size()-1,-1,-1):
		var v=visuals[i];v.life-=dt
		if v.life<=0 or not is_instance_valid(v.n):
			if is_instance_valid(v.n):v.n.queue_free()
			visuals.remove_at(i);continue
		if v.mat:v.mat.albedo_color.a=clampf(v.life/v.max,0,1)*(.17 if v.max==.11 else .85)
		if v.get("text",false):v.n.position.y+=dt*.85;v.n.modulate.a=minf(1,v.life*4)
		elif v.grow>0:v.n.scale+=Vector3.ONE*dt*v.grow
func clear():
	particles.clear();multimesh.visible_instance_count=0
	for v in visuals:
		if is_instance_valid(v.n):v.n.queue_free()
	visuals.clear()
