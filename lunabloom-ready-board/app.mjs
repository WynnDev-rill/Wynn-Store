import {MAX_SLOTS,SLOT_COUNT,DAYPARTS,slotLabel,slotFriendly,slotDaypart,serviceDay,serviceDayLabel,daySlotCounts,peakSlot,toggleSlot,demoCounts} from './lib/core.mjs';

const $=(id)=>document.getElementById(id);
const state={day:serviceDay(),selected:[],counts:Array(SLOT_COUNT).fill(0),filter:'all',mode:'loading',busy:false,backend:null,lastToast:0};
const config=window.LUNABLOOM_CONFIG||{};
const demoMode=!(config.supabaseUrl&&config.supabasePublishableKey);
let toastTimeout=null;
let refreshTimeout=null;
let visitorId=null;

function toast(message){const node=$('toast');node.textContent=message;node.classList.add('show');if(toastTimeout)clearTimeout(toastTimeout);toastTimeout=setTimeout(()=>node.classList.remove('show'),3400)}
function setStatus(kind,text){const pill=$('connection-pill');pill.className=`connection-pill ${kind}`;pill.querySelector('span:last-child').textContent=text}
function demoKey(){return `lunabloom:demo:${state.day}`}
function readDemo(){try{const data=JSON.parse(localStorage.getItem(demoKey())||'[]');return Array.isArray(data)?data.filter(x=>Number.isInteger(x)&&x>=0&&x<48).slice(0,MAX_SLOTS):[]}catch{return []}}
function paintDate(){$('date-label').textContent=serviceDayLabel(state.day)}
function buildSelections(){const strip=$('selection-strip');$('quota-count').textContent=String(state.selected.length);strip.replaceChildren();if(!state.selected.length){const p=document.createElement('span');p.className='muted';p.textContent='Belum memilih jam. Kamu bisa pilih maksimal 5 slot.';strip.append(p);return}
 for(const i of state.selected){const el=document.createElement('span');el.className='selected-chip';el.textContent=`✓ ${slotLabel(i)}`;strip.append(el)}
 const hint=document.createElement('span');hint.className='selection-tip';hint.textContent='Ketuk jam yang ditandai untuk batal';strip.append(hint);
}
function fullCounts(){if(state.mode!=='demo')return state.counts;const values=state.counts.slice();state.selected.forEach(i=>values[i]=Math.min(35,values[i]+1));return values}
function paintPeak(){const p=peakSlot(fullCounts());const banner=$('peak-banner');if(p.index<0){banner.hidden=true;return}banner.hidden=false;$('peak-text').textContent=`${slotLabel(p.index)} · ${slotFriendly(p.index)}`;$('peak-count').textContent=`${p.count} member`;}
function cardHtml(i,count,peakIndex){const selected=state.selected.includes(i);const visible=Math.min(5,count);const dots=visible?Array.from({length:visible},()=>'<span class="member-dot" aria-hidden="true"></span>').join(''):'<span class="no-dots">Belum ada</span>';
 return `<button type="button" class="time-card${selected?' selected':''}${i===peakIndex?' peak':''}" data-slot="${i}" aria-pressed="${selected}" aria-label="${slotLabel(i)}, ${slotFriendly(i)}, ${count} member, ${selected?'sudah dipilih':'belum dipilih'}" ${state.busy?'disabled':''}>
  <span class="time-top"><span><span class="time-main">${slotLabel(i)}</span><span class="time-friendly" style="display:block">${slotFriendly(i)}</span></span>${selected?'<span class="time-check" aria-hidden="true">✓</span>':''}</span>
  <span class="dots-wrap">${dots}</span>${count>5?`<span class="more-count">+${count-5} lainnya</span>`:`<span class="more-count">${count} ${count===1?'member':'member'}</span>`}
  </button>`}
