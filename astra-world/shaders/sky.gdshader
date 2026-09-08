shader_type sky;
void sky(){
 float h=clamp(EYEDIR.y,0.0,1.0);
 vec3 c=mix(vec3(0.72,0.83,0.77),vec3(0.16,0.46,0.60),pow(h,0.5));
 vec3 sun=normalize(vec3(-0.45,0.46,-0.65));
 float d=dot(EYEDIR,sun);
 c+=vec3(1.0,0.67,0.27)*pow(max(d,0.0),95.0)*0.35;
 c=mix(c,vec3(1.0,0.9,0.67),smoothstep(0.9991,0.9996,d));
 float cloud=sin(EYEDIR.x*21.0+EYEDIR.z*9.0+TIME*0.008)*sin(EYEDIR.z*18.0-EYEDIR.x*6.0);
 c=mix(c,vec3(0.89,0.91,0.84),smoothstep(0.22,0.70,cloud)*smoothstep(0.10,0.30,h)*(1.0-smoothstep(0.5,0.75,h))*0.45);
 COLOR=c;
}
