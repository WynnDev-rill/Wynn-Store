import { MAX_SLOTS, SLOT_COUNT, DAYPARTS, slotLabel, serviceDay, serviceDayLabel, daySlotCounts, peakSlot, toggleSlot } from './lib/core.mjs';
import { sameSlots, displayCounts, dotCount, memberLabel } from './lib/view-model.mjs';

const $ = id => document.getElementById(id);
const state = {
  day: null, selected: [], confirmed: [], counts: Array(SLOT_COUNT).fill(0),
  filter: 'pagi', mode: 'loading', busy: false, pending: false, lastSave: 0,
};
let saveTimer = null;
let refreshTimer = null;
let toastTimer = null;
let refreshing = false;
const SAVE_DELAY = 120; // send almost immediately; the full-screen modal blocks extra taps
const REFRESH_INTERVAL = 120000; // one shared read every two minutes while visible

function toast(message) {
  const node = $('toast');
  node.textContent = message;
  node.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => node.classList.remove('show'), 3800);
}
function updateOverlay() {
  const dialog = $('save-dialog');
  const title = $('save-dialog-title');
  const description = $('save-dialog-description');
  const retry = $('save-retry');
  const failed = state.mode === 'error';
  const loading = state.mode === 'loading';
  const saving = state.busy || state.pending; // show the modal on the same tap, not after the save timer
  const visible = failed || loading || saving;

  dialog.classList.toggle('is-error', failed);
  retry.hidden = !failed;
  if (failed) {
    title.textContent = 'Koneksi terganggu';
    description.textContent = 'Pilihan belum bisa dimuat. Periksa internetmu, lalu coba lagi.';
  } else if (saving) {
    title.textContent = 'Menyimpan pilihanmu';
    description.textContent = 'Jadwal sedang diperbarui. Tunggu sebentar, ya.';
  } else if (loading) {
    title.textContent = 'Menyiapkan jadwal';
    description.textContent = 'Sebentar, sedang memuat pilihan member.';
  }
  if (visible && !dialog.open) dialog.showModal();
  else if (!visible && dialog.open) dialog.close();
}
async function api(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  try {
    const res = await fetch('./api/'+path, {
      credentials: 'same-origin',
      signal: controller.signal,
      ...options
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || 'network_error');
    return data;
  } finally { clearTimeout(timeout); }
}
function renderedCounts() {
  return displayCounts(state.counts,state.confirmed,state.selected);
}
function renderSelections() {
  $('quota-count').textContent = String(state.selected.length);
  const el = $('selection-strip');
  el.replaceChildren();
  const label = document.createElement('span');
  label.className = 'selection-label';
  label.textContent = 'Pilihanmu';
  el.append(label);
  if (!state.selected.length) {
    const hint = document.createElement('span');
    hint.className = 'selection-empty';
    hint.textContent = 'Belum memilih jam';
    el.append(hint);
    return;
  }
  for (const i of state.selected) {
    const chip = document.createElement('span');
    chip.className = 'selected-chip';
    chip.textContent = '✓ '+slotLabel(i);
    el.append(chip);
  }
}
function renderPeak(counts) {
  const peak = peakSlot(counts);
  const banner = $('peak-banner');
  banner.hidden = peak.index < 0;
  if(peak.index >= 0) {
    $('peak-text').textContent = slotLabel(peak.index);
    $('peak-count').textContent = memberLabel(peak.count);
  }
  return peak.index;
}
function cardMarkup(i, count, peakIndex) {
  const chosen = state.selected.includes(i);
  const dots = Array.from({length:5}, (_,index) => '<span class="member-dot'+(index < dotCount(count) ? ' filled' : '')+'" aria-hidden="true"></span>').join('');
  return '<button type="button" class="time-card'+(chosen?' selected':'')+(i===peakIndex?' peak':'')+
    (count > 0 ? ' has-members' : '')+'" data-slot="'+i+'" aria-pressed="'+chosen+
    '" aria-label="'+slotLabel(i)+', '+memberLabel(count)+', '+(chosen?'dipilih':'belum dipilih')+'"'+(state.busy||state.mode!=='live'?' disabled':'')+'>'+
    '<span class="time-top"><span class="time-main">'+slotLabel(i)+'</span>'+
    (chosen?'<span class="time-check" aria-hidden="true">✓</span>':'')+
    '</span><span class="dots-wrap">'+dots+'</span><span class="more-count">'+memberLabel(count)+'</span></button>';
}
function renderBoard(counts,peakIndex) {
  const el = $('board-content');
  const focusSlot = document.activeElement?.dataset?.slot;
  const shown = DAYPARTS.filter(x=>x.id===state.filter);
  el.innerHTML = shown.map(group => '<section class="daypart" aria-labelledby="daypart-'+group.id+'">'+
    '<h3 class="daypart-heading" id="daypart-'+group.id+'"><span class="symbol" aria-hidden="true">'+
    group.icon+'</span>'+group.name+'<small>· '+slotLabel(group.start)+'–'+slotLabel(group.end)+'</small></h3>'+
    '<div class="slots-grid">'+Array.from({length:group.end-group.start+1},(_,offset)=>{
      const i=group.start+offset;return cardMarkup(i,counts[i],peakIndex);
    }).join('')+'</div></section>').join('');
  el.querySelectorAll('[data-slot]').forEach(button => button.addEventListener('click',()=>onToggle(Number(button.dataset.slot))));
  if(focusSlot!==undefined) el.querySelector('[data-slot="'+focusSlot+'"]')?.focus({preventScroll:true});
}
function render() {
  $('date-label').textContent = state.day ? serviceDayLabel(state.day) : 'Memuat tanggal…';
  renderSelections();
  const counts = renderedCounts();
  renderBoard(counts,renderPeak(counts));
  updateOverlay();
}
function setFilter(id) {
  if (state.filter === id) return;
  state.filter = id;
  document.querySelectorAll('[data-filter]').forEach(btn=>{
    const active = btn.dataset.filter===state.filter;
    btn.classList.toggle('active',active);
    btn.setAttribute('aria-pressed',String(active));
  });
  render();
}
function clearSave() {
  clearTimeout(saveTimer);
  saveTimer = null;
  state.pending = false;
}
async function start() {
  if (state.busy || state.pending) return;
  state.mode = 'loading'; render();
  try {
    const [me,board] = await Promise.all([api('me'),api('board')]);
    if(me.day !== board.day) throw new Error('day_changed');
    state.day = me.day;
    state.confirmed = Array.isArray(me.slots)?me.slots.map(Number).sort((a,b)=>a-b):[];
    state.selected = [...state.confirmed];
    state.counts = daySlotCounts(board.rows);
    state.mode = 'live';
    state.lastSave = 0;
    render();
  } catch(e) {
    console.warn('Gagal membaca papan Lunabloom:',e);
    state.mode = 'error'; render();
    toast('Koneksi gagal. Muat ulang halaman untuk mencoba lagi.');
  }
}
async function refreshBoard() {
  if(state.busy || state.pending || state.mode!=='live' || refreshing || document.visibilityState==='hidden') return;
  if(Date.now()-state.lastSave < 70000) return; // don't overwrite recent authoritative count with a cached read
  refreshing = true;
  try {
    const board = await api('board');
    if(board.day !== state.day) { refreshing=false; await start();return; }
    state.counts = daySlotCounts(board.rows);
    render();
  } catch(e){console.warn('Pembaruan jadwal tertunda',e);toast('Pembaruan jadwal tertunda. Periksa koneksimu.');}
  finally { refreshing=false; }
}
function refreshSoon() {
  clearTimeout(refreshTimer);
  refreshTimer = setTimeout(refreshBoard,350);
}
async function save() {
  if(!state.pending || state.busy || state.mode!=='live') return;
  clearSave();
  const desired = [...state.selected];
  if(sameSlots(desired,state.confirmed)){render();return;}
  if(serviceDay()!==state.day){await start();toast('Jadwal sudah berganti hari. Silakan pilih lagi.');return;}
  state.busy=true;render();
  const saveStarted=Date.now();
  let reloadAfter=false;
  try {
    const saved=await api('slots',{
      method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({day:state.day,slots:desired}),keepalive:true
    });
    if(saved.day!==state.day){reloadAfter=true;toast('Jadwal sudah berganti hari. Silakan pilih lagi.');return;}
    state.confirmed = saved.slots.map(Number).sort((a,b)=>a-b);
    state.selected = [...state.confirmed];
    state.counts = daySlotCounts(saved.rows);
    state.lastSave = Date.now();
  } catch(e) {
    console.error('Tidak bisa menyimpan pilihan',e);
    const err=String(e.message||'');
    if(err==='day_changed') {reloadAfter=true;toast('Jadwal sudah berganti hari. Silakan pilih lagi.');}
    else if(err==='board_full') toast('Papan penuh: maksimal 35 member.');
    else if(err==='board_busy') toast('Papan sedang sibuk. Coba lagi.');
    else toast('Gagal menyimpan, perubahan dibatalkan.');
    state.selected=[...state.confirmed];
    try {
      const [me,board]=await Promise.all([api('me'),api('board')]);
      state.day=me.day; state.confirmed=me.slots.map(Number);state.selected=[...state.confirmed];
      state.counts=daySlotCounts(board.rows);
    } catch(error){console.warn('Sinkron ulang gagal',error);state.mode='error';}
  } finally {
    // Avoid a jarring flash for very fast responses; never extend a slow request.
    const remaining=450-(Date.now()-saveStarted);
    if(remaining>0) await new Promise(resolve=>setTimeout(resolve,remaining));
    state.busy=false;
    if(reloadAfter) {clearSave();void start();}
    else render();
  }
}
function onToggle(index) {
  if(state.busy || state.mode!=='live') return;
  if(serviceDay()!==state.day){clearSave();start();toast('Jadwal baru dimulai, pilih kembali setelah dimuat.');return;}
  const changed = toggleSlot(state.selected,index);
  if(!changed.ok){toast(changed.reason==='limit'?'Maksimal 5 pilihan. Batalkan jam sebelumnya untuk mengganti.':'Jam tidak valid.');return;}
  state.selected=changed.slots;
  clearTimeout(saveTimer);
  state.pending=!sameSlots(state.selected,state.confirmed);
  if(state.pending)saveTimer=setTimeout(save,SAVE_DELAY);
  render(); // optimistic member total adjusts immediately
}
function prepareRules(){
  const dialog=$('rules-dialog');
  const close=()=>dialog.close();
  $('rules-trigger').addEventListener('click',()=>dialog.showModal());
  $('rules-close').addEventListener('click',close);
  $('rules-understood').addEventListener('click',close);
  dialog.addEventListener('click',(e)=>{if(e.target===dialog)close()});
}
prepareRules();
$('save-dialog').addEventListener('cancel', (event) => event.preventDefault());
$('save-retry').addEventListener('click', () => { if (!state.busy) void start(); });
document.querySelectorAll('[data-filter]').forEach(btn=>btn.addEventListener('click',()=>setFilter(btn.dataset.filter)));
document.addEventListener('visibilitychange',()=>{
  if(document.visibilityState==='hidden'){
    if(state.pending) {clearTimeout(saveTimer);saveTimer=null;void save();}
  } else if(state.mode==='live') {
    if(serviceDay()!==state.day){clearSave();void start();}
    else if(!state.busy&&!state.pending){void api('me').then(me=>{
      if(me.day!==state.day){void start();return;}
      state.confirmed=me.slots.map(Number).sort((a,b)=>a-b);
      state.selected=[...state.confirmed]; render();refreshSoon();
    }).catch(()=>refreshSoon());}
  }
});
setInterval(()=>{if(state.mode==='live'&&serviceDay()!==state.day&&!state.busy){clearSave();void start();}},15000);
setInterval(refreshBoard,REFRESH_INTERVAL);
void start();
