import {load,visitor,reply,fail} from '../lib/blob-store.mjs';
export default async function handler(req,res) {
  if(req.method!=='GET') return reply(res,405,{error:'method_not_allowed'},{Allow:'GET'});
  try {
    const key=visitor(req,res);
    const {day,state}=await load();
    return reply(res,200,{day,slots:state.members[key]?.slots || []},{'Cache-Control':'no-store'});
  } catch(error) { return fail(res,error); }
}
