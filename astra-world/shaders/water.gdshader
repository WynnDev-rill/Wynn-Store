shader_type spatial;
render_mode blend_mix, depth_draw_opaque, cull_disabled;
uniform vec4 deep : source_color = vec4(0.035,0.34,0.43,1.0);
uniform vec4 foam : source_color = vec4(0.40,0.77,0.77,1.0);
varying vec3 wp;
void vertex(){
 wp=(MODEL_MATRIX*vec4(VERTEX,1.0)).xyz;
 VERTEX.y+=sin(wp.x*0.22+TIME*0.7)*0.11+sin(wp.z*0.31-TIME*0.5)*0.08;
}
void fragment(){
 float w=sin(wp.x*0.65+wp.z*0.3+TIME*0.8)*sin(wp.z*0.6-wp.x*0.16-TIME*0.4);
 float line=smoothstep(0.73,0.91,w);
 ALBEDO=mix(deep.rgb,foam.rgb,line*0.65+0.08);
 ROUGHNESS=0.23; METALLIC=0.2;
 NORMAL=normalize(vec3(sin(wp.x*0.2+TIME)*0.09,0.0,1.0));
}
