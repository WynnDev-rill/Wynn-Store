"""AstraSlash original articulated character sculpture. Blender 4.x, no asset imports.
Exported meshes and designs: CC0. Named pivots are animated by rig.gd.
Run: blender --background --python tools/make_models.py
"""
import bpy, math, random
from mathutils import Vector
from pathlib import Path
OUT=Path(__file__).resolve().parents[1]/'assets/models';OUT.mkdir(parents=True,exist_ok=True)
rng=random.Random(174)
def material(name,color,emission=0,metal=0):
 m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
 s=m.node_tree.nodes.get('Principled BSDF');s.inputs['Base Color'].default_value=(*color,1);s.inputs['Roughness'].default_value=.63;s.inputs['Metallic'].default_value=metal
 s.inputs['Emission Color'].default_value=(*color,1);s.inputs['Emission Strength'].default_value=emission
 return m
def pivot(name,pos,parent=None):
 o=bpy.data.objects.new(name,None);bpy.context.collection.objects.link(o);o.parent=parent;o.location=pos;return o
def mesh(name,vs,fs,mat,parent,smooth=True):
 m=bpy.data.meshes.new(name);m.from_pydata(vs,[],fs);m.update();o=bpy.data.objects.new(name,m);bpy.context.collection.objects.link(o);o.parent=parent;o.data.materials.append(mat)
 for p in m.polygons:p.use_smooth=smooth
 return o
def ell(name,pos,scale,mat,parent):
 bpy.ops.mesh.primitive_uv_sphere_add(segments=20,ring_count=12,location=(0,0,0));o=bpy.context.object;o.name=name;o.parent=parent;o.location=pos;o.scale=scale;o.data.materials.append(mat)
 for p in o.data.polygons:p.use_smooth=True
 return o
def profile(name,levels,mat,parent,steps=16):
 vs=[];fs=[]
 for z,w,d,cy in levels:
  for i in range(steps):
   a=i*2*math.pi/steps;vs.append((math.sin(a)*w,cy+math.cos(a)*d,z))
 for j in range(len(levels)-1):
  for i in range(steps):a=j*steps+i;b=j*steps+(i+1)%steps;fs.append((a,b,b+steps,a+steps))
 fs.extend([tuple(range(steps-1,-1,-1)),tuple((len(levels)-1)*steps+i for i in range(steps))]);return mesh(name,vs,fs,mat,parent)
def tube(name,points,radius,mat,parent,sides=8):
 vs=[];fs=[]
 for i,p in enumerate(points):
  p=Vector(p);t=Vector(points[min(i+1,len(points)-1)])-Vector(points[max(i-1,0)]);t.normalize();u=t.cross(Vector((0,1,0))).normalized()
  if u.length<.01:u=Vector((1,0,0))
  v=t.cross(u).normalized()
  for j in range(sides):
   a=j*math.tau/sides;vs.append(p+radius*(u*math.cos(a)+v*math.sin(a)))
 for i in range(len(points)-1):
  for j in range(sides):a=i*sides+j;b=i*sides+(j+1)%sides;fs.append((a,b,b+sides,a+sides))
 fs.extend([tuple(range(sides-1,-1,-1)),tuple((len(points)-1)*sides+i for i in range(sides))]);return mesh(name,vs,fs,mat,parent)
def ribbon(name,points,widths,mat,parent):
 vs=[];fs=[]
 for (x,y,z),w in zip(points,widths):vs.extend([(x-w,y,z),(x,y-.032,z+.005),(x+w,y,z),(x,y+.012,z-.01)])
 for i in range(len(points)-1):
  for j in range(4):a=i*4+j;b=i*4+(j+1)%4;fs.append((a,b,b+4,a+4))
 fs.append((0,3,2,1));return mesh(name,vs,fs,mat,parent,False)
def gem(name,p,w,h,mat,parent):
 x,y,z=p;return mesh(name,[(x-w,y,z),(x,y-w*.55,z),(x+w,y,z),(x,y+w*.55,z),(x,y,z+h),(x,y,z-h)],[(0,1,4),(1,2,4),(2,3,4),(3,0,4),(1,0,5),(2,1,5),(3,2,5),(0,3,5)],mat,parent,False)
