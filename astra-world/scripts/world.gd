extends Node3D
const Art=preload("res://scripts/art.gd")
const Data=preload("res://scripts/data.gd")
var game
var rng=RandomNumberGenerator.new()
var scenery:Node3D
var interactables:Array=[]
var batches:Array=[]
var grass_batches:Array=[]
var environment:WorldEnvironment
var sun:DirectionalLight3D
var trees_count=0
var static_mesh_count=0
var water:MeshInstance3D
var route_points=[Vector2(0,48),Vector2(-63,6),Vector2(61,0),Vector2(-23,-58),Vector2(27,-92)]

static func height_at(x:float,z:float) -> float:
 var r=Vector2(x,z).length()
 var h=7.0+sin(x*0.025+0.5)*3.8+cos(z*0.027)*3.5+sin(x*0.072+z*0.04)*1.1
 h+=exp(-pow((z+78)/38.0,2))*12.0
 for l in Data.LANDMARKS:
  var d=Vector2(x,z).distance_to(l.pos)
  var target=8.0
  match l.id:
   "west":target=9.3
   "east":target=10.0
   "north":target=18.8
   "boss":target=22.0
   "secret":target=13.0
   "coast":target=7.0
  h=lerpf(target,h,smoothstep(10,22,d))
 var shore=126.0+sin(atan2(z,x)*7)*4.0
 h-=smoothstep(shore-12,shore+1,r)*(h+3.0)
 return h

func p(v:Vector2,offset:float=0.0) -> Vector3: return Vector3(v.x,height_at(v.x,v.y)+offset,v.y)

func build(g):
 game=g; rng.seed=926041
 scenery=Node3D.new(); add_child(scenery)
 _lighting(); _terrain(); _settlement(); _landmarks(); _nature(); _interactions(); _batch_static()

func _lighting():
 environment=WorldEnvironment.new(); add_child(environment)
 var e=Environment.new(); environment.environment=e
 e.background_mode=Environment.BG_SKY
 var sky=Sky.new(); var sm=ShaderMaterial.new(); sm.shader=load("res://shaders/sky.gdshader"); sky.sky_material=sm; e.sky=sky
 e.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR; e.ambient_light_color=Color("b6d1c7"); e.ambient_light_energy=0.63
 e.reflected_light_source=Environment.REFLECTED_SOURCE_SKY
 e.tonemap_mode=Environment.TONE_MAPPER_FILMIC
 e.fog_enabled=true; e.fog_light_color=Color("9ec9c4"); e.fog_density=0.0022
 sun=DirectionalLight3D.new(); sun.rotation_degrees=Vector3(-48,-32,0); sun.light_color=Color("ffe1b1"); sun.light_energy=1.5
 sun.shadow_enabled=true; sun.directional_shadow_max_distance=85; sun.directional_shadow_mode=DirectionalLight3D.SHADOW_PARALLEL_2_SPLITS
 add_child(sun)

