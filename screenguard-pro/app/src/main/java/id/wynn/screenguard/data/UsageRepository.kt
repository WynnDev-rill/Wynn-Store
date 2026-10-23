package id.wynn.screenguard.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Process
import id.wynn.screenguard.domain.UsageEvent
import id.wynn.screenguard.domain.UsageTimeline
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId

data class UsageDay(val date: LocalDate, val apps: Map<String, Long>, val estimated: Boolean = false,
    val screenMs: Long? = null, val available: Boolean = true, val screenEstimated: Boolean = false,
    val unlocks: Int = 0) {
    val totalMs get() = screenMs ?: apps.values.sum()
    val appTotalMs get() = apps.values.sum()
}

@Serializable private data class DaySnapshot(val apps: Map<String, Long>, val screenMs: Long,
    val estimated: Boolean, val screenEstimated: Boolean, val unlocks: Int,
    val hourly: Map<String, List<Long>>, val sessions: Map<String, Int>, val longest: Map<String, Long>)

/** Only event-derived snapshots are used. Legacy aggregates remain on disk, never relabelled
 * as exact daily data: queryUsageStats may expand a requested day to whole interval buckets.
 */
class UsageRepository(private val context: Context) {
    private val manager = context.getSystemService(UsageStatsManager::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private fun createSnapshots(db: SQLiteDatabase) = db.execSQL("CREATE TABLE IF NOT EXISTS snapshots_v2 (day TEXT NOT NULL, zone TEXT NOT NULL, data TEXT NOT NULL, PRIMARY KEY(day,zone))")
    private val db = object : SQLiteOpenHelper(context, "usage.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) { createSnapshots(db) }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { createSnapshots(db) }
    }
    private var cachedDays = emptyList<UsageDay>()
    private var cachedDetails = emptyMap<Pair<LocalDate, String>, AppDayDetail>()
    private var loadedAt = 0L
    private var loadedDate: LocalDate? = null
    private var loadedZone: String? = null
    fun bootCount(): Int = android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, -1)
    fun hasAccess(): Boolean = context.getSystemService(AppOpsManager::class.java)
        .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    @Synchronized fun invalidate() { loadedAt = 0 }
    @Synchronized fun day(date: LocalDate = LocalDate.now()): UsageDay {
        load()
        return cachedDays.find { it.date == date } ?: UsageDay(date, emptyMap(), available = false)
    }
    @Synchronized fun range(count: Int): List<UsageDay> { load(); return cachedDays.takeLast(count.coerceIn(1,30)) }
    @Synchronized fun details(pkg: String, date: LocalDate): AppDayDetail {
        load()
        return cachedDetails[date to pkg] ?: AppDayDetail(date, eventsAvailable = day(date).available)
    }
    private fun readSnapshot(date: LocalDate, zone: String): DaySnapshot? = db.readableDatabase.rawQuery(
        "SELECT data FROM snapshots_v2 WHERE day=? AND zone=?", arrayOf(date.toString(), zone)).use { cursor ->
        if (!cursor.moveToFirst()) null else runCatching { json.decodeFromString<DaySnapshot>(cursor.getString(0)) }.getOrNull()
    }
    private fun load() {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        if (!hasAccess()) {
            cachedDays = (29 downTo 0).map { UsageDay(today.minusDays(it.toLong()), emptyMap(), available = false) }
            cachedDetails = emptyMap(); loadedAt = 0; loadedDate = null
            return
        }
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        if (loadedAt > 0 && nowElapsed - loadedAt < 5_000 && loadedDate == today && loadedZone == zone.id) return
        val now = System.currentTimeMillis()
        val warm = cachedDays.isNotEmpty() && loadedDate == today && loadedZone == zone.id
        val queryStart = today.minusDays(if (warm) 2 else 30).atStartOfDay(zone).toInstant().toEpochMilli()
        val events = events(queryStart, now)
        val earliest = events.minOfOrNull { it.time }
        val days = mutableListOf<UsageDay>()
        val details = if (loadedZone == zone.id) cachedDetails.toMutableMap() else mutableMapOf()
        for (offset in 29 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val until = minOf(now, date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
            val stored = readSnapshot(date, zone.id)
            val inRange = events.any { it.time in from until until }
            val canRebuild = until > from && (offset <= 1 || stored == null) && (inRange || earliest != null && earliest <= from)
            val snapshot = if (canRebuild) {
                val relevant = events.filter { it.time >= date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() && it.time <= until }
                val sessions = UsageTimeline.sessions(relevant, from, until).filter { it.packageName.isNotBlank() && it.packageName != "android" && it.packageName != "com.android.systemui" }
                val grouped = sessions.groupBy { it.packageName }
                val screen = UsageTimeline.screenTime(relevant, from, until)
                val hourly = grouped.mapValues { (_, list) ->
                    val hours = MutableList(24) { 0L }
                    var hour = date.atStartOfDay(zone)
                    while (hour.toInstant().toEpochMilli() < until) {
                        val a = hour.toInstant().toEpochMilli()
                        val b = minOf(hour.plusHours(1).toInstant().toEpochMilli(), until)
                        hours[hour.hour] += list.sumOf { (minOf(it.end,b) - maxOf(it.start,a)).coerceAtLeast(0) }
                        hour = hour.plusHours(1)
                    }
                    hours.toList()
                }
                DaySnapshot(grouped.mapValues { (_,list) -> list.sumOf { it.end-it.start } }, screen.durationMs,
                    earliest == null || earliest > from, screen.estimated, screen.unlocks, hourly,
                    grouped.mapValues { (_,list) -> list.count { it.openedAt >= from } },
                    grouped.mapValues { (_,list) -> list.maxOfOrNull { it.end-it.start } ?: 0 })
            } else stored
            if (snapshot == null) { days += UsageDay(date, emptyMap(), available = false); continue }
            days += UsageDay(date, snapshot.apps, snapshot.estimated, snapshot.screenMs, true, snapshot.screenEstimated, snapshot.unlocks)
            for (pkg in snapshot.apps.keys) details[date to pkg] = AppDayDetail(date, snapshot.hourly[pkg] ?: List(24) { 0L },
                snapshot.sessions[pkg] ?: 0, snapshot.longest[pkg] ?: 0, true)
            if (canRebuild && snapshot != stored) db.writableDatabase.execSQL("INSERT OR REPLACE INTO snapshots_v2(day,zone,data) VALUES (?,?,?)",
                arrayOf(date.toString(), zone.id, json.encodeToString(snapshot)))
        }
        db.writableDatabase.execSQL("DELETE FROM snapshots_v2 WHERE day < ?", arrayOf(today.minusDays(90).toString()))
        cachedDays = days; cachedDetails = details.filterKeys { !it.first.isBefore(today.minusDays(30)) }
        loadedAt = nowElapsed; loadedDate = today; loadedZone = zone.id
    }
    private fun events(from: Long, until: Long): List<UsageEvent> {
        val source = manager.queryEvents(from, until) ?: return emptyList()
        val list = mutableListOf<UsageEvent>()
        val e = UsageEvents.Event()
        while (source.hasNextEvent()) {
            source.getNextEvent(e)
            val kind = when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Kind.RESUME
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> UsageEvent.Kind.PAUSE
                UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEvent.Kind.SCREEN_ON
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEvent.Kind.SCREEN_OFF
                UsageEvents.Event.KEYGUARD_SHOWN -> UsageEvent.Kind.LOCK
                UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEvent.Kind.UNLOCK
                UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEvent.Kind.SHUTDOWN
                UsageEvents.Event.DEVICE_STARTUP -> UsageEvent.Kind.STARTUP
                else -> null
            }
            if (kind != null) list += UsageEvent(e.timeStamp, e.packageName.orEmpty(), e.className.orEmpty(), kind)
        }
        return list
    }
}

data class AppDayDetail(val date: LocalDate, val hourly: List<Long> = List(24) { 0L }, val opens: Int = 0,
    val longestMs: Long = 0, val eventsAvailable: Boolean = false)
