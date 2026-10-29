package id.wynn.iwatchyou

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Invoked explicitly before and after installation; uses each installed APK's real store. */
@RunWith(AndroidJUnit4::class)
class UpgradeHistoryFixture {
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 @Test fun seedOlderInstallation() {
  HistoryStore(context).use { store ->
   store.write(TimeLedger.Snapshot("2026-01-02",120_000,mapOf("upgrade.fixture" to 90_000L),null,List(24){if(it==0)120_000L else 0L}))
  }
 }
 @Test fun verifyUpgradedInstallation() {
  HistoryStore(context).use { store ->
   val saved=store.read("2026-01-02")
   assertNotNull("The pre-update day still exists",saved)
   assertEquals(120_000L,saved!!.screenMs)
   assertEquals(mapOf("upgrade.fixture" to 90_000L),saved.apps)
   assertEquals(List(24){if(it==0)120_000L else 0L},saved.hours)
   assertTrue(store.days().none { it<"2026-01-02" })
  }
 }
}