func _terrain():
 var st=SurfaceTool.new(); st.begin(Mesh.PRIMITIVE_TRIANGLES)
 var size=144; var step=2.0
 for zi in range(size):
  for xi in range(size):
   var x=(xi-size/2)*step; var z=(zi-size/2)*step
   for off in [Vector2.ZERO,Vector2(step,0),Vector2(0,step),Vector2(step,0),Vector2(step,step),Vector2(0,step)]:
    var xx=x+off.x; var zz=z+off.y; var h=height_at(xx,zz)
    var n=Vector3(height_at(xx-0.3,zz)-height_at(xx+0.3,zz),0.6,height_at(xx,zz-0.3)-height_at(xx,zz+0.3)).normalized()
    var col=Color("83a875").lerp(Color("a8b888"),(sin(xx*0.14)*cos(zz*0.1)+1)*0.5)
    if n.y<0.82: col=col.lerp(Color("81958d"),clampf((0.9-n.y)*2.0,0,1))
    if h<2.3: col=Color("c7c6a0")
    if _path_distance(Vector2(xx,zz))<2.5 and h>2: col=col.lerp(Color("c6c6a1"),0.75)
    st.set_color(col); st.set_normal(n); st.add_vertex(Vector3(xx,h,zz))
 var mi=MeshInstance3D.new(); mi.mesh=st.commit(); var m=StandardMaterial3D.new(); m.vertex_color_use_as_albedo=true; m.roughness=1.0; m.cull_mode=BaseMaterial3D.CULL_DISABLED; mi.material_override=m; add_child(mi); mi.create_trimesh_collision()
 water=MeshInstance3D.new(); var plane=PlaneMesh.new(); plane.size=Vector2(1500,1500); plane.subdivide_depth=60; plane.subdivide_width=60; water.mesh=plane
 var wm=ShaderMaterial.new(); wm.shader=load("res://shaders/water.gdshader"); water.material_override=wm; water.position.y=0.5; add_child(water)
 for i in range(24):
  var a=i*TAU/24; var dist=rng.randf_range(210,360)
  Art.part(scenery,"rock",Vector3(cos(a)*dist,-11,sin(a)*dist),Vector3(rng.randf_range(20,45),rng.randf_range(27,62),rng.randf_range(20,38)),["769e9e","8db2ae","a2c1b7"][i%3],Vector3(0,i*28,0))
  if i%3==0:
   var island=Vector3(cos(a)*dist,32+rng.randf()*28,sin(a)*dist)
   Art.part(scenery,"rock",island,Vector3(14,10,11),"92b5ad")
   Art.arch(scenery,island+Vector3(0,6,0),a,9)

func _path_distance(v:Vector2) -> float:
 var best=999.0
 var edges=[[Vector2(-5,84),Vector2(0,48)],[Vector2(0,48),Vector2(-63,6)],[Vector2(0,48),Vector2(61,0)],[Vector2(-63,6),Vector2(-23,-58)],[Vector2(61,0),Vector2(-23,-58)],[Vector2(-23,-58),Vector2(27,-92)],[Vector2(61,0),Vector2(67,69)]]
 for edge in edges:
  var a:Vector2=edge[0]; var b:Vector2=edge[1]
  var t=clampf((v-a).dot(b-a)/(b-a).length_squared(),0,1)
  best=minf(best,v.distance_to(a+(b-a)*t))
 return best

func _settlement():
 for h in [[-16,51,0.35],[17,58,-0.6],[-17,35,1.2],[18,32,-1.1],[-8,65,0.2],[29,48,-1.5]]:
  var v=Vector2(h[0],h[1]); Art.house(scenery,p(v),h[2],int(abs(h[0])))
  _collider(p(v,2.2),Vector3(6.8,4.8,5.8),h[2])
 var center=p(Vector2(0,48))
 Art.part(scenery,"cylinder",center+Vector3(0,0.18,0),Vector3(4.3,0.35,4.3),"bcc3a9")
 Art.part(scenery,"cylinder",center+Vector3(0,0.8,0),Vector3(1.4,1.4,1.4),"a5b6a4")
 Art.part(scenery,"cylinder",center+Vector3(0,1.53,0),Vector3(1.7,0.22,1.7),"d3d2b5")
 Art.part(scenery,"cylinder",center+Vector3(0,1.66,0),Vector3(1.3,0.05,1.3),"70b7b8")
 _collider(center+Vector3(0,0.8,0),Vector3(2.7,1.6,2.7))
 for v in [Vector2(-7,58),Vector2(7,60),Vector2(-8,41),Vector2(7,38),Vector2(-25,25),Vector2(25,22)]: Art.lantern(scenery,p(v))
 # Village bunting: each pennant has a modeled silhouette rather than a flat billboard.
 for i in range(17):
  var x=-12+i*1.5; var y=13.0+pow(x/12,2)*1.8
  Art.part(scenery,"box",Vector3(x,y,58),Vector3(1.54,0.045,0.05),"847d61",Vector3(0,0,x*1.5))
  Art.part(scenery,"rock",Vector3(x,y-0.37,58),Vector3(0.37,0.5,0.035),["d6bc82","76aeb0","e1c4a5"][i%3])
 var forge=p(Vector2(10,47))
 Art.part(scenery,"cylinder",forge+Vector3(0,0.55,0),Vector3(1.8,1.1,1.5),"7c8882")
 Art.part(scenery,"sphere",forge+Vector3(0,1.1,0),Vector3(0.85,0.15,0.8),"f8be7c",Vector3.ZERO,0.6)
 Art.part(scenery,"box",forge+Vector3(0,1.38,0),Vector3(1.6,0.5,0.7),"536c71")
 for x in [-2,2]: Art.part(scenery,"taper",forge+Vector3(x,1.7,0),Vector3(0.13,3.4,0.13),"887b5c")
 Art.part(scenery,"box",forge+Vector3(0,3.3,0),Vector3(5.0,0.12,3),"bfb389",Vector3(0,0,5))

