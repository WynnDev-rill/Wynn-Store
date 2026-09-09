shader_type canvas_item;
uniform sampler2D screen_texture : hint_screen_texture, filter_linear;
uniform float pulse=0.;
uniform bool enabled=true;
void fragment(){
 vec2 uv=SCREEN_UV;float off=pulse*.002;
 vec3 c=texture(screen_texture,uv).rgb;
 if(enabled){c.r=texture(screen_texture,uv+vec2(off,0)).r;c.b=texture(screen_texture,uv-vec2(off,0)).b;
 float v=1.-.34*pow(length((uv-.5)*vec2(1.1,1.)),1.4);c*=v;c=mix(c,c*vec3(.93,1.01,1.04),.20);}
 COLOR=vec4(c+vec3(.04,.11,.13)*pulse,1.);
}
