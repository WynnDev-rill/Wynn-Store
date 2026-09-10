extends RefCounted
const D=preload("res://scripts/data.gd")
var path="user://astral_memory.json"
var profile={}
var recovered=false
func _init(storage_path="user://astral_memory.json"):
	path=storage_path
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
			elif typeof(D.SETTINGS[key])==TYPE_BOOL and not profile.settings[key] is bool:profile.settings[key]=D.SETTINGS[key]
			elif typeof(D.SETTINGS[key]) in [TYPE_INT,TYPE_FLOAT] and typeof(profile.settings[key]) not in [TYPE_INT,TYPE_FLOAT]:profile.settings[key]=D.SETTINGS[key]
		for key in ["music","sfx","shake"]:profile.settings[key]=clampf(float(profile.settings[key]),0,1)
		profile.settings.scale=clampf(float(profile.settings.scale),.5,1.2)
		profile.settings.effects=clampf(float(profile.settings.effects),.2,1.25)
		profile.settings.preset=clampi(int(profile.settings.preset),0,3)
		if int(profile.settings.fps) not in [30,60,90,120]:profile.settings.fps=60
		if not valid_run(profile.run):profile.run={}
func valid_run(run):
	if run.is_empty():return true
	if not D.HEROES.has(run.get("hero","")):return false
	for key in ["node","seed","shards","kills","elapsed","best_combo"]:
		if typeof(run.get(key)) not in [TYPE_INT,TYPE_FLOAT]:return false
	if int(run.node) not in range(9):return false
	if run.get("phase","") not in ["run","upgrade","route"]:return false
	if not run.get("player") is Dictionary or not run.get("enemies") is Array or not run.get("relics") is Array:return false
	if run.enemies.size()>80:return false
	for key in ["x","y","hp","energy","potions","skill_cd"]:
		if typeof(run.player.get(key)) not in [TYPE_INT,TYPE_FLOAT]:return false
	for enemy in run.enemies:
		if not enemy is Dictionary:return false
		if enemy.get("kind","") not in ["hollow","shield","oracle","warden","cantor","regent"]:return false
		if not enemy.get("elite") is bool:return false
		for key in ["x","y","hp","phase","attack_index"]:
			if typeof(enemy.get(key)) not in [TYPE_INT,TYPE_FLOAT]:return false
	if not run.get("offers",[]) is Array:return false
	if run.phase=="upgrade":
		if run.get("offers",[]).is_empty():return false
		for id in run.offers:
			if not D.RELICS.has(id):return false
	return true
func read_valid(p):
	if not FileAccess.file_exists(p):return {}
	var parser=JSON.new()
	if parser.parse(FileAccess.get_file_as_string(p))!=OK:return {}
	var raw=parser.data
	if not raw is Dictionary or not raw.get("payload",null) is String:return {}
	if raw.payload.sha256_text()!=raw.get("checksum",""):return {}
	if parser.parse(raw.payload)!=OK:return {}
	var data=parser.data
	if not data is Dictionary or int(data.get("schema",0))!=1:return {}
	return data
func flush():
	var payload=JSON.stringify(profile);var file=FileAccess.open(path+".tmp",FileAccess.WRITE)
	if file==null:return false
	file.store_string(JSON.stringify({"checksum":payload.sha256_text(),"payload":payload}));file.flush();file.close()
	if not read_valid(path).is_empty():DirAccess.copy_absolute(path,path+".bak")
	return DirAccess.rename_absolute(path+".tmp",path)==OK
