extends RefCounted
var axis=Vector2.ZERO
var joy_origin=Vector2(128,595)
var joy_id=-1
var fingers={}
var held={}
var buffer={}
var layout={}
var viewport=Vector2(1280,720)
var enabled=false
const KEYS={KEY_J:"attack",KEY_K:"heavy",KEY_SPACE:"jump",KEY_SHIFT:"dash",KEY_L:"skill",KEY_U:"ult",KEY_H:"heal",KEY_ESCAPE:"pause"}
func resize(size):
	viewport=size;joy_origin=Vector2(128,size.y-125)
	layout={"attack":[Vector2(size.x-143,size.y-123),49],"jump":[Vector2(size.x-108,size.y-244),36],"dash":[Vector2(size.x-276,size.y-92),36],"heavy":[Vector2(size.x-261,size.y-209),36],"skill":[Vector2(size.x-386,size.y-132),34],"ult":[Vector2(size.x-375,size.y-247),34],"heal":[Vector2(394,59),23]}
func reset():
	axis=Vector2.ZERO;joy_id=-1;fingers.clear();held.clear();buffer.clear()
func press(action):buffer[action]=.18;held[action]=true
func take(action):
	if buffer.has(action):buffer.erase(action);return true
	return false
func tick(dt):
	for k in buffer.keys():
		buffer[k]-=dt
		if buffer[k]<=0:buffer.erase(k)
func movement():
	var k=Vector2(float(Input.is_physical_key_pressed(KEY_D) or Input.is_physical_key_pressed(KEY_RIGHT))-float(Input.is_physical_key_pressed(KEY_A) or Input.is_physical_key_pressed(KEY_LEFT)),float(Input.is_physical_key_pressed(KEY_S) or Input.is_physical_key_pressed(KEY_DOWN))-float(Input.is_physical_key_pressed(KEY_W) or Input.is_physical_key_pressed(KEY_UP)))
	if Input.get_connected_joypads().size()>0:
		var j=Vector2(Input.get_joy_axis(0,JOY_AXIS_LEFT_X),Input.get_joy_axis(0,JOY_AXIS_LEFT_Y))
		if j.length()>.2:k=j
	return axis if joy_id>=0 else k
func event(e):
	if e is InputEventKey and not e.echo and KEYS.has(e.physical_keycode):
		if e.pressed:press(KEYS[e.physical_keycode])
		else:held.erase(KEYS[e.physical_keycode])
	if e is InputEventJoypadButton:
		var actions={JOY_BUTTON_A:"jump",JOY_BUTTON_X:"attack",JOY_BUTTON_Y:"heavy",JOY_BUTTON_B:"dash",JOY_BUTTON_RIGHT_SHOULDER:"skill",JOY_BUTTON_LEFT_SHOULDER:"ult",JOY_BUTTON_DPAD_UP:"heal",JOY_BUTTON_START:"pause"}
		if actions.has(e.button_index):
			if e.pressed:press(actions[e.button_index])
			else:held.erase(actions[e.button_index])
	if not enabled:return
	if e is InputEventScreenTouch:
		if e.pressed:
			for k in layout:
				if e.position.distance_to(layout[k][0])<layout[k][1]+7:
					fingers[e.index]=k;press(k);return
			if joy_id<0 and e.position.x<viewport.x*.35 and e.position.y>viewport.y*.42:
				joy_id=e.index;joy_origin=e.position;axis=Vector2.ZERO
		else:
			if e.index==joy_id:joy_id=-1;axis=Vector2.ZERO
			if fingers.has(e.index):held.erase(fingers[e.index]);fingers.erase(e.index)
	if e is InputEventScreenDrag and e.index==joy_id:axis=((e.position-joy_origin)/64).limit_length()
