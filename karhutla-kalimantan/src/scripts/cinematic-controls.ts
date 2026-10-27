/** One explicit pause contract for the opening, forest, weather and DOM motion. */
export {};
const pause=document.querySelector<HTMLButtonElement>('[data-cinematic-pause]');
const reduced=matchMedia('(prefers-reduced-motion:reduce)');
const checkbox=document.querySelector<HTMLInputElement>('[data-weather-motion]');
let userPaused=false;
function sync(){
 const enabled=!userPaused&&!reduced.matches;
 document.body.dataset.cinematicMotion=enabled?'running':'paused';
 if(!enabled)document.body.dataset.cinematicEntrance='complete';
 pause?.setAttribute('aria-pressed',String(!enabled));
 pause?.setAttribute('aria-label',reduced.matches?'Preferensi pengurangan gerak aktif':enabled?'Jeda gerak sinematik':'Lanjutkan gerak sinematik');
 if(pause)pause.disabled=reduced.matches;
 const label=pause?.querySelector('[data-pause-label]');if(label)label.textContent=reduced.matches?'Gerak dikurangi':enabled?'Jeda gerak':'Lanjutkan';
 const icon=pause?.querySelector('[data-pause-icon] path');if(icon)icon.setAttribute('d',enabled?'M4 3h3v10H4zm5 0h3v10H9z':'M4 2l9 6-9 6z');
 if(checkbox){checkbox.checked=enabled;checkbox.disabled=reduced.matches;}
 document.dispatchEvent(new CustomEvent('documentary-motion',{detail:{enabled}}));
}
pause?.addEventListener('click',()=>{userPaused=!userPaused;if(reduced.matches&&!userPaused){userPaused=true;}sync();});
checkbox?.addEventListener('change',()=>{userPaused=!checkbox.checked;sync();});
reduced.addEventListener('change',sync);sync();
