import * as THREE from 'three';
import outlines from '../data/terrain-outlines.json';

/** Geographical sculpture, not terrain elevation or a hotspot visualization. */
export function initLandscape(container:HTMLElement){
  const compact=matchMedia('(max-width:760px)');
  const mobile=compact.matches;
  const reduced=matchMedia('(prefers-reduced-motion:reduce)');
  const renderer=new THREE.WebGLRenderer({alpha:true,antialias:true,powerPreference:'low-power'});
  renderer.setPixelRatio(Math.min(devicePixelRatio||1,mobile?1.2:1.5));
  renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.toneMappingExposure=1.35;
  renderer.setClearColor(0x07100e,0);container.append(renderer.domElement);
  const scene=new THREE.Scene(),camera=new THREE.PerspectiveCamera(38,1,.1,90);
  scene.fog=new THREE.FogExp2(0x07100e,.018);
  const landscape=new THREE.Group();landscape.rotation.y=-.24;landscape.position.set(1.3,0,0);scene.add(landscape);
  const resources:{dispose:()=>void}[]=[];
  const own=<T extends {dispose:()=>void}>(v:T)=>{resources.push(v);return v;};
  const top=own(new THREE.MeshStandardMaterial({color:0x4b6049,roughness:.92,metalness:.04}));
  new THREE.TextureLoader().load('/textures/forest-floor-color.jpg',texture=>{if(disposed){texture.dispose();return;}own(texture);texture.colorSpace=THREE.SRGBColorSpace;texture.wrapS=texture.wrapT=THREE.RepeatWrapping;texture.repeat.set(.8,.8);top.map=texture;top.needsUpdate=true;invalidate();},undefined,()=>{/* Base materials remain complete when an optional map fails. */});
  const sides=own(new THREE.MeshStandardMaterial({color:0x31251b,roughness:.78,metalness:.15}));
  const edgeMat=own(new THREE.LineBasicMaterial({color:0xc99e63,transparent:true,opacity:.55}));
  const shapes:THREE.Shape[]=[];
  for(const province of outlines)for(const ring of province.rings){
    const shape=new THREE.Shape(ring.map(([x,y])=>new THREE.Vector2(x!,y!)));shapes.push(shape);
    const geometry=own(new THREE.ExtrudeGeometry(shape,{depth:.48,bevelEnabled:false,steps:1,curveSegments:1}));
    geometry.rotateX(-Math.PI/2);
    const mesh=new THREE.Mesh(geometry,[top,sides]);mesh.position.y=-.48;landscape.add(mesh);
    const border=ring.map(([x,y])=>new THREE.Vector3(x!,0.012,-y!));border.push(border[0]!.clone());
    const line=own(new THREE.BufferGeometry().setFromPoints(border));landscape.add(new THREE.Line(line,edgeMat));
    for(let i=0;i<3;i++){
      const strata=new THREE.Line(line,own(new THREE.LineBasicMaterial({color:0xab7543,transparent:true,opacity:.18-i*.04})));
      strata.position.y=-.15-i*.13;landscape.add(strata);
    }
  }
  // Deterministic grove sampling stays inside the supplied geographical outlines.
  let seed=724;const random=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296;};
  const contains=(x:number,y:number)=>outlines.some(p=>p.rings.some(ring=>{let inside=false;for(let i=0,j=ring.length-1;i<ring.length;j=i++){const a=ring[i]!,b=ring[j]!;if((a[1]!>y)!==(b[1]!>y)&&x<(b[0]!-a[0]!)*(y-a[1]!)/(b[1]!-a[1]!)+a[0]!)inside=!inside;}return inside;}));
  const treeCount=mobile?400:850;
  const leaves=own(new THREE.IcosahedronGeometry(1,1));
  const leafMat=own(new THREE.MeshStandardMaterial({color:0x7c8d72,roughness:.98,flatShading:false}));
  const trees=new THREE.InstancedMesh(leaves,leafMat,treeCount);const dummy=new THREE.Object3D();
  for(let i=0;i<treeCount;i++){
    let x=0,y=0;for(let a=0;a<300;a++){x=random()*11-5;y=random()*8-3.5;if(contains(x,y))break;}
    const height=.035+random()*.07;dummy.position.set(x,height*.6,-y);dummy.scale.set(.04+random()*.055,height,.04+random()*.055);dummy.rotation.set(random()*.3,random()*6,0);dummy.updateMatrix();trees.setMatrixAt(i,dummy.matrix);trees.setColorAt(i,new THREE.Color().setHSL(.29+random()*.05,.17+random()*.15,.12+random()*.09));
  }landscape.add(trees);
  // Quiet cartographic orbits bring depth without inventing measurement markers.
  const orbitMat=own(new THREE.LineBasicMaterial({color:0xba955e,transparent:true,opacity:.17}));
  for(const radius of [5.5,6.1,7.3]){
    const points=Array.from({length:193},(_,i)=>{const a=i/192*Math.PI*2;return new THREE.Vector3(Math.cos(a)*radius,-.64,Math.sin(a)*radius*.83)});
    const orbit=new THREE.Line(own(new THREE.BufferGeometry().setFromPoints(points)),orbitMat);landscape.add(orbit);
  }
  const grid=new THREE.GridHelper(25,50,0x596553,0x283c31);grid.position.y=-1;const gridMat=grid.material as THREE.Material;gridMat.transparent=true;gridMat.opacity=.12;resources.push(grid.geometry,gridMat);scene.add(grid);
  const hemisphere=new THREE.HemisphereLight(0xc9dec7,0x11140f,2);scene.add(hemisphere);
  const key=new THREE.DirectionalLight(0xffd19a,5);key.position.set(-4,5,-3);scene.add(key);
  const rim=new THREE.DirectionalLight(0x9fc9ba,2.5);rim.position.set(6,2,5);scene.add(rim);
  const back=new THREE.PointLight(0xf9ac54,45,25,2);back.position.set(-3,1,-3);scene.add(back);
  const glowCanvas=document.createElement('canvas');glowCanvas.width=glowCanvas.height=128;const ctx=glowCanvas.getContext('2d');
  if(ctx){const g=ctx.createRadialGradient(64,64,0,64,64,64);g.addColorStop(0,'rgba(255,209,135,.7)');g.addColorStop(.12,'rgba(255,190,100,.25)');g.addColorStop(1,'rgba(255,190,100,0)');ctx.fillStyle=g;ctx.fillRect(0,0,128,128);}
  const glowMap=own(new THREE.CanvasTexture(glowCanvas));
  const glow=new THREE.Sprite(own(new THREE.SpriteMaterial({map:glowMap,color:0xffc886,transparent:true,opacity:.58,blending:THREE.AdditiveBlending,depthWrite:false})));glow.position.set(-2.8,0,-3);glow.scale.set(9,6,1);scene.add(glow);
  const particleCount=mobile?75:150;const positions=new Float32Array(particleCount*3);
  for(let i=0;i<positions.length;i+=3){positions[i]=(random()-.5)*20;positions[i+1]=random()*6-1;positions[i+2]=(random()-.5)*14;}
  const dustGeometry=own(new THREE.BufferGeometry());dustGeometry.setAttribute('position',new THREE.BufferAttribute(positions,3));
  const dust=new THREE.Points(dustGeometry,own(new THREE.PointsMaterial({color:0xd6be8a,size:.025,map:glowMap,transparent:true,opacity:.55,depthWrite:false,blending:THREE.AdditiveBlending})));scene.add(dust);
  const dolly=new THREE.CatmullRomCurve3([new THREE.Vector3(10,9,13),new THREE.Vector3(8,6.7,10),new THREE.Vector3(4,4,7)]);
  let target=0,progress=0,pointerX=0,pointerY=0,lookX=0,lookY=0,frame=0,visible=false,lost=false,disposed=false,last=0;
  let enabled=document.body.dataset.cinematicMotion!=='paused',activeTime=0;
  function draw(time:number){
    const moving=enabled&&!reduced.matches;const delta=Math.min(64,Math.max(0,time-last));const ease=1-Math.exp(-delta/180);last=time;
    if(reduced.matches)progress=.12;else if(moving)progress=THREE.MathUtils.lerp(progress,target,ease);
    if(moving)activeTime+=delta;
    lookX=moving?THREE.MathUtils.lerp(lookX,pointerX,.04):0;lookY=moving?THREE.MathUtils.lerp(lookY,pointerY,.04):0;
    camera.position.copy(dolly.getPointAt(progress*.85));if(compact.matches)camera.position.multiplyScalar(1.3);else camera.position.multiplyScalar(1.1);camera.position.x+=lookX*.3;camera.position.y+=lookY*.2;
    camera.lookAt(compact.matches?1.4:.1,0,0);landscape.rotation.y=-.24+progress*.22;dust.rotation.y=activeTime*.000008;
    renderer.render(scene,camera);container.dataset.ready='true';container.dataset.cameraProgress=progress.toFixed(4);
  }
  function tick(time:number){frame=0;if(!visible||lost||disposed||document.hidden)return;draw(time);if(enabled&&!reduced.matches)frame=requestAnimationFrame(tick);}
  function invalidate(){if(!frame&&visible&&!lost&&!disposed&&!document.hidden)frame=requestAnimationFrame(tick);}
  function scroll(){const section=container.closest('section')!;target=THREE.MathUtils.clamp(-section.getBoundingClientRect().top/Math.max(1,section.clientHeight),0,1);invalidate();}
  function resize(){renderer.setPixelRatio(Math.min(devicePixelRatio||1,compact.matches?1.2:1.5));const w=container.clientWidth,h=container.clientHeight;renderer.setSize(Math.max(1,w),Math.max(1,h),false);camera.aspect=w/Math.max(1,h);camera.updateProjectionMatrix();scroll();}
  const resizeObserver=new ResizeObserver(resize);resizeObserver.observe(container);
  const observer=new IntersectionObserver(([entry])=>{visible=!!entry?.isIntersecting;if(visible){last=performance.now();invalidate()}else{cancelAnimationFrame(frame);frame=0;}},{threshold:0});observer.observe(container);
  const onVisibility=()=>{cancelAnimationFrame(frame);frame=0;invalidate()};
  const onMotion=(event:Event)=>{enabled=(event as CustomEvent<{enabled:boolean}>).detail.enabled;cancelAnimationFrame(frame);frame=0;invalidate();};
  const onPreference=()=>{cancelAnimationFrame(frame);frame=0;invalidate()};
  const onPointer=(e:PointerEvent)=>{pointerX=e.clientX/innerWidth-.5;pointerY=e.clientY/innerHeight-.5;};
  const onLost=(e:Event)=>{e.preventDefault();lost=true;cancelAnimationFrame(frame);frame=0;container.dataset.ready='false';};
  const onRestored=()=>{lost=false;invalidate();};
  renderer.domElement.addEventListener('webglcontextlost',onLost);renderer.domElement.addEventListener('webglcontextrestored',onRestored);
  addEventListener('scroll',scroll,{passive:true});addEventListener('pointermove',onPointer,{passive:true});document.addEventListener('visibilitychange',onVisibility);document.addEventListener('documentary-motion',onMotion);reduced.addEventListener('change',onPreference);
  resize();
  const dispose=()=>{disposed=true;cancelAnimationFrame(frame);observer.disconnect();resizeObserver.disconnect();removeEventListener('scroll',scroll);removeEventListener('pointermove',onPointer);document.removeEventListener('visibilitychange',onVisibility);document.removeEventListener('documentary-motion',onMotion);reduced.removeEventListener('change',onPreference);renderer.domElement.removeEventListener('webglcontextlost',onLost);renderer.domElement.removeEventListener('webglcontextrestored',onRestored);resources.forEach(r=>r.dispose());renderer.dispose();renderer.domElement.remove();};
  addEventListener('pagehide',e=>{if(!(e as PageTransitionEvent).persisted)dispose();},{once:true});
  return {dispose};
}
