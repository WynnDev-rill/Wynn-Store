package id.wynn.screenguard

import android.app.AppOpsManager
import android.app.usage.UsageEvents.Event
import android.app.usage.UsageStatsManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Process
import id.wynn.screenguard.data.UsageRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowUsageStatsManager.EventBuilder
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class UsageRepositoryAccuracyTest {
    private lateinit var context: Context
    private val yesterday get() = LocalDate.now().minusDays(1)
    private val start get() = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    @Before fun prepare() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("usage.db")
        shadowOf(context.getSystemService(AppOpsManager::class.java)).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, AppOpsManager.MODE_ALLOWED)
    }
    private fun event(at: Long, type: Int, pkg: String = "com.example.test") {
        shadowOf(context.getSystemService(UsageStatsManager::class.java)).addEvent(EventBuilder.buildEvent()
            .setPackage(pkg).setClass("Main").setTimeStamp(at).setEventType(type).build())
    }
    @Test fun legacyInflatedAggregateIsNeverPresentedAsAnExactDay() {
        val file = context.getDatabasePath("usage.db"); file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
            db.execSQL("CREATE TABLE days(day TEXT PRIMARY KEY,zone TEXT,data TEXT,estimated INTEGER)")
            db.execSQL("INSERT INTO days VALUES (?,?,?,?)", arrayOf(yesterday.toString(),ZoneId.systemDefault().id,"{\"com.example.test\":999999999}",0))
            db.version=1
        }
        val result = UsageRepository(context).day(yesterday)
        assertFalse(result.available); assertTrue(result.apps.isEmpty())
        SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM days",null).use { it.moveToFirst();assertEquals(1,it.getInt(0)) }
        }
    }
    @Test fun dailyAndHourlyTotalsUseTheSameBoundedSessionsAndSurviveRecreation() {
        event(start-1000,Event.SCREEN_NON_INTERACTIVE)
        event(start+1000,Event.SCREEN_INTERACTIVE)
        event(start+2000,Event.ACTIVITY_RESUMED)
        event(start+62000,Event.ACTIVITY_PAUSED)
        event(start+70000,Event.SCREEN_NON_INTERACTIVE)
        val repo=UsageRepository(context)
        val day=repo.day(yesterday)
        assertTrue(day.available);assertFalse(day.estimated)
        assertEquals(60000L,day.apps["com.example.test"])
        assertEquals(69000L,day.totalMs)
        assertEquals(60000L,repo.details("com.example.test",yesterday).hourly.sum())
        assertEquals(day,UsageRepository(context).day(yesterday))
    }
    @Test fun revokedUsageAccessShowsUnknownInsteadOfOldStatistics() {
        event(start+1000,Event.ACTIVITY_RESUMED)
        event(start+62000,Event.ACTIVITY_PAUSED)
        val repo=UsageRepository(context); assertTrue(repo.day(yesterday).available)
        shadowOf(context.getSystemService(AppOpsManager::class.java)).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, AppOpsManager.MODE_IGNORED)
        assertFalse(repo.day(yesterday).available);assertTrue(repo.day(yesterday).apps.isEmpty())
    }
}