func _landmarks():
 for id in Data.BEACONS:
  var l=Data.landmark(id); var pos=p(l.pos)
  Art.beacon(scenery,pos)
  for i in range(8):
   var a=i*TAU/8; var v=l.pos+Vector2(cos(a),sin(a))*9
   Art.part(scenery,"box",p(v,0.18),Vector3(2.1,0.35,2.1),"b6c3ad",Vector3(0,rad_to_deg(a),0))
  if id=="north":
   for v in [Vector2(-8,-6),Vector2(8,-6),Vector2(-8,7),Vector2(8,7)]:
    Art.arch(scenery,pos+Vector3(v.x,0,v.y),PI/2,6)
   Art.part(scenery,"box",pos+Vector3(0,0.2,-8),Vector3(18,0.3,5),"aeb9a6")
  elif id=="west":
   Art.arch(scenery,pos+Vector3(0,0,12),0,6)
   for i in range(5):
    var bpos=pos+Vector3(-11+i*5,3.8,-9)
    Art.part(scenery,"cone",bpos,Vector3(0.38,0.6,0.38),"d7bf87")
    Art.part(scenery,"box",bpos+Vector3(0,1.4,0),Vector3(0.04,2.4,0.04),"9c997b")
  else:
   Art.arch(scenery,pos+Vector3(0,0,-11),0,7)
   var pool=Art.part(scenery,"cylinder",pos+Vector3(12,0.05,0),Vector3(8,0.12,6),"76bfc0")
   pool.material_override=water.material_override
   for i in range(12):
    var a=i*TAU/12
    Art.part(scenery,"rock",pos+Vector3(12+cos(a)*8,0.25,sin(a)*6),Vector3(1.2,0.7,1.1),"a7bfae")
 var arena=p(Vector2(27,-92))
 Art.part(scenery,"cylinder",arena+Vector3(0,0.12,0),Vector3(13,0.2,13),"adb7a7")
 for i in range(12):
  var a=i*TAU/12
  Art.part(scenery,"box",arena+Vector3(cos(a)*12,0.22,sin(a)*12),Vector3(2,0.2,1.2),"d7c18c",Vector3(0,-rad_to_deg(a),0))
  if i%2==0:
   Art.part(scenery,"taper",arena+Vector3(cos(a)*15,4,sin(a)*15),Vector3(1,8,1),"9ea99c")
   Art.part(scenery,"rock",arena+Vector3(cos(a)*15,8.5,sin(a)*15),Vector3(1.3,1.3,1.3),"cec09b")
 Art.arch(scenery,arena+Vector3(0,0,16),0,10)
 Art.arch(scenery,p(Vector2(-88,-40))+Vector3(0,0,7),0,5)
 # A long ruined aqueduct gives the distant view a unique readable silhouette.
 for i in range(5): Art.arch(scenery,p(Vector2(82-i*7,-42))+Vector3(0,0,0),0,8+i*0.3)

