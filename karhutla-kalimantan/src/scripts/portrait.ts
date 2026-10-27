/** Automatic optical input where browsers expose orientation without a permission prompt. */
const reducedPortraitMotion=matchMedia('(prefers-reduced-motion: reduce)');
const clampPortrait=(n:number)=>Math.max(-1,Math.min(1,n));
document.querySelectorAll<HTMLElement>('[data-portrait]').forEach(card=>{
 let x=0,y=0,frame=0,visible=false,base:{beta:number;gamma:number}|null=null;
 function paint(){frame=0;card.style.setProperty('--portrait-rx',`${-y*7}deg`);card.style.setProperty('--portrait-ry',`${x*9}deg`);card.style.setProperty('--portrait-lx',`${50+x*35}%`);card.style.setProperty('--portrait-ly',`${42+y*32}%`);card.style.setProperty('--portrait-shift',`${x*14}%`);}
 function tilt(a:number,b:number){if(reducedPortraitMotion.matches||document.body.dataset.cinematicMotion==='paused')return;x=clampPortrait(a);y=clampPortrait(b);card.dataset.portraitActive='true';if(!frame)frame=requestAnimationFrame(paint);}
 function reset(){x=y=0;card.dataset.portraitActive='false';cancelAnimationFrame(frame);paint();}
 card.addEventListener('pointermove',e=>{const r=card.getBoundingClientRect();tilt((e.clientX-r.left)/r.width*2-1,(e.clientY-r.top)/r.height*2-1);},{passive:true});
 ['pointerleave','pointercancel','blur'].forEach(type=>card.addEventListener(type,reset));
 card.addEventListener('pointerup',e=>{if(e.pointerType!=='mouse')reset();});
 card.addEventListener('keydown',e=>{if(e.key==='Escape'){reset();return;}if(['ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(e.key)){e.preventDefault();tilt(x+(e.key==='ArrowRight'?.25:e.key==='ArrowLeft'?-.25:0),y+(e.key==='ArrowDown'?.25:e.key==='ArrowUp'?-.25:0));}});
 const orientation=(e:DeviceOrientationEvent)=>{if(!visible||document.hidden||reducedPortraitMotion.matches||e.beta===null||e.gamma===null)return;base??={beta:e.beta,gamma:e.gamma};const delta=(a:number,b:number)=>(a-b+540)%360-180;tilt(delta(e.gamma,base.gamma)/22,delta(e.beta,base.beta)/28);};
 // Android/other unrestricted implementations emit directly. Restricted browsers retain touch input.
 if(isSecureContext&&typeof DeviceOrientationEvent!=='undefined')window.addEventListener('deviceorientation',orientation,{passive:true});
 new IntersectionObserver(([e])=>{visible=!!e?.isIntersecting;if(!visible){base=null;reset();}}).observe(card);
 reducedPortraitMotion.addEventListener('change',reset);document.addEventListener('visibilitychange',()=>{if(document.hidden){base=null;reset();}});
 reset();
});
