import {load,reply,fail} from '../lib/blob-store.mjs';
import {totals} from '../lib/board-model.mjs';
export default async function handler(req,res) {
  if(req.method!=='GET') return reply(res,405,{error:'method_not_allowed'},{Allow:'GET'});
  try {
    const {day,state}=await load();
    return reply(res,200,{day,rows:totals(state)},{'Cache-Control':'public, max-age=0, s-maxage=60, stale-while-revalidate=15'});
  } catch(error) { return fail(res,error); }
}