func _nature():
 for i in range(330):
  var v=Vector2(rng.randf_range(-123,123),rng.randf_range(-119,119)); var h=height_at(v.x,v.y)
  if h<4 or _path_distance(v)<4.5: continue
  var near=false
  for l in Data.LANDMARKS:
   if v.distance_to(l.pos)<(25 if l.id=="village" else 17): near=true
  if near: continue
  Art.tree(scenery,p(v),rng,v.x<-40 and v.y>-20); trees_count+=1
  if i%4==0: _collider(p(v,1.0),Vector3(0.6,2,0.6))
 for i in range(480):
  var v=Vector2(rng.randf_range(-126,126),rng.randf_range(-123,123))
  if height_at(v.x,v.y)<2 or _path_distance(v)<3: continue
  var near=false
  for l in Data.LANDMARKS:
   if v.distance_to(l.pos)<15: near=true
  if near: continue
  Art.part(scenery,"rock",p(v,0.25),Vector3.ONE*rng.randf_range(0.45,2.8)*Vector3(1.3,0.7,1),["a1b29b","80968b","a9b99c"][i%3],Vector3(0,rng.randf()*360,0))
 # Grass cells have independent culling and an actual density scalability setting.
 var gm=ShaderMaterial.new(); gm.shader=load("res://shaders/foliage.gdshader")
 for cz in range(-4,5):
  for cx in range(-4,5):
   var transforms:Array[Transform3D]=[]
   for j in range(90):
    var v=Vector2(cx*28+rng.randf_range(-14,14),cz*28+rng.randf_range(-14,14))
    if v.length()>119 or height_at(v.x,v.y)<3 or _path_distance(v)<2.7: continue
    var blocked=false
    for l in Data.LANDMARKS:
     if v.distance_to(l.pos)<14: blocked=true
    if blocked: continue
    var b=Basis(Vector3.UP,rng.randf()*TAU).scaled(Vector3.ONE*rng.randf_range(0.7,1.4))
    transforms.append(Transform3D(b,p(v)))
   if transforms.is_empty(): continue
   var mm=MultiMesh.new(); mm.transform_format=MultiMesh.TRANSFORM_3D; mm.mesh=Art.mesh("grass"); mm.instance_count=transforms.size()
   for j in transforms.size(): mm.set_instance_transform(j,transforms[j])
   var mi=MultiMeshInstance3D.new(); mi.multimesh=mm; mi.material_override=gm; mi.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF; mi.visibility_range_end=58; add_child(mi); grass_batches.append(mi)
 for i in range(170):
  var v=Vector2(rng.randf_range(-98,98),rng.randf_range(-85,91))
  if height_at(v.x,v.y)<4 or _path_distance(v)<3: continue
  for j in range(3):
   var fp=p(v+Vector2(j*0.25,cos(j)*0.25))
   Art.part(scenery,"taper",fp+Vector3(0,0.27,0),Vector3(0.028,0.55,0.028),"729268")
   Art.part(scenery,"sphere",fp+Vector3(0,0.58,0),Vector3(0.16,0.09,0.16),["ead4a1","c1bed0","efbda5"][i%3])

