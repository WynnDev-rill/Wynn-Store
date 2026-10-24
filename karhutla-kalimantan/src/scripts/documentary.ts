import {archive,peatStages} from '../data/content';
import {lockBody,unlockBody} from './site';
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
const mediaDialog=document.querySelector<HTMLDialogElement>('#media-dialog');
const mediaContainer=document.querySelector<HTMLElement>('#media-container');
let lastMediaTrigger:HTMLElement|null=null;
function cleanupMedia(){const v=mediaContainer?.querySelector('video');v?.pause();mediaContainer?.replaceChildren();unlockBody();lastMediaTrigger?.focus();}
function closeMedia(){cleanupMedia();mediaDialog?.close();}
mediaDialog?.querySelector('[data-close-media]')?.addEventListener('click',closeMedia);
mediaDialog?.addEventListener('click',e=>{if(e.target===mediaDialog)closeMedia();});
mediaDialog?.addEventListener('close',cleanupMedia);
mediaDialog?.addEventListener('cancel',e=>{e.preventDefault();closeMedia();});
document.querySelectorAll<HTMLButtonElement>('[data-archive-id]').forEach(b=>b.addEventListener('click',()=>{
 const item=archive.find(a=>a.id===b.dataset.archiveId);if(!item||!mediaContainer||!mediaDialog)return;
 lastMediaTrigger=b;mediaContainer.replaceChildren();
 if(item.type==='video'){
   const video=document.createElement('video');video.controls=true;video.preload='none';video.playsInline=true;video.poster=item.src;video.src='/media/indonesia-fire-weather-2015.webm';video.setAttribute('aria-label',item.title);video.setAttribute('aria-describedby','video-description');
   const track=document.createElement('track');track.kind='captions';track.label='Penjelasan Bahasa Indonesia';track.srclang='id';track.src='/media/fire-weather-id.vtt';track.default=true;video.append(track);mediaContainer.append(video);
   const desc=document.createElement('p');desc.id='video-description';desc.className='video-transcript';desc.textContent='Video tanpa narasi. Peta Indonesia bergerak dari Juni hingga November 2015. Lingkaran hitam menunjukkan deteksi api aktif, warna latar menunjukkan Drought Code, dan lapisan biru menunjukkan hujan. Legenda dan tanggal ada pada video. Visualisasi ini tidak menunjukkan luas lahan terbakar.';mediaContainer.append(desc);
 }else{const image=document.createElement('img');image.src=item.src;image.alt=`${item.title}. ${item.caption}`;mediaContainer.append(image);}
 const set=(selector:string,text:string)=>{const el=document.querySelector(selector);if(el)el.textContent=text;};
 set('#media-dialog-title',item.title);set('#media-date',`${item.date} · ${item.location}`);set('#media-caption',item.caption);set('#media-credit',item.credit);
 const link=document.querySelector<HTMLAnchorElement>('#media-source');if(link)link.href=`/sumber/#${item.source}`;
 mediaDialog.showModal();lockBody();
}));
const filters=Array.from(document.querySelectorAll<HTMLButtonElement>('[data-archive-filter]'));
filters.forEach(b=>b.addEventListener('click',()=>{filters.forEach(f=>f.setAttribute('aria-pressed',String(f===b)));let count=0;document.querySelectorAll<HTMLElement>('[data-archive-type]').forEach(item=>{const visible=b.dataset.archiveFilter==='all'||item.dataset.archiveType===b.dataset.archiveFilter;item.hidden=!visible;if(visible)count++;});const result=document.querySelector('#archive-results');if(result)result.textContent=`${count} arsip ditampilkan`;}));
