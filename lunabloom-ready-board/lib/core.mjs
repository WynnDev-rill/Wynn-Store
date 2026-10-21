export const MAX_SLOTS = 5;
export const MAX_MEMBERS = 35;
export const SLOT_MINUTES = 30;
export const SLOT_COUNT = 48;
export const DAYPARTS = [
  {id:'pagi',name:'Pagi',icon:'☀',start:0,end:11},
  {id:'siang',name:'Siang',icon:'☼',start:12,end:19},
  {id:'sore',name:'Sore',icon:'◕',start:20,end:25},
  {id:'malam',name:'Malam',icon:'☾',start:26,end:37},
  {id:'dini',name:'Dini hari',icon:'✧',start:38,end:47},
];
export function slotClock(index){
  if(!Number.isInteger(index)||index<0||index>=SLOT_COUNT) throw new RangeError('Slot tidak valid');
  const minute=(5*60+index*SLOT_MINUTES)%(24*60);
  return {hour:Math.floor(minute/60),minute:minute%60};
}
export function slotLabel(index){const {hour,minute}=slotClock(index);return `${String(hour).padStart(2,'0')}.${String(minute).padStart(2,'0')}`}
export function slotFriendly(index){const {hour,minute}=slotClock(index);const h=hour%12||12;const part=hour<5?'dini hari':hour<11?'pagi':hour<15?'siang':hour<18?'sore':'malam';return `${h}${minute?`.${String(minute).padStart(2,'0')}`:''} ${part}`}
export function slotDaypart(index){return DAYPARTS.find(p=>index>=p.start&&index<=p.end)}
export function jakartaParts(time = new Date()){const parts=new Intl.DateTimeFormat('en-GB',{timeZone:'Asia/Jakarta',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).formatToParts(time);return Object.fromEntries(parts.filter(p=>p.type!=='literal').map(p=>[p.type,+p.value]));}
export function serviceDay(time = new Date()) {const p=jakartaParts(time);const date=new Date(Date.UTC(p.year,p.month-1,p.day));if(p.hour<5)date.setUTCDate(date.getUTCDate()-1);return date.toISOString().slice(0,10)}
export function serviceDayLabel(day){return new Intl.DateTimeFormat('id-ID',{weekday:'long',day:'numeric',month:'long',year:'numeric',timeZone:'UTC'}).format(new Date(`${day}T12:00:00Z`));}
export function daySlotCounts(rows){const counts=Array(SLOT_COUNT).fill(0);for(const r of rows||[]){const slot=Number(r.slot);const n=Number(r.members??r.member_count??r.count);if(Number.isInteger(slot)&&slot>=0&&slot<SLOT_COUNT&&Number.isFinite(n))counts[slot]=Math.max(0,Math.min(MAX_MEMBERS,n));}return counts}
export function peakSlot(counts){let max=0, index=-1;counts.forEach((v,i)=>{if(v>max){max=v;index=i}});return {index,count:max}}
export function toggleSlot(slots,index){if(!Number.isInteger(index)||index<0||index>=SLOT_COUNT)return{ok:false,reason:'invalid',slots};if(slots.includes(index))return{ok:true,slots:slots.filter(i=>i!==index)};if(slots.length>=MAX_SLOTS)return{ok:false,reason:'limit',slots};return{ok:true,slots:[...slots,index].sort((a,b)=>a-b)}}
export function demoCounts(){return Array.from({length:SLOT_COUNT},(_,i)=>{const h=slotClock(i).hour;if(h>=19&&h<22)return [5,6,9,8,7,8][(i-28+6)%6];if(h>=15&&h<19)return 2+(i%4);if(h>=11&&h<15)return 1+(i%3);if(h>=22||h<2)return 1+(i%4);return i%6===0?1:0;});}
