@static_unload
extends RefCounted
## Original mesh kit. Shared primitives are sculpted into authored silhouettes,
## then baked to material/mesh MultiMeshes by the world builder.
static var materials:Dictionary={}
static var meshes:Dictionary={}

static func mat(hex:String, glow:float=0.0) -> StandardMaterial3D:
 var key=hex+str(glow)
 if materials.has(key): return materials[key]
 var m=StandardMaterial3D.new()
 m.albedo_color=Color(hex); m.roughness=0.86
 if glow>0:
  m.emission_enabled=true; m.emission=Color(hex); m.emission_energy_multiplier=glow
 materials[key]=m
 return m

static func mesh(kind:String) -> Mesh:
 if meshes.has(kind): return meshes[kind]
 var m:Mesh
 match kind:
  "box": m=BoxMesh.new()
  "sphere":
   var s=SphereMesh.new(); s.radius=1; s.height=2; s.radial_segments=12; s.rings=6; m=s
  "cylinder","cone","roof","taper":
   var c=CylinderMesh.new(); c.height=1; c.bottom_radius=1; c.top_radius=1 if kind=="cylinder" else (0.62 if kind=="taper" else 0.0); c.radial_segments=4 if kind=="roof" else 10; m=c
  "ring":
   var t=TorusMesh.new(); t.inner_radius=0.86; t.outer_radius=1.0; t.rings=24; t.ring_segments=6; m=t
  "rock":
   var st=SurfaceTool.new(); st.begin(Mesh.PRIMITIVE_TRIANGLES)
   var v=[Vector3(-1,0,0),Vector3(1,0,0),Vector3(0,-0.85,0),Vector3(0,1.15,0),Vector3(0,0,-1),Vector3(0,0,1)]
   for f in [[0,3,4],[4,3,1],[1,3,5],[5,3,0],[4,2,0],[1,2,4],[5,2,1],[0,2,5]]:
    for i in f: st.add_vertex(v[i])
   st.generate_normals(); m=st.commit()
  "grass":
   var st=SurfaceTool.new(); st.begin(Mesh.PRIMITIVE_TRIANGLES)
   for a in [0.0,1.2,2.4]:
    var side=Vector3(cos(a),0,sin(a))*0.16
    for v in [-side,Vector3(0.12,0.75,0.08),side]:
     st.set_normal(Vector3.UP); st.add_vertex(v)
   m=st.commit()
 meshes[kind]=m
 return m

static func part(parent:Node3D,kind:String,pos:Vector3,size:Vector3,color:String,rot:Vector3=Vector3.ZERO,glow:float=0.0) -> MeshInstance3D:
 var p=MeshInstance3D.new(); p.mesh=mesh(kind); p.material_override=mat(color,glow)
 p.position=pos; p.scale=size; p.rotation_degrees=rot; parent.add_child(p)
 return p

static func group(parent:Node3D,pos:Vector3=Vector3.ZERO) -> Node3D:
 var n=Node3D.new(); parent.add_child(n); n.position=pos; return n

static func tree(parent:Node3D,pos:Vector3,rng:RandomNumberGenerator,bloom:bool=false):
 var g=group(parent,pos); g.rotation.y=rng.randf()*TAU
 var h=rng.randf_range(5,9)
 part(g,"taper",Vector3(0,h*0.4,0),Vector3(0.4,h*0.8,0.45),"635f51",Vector3(0,0,8))
 for i in range(4):
  var a=i*2.1
  part(g,"taper",Vector3(cos(a)*0.65,h*0.65,sin(a)*0.65),Vector3(0.17,h*0.34,0.17),"77705a",Vector3(24,rad_to_deg(a),28))
 var colors=["e2b7a1","d69f92","f1ccad"] if bloom else ["628b66","82a979","a0bc8a","537f65"]
 for i in range(7):
  var a=i*2.39; var r=1.6 if i>0 else 0.0
  part(g,"sphere",Vector3(cos(a)*r,h-0.5+sin(a*3)*0.65,sin(a)*r),Vector3(2.2,1.5,2.0),colors[i%colors.size()])
 for i in range(3):
  part(g,"rock",Vector3(cos(i*2.1)*0.5,0.16,sin(i*2.1)*0.5),Vector3(0.6,0.25,0.5),"698b65")

