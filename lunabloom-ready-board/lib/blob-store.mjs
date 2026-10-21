import { get, put } from '@vercel/blob';
import { createHash, randomUUID } from 'node:crypto';
import { serviceDay, normalize, totals, change } from './board-model.mjs';
const PATH = 'lunabloom/board-v2.json';
const COOKIE = 'lunabloom_board_id';
function cookieValue(req) {
  const cookie = String(req.headers.cookie || '').split(';').map(x => x.trim()).find(x => x.startsWith(COOKIE+'='));
  return cookie?.slice(COOKIE.length+1);
}
export function visitor(req, res) {
  let id = cookieValue(req);
  if (!id || !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id)) {
    id = randomUUID();
    res.setHeader('Set-Cookie', COOKIE + '=' + id + '; Path=/; Max-Age=7776000; Secure; HttpOnly; SameSite=Lax');
  }
  return createHash('sha256').update('lunabloom-anonymous-v1:' + id).digest('hex');
}
export async function load() {
  const day = serviceDay();
  const result = await get(PATH, {access:'private', useCache:false});
  if (!result) return {day,etag:null,state:normalize(null,day)};
  if (result.statusCode !== 200 || !result.stream) throw new Error('blob_read_unavailable');
  const data = await new Response(result.stream).json();
  return {day,etag:result.blob.etag,state:normalize(data,day)};
}
export async function update(key, requested) {
  for (let attempt=0;attempt<8;attempt++) {
    const previous = await load();
    const result = change(previous.state,key,requested);
    if (!result.changed) return {day:previous.day,slots:previous.state.members[key]?.slots || [],rows:totals(previous.state)};
    try {
      await put(PATH, JSON.stringify(result.state), {
        access:'private', contentType:'application/json',
        ...(previous.etag ? {allowOverwrite:true,ifMatch:previous.etag} : {allowOverwrite:false}),
        cacheControlMaxAge:60
      });
      return {day:previous.day,slots:result.state.members[key]?.slots || [],rows:totals(result.state)};
    } catch (e) {
      // Competing writes are retried with a fresh ETag. Never silently lose a vote.
      if (!/Precondition|AlreadyExists|already exist|if-match|conflict|409|412/i.test(String(e?.name)+' '+String(e?.message))) throw e;
      await new Promise(resolve=>setTimeout(resolve,30+attempt*65));
    }
  }
  throw Object.assign(new Error('board_busy'),{status:503});
}
export function reply(res,code,body,headers={}) {
  res.statusCode=code;
  res.setHeader('Content-Type','application/json; charset=utf-8');
  res.setHeader('X-Content-Type-Options','nosniff');
  for(const [k,v] of Object.entries(headers)) res.setHeader(k,v);
  res.end(JSON.stringify(body));
}
export function fail(res,error) {
  const status=error?.status || 500;
  if(status===500) console.error('Lunabloom API:',error);
  return reply(res,status,{error:status===500?'temporarily_unavailable':error.message},{'Cache-Control':'no-store'});
}
