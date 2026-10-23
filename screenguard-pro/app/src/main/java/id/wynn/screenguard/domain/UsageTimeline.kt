package id.wynn.screenguard.domain

/** Foreground union per package: handles duplicate resumes, multi-activity, screen off,
 * shutdown (duration unknown), and clipping sessions crossing midnight. */
data class UsageEvent(val time: Long, val packageName: String, val activity: String, val kind: Kind) {
    enum class Kind { RESUME, PAUSE, SCREEN_ON, SCREEN_OFF, LOCK, UNLOCK, SHUTDOWN, STARTUP }
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
        var interactive = true
        var unlocked = true
        fun close(pkg: String, at: Long) {
            val start = started.remove(pkg) ?: return
            val duration = (at.coerceAtMost(until) - start.coerceAtLeast(from)).coerceAtLeast(0)
            if (duration > 0) sessions += UsageSession(pkg, start.coerceAtLeast(from), at.coerceAtMost(until), start)
            active.remove(pkg)
        }
        events.asSequence().filter { it.time <= until }.sortedBy { it.time }.forEach { e ->
            when (e.kind) {
                UsageEvent.Kind.RESUME -> {
                    if (!interactive || !unlocked) return@forEach
                    val activities = active.getOrPut(e.packageName) { mutableSetOf() }
                    if (activities.isEmpty()) started[e.packageName] = e.time
                    activities.add(e.activity)
                }
                UsageEvent.Kind.PAUSE -> {
                    active[e.packageName]?.remove(e.activity)
                    if (active[e.packageName]?.isEmpty() == true) close(e.packageName, e.time)
                }
                UsageEvent.Kind.SCREEN_ON -> interactive = true
                UsageEvent.Kind.UNLOCK -> { unlocked = true; interactive = true }
                UsageEvent.Kind.SCREEN_OFF, UsageEvent.Kind.LOCK -> {
                    active.keys.toList().forEach { close(it, e.time) }
                    if (e.kind == UsageEvent.Kind.SCREEN_OFF) interactive = false else unlocked = false
                }
                UsageEvent.Kind.SHUTDOWN, UsageEvent.Kind.STARTUP -> { active.clear(); started.clear(); interactive = true; unlocked = true }
            }
        }
        active.keys.toList().forEach { close(it, until) }
        return sessions
    }

    /** Split-screen foreground intervals must never double count physical screen time. */
    fun unionDuration(intervals: List<Pair<Long, Long>>): Long {
        var end = Long.MIN_VALUE
        var total = 0L
        for ((a, b) in intervals.filter { it.second > it.first }.sortedBy { it.first }) {
            total += (b - maxOf(a, end)).coerceAtLeast(0)
            end = maxOf(end, b)
        }
        return total
    }

    data class ScreenTime(val durationMs: Long, val estimated: Boolean, val unlocks: Int)

    /** Prefer screen/keyguard events; ROMs omitting these use an explicitly labelled union.
     * An unmatched shutdown tail is discarded: Android cannot give its actual closing time.
     */
    fun screenTime(events: List<UsageEvent>, from: Long, until: Long): ScreenTime {
        val ordered = events.filter { it.time <= until }.sortedBy { it.time }
        val display = ordered.any { it.kind == UsageEvent.Kind.SCREEN_ON || it.kind == UsageEvent.Kind.SCREEN_OFF }
        val unlocks = ordered.count { it.kind == UsageEvent.Kind.UNLOCK && it.time in from until until }
        if (!display) return ScreenTime(unionDuration(sessions(ordered, from, until).map { it.start to it.end }), true, unlocks)
        val intervals = mutableListOf<Pair<Long, Long>>()
        var interactive = false
        var unlocked = true
        var start: Long? = null
        fun close(at: Long) {
            start?.let { a -> if (minOf(at, until) > maxOf(a, from)) intervals += maxOf(a, from) to minOf(at, until) }
            start = null
        }
        for (e in ordered) {
            when (e.kind) {
                UsageEvent.Kind.SCREEN_ON -> interactive = true
                UsageEvent.Kind.SCREEN_OFF -> { close(e.time); interactive = false }
                UsageEvent.Kind.LOCK -> { close(e.time); unlocked = false }
                UsageEvent.Kind.UNLOCK -> { interactive = true; unlocked = true }
                UsageEvent.Kind.SHUTDOWN, UsageEvent.Kind.STARTUP -> { start = null; interactive = false; unlocked = true }
                else -> Unit
            }
            if (interactive && unlocked && start == null) start = e.time
        }
        close(until)
        return ScreenTime(unionDuration(intervals), false, unlocks)
    }
}
