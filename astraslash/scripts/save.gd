extends RefCounted
const D=preload("res://scripts/data.gd")
var path="user://astral_memory.json"
var profile={}
var recovered=false
func _init():
	profile={"schema":1,"shards":0,"selected":"rei","meta":{"vitality":0,"surge":0,"keepsake":0},"settings":D.SETTINGS.duplicate(),"wins":0,"best":0.0,"tutorial":false,"run":{}}
	var p=read_valid(path)
	if p.is_empty():
		p=read_valid(path+".bak");recovered=not p.is_empty()
	if not p.is_empty():
		for key in profile:
			if p.has(key) and typeof(p[key])==typeof(profile[key]):profile[key]=p[key]
		# JSON numbers are floats; explicitly normalize integer counters.
		for key in ["shards","wins"]:profile[key]=maxi(0,int(p.get(key,0)))
		for key in D.META:profile.meta[key]=clampi(int(profile.meta.get(key,0)),0,D.META[key][3])
		if not D.HEROES.has(profile.selected):profile.selected="rei"
		for key in D.SETTINGS:
			if not profile.settings.has(key):profile.settings[key]=D.SETTINGS[key]
		for key in ["music","sfx","shake"]:profile.settings[key]=clampf(float(profile.settings[key]),0,1)
		profile.settings.scale=clampf(float(profile.settings.scale),.5,1.2)
		profile.settings.effects=clampf(float(profile.settings.effects),.2,1.25)
		profile.settings.preset=clampi(int(profile.settings.preset),0,3)
		if int(profile.settings.fps) not in [30,60,90,120]:profile.settings.fps=60
		if not profile.run.is_empty():
			if not D.HEROES.has(profile.run.get("hero","")) or int(profile.run.get("node",-1)) not in range(9):profile.run={}
func read_valid(p):
	if not FileAccess.file_exists(p):return {}
	var raw=JSON.parse_string(FileAccess.get_file_as_string(p))
	if not raw is Dictionary or not raw.get("payload",null) is String:return {}
	if raw.payload.sha256_text()!=raw.get("checksum",""):return {}
	var data=JSON.parse_string(raw.payload)
	if not data is Dictionary or int(data.get("schema",0))!=1:return {}
	return data
func flush():
	var payload=JSON.stringify(profile);var file=FileAccess.open(path+".tmp",FileAccess.WRITE)
	if file==null:return false
	file.store_string(JSON.stringify({"checksum":payload.sha256_text(),"payload":payload}));file.flush();file.close()
	if not read_valid(path).is_empty():DirAccess.copy_absolute(path,path+".bak")
	return DirAccess.rename_absolute(path+".tmp",path)==OK
