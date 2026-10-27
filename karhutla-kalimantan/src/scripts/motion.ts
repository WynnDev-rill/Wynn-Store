/** Natural scrolling drives one shared visual journey. Content never depends on effects. */
document.documentElement.classList.add('has-motion');
const reduced=matchMedia('(prefers-reduced-motion: reduce)');
const clamp=(n:number)=>Math.min(1,Math.max(0,n));
const hero=document.querySelector<HTMLElement>('.hero');
const title=document.querySelector<HTMLElement>('.hero-title-wrap');
const peat=document.querySelector<HTMLElement>('.peat-scrolly');
const canvas=document.querySelector<HTMLElement>('#peat-canvas');
const visual=document.querySelector<HTMLElement>('.recovery-visual img');
const portal=document.querySelector<HTMLElement>('.depth-portal');
const impact=document.querySelector<HTMLElement>('.impact-atlas');
const images=Array.from(document.querySelectorAll<HTMLElement>('[data-depth-image]'));
const activeAnimations=new Set<Animation>();
let scheduled=false,lastProgress=-1;
function render(){scheduled=false;const h=innerHeight;
 if(document.body.dataset.cinematicMotion==='paused'&&!reduced.matches)return;
 if(reduced.matches){if(title)title.style.transform='none';images.forEach(e=>e.style.removeProperty('transform'));return;}
 if(hero&&title){const p=clamp(-hero.getBoundingClientRect().top/h);title.style.transform=`translate3d(${-p*22}px,${-p*75}px,0)`;hero.style.setProperty('--hero-progress',String(p));const em=hero.querySelector<HTMLElement>('.hero-second em');if(em)em.style.transform=`translate3d(${p*55}px,0,0) rotate(${-p*6}deg)`;}
 if(peat&&canvas){const r=peat.getBoundingClientRect();const p=clamp((h*.3-r.top)/Math.max(1,r.height-h*.65));if(Math.abs(p-lastProgress)>.003){canvas.dispatchEvent(new CustomEvent('peat-progress',{detail:{progress:p}}));lastProgress=p;}}
 const signal=document.querySelector<HTMLElement>('.signal-section');if(signal){const r=signal.getBoundingClientRect();signal.style.setProperty('--signal-progress',String(clamp((h-r.top)/(h+r.height))));}
 const recovery=document.querySelector<HTMLElement>('.recovery-content');if(recovery){const r=recovery.getBoundingClientRect();recovery.style.setProperty('--recovery-progress',String(clamp((h-r.top)/(h+r.height))));}
 if(visual){const r=visual.parentElement!.getBoundingClientRect();if(r.bottom>0&&r.top<h)visual.style.transform=`translate3d(0,${(r.top/h)*35}px,0) scale(1.08)`;}
 if(portal){const r=portal.getBoundingClientRect();const p=clamp((h-r.top)/(h+r.height));portal.style.setProperty('--depth-progress',String(p));}
 if(impact){const r=impact.getBoundingClientRect();if(r.bottom>0&&r.top<h)impact.style.setProperty('--impact-progress',String(clamp((h-r.top)/(h+r.height))));}
 images.forEach(el=>{const r=el.getBoundingClientRect();if(r.bottom>0&&r.top<h){const p=clamp((h-r.top)/(h+r.height));const angle=el.classList.contains('creator-image-wrap')?-3:-4;el.style.transform=`perspective(1200px) translate3d(0,${(p-.5)*-25}px,0) rotate(${angle+(p-.5)*3}deg) rotateY(${(p-.5)*5}deg)`;}});
}
const schedule=()=>{if(!scheduled){scheduled=true;requestAnimationFrame(render);}};
addEventListener('scroll',schedule,{passive:true});addEventListener('resize',schedule,{passive:true});
reduced.addEventListener('change',()=>{if(reduced.matches){activeAnimations.forEach(a=>a.finish());activeAnimations.clear();}schedule();});
const observer=new IntersectionObserver(entries=>{for(const e of entries)if(e.isIntersecting){const el=e.target as HTMLElement;el.classList.add('in-view');if(!reduced.matches&&document.body.dataset.cinematicMotion!=='paused'){const words=el.querySelectorAll<HTMLElement>('.motion-word');if(words.length){words.forEach((word,i)=>{const a=word.animate([{transform:'translateY(105%) rotate(3deg)'},{transform:'translateY(0) rotate(0)'}],{duration:850,delay:Math.min(i*38,360),easing:'cubic-bezier(.22,1,.36,1)',fill:'both'});activeAnimations.add(a);a.onfinish=()=>activeAnimations.delete(a);});}else{const a=el.animate([{transform:'translateY(25px)',clipPath:'inset(0 0 8% 0)'},{transform:'translateY(0)',clipPath:'inset(0)'}],{duration:850,easing:'cubic-bezier(.22,1,.36,1)'});activeAnimations.add(a);a.onfinish=()=>activeAnimations.delete(a);}}observer.unobserve(el);}}, {threshold:.12});
document.querySelectorAll<HTMLElement>('[data-reveal]').forEach(el=>{if(/^H[1-3]$/.test(el.tagName)){const walker=document.createTreeWalker(el,NodeFilter.SHOW_TEXT);const nodes:Text[]=[];let node;while((node=walker.nextNode()))if(node.textContent?.trim())nodes.push(node as Text);nodes.forEach(node=>{const frag=document.createDocumentFragment();node.textContent!.split(/(\s+)/).forEach(word=>{if(!word.trim())frag.append(document.createTextNode(word));else{const wrap=document.createElement('span');wrap.className='motion-word-wrap';const inner=document.createElement('span');inner.className='motion-word';inner.textContent=word;wrap.append(inner);frag.append(wrap);}});node.replaceWith(frag);});}observer.observe(el);});
document.addEventListener('documentary-motion',()=>{if(document.body.dataset.cinematicMotion==='paused'){activeAnimations.forEach(animation=>animation.finish());activeAnimations.clear();}schedule();});
render();
