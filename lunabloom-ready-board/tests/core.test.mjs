import test from 'node:test';import assert from 'node:assert/strict';
import {slotLabel,slotFriendly,slotDaypart,serviceDay,daySlotCounts,peakSlot,toggleSlot,demoCounts} from '../lib/core.mjs';
test('jam dimulai 05.00 dan melewati pergantian tanggal',()=>{assert.equal(slotLabel(0),'05.00');assert.equal(slotLabel(37),'23.30');assert.equal(slotLabel(38),'00.00');assert.equal(slotLabel(47),'04.30')});
test('pembagian hari tetap konsisten',()=>{assert.equal(slotDaypart(0).id,'pagi');assert.equal(slotDaypart(37).id,'malam');assert.equal(slotDaypart(38).id,'dini')});
test('reset tepat 05.00 WIB termasuk perubahan tahun',()=>{assert.equal(serviceDay(new Date('2026-12-31T21:59:59Z')),'2026-12-31');assert.equal(serviceDay(new Date('2026-12-31T22:00:00Z')),'2027-01-01')});
test('pilihan unik dan dibatasi lima',()=>{let selected=[];for(let i=0;i<5;i++){const t=toggleSlot(selected,i);assert.equal(t.ok,true);selected=t.slots}assert.deepEqual(selected,[0,1,2,3,4]);assert.equal(toggleSlot(selected,5).reason,'limit');const undone=toggleSlot(selected,2);assert.deepEqual(undone.slots,[0,1,3,4]);assert.deepEqual(toggleSlot(undone.slots,6).slots,[0,1,3,4,6])});
test('hasil total aman dan tidak melewati 35',()=>{const counts=daySlotCounts([{slot:8,members:45},{slot:9,members:-1},{slot:49,members:300}]);assert.equal(counts[8],35);assert.equal(counts[9],0);assert.equal(counts.length,48)});
test('memilih jam paling ramai tanpa tanggal dummy',()=>{assert.deepEqual(peakSlot([0,3,3,2]),{index:1,count:3});assert.deepEqual(peakSlot([0,0]),{index:-1,count:0})});
test('contoh jadwal berbentuk 48 slot',()=>{const counts=demoCounts();assert.equal(counts.length,48);assert.ok(counts.every(x=>Number.isInteger(x)&&x>=0&&x<=35))});
