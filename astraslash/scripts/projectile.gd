extends Node3D
var g
var friendly=true
var velocity=Vector3.ZERO
var damage=0.0
var life=2.0
var radius=.25
var pierce=false
var homing=false
var hit_ids=[]
var color=Color.WHITE
var trail_timer=0.0
func setup(game,p,v,d,friend=true,penetrate=false,seek=false,r=.25):
	g=game;position=p;velocity=v;damage=d;friendly=friend;pierce=penetrate;homing=seek;radius=r;color=g.player.color if friend else Color("ff7159")
	var n=MeshInstance3D.new();var mesh=SphereMesh.new();mesh.radius=r;mesh.height=r*2;mesh.radial_segments=12;mesh.rings=6;n.mesh=mesh;n.material_override=g.fx.material(color);add_child(n)
	if friendly:n.scale=Vector3(2.5,.25,.25)
func tick(dt):
	life-=dt
	if life<=0 or position.y<-.4 or position.x< -3 or position.x>49:return false
	if homing:
		var target=g.nearest(position)
		if target:velocity=velocity.lerp((target.position+Vector3.UP*1.1-position).normalized()*velocity.length(),minf(1,dt*4))
	var old=position;position+=velocity*dt;trail_timer-=dt
	if trail_timer<=0:
		trail_timer=.06;g.fx.beam(old,position,color,.035,.13)
	if friendly:
		for e in g.enemies.duplicate():
			if not is_instance_valid(e) or e.dead or e.get_instance_id() in hit_ids:continue
			var c=e.position+Vector3.UP*e.height*.5;var closest=Geometry3D.get_closest_point_to_segment(c,old,position)
			if c.distance_to(closest)<radius+e.height*.33:
				hit_ids.append(e.get_instance_id());e.hurt(damage,signf(velocity.x),3.0 if position.y>2 else 0.0,damage>25);g.on_hit(e,damage,damage>25)
				if not pierce:return false
	else:
		var c=g.player.position+Vector3.UP;var close=Geometry3D.get_closest_point_to_segment(c,old,position)
		if c.distance_to(close)<radius+.38:
			g.player.hurt(damage,signf(velocity.x));return false
	return true
