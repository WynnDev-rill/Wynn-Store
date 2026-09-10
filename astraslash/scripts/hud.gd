extends Control
const D=preload("res://scripts/data.gd")
var g
var body=preload("res://assets/fonts/Barlow-Regular.ttf")
var bold=preload("res://assets/fonts/BarlowCondensed-Bold.ttf")
var paper=Color("f5f0df")
var muted=Color("aabbd0")
func label(value,p,fs=20,color=Color("f5f0df"),heading=false):draw_string(bold if heading else body,p,value,HORIZONTAL_ALIGNMENT_LEFT,-1,fs,color)
func bar(p,s,ratio,color):
	draw_style_box(style(Color(.025,.047,.073,.83),Color("365266")),Rect2(p,s));draw_rect(Rect2(p+Vector2(2,2),Vector2(maxf(0,(s.x-4)*clampf(ratio,0,1)),s.y-4)),color)
func style(color,border):
	var st=StyleBoxFlat.new();st.bg_color=color;st.border_color=border;st.set_border_width_all(1);st.set_corner_radius_all(4);return st
func _draw():
	if not g or not is_instance_valid(g.player):return
	var p=g.player;var s=g.inputs.viewport;var z=D.ZONES[g.zone()]
	draw_style_box(style(Color(.025,.047,.08,.83),Color("314b61")),Rect2(22,18,396,94));draw_rect(Rect2(22,18,4,94),p.color)
	label(D.HEROES[p.hero].name,Vector2(40,49),26,paper,true);label(str(int(p.hp))+" / "+str(int(p.max_hp)),Vector2(188,48),17,muted,true)
	bar(Vector2(40,60),Vector2(307,14),p.hp/p.max_hp,Color("93e3c9"));bar(Vector2(40,85),Vector2(307,7),p.energy/100.0,p.color)
	label(z.name,Vector2(s.x*.40,43),23,paper,true);label("SIMPUL "+str(g.node+1)+" / 9     ✦ "+str(g.shards),Vector2(s.x*.40,68),15,muted,true)
	var boss=null
	for e in g.enemies:
		if not is_instance_valid(e) or e.dead:continue
		if e.boss:boss=e;continue
		var point=g.camera.unproject_position(e.position+Vector3.UP*(e.height+.4))
		if point.x>20 and point.x<s.x-20:
			bar(point-Vector2(29,0),Vector2(58,5),e.hp/e.max_hp,Color("ed9c73") if e.elite else Color("cc887d"))
			if e.elite:label("ELITE",point+Vector2(-17,-7),13,Color("f3c083"),true)
	if boss:
		var w=s.x*.44;label(z.boss_name,Vector2(s.x*.5-w*.5,102),19,paper,true);bar(Vector2(s.x*.5-w*.5,116),Vector2(w,10),boss.hp/boss.max_hp,Color("e77f68"));label("II" if boss.phase==2 else "I",Vector2(s.x*.5+w*.5+10,125),22,Color("f5cf95"),true)
	if g.combo>1:
		var rank="SS" if g.combo>=45 else ("S" if g.combo>=28 else ("A" if g.combo>=18 else ("B" if g.combo>=10 else ("C" if g.combo>=5 else "D"))))
		label(rank,Vector2(40,209),64,p.color,true);label(str(g.combo)+" HIT",Vector2(42,239),22,paper,true);draw_line(Vector2(42,251),Vector2(42+95*g.combo_timer/3.4,251),p.color,2)
	if g.message_timer>0:
		var width=bold.get_string_size(g.message,HORIZONTAL_ALIGNMENT_LEFT,-1,29).x;draw_style_box(style(Color(.025,.045,.075,.78),Color(.4,.6,.68,.4)),Rect2(s.x/2-width/2-20,158,width+40,47));label(g.message,Vector2(s.x/2-width/2,192),29,paper,true)
	if g.node==0 and g.elapsed<24:
		var hint="GESER UNTUK BERGERAK   ·   TAHAN SERANG UNTUK KOMBO   ·   ATAS + BERAT UNTUK LAUNCHER" if OS.has_feature("android") else "J  SERANG    K  BERAT    SPASI  LOMPAT    SHIFT  DODGE    L  SKILL"
		label(hint,Vector2(s.x*.23,s.y-26),16,muted,true)
	if not g.settings.touch:return
	var origin=g.inputs.joy_origin if g.inputs.joy_id>=0 else Vector2(128,s.y-125)
	draw_circle(origin,64,Color(.025,.055,.085,.38));draw_arc(origin,64,0,TAU,64,Color(.55,.78,.85,.32),1.4,true);draw_arc(origin,50,-.4,.4,10,Color(.62,.89,.94,.6),2,true);draw_arc(origin,50,PI-.4,PI+.4,10,Color(.62,.89,.94,.6),2,true);draw_circle(origin+g.inputs.axis*47,22,Color(.58,.84,.88,.28));draw_arc(origin+g.inputs.axis*47,22,0,TAU,32,Color(.65,.88,.9,.48),1,true)
	for id in g.inputs.layout:
		var point=g.inputs.layout[id][0];var r=float(g.inputs.layout[id][1]);var color=p.color if id in ["attack","ult"] else Color("c5d9e2");var held=g.inputs.held.get(id,false)
		draw_circle(point,r,Color(.02,.06,.10,.72 if held else .47));draw_arc(point,r,-PI*.8,PI*.8,48,Color(color,.88 if held else .53),2.0,true);draw_arc(point,r-5,PI*.65,PI*1.25,15,Color(color,.3),1.2,true)
		glyph(id,point,r*.41,color)
		var cd=p.skill_cd if id=="skill" else (p.dash_cd if id=="dash" else (p.heal_cd if id=="heal" else 0.0))
		if cd>.05:
			draw_circle(point,r-2,Color(.015,.026,.045,.76));label(str(int(ceil(cd))),point+Vector2(-8,10),28,paper,true)
		if id=="ult":
			draw_arc(point,r+4,-PI/2,-PI/2+TAU*maxf(.005,p.energy/100),52,p.color,3,true)
			if p.energy<100:label(str(int(p.energy)),point+Vector2(-11,7),18,paper,true)
		if id=="heal":label(str(p.potions),point+Vector2(11,19),16,paper,true)
		var names={"attack":"SERANG","heavy":"BERAT","jump":"LOMPAT","dash":"DODGE","skill":"SKILL","ult":"ULTIMATE","heal":""};var value=names[id];var width=bold.get_string_size(value,HORIZONTAL_ALIGNMENT_LEFT,-1,12).x;label(value,point+Vector2(-width/2,r+17),12,color,true)
