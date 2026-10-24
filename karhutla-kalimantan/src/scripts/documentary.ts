import {peatStages} from '../data/content';
const range=document.querySelector<HTMLInputElement>('#compare-range');
range?.addEventListener('input',()=>{document.querySelector<HTMLElement>('#image-comparison')?.style.setProperty('--split',`${range.value}%`);range.setAttribute('aria-valuetext',`${range.value} persen citra 2022, ${100-Number(range.value)} persen citra 2023`);});
const tabs=Array.from(document.querySelectorAll<HTMLButtonElement>('[data-history-tab]'));
const selectTab=(index:number,focus=false)=>{tabs.forEach((t,i)=>{t.setAttribute('aria-selected',String(i===index));t.tabIndex=i===index?0:-1;const panel=document.getElementById(t.getAttribute('aria-controls')!);if(panel)panel.hidden=i!==index;});if(focus)tabs[index].focus();};
tabs.forEach((t,index)=>{t.addEventListener('click',()=>selectTab(index));t.addEventListener('keydown',e=>{let next=index;if(e.key==='ArrowRight')next=(index+1)%tabs.length;else if(e.key==='ArrowLeft')next=(index-1+tabs.length)%tabs.length;else if(e.key==='Home')next=0;else if(e.key==='End')next=tabs.length-1;else return;e.preventDefault();selectTab(next,true);});});
const canvas=document.querySelector<HTMLElement>('#peat-canvas');
const buttons=Array.from(document.querySelectorAll<HTMLButtonElement>('[data-peat-button]'));
let currentStage=0,manualUntil=0;
function setPeatStage(stage:number){currentStage=stage;buttons.forEach((b,i)=>b.setAttribute('aria-pressed',String(i===stage)));if(canvas){canvas.dataset.stage=String(stage);canvas.setAttribute('aria-label',`Ilustrasi tiga dimensi penampang gambut: ${peatStages[stage].label}`);canvas.dispatchEvent(new CustomEvent('peat-stage',{detail:{stage}}));}const label=document.querySelector('#peat-state-label');if(label)label.textContent=peatStages[stage].label;document.querySelectorAll('.peat-step').forEach((e,i)=>e.classList.toggle('active',i===stage));}
buttons.forEach((b,i)=>b.addEventListener('click',()=>{manualUntil=Date.now()+6500;setPeatStage(i);}));
const peatObserver=new IntersectionObserver(entries=>{for(const e of entries)if(e.isIntersecting && Date.now()>manualUntil)setPeatStage(Number((e.target as HTMLElement).dataset.peatStep));},{rootMargin:'-25% 0px -40% 0px',threshold:0});
document.querySelectorAll('.peat-step').forEach(e=>peatObserver.observe(e));
if(canvas){const loadObserver=new IntersectionObserver(async entries=>{if(!entries.some(e=>e.isIntersecting))return;loadObserver.disconnect();try{const {initPeatScene}=await import('./peat-scene');const scene=initPeatScene(canvas);scene?.setStage(currentStage);}catch{canvas.dataset.ready='false';}},{rootMargin:'500px'});loadObserver.observe(canvas);}
setPeatStage(0);
