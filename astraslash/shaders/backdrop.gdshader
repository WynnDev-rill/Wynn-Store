shader_type canvas_item;
uniform sampler2D atlas : source_color, filter_linear;
uniform int zone=0;
uniform float travel=0.;
void fragment(){
 vec2 uv=SCREEN_UV;
 float x=clamp(.17+uv.x*.66+travel,.001,.999);
 float y=(float(zone)+clamp(uv.y,.015,.975))/3.;
 vec3 c=texture(atlas,vec2(x,y)).rgb;
 // Keep the painted scenery behind the silhouettes and red attack telegraphs.
 float depth=mix(.76,.52,smoothstep(.25,.95,uv.y));
 COLOR=vec4(c*vec3(.75,.82,.94)*depth,1.);
}
