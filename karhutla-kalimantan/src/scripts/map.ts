import data from '../data/burned-area.json';
const root=document.querySelector<HTMLElement>('#data-map');
if(root){
 const format=(n:number,precise=false)=>n.toLocaleString('id-ID',{maximumFractionDigits:precise?2:0});
 let year=2023,province='tengah';
 const yearButtons=Array.from(document.querySelectorAll<HTMLButtonElement>('[data-year]'));
 const select=document.querySelector<HTMLSelectElement>('#province-select');
 const paths=Array.from(root.querySelectorAll<SVGPathElement>('[data-province]'));
 const setText=(sel:string,value:string)=>{document.querySelectorAll(sel).forEach(e=>e.textContent=value);};
 const color=(v:number)=>v<10000?'#52665c':v<50000?'#b8b58b':v<100000?'#d5a367':v<200000?'#d37445':'#b54e33';
 const update=()=>{
   const index=data.years.indexOf(year), selected=data.provinces.find(p=>p.id===province)!;
   const value=selected.values[index];
   setText('#selected-province-name',selected.name);setText('#selected-area',format(value));setText('#selected-year',String(year));
   setText('[data-total-year]',String(year));setText('#total-area',format(data.provinces.reduce((sum,p)=>sum+p.values[index],0)));
   const title=document.querySelector('#map-title');if(title)title.textContent=`Luas karhutla lima provinsi Kalimantan tahun ${year}`;
   yearButtons.forEach(b=>{const active=Number(b.dataset.year)===year;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active));});
   paths.forEach(path=>{
     const p=data.provinces.find(p=>p.id===path.dataset.province)!;
     path.setAttribute('fill',color(p.values[index]));
     const active=p.id===province;path.classList.toggle('selected',active);path.setAttribute('aria-pressed',String(active));
     path.setAttribute('aria-label',`${p.name}: ${format(p.values[index],true)} hektare, ${year}`);
     const t=path.querySelector('title');if(t)t.textContent=`${p.name}: ${format(p.values[index],true)} ha (${year})`;
     setText(`[data-map-label="${p.id}"]`,`${format(p.values[index])} ha`);
     document.querySelector(`[data-label-province="${p.id}"]`)?.setAttribute('fill',p.values[index]<10000||p.values[index]>=200000?'#fff':'#12251c');
   });
   const selectedPath=paths.find(path=>path.dataset.province===province);const outline=document.querySelector('#province-outline');if(selectedPath&&outline)outline.setAttribute('d',selectedPath.getAttribute('d')!);
   if(select)select.value=province;
   const chart=document.querySelector('.mini-chart');chart?.setAttribute('aria-label',`Perbandingan luas karhutla ${selected.name} 2019 sampai 2023`);
   const max=Math.max(...selected.values);
   data.years.forEach((y,i)=>{
     const bar=document.querySelector<HTMLElement>(`[data-bar-year="${y}"]`);
     if(bar){bar.style.setProperty('--bar-height',`${Math.max(3,selected.values[i]/max*100)}%`);bar.classList.toggle('current',y===year);bar.title=`${y}: ${format(selected.values[i],true)} ha`;const sr=bar.querySelector('.sr-only');if(sr)sr.textContent=`${format(selected.values[i],true)} hektare`;}
   });
 };
 yearButtons.forEach(b=>b.addEventListener('click',()=>{year=Number(b.dataset.year);update();}));
 select?.addEventListener('change',()=>{province=select.value;update();});
 paths.forEach(path=>{
   const activate=()=>{province=path.dataset.province!;update();};
   path.addEventListener('click',activate);
   path.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();activate();}});
 });
 update();
}
