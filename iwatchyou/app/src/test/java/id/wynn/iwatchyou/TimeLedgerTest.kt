package id.wynn.iwatchyou
import org.junit.Assert.*
import org.junit.Test
class TimeLedgerTest {
 @Test fun backgroundAppsAreNotCounted() {
  val t = TimeLedger("24")
  t.screenOn(100)
  t.appChanged("tiktok", 200)
  t.appChanged("music", 1200)
  t.appChanged(null, 1700)
  assertEquals(1000L, t.snapshot(1900).apps["tiktok"])
  assertEquals(500L, t.snapshot(1900).apps["music"])
  assertEquals(1800L, t.snapshot(1900).screenMs)
 }
 @Test fun resumingDoesNotRestartCounter() {
  val t = TimeLedger("24")
  t.screenOn(100); t.appChanged("game", 100); t.screenOff(1100)
  t.screenOn(10000); t.appChanged("game", 10000)
  assertEquals(2000L, t.snapshot(11000).apps["game"])
  assertEquals(2000L, t.snapshot(11000).screenMs)
 }
 @Test fun rolloverClearsTodayAndStartsAgain() {
  val t = TimeLedger("24")
  t.screenOn(0); t.appChanged("video", 0); t.nextDay("25", 5000)
  assertEquals(0L, t.snapshot(5000).apps["video"])
  assertEquals(1500L, t.snapshot(6500).apps["video"])
 }
 @Test fun checkpointDoesNotDoubleCount() {
  val t = TimeLedger("24")
  t.screenOn(0); t.appChanged("app", 0); t.checkpoint(500)
  assertEquals(1000L, t.snapshot(1000).apps["app"])
 }
}
