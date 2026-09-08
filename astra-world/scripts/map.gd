extends Control
const World=preload("res://scripts/world.gd")
const Data=preload("res://scripts/data.gd")
var game
var font:Font

func _ready():
 custom_minimum_size=Vector2(660,445); size_flags_horizontal=Control.SIZE_EXPAND_FILL; font=load("res://assets/branding/body.ttf")

func project(v:Vector2) -> Vector2: return size/2+v*minf(size.x,size.y)/278

func _draw():
 if game==null: return
 draw_rect(Rect2(Vector2.ZERO,size),Color("193f49"))
 var cell=minf(size.x,size.y)/278*5
 for z in range(-140,140,5):
  for x in range(-140,140,5):
   var h=World.height_at(x,z)
   if h<0: continue
   var col=Color("557d70").lerp(Color("bdc09b"),clampf(h/25,0,1))
   if Vector2(x,z).length()>113: col=Color("acbb9c")
   draw_rect(Rect2(project(Vector2(x,z)),Vector2.ONE*(cell+0.6)),col)
 for edge in [[Vector2(0,48),Vector2(-63,6)],[Vector2(0,48),Vector2(61,0)],[Vector2(-63,6),Vector2(-23,-58)],[Vector2(61,0),Vector2(-23,-58)],[Vector2(-23,-58),Vector2(27,-92)]]:
  draw_line(project(edge[0]),project(edge[1]),Color("d1c598"),1.7,true)
 for l in Data.LANDMARKS:
  var pos=project(l.pos); var found=l.id in game.state.s.discovered
  draw_circle(pos,6,Color("f5d9a0") if found else Color("7e9e8c"),true,-1,true)
  draw_arc(pos,9,0,TAU,24,Color("254b4c"),1.5,true)
  if found: draw_string(font,pos+Vector2(14,4),l.name,HORIZONTAL_ALIGNMENT_LEFT,-1,12,Color("f7f0d6"))
 var p=project(Vector2(game.player.position.x,game.player.position.z))
 draw_circle(p,6,Color("f4f4e7"),true,-1,true); draw_arc(p,10,0,TAU,24,Color("91e9d5"),2.0,true)
 var q=project(game.quest().pos)
 draw_arc(q,15,0,TAU,4,Color("f0c775"),2,true)
 draw_string(font,Vector2(18,26),"A E R A L I S",HORIZONTAL_ALIGNMENT_LEFT,-1,19,Color("ebd5a4"))
 draw_string(font,Vector2(size.x-40,26),"U ↑",HORIZONTAL_ALIGNMENT_LEFT,-1,14,Color("ebd5a4"))
 draw_string(font,Vector2(18,size.y-16),"◇ Tujuan utama     ● Nara     ○ Landmark",HORIZONTAL_ALIGNMENT_LEFT,-1,12,Color("dbe4cf"))

func _gui_input(e):
 if e is InputEventMouseButton and e.pressed:
  for l in Data.LANDMARKS:
   if project(l.pos).distance_to(e.position)<20 and l.id in game.state.s.discovered:
    game.ui.toast(l.name+" · "+l.short)
