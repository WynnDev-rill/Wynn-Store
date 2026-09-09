# Editor-only input bridge. Commands use the same input events as a player.
extends Node
var g
var directory=""
var last_id=-1
var timer=0.0
func setup(game):
	g=game
	for arg in OS.get_cmdline_user_args():
		if arg.begins_with("--qa-dir="):directory=arg.trim_prefix("--qa-dir=")
func _process(dt):
	if directory=="":return
	timer+=dt
	if timer<.06:return
	timer=0
	var command=JSON.parse_string(FileAccess.get_file_as_string(directory+"/command.json")) if FileAccess.file_exists(directory+"/command.json") else null
	if command is Dictionary and int(command.get("id",-1))!=last_id:
		last_id=int(command.id)
		for event in command.get("events",[]):
			var e=InputEventKey.new();e.physical_keycode=int(event.key);e.pressed=bool(event.pressed);Input.parse_input_event(e)
		if command.has("capture"):g.capture(command.capture)
	var rows=[]
	for e in g.enemies:
		if is_instance_valid(e) and not e.dead:rows.append({"x":e.position.x,"y":e.position.y,"hp":e.hp,"max_hp":e.max_hp,"kind":e.kind,"state":e.state,"timer":e.timer,"duration":e.duration,"facing":e.facing})
	var data={"ack":last_id,"mode":g.mode,"node":g.node,"wave":g.wave,"enemies":rows,"relics":g.relics,"offers":g.offers,"elapsed":g.elapsed,"fps":Engine.get_frames_per_second(),"draw_calls":Performance.get_monitor(Performance.RENDER_TOTAL_DRAW_CALLS_IN_FRAME),"objects":Performance.get_monitor(Performance.RENDER_TOTAL_OBJECTS_IN_FRAME),"message":g.message}
	if is_instance_valid(g.player):data.player={"x":g.player.position.x,"y":g.player.position.y,"hp":g.player.hp,"max_hp":g.player.max_hp,"energy":g.player.energy,"potions":g.player.potions,"skill_cd":g.player.skill_cd,"dash_cd":g.player.dash_cd,"state":g.player.state,"ground":g.player.ground}
	var f=FileAccess.open(directory+"/state.tmp",FileAccess.WRITE);f.store_string(JSON.stringify(data));f.close();DirAccess.rename_absolute(directory+"/state.tmp",directory+"/state.json")
