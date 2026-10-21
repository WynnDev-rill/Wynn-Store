import test from 'node:test';
import assert from 'node:assert/strict';
import {sameSlots,displayCounts,dotCount,memberLabel} from '../lib/view-model.mjs';
test('optimistic vote appears immediately once, then is included on save',()=>{
 const zero=new Array(48).fill(0);
 assert.equal(displayCounts(zero,[],[5])[5],1);
 assert.equal(displayCounts(zero,[5],[5])[5],1);
 const authoritative=zero.slice();authoritative[5]=4;
 assert.equal(displayCounts(authoritative,[5],[5])[5],4);
 assert.equal(displayCounts(authoritative,[5],[])[5],3);
 assert.equal(displayCounts(authoritative,[5],[7])[5],3);
 assert.equal(displayCounts(authoritative,[5],[7])[7],1);
});
test('no duplicate count after reversible taps or stale board reads',()=>{
 const base=new Array(48).fill(0);base[6]=1;
 assert.equal(displayCounts(base,[6],[])[6],0);
 assert.equal(displayCounts(base,[6],[6])[6],1);
 assert.equal(displayCounts(new Array(48).fill(0),[6],[6])[6],1);
 assert.equal(sameSlots([1,2],[1,2]),true);
 assert.equal(sameSlots([1,2],[2,1]),false);
});
test('member total is displayed without +N; five dots at most',()=>{
 assert.equal(dotCount(0),0);assert.equal(dotCount(5),5);assert.equal(dotCount(35),5);
 assert.equal(memberLabel(0),'0 member');assert.equal(memberLabel(18),'18 member');
});
