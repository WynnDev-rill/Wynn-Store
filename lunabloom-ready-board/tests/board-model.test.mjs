import test from 'node:test';
import assert from 'node:assert/strict';
import {serviceDay,slotsFrom,normalize,totals,change} from '../lib/board-model.mjs';
test('rollover at 05:00 WIB',()=>{
  assert.equal(serviceDay(Date.parse('2026-10-21T04:59:59+07:00')),'2026-10-20');
  assert.equal(serviceDay(Date.parse('2026-10-21T05:00:00+07:00')),'2026-10-21');
});
test('limits, duplicates and invalid',()=>{
  assert.deepEqual(slotsFrom([5,2,0]),[0,2,5]);
  assert.throws(()=>slotsFrom([0,1,2,3,4,5]));
  assert.throws(()=>slotsFrom([0,0]));
  assert.throws(()=>slotsFrom([48]));
});
test('35 anonymous participants, no double vote',()=>{
  const d=serviceDay();let s=normalize(null,d);const ids=Array.from({length:35},(_,i)=>i.toString(16).padStart(64,'0'));
  for(const key of ids)s=change(s,key,[3]).state;
  assert.equal(totals(s)[0].members,35);
  assert.equal(change(s,ids[0],[3]).changed,false);
  assert.throws(()=>change(s,'f'.repeat(64),[3]),/board_full/);
  s=change(s,ids[0],[]).state;
  s=change(s,'f'.repeat(64),[3]).state;
  assert.equal(totals(s)[0].members,35);
});
test('different service day resets board',()=>{
  const d='2026-10-21';const key='a'.repeat(64);
  const s=change(normalize(null,d),key,[5]).state;
  assert.equal(totals(normalize(s,'2026-10-22')).length,0);
});
