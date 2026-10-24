import assert from 'node:assert/strict';
import {readFileSync,existsSync,statSync,readdirSync} from 'node:fs';
import {resolve,join,extname} from 'node:path';
const root=resolve('dist');
const html=[];
function collect(dir){for(const entry of readdirSync(dir,{withFileTypes:true})){const p=join(dir,entry.name);if(entry.isDirectory())collect(p);else if(extname(p)==='.html')html.push(p);}}
collect(root);
for(const file of html){
 const content=readFileSync(file,'utf8');
 assert.match(content,/<html[^>]*lang="id"/,`${file}: language`);
 for(const [,attrs,body]of content.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/g)){
  if(/type="application\/(?:ld\+)?json"/.test(attrs))continue;
  assert.match(attrs,/src="\/_astro\//,`${file}: executable inline script would be blocked by production CSP`);
  assert.equal(body.trim(),'',`${file}: unexpected executable inline body`);
 }
 for(const [,url]of content.matchAll(/(?:href|src)="([^"\s]+)"/g)){
  if(!url.startsWith('/')||url.startsWith('//'))continue;
  const path=join(root,decodeURIComponent(url.split(/[?#]/)[0]));
  assert(existsSync(path),`${file}: missing local asset ${url}`);
  if(statSync(path).isDirectory())assert(existsSync(join(path,'index.html')),`${file}: missing page ${url}`);
 }
 for(const [,list]of content.matchAll(/srcset="([^"]+)"/g))for(const image of list.split(',')){const url=image.trim().split(/\s/)[0];if(url.startsWith('/'))assert(existsSync(join(root,url)),`${file}: missing responsive image ${url}`);}
}
console.log(`Static build audit passed: ${html.length} pages, CSP-compatible scripts, local links and responsive media.`);
