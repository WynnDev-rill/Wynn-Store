# State-assisted automation. Only ordinary key and GUI pointer events are issued.
extends Node
var g
var hero="rei"
var clock=0.0
var decision=0.0
var entered=""
var wait_menu=0.0
var heavy_time=0.0
var jump_time=0.0
var dash_time=0.0
var held={}
var log=[]
var last_node=-1
var path=""
var clicks=0
func setup(game,id,out):
	g=game;hero=id;path=out
	if "--fast-qa" in OS.get_cmdline_user_args() and DisplayServer.get_name()=="headless":Engine.max_fps=0
func key(code,pressed):
	if held.get(code,false)==pressed:return
	held[code]=pressed;var e=InputEventKey.new();e.physical_keycode=code;e.pressed=pressed;Input.parse_input_event(e)
func tap(code):key(code,false);key(code,true);key(code,false)
func release():
	for k in held.keys():key(k,false)
func click(p):
	var m=InputEventMouseMotion.new();m.position=p;m.global_position=p;g.get_viewport().push_input(m,true)
	for down in [true,false]:
		var e=InputEventMouseButton.new();e.button_index=MOUSE_BUTTON_LEFT;e.position=p;e.global_position=p;e.pressed=down;g.get_viewport().push_input(e,true)
	clicks+=1
	if clicks<4:print("QA_CLICK ",p," viewport ",g.inputs.viewport," hover ",g.get_viewport().gui_get_hovered_control())
func _process(dt):
	clock+=dt;decision+=dt;wait_menu-=dt
	if decision<.08:return
	decision=0
	if entered!=g.mode:
		entered=g.mode;wait_menu=.4;release();log.append({"at":clock,"mode":entered,"node":g.node,"hp":g.player.hp if is_instance_valid(g.player) else 0})
	if last_node!=g.node:last_node=g.node;print("QA_NODE ",hero," ",g.node," at ",int(clock))
	if wait_menu>0:return
	var s=g.inputs.viewport
	match g.mode:
		"title":click(Vector2(230,s.y*.75));wait_menu=.6
		"hub":
			if g.save.profile.selected!=hero:click(Vector2(s.x*.57+s.x*.39*(.25 if hero=="rei" else .75),112))
			else:click(Vector2(s.x*.76,s.y-95))
			wait_menu=.6
		"dialog":click(Vector2(s.x-109,31));wait_menu=.6
		"upgrade":
			var pick=0
			for id in ["revive","orbit","chain","volatile","vampire","meteor","triple","perfect","dash_burst","pierce","tempo","air_blade","thunderfall","overdrive","air_step","gravity","echo","frost"]:
				if id in g.offers:pick=g.offers.find(id);break
			click(Vector2(52+(s.x-144)/6+pick*((s.x-144)/3+20),s.y-115));wait_menu=.6
		"route":click(Vector2(s.x*.75,541));wait_menu=.6
		"run":fight()
		"defeat","victory":
			release();var result={"hero":hero,"result":g.mode,"elapsed":g.elapsed,"simulation_clock":clock,"node":g.node,"kills":g.kills,"best_combo":g.best_combo,"shards":g.shards,"relics":g.relics,"menu_clicks":clicks,"transitions":log};var f=FileAccess.open(path+"/playthrough-"+hero+".json",FileAccess.WRITE);f.store_string(JSON.stringify(result,"  "));f.close();print("QA_RESULT ",JSON.stringify(result));g.get_tree().quit(0 if g.mode=="victory" else 2)
	if clock>1200:print("QA_TIMEOUT ",g.mode," ",g.node);g.get_tree().quit(3)
func fight():
	var p=g.player;var e=g.nearest(p.position)
	if not e:release();return
	var dx=e.position.x-p.position.x;var desired=0
	if hero=="rei":
		if absf(dx)>2.0:desired=1 if dx>0 else -1
	else:
		if absf(dx)>8.5:desired=1 if dx>0 else -1
		elif absf(dx)<4.8:desired=-1 if dx>0 else 1
		if (p.position.x<3 and desired<0) or (p.position.x>40 and desired>0):desired=-desired;tap(KEY_SHIFT);tap(KEY_SPACE)
	key(KEY_A,desired<0);key(KEY_D,desired>0);key(KEY_J,true)
	if p.hp<p.max_hp*.52 and p.potions>0 and p.heal_cd<=0:tap(KEY_H)
	if p.energy>=100:tap(KEY_U)
	if p.skill_cd<=0 and absf(dx)<(6 if hero=="rei" else 13):tap(KEY_L)
	if clock-heavy_time>.65:heavy_time=clock;key(KEY_W,int(clock)%7<2 and p.ground and hero=="rei");tap(KEY_K)
	else:key(KEY_W,false)
	if e.state=="windup" and e.timer>e.duration-.22 and absf(dx)<7 and clock-dash_time>.7:dash_time=clock;tap(KEY_SHIFT)
	if clock-jump_time>(1.9 if e.boss else 3.1):jump_time=clock;tap(KEY_SPACE)
