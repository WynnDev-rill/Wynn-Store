package id.wynn.iwatchyou

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTest {
 @Before fun configureAutomation() { Configurator.getInstance().uiAutomationFlags=1 }
 @get:Rule val testName=org.junit.rules.TestName()
 @After fun restoreDisplay() {
  val dir=java.io.File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
  runCatching { device.takeScreenshot(java.io.File(dir,testName.methodName+".png"));device.dumpWindowHierarchy(java.io.File(dir,testName.methodName+".xml")) }
  println("Final ledger: "+WatchService.instance?.current())
  shell("settings put secure enabled_accessibility_services ''")
  shell("settings put system font_scale 1.0");device.setOrientationNatural();device.unfreezeRotation() }
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 private val automation get()=InstrumentationRegistry.getInstrumentation().getUiAutomation(1)
 private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
 private fun shell(command: String) { automation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() } }
 private fun waitFor(timeout: Long=8000,condition:()->Boolean) { val until=SystemClock.uptimeMillis()+timeout; while(!condition() && SystemClock.uptimeMillis()<until)SystemClock.sleep(50);assertTrue("Timed out waiting for condition",condition()) }
 @Test fun historyIsAtomicAndSurvivesReopen() {
  val day="2001-01-01"
  HistoryStore(context).use { it.write(TimeLedger.Snapshot(day,12345,mapOf("test.app" to 9999L),null,List(24){if(it==5)12345L else 0L})) }
  HistoryStore(context).use { val s=it.read(day)!!;assertEquals(12345L,s.screenMs);assertEquals(9999L,s.apps["test.app"]);assertEquals(12345L,s.hours[5]);assertTrue(it.days().contains(day)) }
 }
 @Test fun serviceTracksFocusStopsOnLockAndResumes() {
  automation
  if(android.os.Build.VERSION.SDK_INT>=33) shell("pm grant id.wynn.iwatchyou android.permission.POST_NOTIFICATIONS")
  shell("settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService")
  shell("settings put secure accessibility_enabled 1")
  waitFor { WatchService.instance!=null }
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("am start -a android.settings.SETTINGS")
  waitFor { WatchService.instance?.current()?.foreground=="com.android.settings" }
  val before=WatchService.instance!!.current().apps["com.android.settings"] ?: 0L
  SystemClock.sleep(1600)
  val after=WatchService.instance!!.current().apps["com.android.settings"] ?: 0L
  assertTrue("Foreground timer advances",after-before in 1200..2400)
  // Dimmed swipes do not move the timer; a tap unlocks the following drag.
  SystemClock.sleep(3300)
  val timer=device.findObject(By.clazz("android.widget.Chronometer"))
  assertNotNull("Timer overlay is present",timer)
  val rect=android.graphics.Rect(timer.visibleBounds)
  device.swipe(rect.centerX(),rect.centerY(),rect.centerX()+100,rect.centerY()+90,12)
  val still=device.findObject(By.clazz("android.widget.Chronometer")).visibleBounds
  assertEquals(rect,still)
  device.click(rect.centerX(),rect.centerY());SystemClock.sleep(250)
  device.swipe(rect.centerX(),rect.centerY(),rect.centerX()+100,rect.centerY()+90,12)
  SystemClock.sleep(200)
  val moved=device.findObject(By.clazz("android.widget.Chronometer")).visibleBounds
  assertTrue("Unlocked timer can move",moved.left>rect.left || moved.top>rect.top)
  val x=moved.left;val y=moved.top
  device.setOrientationLeft();SystemClock.sleep(600);device.setOrientationNatural();SystemClock.sleep(600)
  val restored=device.findObject(By.clazz("android.widget.Chronometer")).visibleBounds
  assertEquals(x,restored.left);assertEquals(y,restored.top)
  // Repeated transitions exercise the event adapter as well as the pure ledger.
  repeat(3) {
   shell("am start -n id.wynn.iwatchyou/.MainActivity")
   waitFor { WatchService.instance?.current()?.foreground==null }
   val stopped=WatchService.instance!!.current().apps["com.android.settings"]!!
   SystemClock.sleep(120)
   assertEquals(stopped,WatchService.instance!!.current().apps["com.android.settings"])
   shell("am start -a android.settings.SETTINGS")
   waitFor { WatchService.instance?.current()?.foreground=="com.android.settings" }
  }
  device.pressHome()
  waitFor { WatchService.instance?.current()?.foreground==null }
  val paused=WatchService.instance!!.current().apps["com.android.settings"]!!
  SystemClock.sleep(1100)
  assertEquals(paused,WatchService.instance!!.current().apps["com.android.settings"])
  device.sleep()
  waitFor {
   val off=!context.getSystemService(android.os.PowerManager::class.java).isInteractive
   val notices=context.getSystemService(android.app.NotificationManager::class.java).activeNotifications
   off && notices.any { !it.notification.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER) }
  }
  val locked=WatchService.instance!!.current().screenMs
  SystemClock.sleep(1200);assertEquals(locked,WatchService.instance!!.current().screenMs)
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("am start -a android.settings.SETTINGS")
  waitFor { WatchService.instance?.current()?.foreground=="com.android.settings" }
  assertTrue(WatchService.instance!!.current().apps["com.android.settings"]!!>=paused)
  shell("settings put secure enabled_accessibility_services ''")
 }
 @Test fun pipDoesNotKeepCountingTheFloatingVideo() {
  automation
  if(android.os.Build.VERSION.SDK_INT>=33) shell("pm grant id.wynn.iwatchyou android.permission.POST_NOTIFICATIONS")
  shell("settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService")
  shell("settings put secure accessibility_enabled 1")
  waitFor { WatchService.instance!=null }
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("am start -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.ProbeActivity")
  waitFor { WatchService.instance?.current()?.foreground=="id.wynn.iwatchyou.test" }
  SystemClock.sleep(700)
  assertTrue(device.wait(Until.hasObject(By.text("PiP fixture")),8000))
  device.findObject(By.text("PiP fixture")).click()
  waitFor { automation.windows.any { it.isInPictureInPictureMode } }
  shell("am start -a android.settings.SETTINGS")
  waitFor(20_000) { WatchService.instance?.current()?.foreground=="com.android.settings" }
  val paused=WatchService.instance!!.current().apps["id.wynn.iwatchyou.test"]!!
  SystemClock.sleep(1200)
  assertEquals(paused,WatchService.instance!!.current().apps["id.wynn.iwatchyou.test"])
  shell("am start -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.ProbeActivity --activity-clear-task --activity-new-task --ez finish true")
 }
 @Test fun activityEventsTrackWhenWindowRootsAreUnavailable() {
  automation
  if(android.os.Build.VERSION.SDK_INT>=33) shell("pm grant id.wynn.iwatchyou android.permission.POST_NOTIFICATIONS")
  shell("settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService")
  shell("settings put secure accessibility_enabled 1")
  waitFor { WatchService.instance!=null }
  val service=WatchService.instance!!
  val info=service.serviceInfo
  val oldFlags=info.flags
  InstrumentationRegistry.getInstrumentation().runOnMainSync {
   info.flags=oldFlags and android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS.inv()
   service.serviceInfo=info
  }
  try {
   repeat(3) {
    shell("am start -a android.settings.SETTINGS")
    waitFor(20_000) { service.current().foreground=="com.android.settings" }
    val start=service.current().apps["com.android.settings"] ?: 0L
    SystemClock.sleep(250)
    assertTrue(service.current().apps["com.android.settings"]!!>start)
    device.pressHome()
    waitFor { service.current().foreground==null }
   }
  } finally {
   InstrumentationRegistry.getInstrumentation().runOnMainSync { info.flags=oldFlags; service.serviceInfo=info }
  }
 }
 @Test fun mainScreenSurvivesRotationAndLargeText() {
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("am start -n id.wynn.iwatchyou/.MainActivity")
  assertTrue(device.wait(Until.hasObject(By.text("IWatchYou")),5000))
  assertTrue(device.wait(Until.hasObject(By.desc("Pemantauan")),8000))
  device.setOrientationLeft();SystemClock.sleep(800)
  assertTrue(device.wait(Until.hasObject(By.text("IWatchYou")),8000))
  assertTrue(device.wait(Until.hasObject(By.desc("Pemantauan")),8000))
  shell("settings put system font_scale 2.0");SystemClock.sleep(800)
  assertTrue(device.wait(Until.hasObject(By.desc("Pemantauan")),8000))
  shell("settings put system font_scale 1.0");device.setOrientationNatural();device.unfreezeRotation()
 }
}
