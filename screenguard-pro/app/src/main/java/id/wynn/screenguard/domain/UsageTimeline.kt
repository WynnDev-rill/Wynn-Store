package id.wynn.screenguard.domain

/** Foreground union per package: handles duplicate resumes, multi-activity, screen off,
 * shutdown (duration unknown), and clipping sessions crossing midnight. */
data class UsageEvent(val time: Long, val packageName: String, val activity: String, val kind: Kind) {
    enum class Kind { RESUME, PAUSE, SCREEN_OFF, SHUTDOWN, STARTUP }
}
data class UsageSession(val packageName: String, val start: Long, val end: Long, val openedAt: Long)

object UsageTimeline {
    fun calculate(events: List<UsageEvent>, from: Long, until: Long): Map<String, Long> =
        sessions(events, from, until).groupBy { it.packageName }.mapValues { (_, sessions) -> sessions.sumOf { it.end - it.start } }

    fun sessions(events: List<UsageEvent>, from: Long, until: Long): List<UsageSession> {
        require(until >= from)
        val active = mutableMapOf<String, MutableSet<String>>()
        val started = mutableMapOf<String, Long>()
        val sessions = mutableListOf<UsageSession>()
        fun close(pkg: String, at: Long) {
            val start = started.remove(pkg) ?: return
            val duration = (at.coerceAtMost(until) - start.coerceAtLeast(from)).coerceAtLeast(0)
            if (duration > 0) sessions += UsageSession(pkg, start.coerceAtLeast(from), at.coerceAtMost(until), start)
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
        return sessions
    }
}
