extends Node
var g
var tracks={}
var effects={}
var voices=[]
var music=[]
var current=""
var active=0
var fade=1.0
func setup(game):
	g=game
	for name in ["arunika","boss","eclipse_core","glass_garden","orbit_home"]:
		var stream=load("res://assets/audio/"+name+".ogg");stream.loop=true;tracks[name]=stream
	for name in ["break","dash","heavy","hit","hurt","jump","loot","parry","shot","slash1","slash2","slash3","telegraph","ui","ultimate","victory"]:effects[name]=load("res://assets/audio/"+name+".wav")
	for i in range(18):
		var p=AudioStreamPlayer.new();add_child(p);voices.append(p)
	for i in range(2):
		var p=AudioStreamPlayer.new();add_child(p);music.append(p)
func track(name):
	if current==name:return
	current=name;active=1-active;music[active].stream=tracks[name];music[active].play();fade=0
func play(name,pitch=1.0,volume=1.0):
	if not effects.has(name):return
	var p=voices[0]
	for v in voices:
		if not v.playing:p=v;break
	p.stream=effects[name];p.pitch_scale=pitch*randf_range(.96,1.04);p.volume_db=linear_to_db(maxf(.0001,g.settings.sfx*volume*.65));p.play()
func tick(dt):
	fade=minf(1,fade+dt*.8)
	music[active].volume_db=linear_to_db(maxf(.0001,g.settings.music*fade*.58))
	music[1-active].volume_db=linear_to_db(maxf(.0001,g.settings.music*(1-fade)*.58))
	if fade>=1:music[1-active].stop()
