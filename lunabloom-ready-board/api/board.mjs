import {load,reply,fail} from '../lib/blob-store.mjs';
import {totals,serviceDay} from '../lib/board-model.mjs';
// Per-function-instance memory cache. A public CDN TTL also avoids repeated Blob reads.
let cached = {day:'',expires:0,rows:[]};
function ttlUntilReset(){
  const next = Date.parse(serviceDay()+'T00:00:00Z') + 22*3600000; // next 05:00 WIB
  return Math.max(0, Math.floor((next-Date.now())/1000));
}
export default async function handler(req,res){
  if(req.method!=='GET') return reply(res,405,{error:'method_not_allowed'},{Allow:'GET'});
  try {
    const day=serviceDay();
    let rows;
    if(cached.day===day&&Date.now()<cached.expires) rows=cached.rows;
    else {
      const snapshot=await load();
      rows=totals(snapshot.state);
      cached={day:snapshot.day,rows,expires:Date.now()+20000};
    }
    const ttl=Math.min(40,ttlUntilReset());
    return reply(res,200,{day,rows},{
      'Cache-Control':'public, max-age=0, must-revalidate',
      'Vercel-CDN-Cache-Control':'public, max-age='+ttl,
      'CDN-Cache-Control':'public, max-age='+ttl
    });
  } catch(e){return fail(res,e);}
}
