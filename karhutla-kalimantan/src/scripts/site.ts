const header = document.querySelector<HTMLElement>('.site-header');
const progress = document.querySelector<HTMLElement>('.reading-progress');
const menu = document.querySelector<HTMLDialogElement>('#chapter-menu');
const toggle = document.querySelector<HTMLButtonElement>('.menu-toggle');
export function lockBody() {
  document.body.classList.add('dialog-open');
}
export function unlockBody() { document.body.classList.remove('dialog-open'); }
function closeMenu(){menu?.close(); toggle?.setAttribute('aria-expanded','false'); unlockBody();}
toggle?.addEventListener('click',()=>{if(!menu)return;menu.showModal();lockBody();toggle.setAttribute('aria-expanded','true');});
menu?.querySelector('[data-close-menu]')?.addEventListener('click',closeMenu);
menu?.addEventListener('cancel',e=>{e.preventDefault();closeMenu();});
menu?.addEventListener('close',()=>{toggle?.setAttribute('aria-expanded','false');unlockBody();});
menu?.addEventListener('click',e=>{if(e.target===menu)closeMenu();});
menu?.querySelectorAll('a').forEach(link=>link.addEventListener('click',closeMenu));
let ticking=false;
function renderScroll(){
  const y=window.scrollY;
  header?.classList.toggle('scrolled',y>80);
  const distance=document.documentElement.scrollHeight-window.innerHeight;
  if(progress)progress.style.transform=`scaleX(${distance>0?y/distance:0})`;
  const hero=document.querySelector<HTMLElement>('.hero-picture');
  if(hero && y<window.innerHeight*1.3 && !matchMedia('(prefers-reduced-motion: reduce)').matches){
    hero.style.transform=`translate3d(0,${y*.13}px,0) scale(${1+y*.000045})`;
  }
  ticking=false;
}
window.addEventListener('scroll',()=>{if(!ticking){requestAnimationFrame(renderScroll);ticking=true;}},{passive:true});
const chapterLinks=document.querySelectorAll<HTMLAnchorElement>('[data-chapter-link]');
function updateChapter(){const sections=Array.from(chapterLinks).map(a=>document.getElementById(a.dataset.chapterLink!)).filter((e):e is HTMLElement=>!!e);const edge=innerWidth<=760?100:200;const active=sections.filter(e=>e.getBoundingClientRect().top<=edge).sort((a,b)=>b.getBoundingClientRect().top-a.getBoundingClientRect().top)[0]?.id;chapterLinks.forEach(a=>{const selected=a.dataset.chapterLink===active;a.classList.toggle('current',selected);if(selected)a.setAttribute('aria-current','location');else a.removeAttribute('aria-current');});}
window.addEventListener('scroll',updateChapter,{passive:true});window.addEventListener('resize',updateChapter,{passive:true});renderScroll();updateChapter();
