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
 @Before fun configureAutomation() { Configurator.getInstance().apply { uiAutomationFlags=1;waitForIdleTimeout=0 } }
 @get:Rule val testName=org.junit.rules.TestName()
 @After fun restoreDisplay() {
  val dir=java.io.File(context.filesDir,"qa").apply { mkdirs() }
  runCatching { device.takeScreenshot(java.io.File(dir,testName.methodName+".png"));device.dumpWindowHierarchy(java.io.File(dir,testName.methodName+".xml")) }
  println("Final ledger: "+WatchService.instance?.current())
  shell("settings put secure enabled_accessibility_services ''")
  waitFor(20_000) { WatchService.instance==null }
  shell("settings put system font_scale 1.0");device.setOrientationNatural();device.unfreezeRotation() }
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 private val automation get()=InstrumentationRegistry.getInstrumentation().getUiAutomation(1)
 private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
 private fun shell(command: String) { automation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() } }
 private fun waitFor(timeout: Long=8000,condition:()->Boolean) { val until=SystemClock.uptimeMillis()+timeout; while(!condition() && SystemClock.uptimeMillis()<until)SystemClock.sleep(50);assertTrue("Timed out waiting for condition",condition()) }
 private fun focusedApplication(): String? {
  // Independent oracle: these tests deliberately change root availability;
  // UiAutomation may also temporarily have no cached root.
  val state=automation.executeShellCommand("dumpsys window displays").use {
   android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
  }
  return Regex("""mCurrentFocus=Window\{[^\n]*? u\d+ ([^/\s]+)/""").find(state)?.groupValues?.get(1)
 }
 private fun startMonitoringFixture() {
  automation
  context.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit().clear().commit()
  if(android.os.Build.VERSION.SDK_INT>=33)shell("pm grant id.wynn.iwatchyou android.permission.POST_NOTIFICATIONS")
  shell("settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService")
  shell("settings put secure accessibility_enabled 1")
  waitFor { WatchService.instance!=null }
  InstrumentationRegistry.getInstrumentation().runOnMainSync { WatchService.instance!!.resetBubbleControls() }
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("am start -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.FocusProbe")
  waitFor(20_000) { WatchService.instance?.current()?.foreground=="id.wynn.iwatchyou.test" }
 }
 private fun editableTimer(): UiObject2 {
  val until=SystemClock.uptimeMillis()+10_000
  while(SystemClock.uptimeMillis()<until) {
   device.findObject(By.clazz("android.widget.Chronometer"))?.let { return it }
   device.findObject(By.desc("Tampilkan kontrol timer"))?.click()
   SystemClock.sleep(50)
  }
  throw AssertionError("Edge handle did not restore editable timer")
 }

 @Test fun bubbleCollapsesResizesAndRetainsStateWithoutResettingTime() {
  startMonitoringFixture()
  val initial=editableTimer().visibleBounds
  repeat(4) {
   editableTimer()
   device.findObject(By.desc("Perkecil timer")).click()
  }
  val small=editableTimer().visibleBounds
  assertTrue("Timer becomes smaller",small.width()<initial.width())
  assertTrue("Minimum remains 80 percent",small.width()>=initial.width()*0.79)
  device.findObject(By.desc("Perkecil timer")).click()
  assertEquals(small.width(),editableTimer().visibleBounds.width())
  val before=WatchService.instance!!.current().apps["id.wynn.iwatchyou.test"]!!
  device.findObject(By.desc("Sembunyikan timer")).click()
  assertTrue(device.wait(Until.hasObject(By.desc("Tampilkan kontrol timer")),8000))
  assertFalse(device.hasObject(By.clazz("android.widget.Chronometer")))
  SystemClock.sleep(1100)
  assertTrue(WatchService.instance!!.current().apps["id.wynn.iwatchyou.test"]!!>before)
  device.pressHome()
  waitFor { WatchService.instance?.current()?.foreground==null }
  shell("am start -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.FocusProbe")
  waitFor { WatchService.instance?.current()?.foreground=="id.wynn.iwatchyou.test" }
  assertTrue(device.wait(Until.hasObject(By.desc("Tampilkan kontrol timer")),8000))
  assertFalse("Docked state survives app switches",device.hasObject(By.clazz("android.widget.Chronometer")))
  assertEquals(small.width(),editableTimer().visibleBounds.width())
  InstrumentationRegistry.getInstrumentation().runOnMainSync { WatchService.instance!!.resetBubbleControls() }
  assertEquals(initial.width(),editableTimer().visibleBounds.width())
 }
 @Test fun fadedBubbleForwardsTapsToTheActualApp() {
  startMonitoringFixture()
  val bounds=editableTimer().visibleBounds
  // Fixture counts every ACTION_DOWN anywhere, independently of accessibility metadata.
  SystemClock.sleep(3400)
  assertTrue(device.wait(Until.hasObject(By.desc("Tampilkan kontrol timer")),8000))
  assertFalse("Faded timer is absent from touch/accessibility actions",device.hasObject(By.desc("Perkecil timer")))
  val label=device.findObject(By.textStartsWith("Touches: "))
  val before=label.text.substringAfter(": ").toInt()
  device.click(bounds.left+bounds.width()/3,bounds.centerY())
  waitFor { device.findObject(By.textStartsWith("Touches: ")).text.substringAfter(": ").toInt()>before }
  assertTrue("Body tap cannot unlock faded controls",device.hasObject(By.desc("Tampilkan kontrol timer")))
  assertFalse(device.hasObject(By.desc("Sembunyikan timer")))
  editableTimer()
  assertNotNull(device.findObject(By.desc("Sembunyikan timer")))
 }
 @Test fun historicalDatesHaveNoInventedRowsAndSurviveUpdates() {
  val old="2026-01-02";val next="2026-01-03"
  HistoryStore(context).use {
   it.write(TimeLedger.Snapshot(old,120_000,mapOf("historical.app" to 90_000L),null,List(24){h -> if(h==8)120_000L else 0L}))
   it.write(TimeLedger.Snapshot(next,60_000,emptyMap(),null))
   it.write(TimeLedger.Snapshot(next,65_000,emptyMap(),null))
  }
  HistoryStore(context).use {
   assertNull(it.read("2026-01-01"))
   assertEquals(120_000L,it.read(old)!!.screenMs)
   assertEquals(90_000L,it.read(old)!!.apps["historical.app"])
   assertEquals(120_000L,it.read(old)!!.hours[8])
   assertEquals(65_000L,it.read(next)!!.screenMs)
   assertFalse(it.days().contains("2026-01-01"))
  }
 }
 @Test fun setupRequiresUnusedAppRestrictionsToBeDisabled() {
  if(android.os.Build.VERSION.SDK_INT>=33)shell("pm grant id.wynn.iwatchyou android.permission.POST_NOTIFICATIONS")
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("cmd appops set --uid id.wynn.iwatchyou AUTO_REVOKE_PERMISSIONS_IF_UNUSED allow")
  val enabled=androidx.core.content.PackageManagerCompat.getUnusedAppRestrictionsStatus(context).get()
  println("Unused restrictions after allow: $enabled; whitelisted=${context.packageManager.isAutoRevokeWhitelisted}")
  assertTrue("Fixture enables unused-app restrictions ($enabled)",enabled in listOf(androidx.core.content.UnusedAppRestrictionsConstants.API_30,androidx.core.content.UnusedAppRestrictionsConstants.API_31))
  shell("am start -n id.wynn.iwatchyou/.MainActivity")
  device.wait(Until.hasObject(By.desc("Pemantauan")),8000)
  device.findObject(By.desc("Pemantauan")).click()
  assertTrue(device.wait(Until.hasObject(By.text("Lanjutkan")),8000))
  device.findObject(By.text("Lanjutkan")).click()
  SystemClock.sleep(250)
  assertNotNull("Continue cannot leave setup while restrictions are enabled",device.findObject(By.text("Lanjutkan")))
  device.pressBack();device.pressHome()
  waitFor { focusedApplication()==device.launcherPackageName }
  shell("cmd appops set --uid id.wynn.iwatchyou AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore")
  shell("am start -W -n id.wynn.iwatchyou/.MainActivity")
  assertTrue(device.wait(Until.hasObject(By.desc("Pemantauan")),8000))
  device.findObject(By.desc("Pemantauan")).click()
  assertTrue(device.wait(Until.hasObject(By.text("Lanjutkan")),8000))
  device.findObject(By.text("Periksa lagi"))?.click()
  val disabled=androidx.core.content.PackageManagerCompat.getUnusedAppRestrictionsStatus(context).get()
  assertEquals(androidx.core.content.UnusedAppRestrictionsConstants.DISABLED,disabled)
  device.findObject(By.text("Lanjutkan")).click()
  waitFor { focusedApplication()=="com.android.settings" }
 }
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
  // Faded bodies pass through. Only the solid edge handle unlocks editing.
  SystemClock.sleep(3300)
  assertTrue(device.wait(Until.hasObject(By.desc("Tampilkan kontrol timer")),8000))
  device.findObject(By.desc("Tampilkan kontrol timer")).click()
  val timer=device.wait(Until.findObject(By.clazz("android.widget.Chronometer")),8000)
  assertNotNull("Timer overlay is present",timer)
  val rect=android.graphics.Rect(timer.visibleBounds)
  device.swipe(rect.left+20,rect.centerY(),rect.left+120,rect.centerY()+90,12)
  SystemClock.sleep(200)
  val moved=device.findObject(By.clazz("android.widget.Chronometer")).visibleBounds
  assertTrue("Unlocked timer can move",moved.left>rect.left || moved.top>rect.top)
  val x=moved.left;val y=moved.top
  device.setOrientationLeft()
  waitFor { context.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE }
  assertNotNull("Timer returns after landscape rotation",editableTimer())
  device.setOrientationNatural()
  waitFor { context.resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT }
  val restoredTimer=editableTimer()
  assertNotNull("Timer returns after portrait rotation",restoredTimer)
  val restored=restoredTimer.visibleBounds
  assertEquals(x,restored.left);assertEquals(y,restored.top)
  // Repeated transitions exercise the event adapter as well as the pure ledger.
  repeat(3) {
   shell("am start -W -n id.wynn.iwatchyou/.MainActivity")
   assertTrue("Dashboard is visible before measuring pause",device.wait(Until.hasObject(By.text("IWatchYou").pkg(context.packageName)),8000))
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
  shell("am start -f 0x10008000 -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.ProbeActivity --ez finish true")
  waitFor { automation.windows.none { it.isInPictureInPictureMode } }
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
    device.pressHome()
    waitFor(20_000) { focusedApplication()==device.launcherPackageName && service.current().foreground==null }
    shell("am start -W -a android.settings.SETTINGS")
    waitFor(20_000) { focusedApplication()=="com.android.settings" && service.current().foreground=="com.android.settings" }
    val start=service.current().apps["com.android.settings"] ?: 0L
    SystemClock.sleep(250)
    assertTrue(service.current().apps["com.android.settings"]!!>start)
    device.pressHome()
    waitFor(20_000) { focusedApplication()==device.launcherPackageName && service.current().foreground==null }
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
 @Test fun externalAppWithoutLauncherOrActivitySuffixShowsTimerWithoutWindowRoots() {
  automation
  shell("settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService")
  shell("settings put secure accessibility_enabled 1")
  waitFor(60_000) { WatchService.instance!=null }
  val service=WatchService.instance!!
  val info=service.serviceInfo
  val oldFlags=info.flags
  val oldImePreference=android.provider.Settings.Secure.getString(context.contentResolver,"show_ime_with_hard_keyboard")
  InstrumentationRegistry.getInstrumentation().runOnMainSync {
   info.flags=oldFlags and android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS.inv()
   service.serviceInfo=info
  }
  try {
   device.wakeUp();shell("wm dismiss-keyguard")
   shell("settings put secure show_ime_with_hard_keyboard 1")
   repeat(3) {
    device.pressHome()
    waitFor(20_000) { focusedApplication()==device.launcherPackageName && service.current().foreground==null }
    shell("am start -W -a android.settings.SETTINGS")
    waitFor(20_000) { service.current().foreground=="com.android.settings" }
    shell("am start -W -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.FocusProbe")
    assertTrue("External app is visible",device.wait(Until.hasObject(By.text("External focus fixture").pkg("id.wynn.iwatchyou.test")),8000))
    waitFor { focusedApplication()=="id.wynn.iwatchyou.test" }
    waitFor(20_000) { service.current().foreground=="id.wynn.iwatchyou.test" }
    val displayed=editableTimer().text
    val before=service.current().apps["id.wynn.iwatchyou.test"] ?: 0L
    SystemClock.sleep(1100)
    assertTrue("External app time advances",service.current().apps["id.wynn.iwatchyou.test"]!!>before)
    assertNotEquals("Displayed timer advances even after automatic dimming",displayed,editableTimer().text)
    device.findObject(By.text("Show keyboard").pkg("id.wynn.iwatchyou.test")).click()
    waitFor { automation.windows.any { it.type==android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD } }
    SystemClock.sleep(350)
    assertEquals("Real keyboard must not steal focus","id.wynn.iwatchyou.test",service.current().foreground)
    device.pressBack()
    waitFor { automation.windows.none { it.type==android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD } }
    // With no windows available, the event adapter must also reject an IME window.
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
     val event=android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
     event.packageName="test.keyboard"
     event.className="android.inputmethodservice.SoftInputWindow"
     service.onAccessibilityEvent(event)
     @Suppress("DEPRECATION") event.recycle()
    }
    SystemClock.sleep(350)
    assertEquals("Keyboard must not steal the rootless app hint","id.wynn.iwatchyou.test",service.current().foreground)
    device.pressHome()
    waitFor(20_000) { focusedApplication()==device.launcherPackageName && service.current().foreground==null }
    assertTrue("Bubble disappears at Home",device.wait(Until.gone(By.clazz("android.widget.Chronometer")),8000))
    val paused=service.current().apps["id.wynn.iwatchyou.test"]!!
    SystemClock.sleep(200)
    assertEquals(paused,service.current().apps["id.wynn.iwatchyou.test"])
    // Match leaving Settings via Home, then launching another app with a null hint.
    shell("am start -W -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.FocusProbe")
    waitFor(20_000) { service.current().foreground=="id.wynn.iwatchyou.test" }
    assertNotNull("Bubble reappears after launching from Home",editableTimer())
    device.pressHome()
    waitFor(20_000) { focusedApplication()==device.launcherPackageName && service.current().foreground==null }
   }
  } finally {
   InstrumentationRegistry.getInstrumentation().runOnMainSync { info.flags=oldFlags; service.serviceInfo=info }
   if(oldImePreference==null) shell("settings delete secure show_ime_with_hard_keyboard")
   else shell("settings put secure show_ime_with_hard_keyboard $oldImePreference")
  }
 }
}
