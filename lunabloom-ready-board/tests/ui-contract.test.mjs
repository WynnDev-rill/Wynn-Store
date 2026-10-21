import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const html=readFileSync(new URL('../index.html',import.meta.url),'utf8');
const stylesheet=readFileSync(new URL('../styles.css',import.meta.url),'utf8');
const script=readFileSync(new URL('../app.mjs',import.meta.url),'utf8');
test('only the five daily group filters remain',()=>{
 const filters=html.match(/data-filter="[^"]+"/g)||[];
 assert.deepEqual(filters,['data-filter="pagi"','data-filter="siang"','data-filter="sore"','data-filter="malam"','data-filter="dini"']);
});
test('member totals and dots are adaptive; no redundant friendly labels',()=>{
 assert.match(script,/memberLabel\(count\)/);
 assert.doesNotMatch(script,/Belum ada/);
 assert.doesNotMatch(script,/slotFriendly/);
 assert.doesNotMatch(html,/data-filter="all"/);
});
test('API writes are grouped and board polling limited',()=>{
 assert.match(script,/SAVE_DELAY = 2300/);
 assert.match(script,/REFRESH_INTERVAL = 120000/);
 assert.match(script,/day:state\.day,slots:desired/);
});

test('Pagi is exclusive default category, with no all-slots mode',()=>{
 assert.match(script,/filter: 'pagi'/);
 assert.match(script,/const shown = DAYPARTS\.filter/);
 assert.doesNotMatch(script,/state\.filter === id \? null/);
 assert.match(html,/class="filter active" type="button" data-filter="pagi" aria-pressed="true"/);
});
test('minimal single board includes inline choice summary and transient saving state',()=>{
 assert.doesNotMatch(html,/class="interaction-card"/);
 assert.doesNotMatch(html,/01 \/ PILIH JAM|02 \/ JADWAL BERSAMA/);
 assert.match(html,/>JADWAL BERSAMA</);
 assert.doesNotMatch(html,/connection-pill|Tersinkron/);
 assert.match(html,/id="save-dialog"/);
 assert.match(html,/id="quota-count"/);
 assert.match(html,/id="selection-strip"/);
 assert.match(stylesheet,/\.save-orbit/);
});
test('always five visible placeholder dots; filled count green and empty grey',()=>{
 assert.match(script,/Array\.from\(\{length:5\}/);
 assert.match(stylesheet,/\.member-dot\.filled\{background:#40af70/);
 assert.match(stylesheet,/\.member-dot\{background:#e1e6e3/);
});

test('selection heading never disappears, even when no time is selected',()=>{
 assert.match(html,/id="selection-strip"[^>]*>/);
 assert.doesNotMatch(html,/id="selection-strip"[^>]*hidden/);
 assert.match(script,/hint.textContent = 'Belum memilih jam'/);
 assert.doesNotMatch(script,/el.hidden = state.selected.length === 0/);
});
test('all-screen save overlay is modal, never a permanent in-card indicator',()=>{
 assert.match(html,/<dialog id="save-dialog"/);
 assert.doesNotMatch(html,/id="save-indicator"/);
 assert.match(script,/dialog.showModal\(\)/);
 assert.match(script,/dialog.close\(\)/);
 assert.match(script,/state.busy/);
 assert.match(script,/addEventListener\('cancel', \(event\) => event.preventDefault\(\)\)/);
 assert.match(stylesheet,/\.save-dialog::backdrop/);
});
test('contact and Wynn credit replace old footer and redundant warning',()=>{
 assert.match(html,/Made by <strong>Wynn<\/strong>/);
 assert.match(html,/https:\/\/wa\.me\/6287769436511/);
 assert.match(html,/class="whatsapp-icon"/);
 assert.doesNotMatch(html,/class="info-note"/);
 assert.doesNotMatch(html,/Made for/);
});
