
export function sameSlots(a,b) {
  return a.length===b.length && a.every((x,i)=>x===b[i]);
}
export function displayCounts(base,confirmed,selected) {
  const counts=base.slice();
  for(const i of confirmed) if(!selected.includes(i)) counts[i]=Math.max(0,(counts[i]||0)-1);
  for(const i of selected) if(!confirmed.includes(i)) counts[i]=Math.min(35,(counts[i]||0)+1);
  // Stale public cache must never hide the visitor's confirmed slot completely.
  for(const i of selected) if(confirmed.includes(i)) counts[i]=Math.max(1,counts[i]||0);
  return counts;
}
export function dotCount(count) { return Math.min(5,Math.max(0,count)); }
export function memberLabel(count) { return String(count)+' member'; }
