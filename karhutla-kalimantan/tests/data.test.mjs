import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const dataset=JSON.parse(readFileSync(new URL('../src/data/burned-area.json',import.meta.url)));
const geo=JSON.parse(readFileSync(new URL('../src/data/provinces.json',import.meta.url)));
const csv=readFileSync(new URL('../public/data/karhutla-kalimantan-2019-2023.csv',import.meta.url),'utf8');
test('all five provinces have one finite nonnegative value per year and an actual boundary',()=>{
 assert.equal(dataset.provinces.length,5);
 for(const p of dataset.provinces){assert.equal(p.values.length,dataset.years.length);assert(p.values.every(v=>Number.isFinite(v)&&v>=0));assert(geo.features.some(f=>f.properties.shapeName===p.shapeName));}
});
test('transcribed KLHK rows retain the correct source-edition precision',()=>{
 assert.deepEqual(dataset.provinces.find(p=>p.id==='utara').values,[8559,1721,1678,370,796.36]);
 assert.equal(dataset.provinces.find(p=>p.id==='barat').values[4],111848.43);
 const sum=i=>Math.round(dataset.provinces.reduce((s,p)=>s+p.values[i],0)*100)/100;
 assert.equal(sum(0),684599);assert.equal(sum(2),37575);assert.equal(sum(4),508430.22);
});
test('published JSON and CSV match the editorial dataset exactly',()=>{
 const publicData=JSON.parse(readFileSync(new URL('../public/data/burned-area.json',import.meta.url)));
 assert.deepEqual(publicData,dataset);
 const rows=csv.trim().replace(/^\uFEFF/,'').split(/\r?\n/);assert.equal(rows.length,26);
 for(const p of dataset.provinces)dataset.years.forEach((y,i)=>assert(rows.includes(`${p.name},${y},${p.values[i]},KLHK_2023,7.2`)));
});
test('every factual citation resolves to a unique bibliography entry',async()=>{
 const {sources,chronology,archive}=await import('../src/data/content.ts');
 assert.equal(new Set(sources.map(s=>s.id)).size,sources.length);
 for(const item of [...chronology,...archive])assert(sources.some(s=>s.id===item.source));
 const index=readFileSync(new URL('../src/pages/index.astro',import.meta.url),'utf8');
 for(const [,id]of index.matchAll(/<Cite id="([^"]+)"/g))assert(sources.some(s=>s.id===id),`unresolved citation: ${id}`);
});