static func house(parent:Node3D,pos:Vector3,rot:float,variant:int=0):
 var g=group(parent,pos); g.rotation.y=rot
 part(g,"box",Vector3(0,0.4,0),Vector3(7.4,0.8,6.4),"929180")
 part(g,"box",Vector3(0,2.6,0),Vector3(6.8,4.0,5.8),"e5d8b5")
 part(g,"roof",Vector3(0,5.7,0),Vector3(5.6,3.2,5.0),"456f73" if variant%2==0 else "70847b",Vector3(0,45,0))
 part(g,"roof",Vector3(0,5.46,0),Vector3(5.82,0.45,5.22),"c8ae73",Vector3(0,45,0))
 for x in [-3.25,3.25]:
  part(g,"box",Vector3(x,2.6,2.92),Vector3(0.22,4.2,0.16),"897a5b")
 part(g,"box",Vector3(0,1.5,2.94),Vector3(1.5,2.5,0.18),"5e6a5c")
 part(g,"sphere",Vector3(0.48,1.5,3.09),Vector3(0.08,0.08,0.08),"e7b972")
 for x in [-2.2,2.2]:
  part(g,"box",Vector3(x,2.65,2.97),Vector3(1.05,1.2,0.12),"bd9861")
  part(g,"box",Vector3(x,2.65,3.05),Vector3(0.84,0.95,0.1),"ffe1a0",Vector3.ZERO,0.3)
  part(g,"box",Vector3(x,2.65,3.13),Vector3(0.08,1.1,0.1),"6d765f")
  part(g,"box",Vector3(x,2.65,3.13),Vector3(1.0,0.08,0.1),"6d765f")
 for i in range(3):
  part(g,"box",Vector3(0,0.14+i*0.13,4.2-i*0.38),Vector3(2.9,0.26,0.9),"b9b9a2")
 part(g,"cylinder",Vector3(3.0,0.7,3.45),Vector3(0.52,1.3,0.52),"a97a59")
 part(g,"sphere",Vector3(3.0,1.5,3.45),Vector3(0.8,0.65,0.8),"86a972")
 part(g,"box",Vector3(1.75,5.8,-1.4),Vector3(0.7,2.5,0.7),"b6b4a0")

static func arch(parent:Node3D,pos:Vector3,rot:float=0.0,height:float=7.0):
 var g=group(parent,pos); g.rotation.y=rot
 for side in [-1,1]:
  part(g,"box",Vector3(side*2.8,height*0.4,0),Vector3(1.1,height*0.8,1.4),"c2c7b0")
  for y in [0.3,height*0.76]: part(g,"box",Vector3(side*2.8,y,0),Vector3(1.6,0.5,1.9),"d4d2b7")
 for i in range(9):
  var a=i*PI/8
  part(g,"box",Vector3(cos(a)*2.8,height*0.77+sin(a)*2.8,0),Vector3(1.18,1.15,1.5),"cbd0b8",Vector3(0,0,rad_to_deg(a)-90))
 part(g,"rock",Vector3(-2.8,0.4,1),Vector3(1.2,0.65,1.1),"738f73")

static func lantern(parent:Node3D,pos:Vector3):
 var g=group(parent,pos)
 part(g,"taper",Vector3(0,1.7,0),Vector3(0.11,3.4,0.11),"58665e")
 part(g,"box",Vector3(0,3.2,0),Vector3(0.6,0.75,0.6),"f6d99a",Vector3.ZERO,0.6)
 part(g,"roof",Vector3(0,3.7,0),Vector3(0.6,0.4,0.6),"567d78",Vector3(0,45,0))

static func beacon(parent:Node3D,pos:Vector3) -> Node3D:
 var g=group(parent,pos)
 for i in range(3): part(g,"cylinder",Vector3(0,0.2+i*0.32,0),Vector3(3.4-i*0.4,0.4,3.4-i*0.4),"bdc8b4")
 for x in [-1,1]:
  part(g,"taper",Vector3(x*1.3,3.2,0),Vector3(0.36,4.5,0.36),"ded6ae",Vector3(0,0,-x*13))
  part(g,"rock",Vector3(x*0.7,5.6,0),Vector3(0.4,1.3,0.25),"dac791",Vector3(0,0,-x*28))
 part(g,"ring",Vector3(0,3.45,0),Vector3(1.15,1.15,1.15),"dec994",Vector3(90,0,0))
 part(g,"rock",Vector3(0,3.45,0),Vector3(0.47,1.05,0.47),"79dec7",Vector3.ZERO,0.65)
 part(g,"cone",Vector3(0,2.75,0),Vector3(0.68,1.1,0.68),"f1e0b8")
 return g

