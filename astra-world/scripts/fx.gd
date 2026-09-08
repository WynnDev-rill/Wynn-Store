extends Node3D
const Art=preload("res://scripts/art.gd")
const World=preload("res://scripts/world.gd")
var game
var rng=RandomNumberGenerator.new()

func burst(pos:Vector3,color:String,n:int=10):
 if not game.state.settings.effects: return
 for i in n:
  var p=Art.part(self,"rock",pos,Vector3.ONE*0.08,color,Vector3.ZERO,0.3)
  var dir=Vector3(rng.randf_range(-1,1),rng.randf_range(0.3,1.7),rng.randf_range(-1,1))
  var tw=create_tween().set_parallel(); tw.tween_property(p,"position",pos+dir*1.5,0.65).set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_OUT); tw.tween_property(p,"scale",Vector3.ZERO,0.65); tw.chain().tween_callback(p.queue_free)

func wave(pos:Vector3,radius:float,color:String,duration:float):
 var p=Art.part(self,"ring",pos,Vector3(0.2,0.2,0.2),color,Vector3.ZERO,0.4)
 var tw=create_tween(); tw.tween_property(p,"scale",Vector3(radius,0.15,radius),duration).set_trans(Tween.TRANS_CUBIC).set_ease(Tween.EASE_OUT); tw.tween_callback(p.queue_free)

func slash(pos:Vector3,yaw:float,combo:int):
 var g=Art.group(self,pos); g.rotation.y=yaw; g.rotation.z=0.25 if combo!=0 else -0.5
 for i in range(9):
  var a=(i/8.0-0.5)*2.5
  Art.part(g,"rock",Vector3(sin(a)*1.7,0,-cos(a)*1.7),Vector3(0.28,0.03,0.1),"e9e6bb",Vector3(0,-rad_to_deg(a),0),0.5)
 var tw=create_tween(); tw.tween_property(g,"scale",Vector3.ONE*1.3,0.13); tw.tween_property(g,"scale",Vector3.ZERO,0.1); tw.tween_callback(g.queue_free)

func number(pos:Vector3,value:String,color:String):
 var label=Label3D.new(); label.text=value; label.font_size=46; label.pixel_size=0.012; label.modulate=Color(color); label.outline_size=7; label.billboard=BaseMaterial3D.BILLBOARD_ENABLED; label.no_depth_test=true; add_child(label); label.position=pos
 var tw=create_tween().set_parallel(); tw.tween_property(label,"position",pos+Vector3.UP*1.1,0.7); tw.tween_property(label,"modulate:a",0.0,0.7); tw.chain().tween_callback(label.queue_free)

func hazard(pos:Vector3,radius:float,delay:float):
 pos.y=World.height_at(pos.x,pos.z)+0.23
 var ring=Art.part(self,"ring",pos,Vector3(radius,0.15,radius),"eb8271",Vector3.ZERO,0.6)
 var tw=create_tween(); tw.tween_interval(delay)
 tw.tween_callback(func():
  if game.playing and not game.ui.blocking:
   wave(pos,radius,"f3b397",0.3)
   if game.player.global_position.distance_to(pos)<radius: game.player.hurt(22)
  ring.queue_free())