function paintBoard(){const container=$('board-content');const counts=fullCounts();const peak=peakSlot(counts);const shown=state.filter==='all'?DAYPARTS:DAYPARTS.filter(x=>x.id===state.filter);
 container.innerHTML=shown.map(group=>`<section class="daypart" aria-labelledby="daypart-${group.id}"><h3 class="daypart-heading" id="daypart-${group.id}"><span class="symbol" aria-hidden="true">${group.icon}</span>${group.name}<small>· ${slotLabel(group.start)}–${slotLabel(group.end)}</small></h3><div class="slots-grid">${Array.from({length:group.end-group.start+1},(_,idx)=>{const i=idx+group.start;return cardHtml(i,counts[i],peak.index) }).join('')}</div></section>`).join('');
 container.querySelectorAll('[data-slot]').forEach(btn=>btn.addEventListener('click',()=>onToggle(Number(btn.dataset.slot))));
}
function render(){paintDate();buildSelections();paintPeak();paintBoard()}
function setFilter(filter){state.filter=filter;document.querySelectorAll('[data-filter]').forEach(btn=>{const chosen=btn.dataset.filter===filter;btn.classList.toggle('active',chosen);btn.setAttribute('aria-pressed',String(chosen))});paintBoard()}
async function onToggle(index){if(state.busy)return;if(state.mode==='error'){toast('Belum tersambung. Coba muat ulang halaman.');return}
 const result=toggleSlot(state.selected,index);if(!result.ok){toast(result.reason==='limit'?'Maksimal 5 jam. Batalkan pilihan lama untuk menggantinya.':'Jam tidak valid.');return}
 if(state.mode==='demo'){state.selected=result.slots;localStorage.setItem(demoKey(),JSON.stringify(state.selected));render();return}
 if(state.mode!=='live'){toast('Sedang menghubungkan jadwal…');return}
 state.busy=true;paintBoard();try{const {data,error}=await state.backend.rpc('lunabloom_set_slots',{p_slots:result.slots,p_visitor_id:visitorId});if(error)throw error;state.selected=Array.isArray(data)?data.map(Number):result.slots;await reloadCounts();render()}
 catch(err){console.error('Gagal menyimpan pilihan',err);let msg='Gagal menyimpan. Coba lagi.';const s=(err.message||'').toLowerCase();if(s.includes('board_full'))msg='Papan penuh (35 member). Coba lagi besok.';else if(s.includes('max_five'))msg='Maksimal 5 pilihan.';else if(s.includes('jwt')||s.includes('auth'))msg='Sesi browser bermasalah. Muat ulang halaman.';toast(msg)}finally{state.busy=false;paintBoard()}
}
async function reloadCounts(){const {data,error}=await state.backend.rpc('lunabloom_board');if(error)throw error;state.counts=daySlotCounts(data);paintPeak();paintBoard()}
async function reloadMine(){const {data,error}=await state.backend.rpc('lunabloom_my_slots',{p_visitor_id:visitorId});if(error)throw error;state.selected=Array.isArray(data)?data.map(Number).sort((a,b)=>a-b):[];buildSelections()}
function scheduleReload(){if(refreshTimeout)clearTimeout(refreshTimeout);refreshTimeout=setTimeout(()=>{if(state.mode==='live')reloadCounts().catch(e=>{console.warn('Pembaruan terganggu',e);setStatus('error','Menghubungkan ulang')})},450)}
async function startLive(){try{setStatus('','Menghubungkan…');const {createClient}=await import('https://esm.sh/@supabase/supabase-js@2.81.1?bundle');
 const client=createClient(config.supabaseUrl,config.supabasePublishableKey,{auth:{persistSession:false,autoRefreshToken:false,detectSessionInUrl:false},realtime:{params:{eventsPerSecond:5}}});state.backend=client;
 visitorId=localStorage.getItem('lunabloom:visitor');if(!visitorId||!/^[a-f0-9-]{36}$/i.test(visitorId)){visitorId=crypto.randomUUID();localStorage.setItem('lunabloom:visitor',visitorId)}
 await Promise.all([reloadCounts(),reloadMine()]);state.mode='live';setStatus('online','Tersinkron');render();
 client.channel('lunabloom-counts').on('postgres_changes',{event:'*',schema:'public',table:'lunabloom_slot_totals'},scheduleReload).subscribe((status)=>{if(status==='SUBSCRIBED')setStatus('online','Real-time aktif');else if(['CHANNEL_ERROR','TIMED_OUT'].includes(status))setStatus('error','Menyambungkan ulang')});
 }catch(error){console.error('Gagal terhubung ke Lunabloom',error);state.mode='error';setStatus('error','Koneksi gagal');toast('Gagal terhubung. Coba buka ulang halaman.');render()}}
function startDemo(){state.mode='demo';state.counts=demoCounts();state.selected=readDemo();setStatus('demo','Mode pratinjau');const n=document.createElement('div');n.className='preview-notice';n.textContent='Mode pratinjau: pilihan hanya tersimpan di browser ini, belum tersinkron antar-member.';const board=document.querySelector('.board-section');board.parentNode.insertBefore(n,board);render()}
function prepareRules(){const dialog=$('rules-dialog');const close=()=>dialog.close();$('rules-trigger').addEventListener('click',()=>dialog.showModal());$('rules-close').addEventListener('click',close);$('rules-understood').addEventListener('click',close);dialog.addEventListener('click',(e)=>{if(e.target===dialog)close()})}
function attach(){prepareRules();document.querySelectorAll('[data-filter]').forEach(btn=>btn.addEventListener('click',()=>setFilter(btn.dataset.filter)));document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible'&&state.mode==='live')scheduleReload()});setInterval(async()=>{const today=serviceDay();if(today!==state.day){state.day=today;if(state.mode==='demo'){state.selected=readDemo();state.counts=demoCounts()}else if(state.mode==='live'){try{await Promise.all([reloadMine(),reloadCounts()])}catch(e){console.error(e)}}render();toast('Jadwal baru sudah dimulai. Pilihan kemarin telah direset.')}},15000);}
attach();if(demoMode)startDemo();else startLive();
