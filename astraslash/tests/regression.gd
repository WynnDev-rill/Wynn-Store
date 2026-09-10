extends SceneTree
const Save=preload("res://scripts/save.gd")
const Inputs=preload("res://scripts/inputs.gd")
const D=preload("res://scripts/data.gd")
var failures=[]
var checks=0
func check(condition,message):
	checks+=1
	if not condition:failures.append(message);push_error(message)
func _initialize():
	var path="user://regression-memory.json"
	for suffix in ["",".bak",".tmp"]:DirAccess.remove_absolute(path+suffix)
	var save=Save.new(path)
	save.profile.shards=125;save.profile.selected="kael";save.profile.meta.vitality=3
	save.profile.settings.fps=30;save.profile.settings.music=.31
	check(save.flush(),"Initial save writes successfully")
	var restored=Save.new(path)
	check(restored.profile.shards==125 and restored.profile.selected=="kael","Currency and selected hero survive restart")
	check(restored.profile.meta.vitality==3 and restored.profile.settings.fps==30,"Upgrades and integer settings survive JSON roundtrip")
	save.profile.shards=170;save.flush()
	var f=FileAccess.open(path,FileAccess.WRITE);f.store_string("truncated write");f.close()
	var recovered=Save.new(path)
	check(recovered.recovered and recovered.profile.shards==125,"Damaged primary save recovers the last valid backup")
	save.profile.run={"hero":"rei","node":2};save.flush()
	check(Save.new(path).profile.run.is_empty(),"Incomplete run is rejected instead of crashing resume")
	save.profile.settings.music={};save.profile.settings.touch="bad";save.flush()
	restored=Save.new(path)
	check(restored.profile.settings.music==D.SETTINGS.music and restored.profile.settings.touch==true,"Malformed settings recover defaults")
	var run={"hero":"rei","node":2,"seed":42,"shards":20,"kills":4,"elapsed":32.5,"best_combo":8,"phase":"upgrade","player":{"x":4,"y":0,"hp":120,"energy":44,"potions":1,"skill_cd":2.5},"enemies":[],"relics":["echo"],"offers":["orbit","chain","meteor"]}
	save.profile.run=run;save.flush();restored=Save.new(path)
	check(restored.profile.run.get("offers",[])==run.offers,"Pending upgrade choice survives restart")
	run.offers=["missing_relic"];save.profile.run=run;save.flush()
	check(Save.new(path).profile.run.is_empty(),"Unknown upgrade cannot leave a run trapped")
	var input=Inputs.new();input.resize(Vector2(1600,720));input.enabled=true
	for index in [1,2]:
		var touch=InputEventScreenTouch.new();touch.index=index;touch.position=input.layout.attack[0];touch.pressed=true;input.event(touch)
	check(input.take("attack"),"Touch attack queues a normal combat action")
	var up=InputEventScreenTouch.new();up.index=1;up.pressed=false;input.event(up)
	check(input.held.get("attack",false),"Lifting one of two attack fingers keeps the other active")
	up.index=2;input.event(up)
	check(not input.held.get("attack",false),"Final finger release stops attack")
	input.press("dash");input.tick(.2)
	check(not input.take("dash"),"Expired input does not fire later")
	input.reset();check(input.held.is_empty() and input.buffer.is_empty(),"Leaving combat clears queued input")
	var owned=[]
	for room in range(8):
		var offers=D.choices(42,room,owned,room%2==1)
		check(offers.size()==3 and offers[0] not in owned and offers[1] not in owned and offers[2] not in owned,"Upgrade choices remain usable through room "+str(room))
		owned.append(offers[0])
	for suffix in ["",".bak",".tmp"]:DirAccess.remove_absolute(path+suffix)
	print("REGRESSION_RESULT ",JSON.stringify({"checks":checks,"failures":failures}))
	quit(0 if failures.is_empty() else 1)
