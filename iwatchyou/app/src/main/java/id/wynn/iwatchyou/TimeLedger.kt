package id.wynn.iwatchyou

/** Durations use Android's monotonic elapsedRealtime clock. */
class TimeLedger(initialDay: String, initialApps: Map<String, Long> = emptyMap(), initialScreen: Long = 0L) {
 var day: String = initialDay
  private set
 private val totals = initialApps.toMutableMap()
 private var screenCommitted = initialScreen.coerceAtLeast(0)
 private var screenStart: Long? = null
 private var foreground: String? = null
 private var appStart: Long? = null
 data class Snapshot(val day: String, val screenMs: Long, val apps: Map<String, Long>, val foreground: String?)
 fun screenOn(now: Long) { if (screenStart == null) screenStart = now }
 fun appChanged(name: String?, now: Long) {
  settleApp(now)
  foreground = if (screenStart != null) name else null
  appStart = if (foreground != null) now else null
 }
 fun screenOff(now: Long) {
  settleApp(now)
  screenStart?.let { screenCommitted += (now - it).coerceAtLeast(0) }
  screenStart = null
 }
 fun nextDay(newDay: String, now: Long) {
  if (day == newDay) return
  val wasOn = screenStart != null
  val previous = foreground
  screenOff(now)
  day = newDay
  totals.clear()
  screenCommitted = 0
  if (wasOn) { screenOn(now); appChanged(previous, now) }
 }
 fun snapshot(now: Long): Snapshot {
  val apps = totals.toMutableMap()
  val key = foreground
  val started = appStart
  if (key != null && started != null) apps[key] = (apps[key] ?: 0) + (now - started).coerceAtLeast(0)
  return Snapshot(day, screenCommitted + (screenStart?.let { (now - it).coerceAtLeast(0) } ?: 0), apps, foreground)
 }
 fun isScreenRunning() = screenStart != null
 fun checkpoint(now: Long) {
  val active = foreground
  val on = screenStart != null
  screenOff(now)
  if (on) { screenOn(now); appChanged(active, now) }
 }
 private fun settleApp(now: Long) {
  val key = foreground
  val started = appStart
  if (key != null && started != null) totals[key] = (totals[key] ?: 0) + (now - started).coerceAtLeast(0)
  foreground = null
  appStart = null
 }
}
