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
renderScroll();
const chapterLinks=document.querySelectorAll<HTMLAnchorElement>('[data-chapter-link]');
const chapterObserver=new IntersectionObserver(entries=>{for(const entry of entries)if(entry.isIntersecting){chapterLinks.forEach(a=>{const active=a.dataset.chapterLink===entry.target.id;a.classList.toggle('current',active);if(active)a.setAttribute('aria-current','location');else a.removeAttribute('aria-current');});}}, {rootMargin:'-20% 0px -65% 0px'});
document.querySelectorAll('main section[id]').forEach(s=>chapterObserver.observe(s));
