extends SceneTree
func _initialize():
	DirAccess.make_dir_recursive_absolute("res://assets/licenses")
	var file=FileAccess.open("res://assets/licenses/Godot.txt",FileAccess.WRITE)
	file.store_string(Engine.get_license_text());file.close()
	file=FileAccess.open("res://assets/licenses/Godot-third-party.txt",FileAccess.WRITE)
	file.store_string(JSON.stringify(Engine.get_copyright_info(),"  ")+"\n\n")
	var info=Engine.get_license_info()
	for key in info:file.store_string(str(key)+"\n\n"+str(info[key])+"\n\n")
	file.close();quit()
