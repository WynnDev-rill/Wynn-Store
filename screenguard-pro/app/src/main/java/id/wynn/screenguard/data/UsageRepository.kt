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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId

data class UsageDay(val date: LocalDate, val apps: Map<String, Long>, val estimated: Boolean = false) {
    val totalMs get() = apps.values.sum()
}

class UsageRepository(private val context: Context) {
    private val manager = context.getSystemService(UsageStatsManager::class.java)
    private val db = object : SQLiteOpenHelper(context, "usage.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE days (day TEXT PRIMARY KEY, zone TEXT NOT NULL, data TEXT NOT NULL, estimated INTEGER NOT NULL)") }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
    fun bootCount(): Int = android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, -1)
    fun hasAccess(): Boolean = context.getSystemService(AppOpsManager::class.java)
        .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED

    @Synchronized fun day(date: LocalDate = LocalDate.now()): UsageDay {
        if (!hasAccess()) return UsageDay(date, emptyMap())
        val zone = ZoneId.systemDefault()
        if (date.isBefore(LocalDate.now())) db.readableDatabase.rawQuery("SELECT data, estimated FROM days WHERE day=? AND zone=?", arrayOf(date.toString(), zone.id)).use {
            if (it.moveToFirst()) return UsageDay(date, Json.decodeFromString<Map<String, Long>>(it.getString(0)), it.getInt(1) != 0)
        }
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(System.currentTimeMillis(), date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        if (end <= start) return UsageDay(date, emptyMap())
        val recent = !date.isBefore(LocalDate.now().minusDays(2))
        val apps = if (recent) {
            val list = events(date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), end)
            UsageTimeline.calculate(list, start, end)
        } else manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()
            .filter { it.lastTimeUsed >= start && it.firstTimeStamp < end && it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }.mapValues { (_, records) -> records.sumOf { it.totalTimeInForeground } }
        val filtered = apps.filterKeys { it != context.packageName && it != "android" && it != "com.android.systemui" && !it.contains("launcher") }
        val result = UsageDay(date, filtered, !recent)
        if (date.isBefore(LocalDate.now())) {
            db.writableDatabase.execSQL("INSERT OR REPLACE INTO days(day,zone,data,estimated) VALUES (?,?,?,?)", arrayOf(date.toString(), zone.id, Json.encodeToString(filtered), if (recent) 0 else 1))
            db.writableDatabase.execSQL("DELETE FROM days WHERE day < ?", arrayOf(LocalDate.now().minusDays(90).toString()))
        }
        return result
    }
    private fun events(from: Long, until: Long): List<UsageEvent> {
        val source = manager.queryEvents(from, until)
        val list = mutableListOf<UsageEvent>()
        val e = UsageEvents.Event()
        while (source != null && source.hasNextEvent()) {
            source.getNextEvent(e)
            val kind = when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Kind.RESUME
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> UsageEvent.Kind.PAUSE
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEvent.Kind.SCREEN_OFF
                UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEvent.Kind.SHUTDOWN
                UsageEvents.Event.DEVICE_STARTUP -> UsageEvent.Kind.STARTUP
                else -> null
            }
            if (kind != null) list += UsageEvent(e.timeStamp, e.packageName.orEmpty(), e.className.orEmpty(), kind)
        }
        return list
    }
    fun details(pkg: String, date: LocalDate): AppDayDetail {
        if (!hasAccess() || date.isBefore(LocalDate.now().minusDays(2))) return AppDayDetail(date)
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone)
        val until = minOf(System.currentTimeMillis(), date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        val from = start.toInstant().toEpochMilli()
        if (until <= from) return AppDayDetail(date, eventsAvailable = true)
        val sessions = UsageTimeline.sessions(events(start.minusDays(1).toInstant().toEpochMilli(), until), from, until).filter { it.packageName == pkg }
        val hourly = MutableList(24) { 0L }
        var hour = start
        while (hour.toInstant().toEpochMilli() < until) {
            val a = hour.toInstant().toEpochMilli()
            val b = minOf(hour.plusHours(1).toInstant().toEpochMilli(), until)
            hourly[hour.hour] += sessions.sumOf { (minOf(it.end, b) - maxOf(it.start, a)).coerceAtLeast(0) }
            hour = hour.plusHours(1)
        }
        return AppDayDetail(date, hourly, sessions.count { it.openedAt >= from }, sessions.maxOfOrNull { it.end - it.start } ?: 0,
            sessions.isNotEmpty() || day(date).apps.getOrDefault(pkg, 0) == 0L)
    }
    fun range(count: Int): List<UsageDay> = (count.coerceIn(1, 30) - 1 downTo 0).map { day(LocalDate.now().minusDays(it.toLong())) }
}

data class AppDayDetail(val date: LocalDate, val hourly: List<Long> = List(24) { 0L }, val opens: Int = 0,
    val longestMs: Long = 0, val eventsAvailable: Boolean = false)
