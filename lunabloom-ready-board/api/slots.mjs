import {visitor,update,reply,fail} from '../lib/blob-store.mjs';
import {slotsFrom,serviceDay} from '../lib/board-model.mjs';
export default async function handler(req,res) {
  if(req.method!=='POST') return reply(res,405,{error:'method_not_allowed'},{Allow:'POST'});
  if((req.headers['content-type']||'').split(';')[0].trim()!=='application/json') return reply(res,415,{error:'json_required'});
  if(Number(req.headers['content-length']||0)>1500) return reply(res,413,{error:'payload_too_large'});
  try {
    const origin=req.headers.origin;
    if(origin) {
      let expected;
      try{expected=new URL(origin).host;}catch{return reply(res,403,{error:'origin_not_allowed'});}
      if(expected!==req.headers.host) return reply(res,403,{error:'origin_not_allowed'});
    }
    let body;
    try {body=typeof req.body==='string'?JSON.parse(req.body):req.body;}
    catch{return reply(res,400,{error:'invalid_json'});}
    if(!body||typeof body!=='object'||Array.isArray(body))return reply(res,400,{error:'invalid_json'});
    // prevents pending late-night writes from silently carrying into the next service day
    if(body.day!==serviceDay())return reply(res,409,{error:'day_changed'});
    const slots=slotsFrom(body.slots);
    const key=visitor(req,res);
    const result=await update(key,slots);
    if(result.day!==body.day)return reply(res,409,{error:'day_changed'});
    return reply(res,200,result,{'Cache-Control':'no-store'});
  }catch(e){return fail(res,e);}
}