func _interactions():
 for npc in [["ilya","Ilya",Vector2(-3,48),"6c9186"],["sava","Sava",Vector2(8,44),"a08669"],["mira","Mira",Vector2(-9,56),"a58991"]]:
  var n=Art.person(self,true,npc[3]); n.position=p(npc[2]); n.rotation.y=0.3
  _register(npc[0],npc[1],"npc",n)
 _register("forge","Tungku Sava","forge",Art.group(self,p(Vector2(10,47))))
 _register("rest","Beristirahat","rest",Art.group(self,p(Vector2(4,52))))
 for id in Data.BEACONS:
  var pos=p(Data.landmark(id).pos)
  var n=Art.group(self,pos); var ring=Art.part(n,"ring",Vector3(0,3.5,0),Vector3.ONE*1.5,"80e7c9",Vector3(90,0,0),0.4); ring.name="Halo"
  _register(id,"Selaraskan mercusuar","beacon",n)
 # Guaranteed early resources plus authored excursions; no random progression lock.
 var resources=[
 ["wood",Vector2(-6,69)],["wood",Vector2(-9,72)],["wood",Vector2(4,66)],["wood",Vector2(13,72)],["wood",Vector2(-27,43)],
 ["crystal",Vector2(6,74)],["crystal",Vector2(9,78)],["crystal",Vector2(-12,78)],["crystal",Vector2(-25,28)],["crystal",Vector2(29,31)],
 ["herb",Vector2(-4,61)],["herb",Vector2(6,58)],["herb",Vector2(-30,18)],["herb",Vector2(32,16)]]
 for i in range(64):
  var id=["wood","crystal","herb"][i%3]
  var v=Vector2(rng.randf_range(-100,100),rng.randf_range(-80,76))
  if height_at(v.x,v.y)>4: resources.append([id,v])
 for i in resources.size():
  var item=resources[i][0]; var n=Art.group(self,p(resources[i][1])); var color=Data.ITEMS[item].color
  if item=="crystal":
   for j in range(3): Art.part(n,"rock",Vector3((j-1)*0.32,0.55,0),Vector3(0.3,0.9-j*0.14,0.3),color,Vector3(0,j*35,(j-1)*18),0.18)
  elif item=="wood":
   Art.part(n,"taper",Vector3(0,0.18,0),Vector3(0.23,1.55,0.23),"9e8661",Vector3(0,0,82))
   Art.part(n,"sphere",Vector3(0.65,0.4,0),Vector3(0.4,0.17,0.4),"a8b881")
  else:
   for j in range(4): Art.part(n,"rock",Vector3(sin(j)*0.2,0.35,cos(j)*0.2),Vector3(0.15,0.5,0.18),color,Vector3(0,0,sin(j)*25))
  _register("gather_%d"%i,Data.ITEMS[item].name,"gather",n,{"item":item})
 for i in range(8):
  var v=[Vector2(69,71),Vector2(76,8),Vector2(-35,-70),Vector2(-93,-45),Vector2(-79,21),Vector2(43,-43),Vector2(-7,-30),Vector2(93,44)][i]
  var n=Art.group(self,p(v)); Art.part(n,"box",Vector3(0,0.42,0),Vector3(1.3,0.75,0.8),"778e80"); Art.part(n,"box",Vector3(0,0.84,0),Vector3(1.4,0.17,0.9),"cfb780"); Art.part(n,"box",Vector3(0,0.45,-0.43),Vector3(0.18,0.25,0.07),"8be4c9",Vector3.ZERO,0.25)
  _register("chest_%d"%i,"Peti pengelana","chest",n,{"letter":i<3,"secret":i==3})
 var note=Art.group(self,p(Vector2(-88,-33))); Art.part(note,"box",Vector3(0,1,0),Vector3(1.6,1.2,0.3),"b3bda6"); _register("note","Prasasti taman","note",note)
 for i in range(3):
  var n=Art.group(self,p(Vector2(-92+i*4,-40))); Art.part(n,"rock",Vector3(0,0.8,0),Vector3(0.65,1,0.6),["99c49a","89cbd5","e8ca85"][i],Vector3.ZERO,0.2)
  _register("rune_%d"%i,["Akar","Hujan","Fajar"][i],"rune",n,{"order":i})

func _register(id:String,label:String,kind:String,n:Node3D,extra:Dictionary={}):
 var d={"id":id,"label":label,"kind":kind,"node":n}; d.merge(extra); interactables.append(d)
 if id in game.state.s.collected: n.visible=false

func _collider(pos:Vector3,size:Vector3,rot:float=0):
 var body=StaticBody3D.new(); body.position=pos; body.rotation.y=rot; var c=CollisionShape3D.new(); var b=BoxShape3D.new(); b.size=size; c.shape=b; body.add_child(c); add_child(body)

func _batch_static():
 var grouped={}
 var nodes=scenery.find_children("*","MeshInstance3D",true,false)
 static_mesh_count=nodes.size()
 for n in nodes:
  var key=str(n.mesh.get_instance_id())+"_"+str(n.material_override.get_instance_id())
  if not grouped.has(key): grouped[key]={"mesh":n.mesh,"mat":n.material_override,"transforms":[]}
  grouped[key].transforms.append(n.global_transform)
 for data in grouped.values():
  var mm=MultiMesh.new(); mm.transform_format=MultiMesh.TRANSFORM_3D; mm.mesh=data.mesh; mm.instance_count=data.transforms.size()
  for i in mm.instance_count: mm.set_instance_transform(i,data.transforms[i])
  var n=MultiMeshInstance3D.new(); n.multimesh=mm; n.material_override=data.mat; add_child(n); batches.append(n)
 scenery.queue_free()

func apply_settings():
 sun.shadow_enabled=game.state.settings.shadows
 var density=game.state.settings.foliage
 for n in grass_batches:
  n.multimesh.visible_instance_count=int(n.multimesh.instance_count*density)
  n.visibility_range_end=40+35*density

func refresh():
 for d in interactables:
  if d.kind=="beacon":
   d.node.get_node("Halo").visible=d.id in game.state.s.beacons
  elif d.id in game.state.s.collected: d.node.visible=false
