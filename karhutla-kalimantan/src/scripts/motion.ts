/** Scroll is the input, never replaced. All effects retain readable static fallbacks. */
document.documentElement.classList.add('has-motion');
const reduced=matchMedia('(prefers-reduced-motion: reduce)');
const clamp=(n:number)=>Math.min(1,Math.max(0,n));
const hero=document.querySelector<HTMLElement>('.hero');
const title=document.querySelector<HTMLElement>('.hero-title-wrap');
const peat=document.querySelector<HTMLElement>('.peat-scrolly');
const canvas=document.querySelector<HTMLElement>('#peat-canvas');
const visual=document.querySelector<HTMLElement>('.recovery-visual img');
const portal=document.querySelector<HTMLElement>('.depth-portal');
let scheduled=false,lastProgress=-1;
function render(){scheduled=false;const h=innerHeight;
 if(reduced.matches){if(title)title.style.transform='none';return;}
 if(hero&&title){const p=clamp(-hero.getBoundingClientRect().top/h);title.style.transform=`translate3d(0,${-p*55}px,0)`;hero.style.setProperty('--hero-progress',String(p));}
 if(peat&&canvas){const r=peat.getBoundingClientRect();const p=clamp((h*.3-r.top)/Math.max(1,r.height-h*.65));if(Math.abs(p-lastProgress)>.003){canvas.dispatchEvent(new CustomEvent('peat-progress',{detail:{progress:p}}));lastProgress=p;}}
 if(visual){const r=visual.parentElement!.getBoundingClientRect();if(r.bottom>0&&r.top<h)visual.style.transform=`translate3d(0,${(r.top/h)*30}px,0) scale(1.06)`;}
 if(portal){const r=portal.getBoundingClientRect();const p=clamp((h-r.top)/(h+r.height));portal.style.setProperty('--depth-progress',String(p));}
}
const schedule=()=>{if(!scheduled){scheduled=true;requestAnimationFrame(render);}};
addEventListener('scroll',schedule,{passive:true});addEventListener('resize',schedule,{passive:true});reduced.addEventListener('change',schedule);
const observer=new IntersectionObserver(entries=>{for(const e of entries)if(e.isIntersecting){e.target.classList.add('in-view');observer.unobserve(e.target)}},{threshold:.12});
document.querySelectorAll('.dispatch-entry,.current-report-head').forEach(el=>observer.observe(el));
render();
