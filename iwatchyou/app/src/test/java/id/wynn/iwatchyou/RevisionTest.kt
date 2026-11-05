package id.wynn.iwatchyou
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
class RevisionTest {
 @Test fun geometryAndTitleBurstsDoNotInvalidateFocus() {
  assertFalse(ForegroundPolicy.identityChanged(4 or 8 or 128))
  for(bit in listOf(1,2,16,32,64,256,512,1024))assertTrue(ForegroundPolicy.identityChanged(bit))
  assertTrue(ForegroundPolicy.identityChanged(0))
 }
 @Test fun slowFallbackDoesNotBlockMetadataAndOnlyLatestPendingReadRuns() {
  val roots=LatestWork();val windows=LatestWork();val entered=CountDownLatch(1);val release=CountDownLatch(1);val selected=CountDownLatch(1);val latest=CountDownLatch(1)
  val stale=java.util.concurrent.atomic.AtomicInteger()
  try {
   roots.execute { entered.countDown();release.await(2,TimeUnit.SECONDS) }
   assertTrue(entered.await(1,TimeUnit.SECONDS))
   repeat(100) { roots.execute { stale.incrementAndGet() } }
   roots.execute { latest.countDown() }
   windows.execute { selected.countDown() }
   assertTrue("Known focus is independent of blocked root",selected.await(1,TimeUnit.SECONDS))
   release.countDown();assertTrue(latest.await(1,TimeUnit.SECONDS));assertEquals(0,stale.get())
  } finally { release.countDown();roots.shutdownNow();windows.shutdownNow() }
 }
 @Test fun weeklyTotalsUseRecordedTimeIncludingScreenOnlyDays() {
  val a=TimeLedger.Snapshot("2026-11-04",2000,mapOf("notes" to 1000L),null)
  val b=TimeLedger.Snapshot("2026-11-05",3000,mapOf("notes" to 500L,"chat" to 1500L),null)
  val total=UsageSummary.combine(listOf(a,b),b.day)
  assertEquals(5000,total.screenMs);assertEquals(mapOf("notes" to 1500L,"chat" to 1500L),total.apps);assertNull(total.foreground)
  assertEquals(0,UsageSummary.combine(emptyList(),b.day).screenMs)
 }
}
