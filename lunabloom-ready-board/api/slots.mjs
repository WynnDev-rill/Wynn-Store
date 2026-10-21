import {visitor,update,reply,fail} from '../lib/blob-store.mjs';
import {slotsFrom} from '../lib/board-model.mjs';
export default async function handler(req,res) {
  if(req.method!=='POST') return reply(res,405,{error:'method_not_allowed'},{Allow:'POST'});
  if(req.headers['content-type']?.split(';')[0] !== 'application/json') return reply(res,415,{error:'json_required'});
  const length=Number(req.headers['content-length'] || 0);
  if(length>1500) return reply(res,413,{error:'payload_too_large'});
  try {
    const origin=req.headers.origin;
    if(origin && new URL(origin).host!==req.headers.host) return reply(res,403,{error:'origin_not_allowed'});
    const key=visitor(req,res);
    const body=typeof req.body==='string'?JSON.parse(req.body):req.body;
    const slots=slotsFrom(body?.slots);
    const result=await update(key,slots);
    return reply(res,200,result,{'Cache-Control':'no-store'});
  } catch(error) { return fail(res,error); }
}
