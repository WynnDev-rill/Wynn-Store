extends Node3D
func _ready():
 var m = MeshInstance3D.new()
 m.mesh = TorusMesh.new()
 add_child(m)
 var c = Camera3D.new()
 c.position = Vector3(0, 2, 6)
 add_child(c)
 c.look_at(Vector3.ZERO)
 var l = DirectionalLight3D.new()
 l.rotation_degrees = Vector3(-45, -30, 0)
 add_child(l)
 print("ASTRASLASH_FEASIBILITY_OK")
