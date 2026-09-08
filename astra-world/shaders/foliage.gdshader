shader_type spatial;
render_mode cull_disabled;
uniform vec4 tint : source_color = vec4(0.36,0.61,0.34,1.0);
varying float tip;
void vertex(){
 vec3 w=(MODEL_MATRIX*vec4(VERTEX,1.0)).xyz;
 tip=clamp(VERTEX.y,0.0,1.0);
 VERTEX.x+=sin(w.x*0.4+w.z*0.3+TIME*1.8)*tip*tip*0.18;
 VERTEX.z+=cos(w.z*0.7+TIME*1.1)*tip*0.08;
}
void fragment(){ALBEDO=tint.rgb*mix(0.68,1.3,tip); ROUGHNESS=1.0;}
