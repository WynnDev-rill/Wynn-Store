export const LIMIT = 5;
export const CAPACITY = 35;
export function serviceDay(now = Date.now()) {
  // 05:00 in Asia/Jakarta corresponds to 22:00 UTC on the previous day.
  return new Date(now + 2 * 3600000).toISOString().slice(0, 10);
}
export function slotsFrom(value) {
  if (!Array.isArray(value) || value.length > LIMIT || value.some(n => !Number.isInteger(n) || n < 0 || n > 47)) {
    throw Object.assign(new Error('invalid_slots'), { status: 400 });
  }
  const unique = [...new Set(value)].sort((a, b) => a - b);
  if (unique.length !== value.length) throw Object.assign(new Error('duplicate_slots'), { status: 400 });
  return unique;
}
export function normalize(raw, day) {
  if (!raw || raw.day !== day || typeof raw.members !== 'object' || Array.isArray(raw.members)) return {day, members: {}};
  const members = {};
  for (const [key, value] of Object.entries(raw.members).slice(0, CAPACITY)) {
    if (!/^[a-f0-9]{64}$/.test(key)) continue;
    try {
      const slots = slotsFrom(value?.slots);
      if (slots.length) members[key] = { slots };
    } catch { /* Ignore malformed stored records */ }
  }
  return {day, members};
}
export function totals(state) {
  const counts = new Array(48).fill(0);
  for (const member of Object.values(state.members)) for (const slot of member.slots) counts[slot]++;
  return counts.map((members, slot) => ({slot, members})).filter(row => row.members > 0);
}
export function change(state, key, slots) {
  slots = slotsFrom(slots);
  const previous = state.members[key]?.slots || [];
  if (JSON.stringify(previous) === JSON.stringify(slots)) return {state, changed:false};
  if (!previous.length && slots.length && Object.keys(state.members).length >= CAPACITY) {
    throw Object.assign(new Error('board_full'), {status:409});
  }
  const members = {...state.members};
  if (slots.length) members[key] = {slots};
  else delete members[key];
  return {state:{day:state.day,members},changed:true};
}
