shader_type sky;
uniform vec4 horizon : source_color = vec4(.1,.2,.3,1);
uniform vec4 zenith : source_color = vec4(.015,.027,.075,1);
void sky(){
 vec3 d=normalize(EYEDIR);float h=pow(clamp(d.y*1.2+.15,0.,1.),.55);
 vec3 c=mix(horizon.rgb,zenith.rgb,h);
 vec2 uv=vec2(atan(d.z,d.x),asin(d.y))*120.;vec2 cell=floor(uv);
 float hash=fract(sin(dot(cell,vec2(127.1,311.7)))*43758.5453);
 float star=smoothstep(.09,0.,length(fract(uv)-.5))*step(.981,hash)*smoothstep(.05,.5,d.y);
 COLOR=c+vec3(star)*.8;
}
