const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {pathToFileURL}=require('node:url');
const path=require('node:path');
(async()=>{
 const {serviceDay}=await import(pathToFileURL(path.resolve('lunabloom-ready-board/lib/core.mjs')).href);
 const day=serviceDay();
 const browser=await chromium.launch({headless:true,args:['--no-sandbox']});
 try{
  for(const width of [320,375,390,1440]){
   const page=await browser.newPage({viewport:{width,height:900},deviceScaleFactor:1});
   let chosen=[],writeCount=0;
   await page.route('**/api/**', async(route)=>{
    const url=new URL(route.request().url());
    const parts=url.pathname.split('/');const endpoint=parts.at(-1);
    const counts=Array(48).fill(0);chosen.forEach(slot=>counts[slot]++);
    const rows=counts.map((members,slot)=>({slot,members})).filter(x=>x.members);
    let body={day,rows};
    if(endpoint==='me')body={day,slots:chosen};
    if(endpoint==='slots'){
      writeCount++;
      const req=route.request().postDataJSON();
      if(req.day!==day)return route.fulfill({status:409,contentType:'application/json',body:JSON.stringify({error:'day_changed'})});
      if(req.slots.length>5)return route.fulfill({status:400,contentType:'application/json',body:JSON.stringify({error:'max_five'})});
      chosen=req.slots;
      body={day,rows:chosen.map(slot=>({slot,members:1})),slots:chosen};
    }
    await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
   });
   const jsErrors=[];
   page.on('pageerror',e=>jsErrors.push(e.message));
   await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
   await page.locator('.time-card:not([disabled])').first().waitFor({timeout:10000});
   await page.locator('#save-indicator').waitFor({state:'hidden',timeout:10000});
   assert.equal(await page.locator('[data-filter="pagi"]').getAttribute('aria-pressed'),'true','Pagi selected on open');
   assert.equal(await page.locator('.interaction-card').count(),0,'extra selection panel removed');
   assert.equal(await page.locator('#connection-pill').count(),0,'no always-on sync pill');
   assert.equal(await page.locator('.time-card').count(),12,'only Pagi slots visible by default');
   assert.equal(await page.locator('[data-filter="all"]').count(),0,'no Semua button');
   assert.equal(await page.locator('.time-friendly').count(),0,'no duplicate friendly clock labels');
   assert.equal(await page.getByText('Belum ada',{exact:true}).count(),0);
   assert.equal(await page.locator('[data-slot="0"] .more-count').innerText(),'0 member');
   assert.equal(await page.locator('[data-slot="0"] .member-dot:not(.filled)').count(),5,'five grey empty slots shown');
   assert.equal(await page.locator('#selection-strip').isVisible(),false,'chips hidden until selection');
   await page.locator('[data-slot="0"]').click();
   assert.equal(await page.locator('[data-slot="0"] .more-count').innerText(),'1 member','optimistic count appears on tap');
   assert.match(await page.locator('#save-indicator').innerText(),/Menyimpan/);
   assert.equal(await page.locator('[data-slot="0"] .member-dot.filled').count(),1,'grey turns green on vote');
   await page.waitForTimeout(2600);
   assert.equal(await page.locator('#save-indicator').isVisible(),false,'saving indicator disappears after successful save');
   assert.equal(writeCount,1,'single tap makes one write');
   assert.equal(await page.locator('[data-slot="0"] .more-count').innerText(),'1 member');
   await page.locator('[data-slot="0"]').click();
   assert.equal(await page.locator('[data-slot="0"] .more-count').innerText(),'0 member','tapping again updates instant count');
   await page.waitForTimeout(2600);
   assert.equal(chosen.length,0);
   for(const i of [1,2,3,4,5])await page.locator('[data-slot="'+i+'"]').click();
   assert.equal(await page.locator('#quota-count').innerText(),'5');
   assert.equal(await page.locator('[data-slot="1"] .more-count').innerText(),'1 member');
   assert.equal(await page.locator('[data-slot="6"] .more-count').innerText(),'0 member');
   await page.locator('[data-slot="6"]').click();
   assert.equal(await page.locator('#quota-count').innerText(),'5','sixth tap rejected');
   await page.waitForTimeout(2600);
   assert.equal(writeCount,3,'five fast slot taps grouped into one write');
   await page.locator('[data-filter="siang"]').click();
   assert.equal(await page.locator('.time-card').count(),8,'Siang shows only its own daypart');
   await page.locator('[data-filter="siang"]').click();
   assert.equal(await page.locator('.time-card').count(),8,'active tab cannot expose all time slots');
   await page.locator('[data-filter="malam"]').click();
   assert.equal(await page.locator('.time-card').count(),12,'Malam has its own 12 slots');
   await page.locator('[data-filter="pagi"]').click();
   assert.equal(await page.locator('.time-card').count(),12,'switch back to Pagi');
   assert.equal(await page.locator('.member-dot').count(),60,'five dots on 12 time cards');
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1),width+'px viewport has no horizontal overflow');
   assert.deepEqual(jsErrors,[],'no browser script errors');
   fs.mkdirSync('qa-screenshots',{recursive:true});
   await page.screenshot({path:'qa-screenshots/lunabloom-'+width+'.png',fullPage:true});
   console.log('PASS viewport '+width+'px; compact dayparts, five grey dots, save animation, batching');
   await page.close();
  }
 }finally{await browser.close();}
})().catch(err=>{console.error(err);process.exitCode=1});