static func person(parent:Node3D,npc:bool=false,color:String="426c79") -> Node3D:
 var g=group(parent)
 # A compact original human silhouette with layered clothing and separate joints.
 var hips=group(g,Vector3(0,0.96,0)); hips.name="Hips"
 part(hips,"taper",Vector3(0,0.22,0),Vector3(0.29,0.60,0.22),color)
 part(hips,"box",Vector3(0,0.01,0),Vector3(0.59,0.09,0.48),"b79562")
 part(hips,"taper",Vector3(0,-0.1,0.025),Vector3(0.41,0.50,0.29),color)
 var head=group(g,Vector3(0,1.73,0)); head.name="Head"
 part(head,"sphere",Vector3.ZERO,Vector3(0.24,0.30,0.22),"d9b28a")
 part(head,"sphere",Vector3(0,0.12,0.055),Vector3(0.255,0.24,0.235),"444d50" if not npc else "9d9581")
 for x in [-0.085,0.085]: part(head,"sphere",Vector3(x,0.015,-0.207),Vector3(0.027,0.022,0.017),"293b42")
 part(head,"cone",Vector3(0.18,-0.13,0.03),Vector3(0.1,0.3,0.11),"3a4b50",Vector3(0,0,12))
 var cape=group(g,Vector3(0,1.52,0.09)); cape.name="Cape"
 part(cape,"taper",Vector3(0,-0.16,0.06),Vector3(0.48,0.65,0.35),"dfd9bb" if not npc else "b6b7a1")
 part(cape,"box",Vector3(0,-0.47,0.32),Vector3(0.73,0.08,0.10),"caa66e")
 part(cape,"rock",Vector3(0,0.08,-0.36),Vector3(0.08,0.12,0.045),"81ddc4",Vector3.ZERO,0.4)
 for side in [-1,1]:
  var leg=group(g,Vector3(side*0.17,0.85,0)); leg.name="LegL" if side<0 else "LegR"
  part(leg,"taper",Vector3(0,-0.22,0),Vector3(0.115,0.5,0.13),"344d5c")
  part(leg,"taper",Vector3(0,-0.59,0.025),Vector3(0.115,0.36,0.12),"65645a")
  part(leg,"sphere",Vector3(0,-0.75,-0.055),Vector3(0.125,0.1,0.22),"55564f")
  var arm=group(g,Vector3(side*0.34,1.45,0)); arm.name="ArmL" if side<0 else "ArmR"
  part(arm,"taper",Vector3(side*0.035,-0.17,0),Vector3(0.105,0.4,0.115),color,Vector3(0,0,side*10))
  part(arm,"taper",Vector3(side*0.07,-0.45,0),Vector3(0.08,0.24,0.085),"d9b28a")
  part(arm,"sphere",Vector3(side*0.07,-0.61,0),Vector3(0.082,0.1,0.085),"d9b28a")
  if side==1 and not npc:
   var sword=group(arm,Vector3(0.07,-0.61,0)); sword.name="Sword"
   part(sword,"box",Vector3(0,-0.2,0),Vector3(0.06,0.34,0.06),"81684b")
   part(sword,"box",Vector3(0,-0.30,0),Vector3(0.36,0.075,0.11),"d9bc7f")
   part(sword,"rock",Vector3(0,-0.87,0),Vector3(0.1,0.60,0.06),"d2e4d8")
 var glider=group(g,Vector3(0,1.5,0.25)); glider.name="Glider"; glider.visible=false
 for side in [-1,1]:
  part(glider,"rock",Vector3(side*1.0,0,0),Vector3(1.3,0.06,0.7),"e1d3ad",Vector3(0,0,side*12))
  part(glider,"box",Vector3(side*1.0,0,0),Vector3(2.1,0.055,0.045),"8a7c58",Vector3(0,0,side*12))
 return g

static func construct(parent:Node3D,elite:bool=false,boss:bool=false) -> Node3D:
 var g=group(parent)
 var stone="757e7d" if not elite else "697179"
 part(g,"rock",Vector3(0,1.25,0),Vector3(0.75,0.78,0.56),stone)
 part(g,"rock",Vector3(0,1.28,-0.49),Vector3(0.2,0.34,0.08),"f6a076",Vector3.ZERO,0.7)
 part(g,"box",Vector3(0,2.04,-0.12),Vector3(0.7,0.43,0.53),"a8ac98")
 part(g,"box",Vector3(0,2.07,-0.41),Vector3(0.4,0.08,0.04),"fac083",Vector3.ZERO,0.9)
 for side in [-1,1]:
  var arm=group(g,Vector3(side*0.72,1.65,0)); arm.name="ArmL" if side<0 else "ArmR"
  part(arm,"rock",Vector3(0,-0.2,0),Vector3(0.37,0.56,0.35),stone)
  part(arm,"rock",Vector3(side*0.07,-0.74,0),Vector3(0.31,0.33,0.33),"a5a98f")
  var leg=group(g,Vector3(side*0.35,0.83,0)); leg.name="LegL" if side<0 else "LegR"
  part(leg,"rock",Vector3(0,-0.32,0),Vector3(0.25,0.54,0.29),stone)
 if elite or boss:
  for i in range(5):
   var a=(i-2)*0.43
   part(g,"rock",Vector3(sin(a)*0.65,2.37+cos(a)*0.22,0),Vector3(0.15,0.4,0.14),"d4b575",Vector3(0,0,-rad_to_deg(a)))
 if boss: g.scale=Vector3.ONE*2.15
 elif elite: g.scale=Vector3.ONE*1.35
 return g
