package id.wynn.screenguard.domain

/** Foreground union per package: handles duplicate resumes, multi-activity, screen off,
 * shutdown (duration unknown), and clipping sessions crossing midnight. */
data class UsageEvent(val time: Long, val packageName: String, val activity: String, val kind: Kind) {
    enum class Kind { RESUME, PAUSE, SCREEN_OFF, SHUTDOWN, STARTUP }
}
object UsageTimeline {
    fun calculate(events: List<UsageEvent>, from: Long, until: Long): Map<String, Long> {
        require(until >= from)
        val active = mutableMapOf<String, MutableSet<String>>()
        val started = mutableMapOf<String, Long>()
        val totals = mutableMapOf<String, Long>()
        fun close(pkg: String, at: Long) {
            val start = started.remove(pkg) ?: return
            val duration = (at.coerceAtMost(until) - start.coerceAtLeast(from)).coerceAtLeast(0)
            totals[pkg] = totals.getOrDefault(pkg, 0) + duration
            active.remove(pkg)
        }
        events.asSequence().filter { it.time <= until }.sortedBy { it.time }.forEach { e ->
            when (e.kind) {
                UsageEvent.Kind.RESUME -> {
                    val activities = active.getOrPut(e.packageName) { mutableSetOf() }
                    if (activities.isEmpty()) started[e.packageName] = e.time
                    activities.add(e.activity)
                }
                UsageEvent.Kind.PAUSE -> {
                    active[e.packageName]?.remove(e.activity)
                    if (active[e.packageName]?.isEmpty() == true) close(e.packageName, e.time)
                }
                UsageEvent.Kind.SCREEN_OFF -> active.keys.toList().forEach { close(it, e.time) }
                UsageEvent.Kind.SHUTDOWN, UsageEvent.Kind.STARTUP -> { active.clear(); started.clear() }
            }
        }
        active.keys.toList().forEach { close(it, until) }
        return totals.filterValues { it > 0 }
    }
}
