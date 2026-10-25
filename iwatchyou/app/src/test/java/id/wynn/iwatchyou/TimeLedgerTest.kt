package id.wynn.iwatchyou

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class TimeLedgerTest {
 @Test fun backgroundAppsAreNotCounted() {
  val t=TimeLedger("24"); t.screenOn(100);t.appChanged("game",200);t.appChanged("chat",1200);t.appChanged(null,1700)
  assertEquals(1000L,t.snapshot(1900).apps["game"]);assertEquals(500L,t.snapshot(1900).apps["chat"]);assertEquals(1800L,t.snapshot(1900).screenMs)
 }
 @Test fun resumingPreservesDuration() {
  val t=TimeLedger("24");t.screenOn(100);t.appChanged("game",100);t.screenOff(1100);t.screenOn(10000);t.appChanged("game",10000)
  assertEquals(2000L,t.snapshot(11000).apps["game"]);assertEquals(2000L,t.snapshot(11000).screenMs)
 }
 @Test fun rolloverReturnsHistoryAndResetsAtExactBoundary() {
  val t=TimeLedger("24");t.screenOn(0);t.appChanged("video",0);val old=t.nextDay("25",5000)
  assertEquals(5000L,old.apps["video"]);assertEquals(0L,t.snapshot(5000).apps["video"] ?: 0L);assertEquals(1500L,t.snapshot(6500).apps["video"])
 }
 @Test fun checkpointDoesNotDoubleCount() {
  val t=TimeLedger("24");t.screenOn(0);t.appChanged("a",0);repeat(100){t.checkpoint((it+1)*10L)}
  assertEquals(2000L,t.snapshot(2000).apps["a"]);assertEquals(2000L,t.snapshot(2000).screenMs)
 }
 @Test fun duplicateEventsDoNotResetApp() {
  val t=TimeLedger("24");t.screenOn(0);t.appChanged("a",0);t.screenOn(300);t.appChanged("a",600)
  assertEquals(1000L,t.snapshot(1000).apps["a"])
 }
 @Test fun lateEventsCannotCreateNegativeOrDuplicateIntervals() {
  val t=TimeLedger("24");t.screenOn(100);t.appChanged("a",200);t.appChanged("b",100);t.screenOff(500)
  assertEquals(0L,t.snapshot(800).apps["a"]);assertEquals(300L,t.snapshot(800).apps["b"]);assertEquals(400L,t.snapshot(800).screenMs)
 }
 @Test fun rapidSwitchesRemainConserved() {
  val t=TimeLedger("24");t.screenOn(0)
  repeat(10000){t.appChanged(if(it%2==0)"a" else "b",it.toLong())};t.screenOff(10000)
  val s=t.snapshot(20000);assertEquals(10000L,s.apps.values.sum());assertEquals(10000L,s.screenMs)
 }
 @Test fun lockStopsAllTime() {
  val t=TimeLedger("24");t.screenOn(0);t.appChanged("a",0);t.screenOff(1000);t.appChanged("background",2000)
  assertEquals(1000L,t.snapshot(999999).screenMs);assertNull(t.snapshot(999999).foreground);assertFalse(t.snapshot(999999).apps.containsKey("background"))
 }
 @Test fun snapshotsNeverMutateLedger() {
  val t=TimeLedger("24");t.screenOn(0);t.appChanged("a",0);t.snapshot(5000);t.screenOff(1000)
  assertEquals(1000L,t.snapshot(5000).screenMs)
 }
 @Test fun initialTotalsAndRestoredDayAreRetained() {
  val t=TimeLedger("24",mapOf("a" to 400L),500L);t.screenOn(100);t.appChanged("a",100)
  assertEquals(1400L,t.snapshot(1100).apps["a"])
  val saved=TimeLedger.Snapshot("23",300, mapOf("b" to 200L),null)
  t.nextDay("23",1100,saved)
  assertEquals(300L,t.snapshot(1100).screenMs);assertEquals(200L,t.snapshot(1100).apps["b"])
 }
 @Test fun hourlyBarsUseActualClockHours() {
  val wall=Instant.parse("2026-10-24T07:59:59Z").toEpochMilli()
  val t=TimeLedger("24",wallOffset=wall,zone=ZoneOffset.UTC);t.screenOn(0);t.screenOff(3000)
  val s=t.snapshot(3000);assertEquals(1000L,s.hours[7]);assertEquals(2000L,s.hours[8]);assertEquals(s.screenMs,s.hours.sum())
 }
 @Test fun changedWallClockDoesNotChangeDuration() {
  val t=TimeLedger("24",wallOffset=0,zone=ZoneOffset.UTC);t.screenOn(100);t.appChanged("a",100);t.reanchor(1000,90_000_000,ZoneOffset.UTC);t.screenOff(2000)
  assertEquals(1900L,t.snapshot(2000).screenMs);assertEquals(1900L,t.snapshot(2000).apps["a"])
 }
 @Test fun dstHourDoesNotHangOrLoseTime() {
  val wall=Instant.parse("2026-11-01T05:59:59Z").toEpochMilli()
  val t=TimeLedger("day",wallOffset=wall,zone=ZoneId.of("America/New_York"));t.screenOn(0);t.screenOff(7_200_000)
  assertEquals(7_200_000L,t.snapshot(7_200_000).hours.sum())
 }
 @Test fun backgroundGapAfterProcessRestartIsNeverInvented() {
  val t=TimeLedger("24",mapOf("a" to 1000L),1000L)
  assertEquals(1000L,t.snapshot(999999999).screenMs);assertNull(t.snapshot(999999999).foreground)
 }
}
