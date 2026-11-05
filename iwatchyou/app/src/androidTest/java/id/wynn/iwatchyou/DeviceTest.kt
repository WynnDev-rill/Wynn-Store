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
 @Before fun configureAutomation() { Configurator.getInstance().uiAutomationFlags=1;context.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit().clear().commit() }
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
  // A dimmed island remains immediately draggable; a tap expands it.
  SystemClock.sleep(3300)
  val timer=device.findObject(By.clazz("android.widget.Chronometer"))
  assertNotNull("Timer overlay is present",timer)
  val rect=android.graphics.Rect(timer.visibleBounds)
  device.swipe(rect.centerX(),rect.centerY(),rect.centerX()+100,rect.centerY()+90,12)
  SystemClock.sleep(250)
  val moved=device.findObject(By.clazz("android.widget.Chronometer")).visibleBounds
  assertTrue("Timer moves without an unlock tap",moved.left>rect.left || moved.top>rect.top)
  val x=moved.left;val y=moved.top
  device.setOrientationLeft()
  waitFor { device.displayWidth>device.displayHeight }
  assertNotNull("Timer returns after landscape rotation",device.wait(Until.findObject(By.clazz("android.widget.Chronometer")),8000))
  device.setOrientationNatural()
  waitFor { device.displayHeight>device.displayWidth }
  val restoredTimer=device.wait(Until.findObject(By.clazz("android.widget.Chronometer")),8000)
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
    waitFor(2000) { service.current().foreground=="id.wynn.iwatchyou.test" }
    assertTrue("Bubble appears over an external app",device.wait(Until.hasObject(By.clazz("android.widget.Chronometer")),8000))
    val displayed=device.findObject(By.clazz("android.widget.Chronometer")).text
    val before=service.current().apps["id.wynn.iwatchyou.test"] ?: 0L
    SystemClock.sleep(1100)
    assertTrue("External app time advances",service.current().apps["id.wynn.iwatchyou.test"]!!>before)
    waitFor { device.findObject(By.clazz("android.widget.Chronometer"))?.text?.let { it!=displayed }==true }
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
    waitFor(2000) { service.current().foreground=="id.wynn.iwatchyou.test" }
    assertTrue("Bubble reappears after launching from Home",device.wait(Until.hasObject(By.clazz("android.widget.Chronometer")),8000))
    device.pressHome()
    waitFor(20_000) { focusedApplication()==device.launcherPackageName && service.current().foreground==null }
   }
  } finally {
   InstrumentationRegistry.getInstrumentation().runOnMainSync { info.flags=oldFlags; service.serviceInfo=info }
   if(oldImePreference==null) shell("settings delete secure show_ime_with_hard_keyboard")
   else shell("settings put secure show_ime_with_hard_keyboard $oldImePreference")
  }
 }
 @Test fun floatingIslandExpandsDocksAndKeepsCounting() {
  shell("settings put secure enabled_accessibility_services id.wynn.iwatchyou/.WatchService")
  shell("settings put secure accessibility_enabled 1")
  waitFor { WatchService.instance!=null }
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("am start -W -a android.settings.SETTINGS")
  waitFor { WatchService.instance?.current()?.foreground=="com.android.settings" }
  val dir=java.io.File(context.filesDir,"qa").apply { mkdirs() }
  val clock=device.wait(Until.findObject(By.clazz("android.widget.Chronometer")),2000)
  assertNotNull(clock);device.waitForIdle();device.takeScreenshot(java.io.File(dir,"island-compact.png"))
  clock.click()
  assertNotNull(device.wait(Until.findObject(By.desc("Ringkas timer")),2000))
  device.waitForIdle();device.takeScreenshot(java.io.File(dir,"island-expanded.png"))
  device.findObject(By.desc("Sembunyikan timer ke tepi")).click()
  assertNotNull(device.wait(Until.findObject(By.desc("Tampilkan timer")),2000))
  assertTrue(device.wait(Until.gone(By.clazz("android.widget.Chronometer")),2000))
  val before=WatchService.instance!!.current().apps["com.android.settings"]!!
  SystemClock.sleep(1100)
  assertTrue(WatchService.instance!!.current().apps["com.android.settings"]!!>before)
  device.takeScreenshot(java.io.File(dir,"island-edge.png"))
  device.setOrientationLeft();waitFor { device.displayWidth>device.displayHeight }
  assertNotNull(device.wait(Until.findObject(By.desc("Tampilkan timer")),2000))
  device.findObject(By.desc("Tampilkan timer")).click()
  assertNotNull(device.wait(Until.findObject(By.clazz("android.widget.Chronometer")),2000))
  device.setOrientationNatural();waitFor { device.displayHeight>device.displayWidth }
  shell("settings put system font_scale 2.0");device.waitForIdle()
  device.findObject(By.clazz("android.widget.Chronometer")).click()
  assertNotNull(device.wait(Until.findObject(By.desc("Ringkas timer")),2000))
  device.waitForIdle();device.takeScreenshot(java.io.File(dir,"island-expanded-large-text.png"))
 }
 @Test fun overviewLimitsAppsAndWeeklyDaysAreClickable() {
  val end=WatchService.today()
  HistoryStore(context).use { store ->
   (0..6).forEach { offset ->
    val day=java.time.LocalDate.parse(end).minusDays(offset.toLong()).toString()
    store.write(TimeLedger.Snapshot(day,60_000L*(offset+1), (1..7).associate { "fixture.app$it" to 1000L*it },null))
   }
  }
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("am start -W -n id.wynn.iwatchyou/.MainActivity")
  revealText("Lihat selengkapnya")
  assertFalse("Only five apps on overview",device.hasObject(By.text("app1")))
  device.findObject(By.text("Lihat selengkapnya")).click()
  revealText("Mingguan",false);device.findObject(By.text("Mingguan")).click()
  assertNotNull(device.wait(Until.findObject(By.text("Sebelumnya")),8000))
  val dir=java.io.File(context.filesDir,"qa").apply { mkdirs() }
  device.waitForIdle();device.takeScreenshot(java.io.File(dir,"weekly-statistics.png"))
  val yesterday=java.time.LocalDate.parse(end).minusDays(1)
  val dateLabel=yesterday.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy",java.util.Locale.forLanguageTag("id")))
  val bar=device.wait(Until.findObject(By.descStartsWith(dateLabel+":")),8000)
  assertNotNull("Weekly day has real accessible duration",bar);bar.click()
  assertTrue(device.wait(Until.hasObject(By.text("Waktu layar · "+dateLabel)),8000))
  device.pressBack()
 }
 // Bounded real swipes avoid UiScrollable's legacy scroll-event assumptions
 // after a Compose viewport is resized. Read the current on-screen list bounds.
 private fun revealText(text: String,towardEnd: Boolean=true) {
  repeat(16) {
   if(device.hasObject(By.text(text))) { device.waitForIdle();return }
   val bounds=device.wait(Until.findObject(By.scrollable(true)),8000)?.visibleBounds
    ?: throw AssertionError("No scrollable content while finding $text")
   val top=bounds.top+bounds.height()/6;val bottom=bounds.bottom-bounds.height()/6
   device.swipe(bounds.centerX(),if(towardEnd)bottom else top,bounds.centerX(),if(towardEnd)top else bottom,20)
   device.waitForIdle()
  }
  assertTrue("Content is reachable: $text",device.hasObject(By.text(text)))
 }
 @Test fun populatedHistoryKeepsSelectedDateAfterRotation() {
  val day="1999-01-01"
  HistoryStore(context).use { it.write(TimeLedger.Snapshot(day,19_260_000,
   mapOf("com.android.settings" to 12_660_000L,"id.wynn.iwatchyou.test" to 3_840_000L),null,
   List(24) { if(it in 8..17) 1_926_000L else 0L })) }
  device.wakeUp();shell("wm dismiss-keyguard")
  shell("am start -W -n id.wynn.iwatchyou/.MainActivity")
  revealText("Hari ini ▾")
  device.findObject(By.text("Hari ini ▾")).click()
  assertNotNull(device.wait(Until.findObject(By.text("1 Jan 1999")),8000))
  device.findObject(By.text("1 Jan 1999")).click()
  assertTrue(device.wait(Until.hasObject(By.text("1 Jan 1999 ▾")),8000))
  val dir=java.io.File(context.filesDir,"qa").apply { mkdirs() }
  fun summaryCapture(name: String) {
   revealText("IWatchYou",false)
   assertTrue("Historical date is preserved",device.wait(Until.hasObject(By.text("Waktu layar · 1 Jan 1999")),8000))
   revealText("5j 21m 0d")
   assertTrue("Populated screen total is loaded",device.hasObject(By.text("5j 21m 0d")))
   device.waitForIdle();assertTrue(device.takeScreenshot(java.io.File(dir,name)))
  }
  summaryCapture("populated-history-portrait.png")
  revealText("3j 31m 0d")
  assertTrue("Populated app duration is loaded",device.hasObject(By.text("3j 31m 0d")))
  device.takeScreenshot(java.io.File(dir,"populated-history-apps.png"))
  device.setOrientationLeft();waitFor { device.displayWidth>device.displayHeight };device.waitForIdle()
  summaryCapture("populated-history-landscape.png")
  device.setOrientationNatural();waitFor { device.displayHeight>device.displayWidth }
  shell("settings put system font_scale 2.0");device.waitForIdle()
  summaryCapture("populated-history-large-text.png")
  revealText("3j 31m 0d")
  device.takeScreenshot(java.io.File(dir,"populated-history-apps-large-text.png"))
  revealText("IWatchYou",false)
  device.findObject(By.desc("Pengaturan")).click()
  assertNotNull(device.wait(Until.findObject(By.text("Izin & layanan")),8000))
  device.findObject(By.text("Izin & layanan")).click()
  assertTrue(device.wait(Until.hasObject(By.text("Lanjutkan")),8000))
  device.waitForIdle();device.takeScreenshot(java.io.File(dir,"setup-large-text.png"))
  device.pressBack()
  assertTrue(device.wait(Until.gone(By.text("Lanjutkan")),8000))
 }


}
