extends Node
var game
var music:AudioStreamPlayer
var ambience:AudioStreamPlayer
var voices:Array=[]
var mode=""
var tracks={}
var sounds={}

func _exit_tree():
 shutdown()

func shutdown():
 # Release Ogg playback before the audio server and resource cache shut down.
 # Stopping alone leaves the stream assigned to the player.
 for p in [music,ambience]+voices:
  if is_instance_valid(p):
   p.stop()
   p.stream=null
 voices.clear()
 tracks.clear()
 sounds.clear()

func setup(g):
 game=g
 for id in ["aeralis","combat","home"]: tracks[id]=load("res://assets/audio/"+id+".ogg")
 for id in ["step","swing","hit","skill","dash","hurt","heal","warning","slam","gather","ui","beacon"]: sounds[id]=load("res://assets/audio/"+id+".wav")
 music=AudioStreamPlayer.new(); add_child(music)
 ambience=AudioStreamPlayer.new(); ambience.stream=load("res://assets/audio/ambience.ogg"); add_child(ambience); ambience.play()
 for i in range(10):
  var p=AudioStreamPlayer.new(); add_child(p); voices.append(p)
 set_mode("home"); apply()

func set_mode(id:String):
 if mode==id: return
 mode=id; music.stream=tracks[id]; music.play(); apply()

func apply():
 if music: music.volume_db=linear_to_db(maxf(0.0001,game.state.settings.music)) - 5
 if ambience: ambience.volume_db=linear_to_db(maxf(0.0001,game.state.settings.sfx))-12

func sfx(id:String,db:float=0.0):
 if not sounds.has(id): return
 for v in voices:
  if not v.playing:
   v.stream=sounds[id]; v.volume_db=db+linear_to_db(maxf(0.0001,game.state.settings.sfx)); v.pitch_scale=randf_range(0.95,1.05) if id in ["step","hit","swing"] else 1.0; v.play(); break
