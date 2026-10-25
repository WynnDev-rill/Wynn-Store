export {};
/** Chapter seams are spatial accents, never a blocking transition or alternate scroller. */
const preference=matchMedia('(prefers-reduced-motion: reduce)');
const scenes=Array.from(document.querySelectorAll<HTMLElement>('main>section:not(.hero):not(.forest-world)'));
for(const scene of scenes){const layer=document.createElement('div');layer.className='scene-transition-layer';layer.setAttribute('aria-hidden','true');scene.prepend(layer);if(getComputedStyle(scene).position==='static')scene.style.position='relative';}
let queued=false;
function update(){queued=false;const h=innerHeight;for(const scene of scenes){const r=scene.getBoundingClientRect();if(r.bottom>0&&r.top<h){const p=preference.matches?.5:Math.min(1,Math.max(0,(h-r.top)/(h+r.height)));scene.style.setProperty('--scene-progress',String(p));}}}
function schedule(){if(!queued){queued=true;requestAnimationFrame(update);}}
addEventListener('scroll',schedule,{passive:true});addEventListener('resize',schedule,{passive:true});preference.addEventListener('change',schedule);update();
