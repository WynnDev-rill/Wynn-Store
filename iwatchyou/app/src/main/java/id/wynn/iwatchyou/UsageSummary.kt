package id.wynn.iwatchyou

object UsageSummary {
 fun combine(days: List<TimeLedger.Snapshot>,end: String): TimeLedger.Snapshot {
  val apps=mutableMapOf<String,Long>()
  days.forEach { day -> day.apps.forEach { (pkg,time) -> apps[pkg]=(apps[pkg] ?: 0)+time } }
  return TimeLedger.Snapshot(end,days.sumOf { it.screenMs },apps,null)
 }
}
