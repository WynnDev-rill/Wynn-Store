package id.wynn.iwatchyou

import java.time.Instant
import java.time.ZoneId

/** One focused app at a time. All elapsed durations use the monotonic clock. */
class TimeLedger(
 initialDay: String,
 initialApps: Map<String, Long> = emptyMap(),
 initialScreen: Long = 0L,
 initialHours: List<Long> = List(24) { 0L },
 private var wallOffset: Long = 0L,
 private var zone: ZoneId = ZoneId.systemDefault()
) {
 var day = initialDay
  private set
 private val totals = initialApps.filterValues { it > 0 }.toMutableMap()
 private val hours = LongArray(24) { initialHours.getOrElse(it) { 0L }.coerceAtLeast(0) }
 private var screenCommitted = initialScreen.coerceAtLeast(0)
 private var screenStart: Long? = null
 private var foreground: String? = null
 private var appStart: Long? = null
 private var lastEvent = 0L
 data class Snapshot(val day: String, val screenMs: Long, val apps: Map<String, Long>, val foreground: String?, val hours: List<Long> = List(24) { 0L })
 private fun time(now: Long): Long = now.coerceAtLeast(lastEvent).also { lastEvent = it }
 @Synchronized fun screenOn(now: Long) { val t=time(now); if (screenStart == null) screenStart=t }
 @Synchronized fun appChanged(name: String?, now: Long) {
  val t=time(now)
  if (name == foreground && (name == null || appStart != null)) return
  settleApp(t)
  foreground=if (screenStart!=null) name else null
  appStart=if (foreground!=null) t else null
 }
 @Synchronized fun screenOff(now: Long) {
  val t=time(now)
  settleApp(t)
  screenStart?.let { start -> screenCommitted+=t-start; addHours(hours,start,t) }
  screenStart=null
 }
 @Synchronized fun nextDay(newDay: String, now: Long, restored: Snapshot? = null): Snapshot {
  val t=time(now)
  val previous=snapshot(t)
  val on=screenStart!=null
  val app=foreground
  screenOff(t)
  day=newDay
  totals.clear(); totals.putAll(restored?.apps ?: emptyMap())
  screenCommitted=restored?.screenMs ?: 0L
  for (i in 0..23) hours[i]=restored?.hours?.getOrElse(i) { 0L } ?: 0L
  if (on) { screenOn(t); appChanged(app,t) }
  return previous
 }
 @Synchronized fun snapshot(now: Long): Snapshot {
  val t=now.coerceAtLeast(lastEvent)
  val apps=totals.toMutableMap()
  val key=foreground
  val start=appStart
  if (key!=null && start!=null) apps[key]=(apps[key] ?: 0L)+(t-start)
  val h=hours.copyOf()
  screenStart?.let { addHours(h,it,t) }
  return Snapshot(day,screenCommitted+(screenStart?.let { t-it } ?: 0L),apps,foreground,h.toList())
 }
 @Synchronized fun isScreenRunning()=screenStart!=null
 @Synchronized fun checkpoint(now: Long) {
  val t=time(now); val on=screenStart!=null; val app=foreground
  screenOff(t)
  if (on) { screenOn(t); appChanged(app,t) }
 }
 @Synchronized fun reanchor(now: Long, wall: Long, newZone: ZoneId) {
  checkpoint(now); wallOffset=wall-now; zone=newZone
 }
 private fun settleApp(now: Long) {
  foreground?.let { key -> appStart?.let { totals[key]=(totals[key] ?: 0L)+(now-it) } }
  foreground=null; appStart=null
 }
 private fun addHours(target: LongArray, start: Long, end: Long) {
  var cursor=start
  while (cursor<end) {
   val instant=Instant.ofEpochMilli(cursor+wallOffset)
   val local=instant.atZone(zone)
   // Instant arithmetic also handles repeated/skipped DST hours without a loop at midnight.
   val next=local.withMinute(0).withSecond(0).withNano(0).toInstant().plusSeconds(3600).toEpochMilli()-wallOffset
   val until=minOf(end,maxOf(cursor+1,next))
   target[local.hour]+=until-cursor
   cursor=until
  }
 }
}