for kind in ['rei','kael','hollow','shield','oracle','warden','cantor','regent']:
 bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
 hero=kind in ['rei','kael'];kael=kind=='kael';boss=kind in ['warden','cantor','regent']
 navy=material('midnight',(0.023,.045,.084));ink=material('carbon',(.012,.017,.028));ivory=material('porcelain',(.79,.83,.79));gold=material('old gold',(.57,.38,.16),metal=.5)
 skin=material('warm skin',(.78,.53,.39));white=material('eye white',(.94,.95,.90));hair=material('hair',(.80,.87,.86) if kind=='rei' else (.07,.10,.18));red=material('vermilion lining',(.48,.06,.035));metal=material('cold silver',(.34,.45,.49),metal=.45)
 glow=material('star inlay',(.14,.85,.94) if kind=='rei' else ((1,.65,.2) if kael else (1,.12,.05)),2.1)
 iris=material('amber',(.9,.44,.085),.3)
 root=pivot('Model',(0,0,0));hip=pivot('Hip',(0,0,1.12),root);torso=pivot('Torso',(0,0,.16),hip);head=pivot('Head',(0,0,.73),torso)
 profile('tailored core',[(-.15,.17,.105,0),(.02,.178,.115,0),(.18,.165,.102,0),(.40,.23,.13,0),(.51,.246,.118,0),(.57,.115,.083,0)],navy,torso)
 profile('waist belt',[(-.022,.18,.127,0),(.044,.18,.127,0)],ink,torso)
 for side in [-1,1]:
  ribbon('ivory lapel',[(side*.11,-.091,.555),(side*.19,-.12,.43),(side*.075,-.133,.19),(side*.058,-.127,.05)],[.043,.05,.031,.015],ivory,torso)
  tube('lapel gold seam',[(side*.135,-.11,.535),(side*.195,-.145,.435),(side*.097,-.16,.19)],.007,gold,torso)
  for z in [.10,.23,.34]:ell('button',(side*.035,-.135,z),(.012,.008,.012),gold,torso)
 gem('sternum star',(0,-.141,.405),.048,.07,glow,torso);gem('belt clasp',(0,-.133,.015),.046,.045,gold,torso)
 profile('neck',[(.52,.064,.06,0),(.69,.06,.057,0)],skin if hero else ink,torso)
 if hero:
  profile('face',[(-.123,.022,.029,-.038),(-.097,.069,.067,-.03),(-.037,.128,.099,-.007),(.047,.151,.12,0),(.133,.145,.116,.004),(.207,.113,.101,.01),(.244,.033,.036,.018)],skin,head,24)
  for side in [-1,1]:
   ell('ear',(side*.151,0,.025),(.025,.024,.044),skin,head)
   eye=ell('eye almond',(side*.067,-.116,.06),(.056,.022,.027),white,head);eye.rotation_euler[1]=side*-.12
   ell('iris',(side*.066,-.138,.061),(.021,.005,.023),iris,head);ell('pupil',(side*.066,-.143,.061),(.009,.003,.017),ink,head);ell('catchlight',(side*.072,-.147,.073),(.006,.002,.007),white,head)
   tube('lash',[(side*.016,-.127,.077),(side*.068,-.143,.09),(side*.123,-.122,.081)],.006,ink,head)
   tube('brow',[(side*.023,-.12,.123),(side*.071,-.130,.134),(side*.115,-.108,.128)],.007,hair,head)
  mesh('nose',[(-.012,-.122,.024),(.012,-.122,.024),(0,-.159,-.015),(-.008,-.122,-.029),(.008,-.122,-.029)],[(0,1,2),(0,2,3),(1,4,2),(2,4,3)],skin,head)
  tube('mouth',[(-.027,-.108,-.063),(0,-.12,-.067),(.026,-.108,-.063)],.0035,red,head)
  ell('hair crown',(0,.008,.14),(.164,.133,.132),hair,head)
  for j in range(13):
   a=(j/12)*math.pi*1.66-.83*math.pi;x=math.sin(a);y=math.cos(a)
   ribbon('layered hair',[(x*.11,y*.085,.238),(x*.16,y*.133,.13),(x*.17,y*.14,-.01),(x*.137,y*.12,-.13-(j%3)*.025)],[.046,.048,.037,.002],hair,head)
  for j in range(7):
   x=(j-3)*.039
   ribbon('asymmetric fringe',[(x*.6,-.056,.257),(x,-.120,.190),(x+.026,-.143,.098 if j<3 else .032)],[.032,.035,.002],hair,head)
  gem('temple star',(-.162,-.062,.125),.033,.052,glow,head)
  if kael:
   pony=pivot('HairTip',(0,.122,.08),head)
   for i in range(4):ribbon('tied indigo hair',[((i-1.5)*.025,0,.015),((i-1.5)*.03,.04,-.18),((i-1.5)*.025,.025,-.40)],[.037,.039,.002],hair,pony)
 else:
  profile('faceted automaton mask',[(-.13,.045,.075,-.01),(-.06,.11,.104,0),(.13,.153,.10,0),(.24,.07,.08,.01)],ivory,head,8)
  for side in [-1,1]:
   tube('fractured eye',[(side*.019,-.105,.07),(side*.075,-.11,.073),(side*.126,-.08,.10)],.014,glow,head)
   ribbon('crown horn',[(side*.12,0,.16),(side*.18,.02,.36),(side*.15,.05,.50 if boss else .38)],[.04,.03,.001],gold if boss else metal,head)
  gem('mask split',(0,-.112,-.004),.012,.15,glow,head)
 for side,label in [(-1,'L'),(1,'R')]:
  thigh=pivot('Thigh_'+label,(side*.113,0,-.022),hip);shin=pivot('Shin_'+label,(0,0,-.49),thigh)
  profile('fitted trousers',[(.03,.108,.108,0),(-.23,.084,.082,0),(-.49,.067,.061,0)],navy,thigh)
  tube('trouser piping',[(side*.093,-.015,-.03),(side*.083,-.02,-.28),(side*.064,-.01,-.46)],.007,gold,thigh)
  ell('knee shield',(0,-.047,-.012),(.076,.045,.092),metal,shin)
  profile('boot shaft',[(0,.071,.065,0),(-.18,.082,.072,0),(-.40,.066,.067,-.006),(-.50,.075,.081,-.01)],ink,shin)
  ell('boot',(0,-.068,-.505),(.083,.152,.058),ink,shin)
  tube('boot gold rail',[(0,-.073,-.11),(0,-.074,-.34),(0,-.122,-.48)],.009,gold,shin)
  arm=pivot('UpperArm_'+label,(side*.256,0,.486),torso);fore=pivot('Forearm_'+label,(0,0,-.30),arm)
  profile('sleeve',[(.025,.084,.079,0),(-.17,.07,.068,0),(-.30,.055,.056,0)],skin if kael else navy,arm)
  ell('pauldron',(side*.018,0,.006),(.105,.105,.09),ivory,arm)
  tube('shoulder edge',[(side*.092,-.064,.028),(side*.11,-.03,-.026),(side*.095,.04,-.04)],.011,gold,arm)
  profile('gauntlet',[(0,.058,.058,0),(-.13,.071,.062,0),(-.25,.046,.047,0)],metal,fore)
  ribbon('gauntlet rune',[(0,-.062,-.045),(0,-.066,-.12),(0,-.052,-.22)],[.014,.014,.001],glow,fore)
  ell('glove',(0,-.005,-.304),(.046,.041,.062),ink,fore)
  for j in range(4):ell('finger',((j-1.5)*.018,-.021,-.343),(.01,.02,.024),ink,fore)
  coat=pivot('Coat_'+label,(side*.133,.062,.075),hip)
  ribbon('split coat',[(side*.009,.01,0),(side*.047,.066,-.33),(side*.10,.12,-.71),(side*.105,.13,-.89)],[.093,.132,.14,.025],ivory if kael else navy,coat)
  ribbon('coat lining',[(side*.01,-.025,-.06),(side*.05,.025,-.35),(side*.10,.075,-.73)],[.074,.10,.001],red,coat)
  tube('coat piping',[(side*.09,.0,0),(side*.17,.066,-.33),(side*.23,.12,-.7),(side*.13,.13,-.84)],.009,gold,coat)
  if label=='R':
   weapon=pivot('Weapon',(0,-.038,-.32),fore)
   if kael or kind in ['oracle','cantor']:
    pts=[(.24*math.sin(i*math.pi/16),0,.70*math.cos(i*math.pi/16)) for i in range(17)]
    tube('crescent bow',pts,.029,gold,weapon);tube('bow string',[(0,0,.70),(-.02,0,0),(0,0,-.70)],.004,glow,weapon)
    for z in [-.5,0,.5]:gem('bow crystal',(.15,0,z),.045,.11,glow,weapon)
   else:
    tube('sword grip',[(0,0,.10),(0,0,-.13)],.027,ink,weapon);tube('crossguard',[(-.16,0,-.14),(0,-.025,-.18),(.16,0,-.14)],.024,gold,weapon)
    ribbon('saber bevel',[(0,0,-.18),(0,0,-.36),(0,0,-1.24),(0,0,-1.42)],[.071,.065,.042,.001],metal,weapon)
    ribbon('star edge',[(.041,-.029,-.21),(.038,-.029,-.45),(.024,-.026,-1.25),(0,-.006,-1.42)],[.013,.014,.009,.001],glow,weapon)
  if label=='L' and kind in ['shield','warden']:
   sh=pivot('Shield',(0,-.08,-.17),fore)
   ribbon('kite shield',[(0,-.06,.39),(0,-.12,.14),(0,-.13,-.24),(0,-.07,-.51)],[.11,.30,.24,.002],ivory,sh)
   tube('shield spine',[(0,-.10,.37),(0,-.17,.12),(0,-.18,-.24),(0,-.10,-.49)],.018,gold,sh);gem('shield heart',(0,-.19,.10),.08,.14,glow,sh)
 if kind=='regent':
  for side in [-1,1]:
   wing=pivot('Wing_'+str(side),(side*.2,.11,.43),torso)
   for i in range(5):
    ribbon('royal wing',[(0,0,0),(side*(.55+i*.08),.12,.45-i*.16),(side*(.95+i*.11),.16,.38-i*.29)],[.045,.10,.001],ivory if i%2 else gold,wing)
 # Join surfaces per articulation, retaining material groups and hierarchy.
 for parent in [o for o in bpy.context.scene.objects if o.type=='EMPTY']:
  children=[o for o in parent.children if o.type=='MESH']
  if len(children)>1:
   bpy.ops.object.select_all(action='DESELECT')
   for o in children:o.select_set(True)
   bpy.context.view_layer.objects.active=children[0];bpy.ops.object.join()
 bpy.ops.export_scene.gltf(filepath=str(OUT/f'{kind}.glb'),export_format='GLB',export_yup=True,export_animations=False,export_cameras=False,export_lights=False,export_draco_mesh_compression_enable=False)
 print('MODEL_EXPORTED',kind,flush=True)
