extends RefCounted
## Versioned, validated saves. Write/flush/rename with a previous-good fallback.
const Data = preload("res://scripts/data.gd")
var path = "user://aeralis.json"
var s:Dictionary
var settings = {"preset":1,"fps":60,"resolution":0.85,"shadows":true,"effects":true,"foliage":1.0,"music":0.65,"sfx":0.8,"sensitivity":1.0,"invert_y":false,"show_fps":false}
var last_error = ""

func _init():
 reset()
 var c=ConfigFile.new()
 if c.load("user://settings.cfg")==OK:
  for k in settings: settings[k]=c.get_value("settings",k,settings[k])

func reset():
 s={"version":1,"position":[-5.0,82.0],"hp":100.0,"xp":0,"inventory":{"wood":0,"crystal":0,"herb":0,"echo":0,"letter":0,"potion":3,"sword0":1},"weapon":"sword0","armor":"","charm":"","met_ilya":false,"forged":false,"boss":false,"ending":false,"beacons":[],"collected":[],"defeated":[],"discovered":[],"mira":false,"mira_done":false,"puzzle":false,"playtime":0.0}

func level() -> int: return mini(8,1+int(s.xp/100))
func max_hp() -> float: return 100.0+(level()-1)*12.0+(30.0 if s.charm=="charm" else 0.0)
func damage() -> float: return 14.0+3.0*(level()-1)+({"sword0":0,"sword1":8,"sword2":18}.get(s.weapon,0))
func amount(id:String) -> int: return int(s.inventory.get(id,0))
func add(id:String,n:int): s.inventory[id]=maxi(0,amount(id)+n)
func can_craft(recipe:Dictionary) -> bool:
 if recipe.id!="potion" and amount(recipe.id)>0: return false
 for id in recipe.cost:
  if amount(id)<recipe.cost[id]: return false
 return true

func craft(recipe:Dictionary) -> bool:
 if not can_craft(recipe): return false
 for id in recipe.cost: add(id,-recipe.cost[id])
 add(recipe.id,1)
 if recipe.id.begins_with("sword"):
  s.weapon=recipe.id
  s.forged=true
 if recipe.id=="mantle": s.armor="mantle"
 save()
 return true

func save() -> bool:
 var f=FileAccess.open(path+".tmp",FileAccess.WRITE)
 if f==null:
  last_error="Penyimpanan gagal. Ruang perangkat mungkin penuh."
  return false
 f.store_string(JSON.stringify(s)); f.flush(); f.close()
 if FileAccess.file_exists(path):
  DirAccess.copy_absolute(path,path+".bak")
 var err=DirAccess.rename_absolute(path+".tmp",path)
 last_error="" if err==OK else "Perjalanan belum tersimpan. Coba lagi."
 return err==OK

func load_save() -> bool:
 for p in [path,path+".bak"]:
  if not FileAccess.file_exists(p): continue
  var parsed=JSON.parse_string(FileAccess.get_file_as_string(p))
  if not parsed is Dictionary or parsed.get("version",0)!=1: continue
  if not parsed.get("inventory") is Dictionary or not parsed.get("position") is Array: continue
  if parsed.position.size()!=2: continue
  if not (parsed.position[0] is float or parsed.position[0] is int): continue
  if not (parsed.position[1] is float or parsed.position[1] is int): continue
  var okay=true
  for key in ["beacons","collected","defeated","discovered"]:
   if not parsed.get(key,[]) is Array: okay=false
  if not okay: continue
  reset()
  for k in s:
   if parsed.has(k) and typeof(parsed[k])==typeof(s[k]): s[k]=parsed[k]
   elif parsed.has(k) and (s[k] is int or s[k] is float) and (parsed[k] is int or parsed[k] is float): s[k]=parsed[k]
  for id in s.inventory.keys():
   if not Data.ITEMS.has(id) or not (s.inventory[id] is int or s.inventory[id] is float): s.inventory.erase(id)
   else: s.inventory[id]=clampi(int(s.inventory[id]),0,9999)
  if not s.weapon in ["sword0","sword1","sword2"]: s.weapon="sword0"
  s.hp=clampf(float(s.hp),1,max_hp())
  s.position[0]=clampf(float(s.position[0]),-115,115)
  s.position[1]=clampf(float(s.position[1]),-115,115)
  return true
 return false

func save_settings():
 var c=ConfigFile.new()
 for k in settings: c.set_value("settings",k,settings[k])
 c.save("user://settings.cfg")
