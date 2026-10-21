import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const html=readFileSync(new URL('../index.html',import.meta.url),'utf8');
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