func glyph(id,p,r,c):
	if id in ["attack","heavy"]:
		var pts=PackedVector2Array([p+Vector2(-r*.7,r*.7),p+Vector2(r*.65,-r),p+Vector2(r,-r*.65),p+Vector2(-r*.45,r*.85)]);draw_colored_polygon(pts,c);draw_line(p+Vector2(-r*.65,r*.15),p+Vector2(-r*.1,r*.7),c,2.5,true)
		if id=="heavy":draw_line(p+Vector2(-r*.9,-r*.6),p+Vector2(-r*.1,-r*.8),c,2,true)
	elif id=="jump":
		for y in [-r*.35,r*.45]:draw_polyline(PackedVector2Array([p+Vector2(-r*.65,y),p+Vector2(0,y-r*.6),p+Vector2(r*.65,y)]),c,2.8,true)
	elif id=="dash":
		for x in [-r*.4,r*.4]:draw_polyline(PackedVector2Array([p+Vector2(x-r*.3,-r*.6),p+Vector2(x+r*.4,0),p+Vector2(x-r*.3,r*.6)]),c,2.8,true)
	elif id in ["skill","ult"]:
		var pts=PackedVector2Array()
		for i in range(8):var a=i*PI/4;pts.append(p+Vector2(sin(a),cos(a))*r*(1.0 if i%2==0 else .25))
		draw_colored_polygon(pts,c)
		if id=="ult":draw_arc(p,r*1.18,0,TAU,32,c,1.2,true)
	elif id=="heal":draw_line(p-Vector2(r,0),p+Vector2(r,0),Color("9ae2c9"),3,true);draw_line(p-Vector2(0,r),p+Vector2(0,r),Color("9ae2c9"),3,true)
