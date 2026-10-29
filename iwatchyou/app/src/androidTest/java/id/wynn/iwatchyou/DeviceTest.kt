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
 private fun captureScreen(name: String) {
  val dir=java.io.File(context.filesDir,"qa").apply { mkdirs() }
  runCatching { device.takeScreenshot(java.io.File(dir,name+".png"));device.dumpWindowHierarchy(java.io.File(dir,name+".xml")) }
 }
 @After fun restoreDisplay() {
  captureScreen(testName.methodName)
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
 // UiAutomator caches Chronometer nodes past the automatic dim transition.
 // Read actual native window/view bounds, then drive real physical input.
 private fun overlayViewBounds(label: String): android.graphics.Rect? {
  var result: android.graphics.Rect?=null
  InstrumentationRegistry.getInstrumentation().runOnMainSync {
   val service=WatchService.instance ?: return@runOnMainSync
   val overlayField=WatchService::class.java.getDeclaredField("overlay").apply { isAccessible=true }
   val overlay=overlayField.get(service) ?: return@runOnMainSync
   fun field(name: String)=OverlayController::class.java.getDeclaredField(name).apply { isAccessible=true }.get(overlay)
   val root=field("view") as? android.widget.FrameLayout
   val params=field("params") as? android.view.WindowManager.LayoutParams
   val v=when(label) {
    "edge" -> field("tab") as? android.view.View
    "Sembunyikan timer" -> field("hideButton") as? android.view.View
    "timer" -> root
    else -> root?.let { r -> (0 until r.childCount).map { r.getChildAt(it) }.firstOrNull { it.contentDescription==label } }
   }

   if(v!=null && v.isAttachedToWindow && v.isShown && v.width>0 && v.height>0) {
    val xy=IntArray(2);v.getLocationOnScreen(xy)
    result=android.graphics.Rect(xy[0],xy[1],xy[0]+v.width,xy[1]+v.height)
   }
  }
  return result
 }
 private fun editableTimer(): android.graphics.Rect {
  val until=SystemClock.uptimeMillis()+10_000
  while(SystemClock.uptimeMillis()<until) {
   overlayViewBounds("timer")?.let { return it }
   overlayViewBounds("edge")?.let { device.click(it.centerX(),it.centerY()) }
   SystemClock.sleep(50)
  }
  throw AssertionError("Edge handle did not restore editable timer")
 }
 private fun bubbleClick(label: String) {
  editableTimer()
  val bounds=overlayViewBounds(label) ?: throw AssertionError("Missing native bubble control: $label")
  device.click(bounds.centerX(),bounds.centerY())
  SystemClock.sleep(80)
 }
 private fun timerReading(): String {
  editableTimer()
  var value=""
  InstrumentationRegistry.getInstrumentation().runOnMainSync {
   val overlay=WatchService::class.java.getDeclaredField("overlay").apply { isAccessible=true }.get(WatchService.instance)
   val timer=OverlayController::class.java.getDeclaredField("timer").apply { isAccessible=true }.get(overlay) as android.widget.Chronometer
   value=timer.text.toString()
  }
  return value
 }

 @Test fun bubbleCollapsesResizesAndRetainsStateWithoutResettingTime() {
  startMonitoringFixture()
  val initial=editableTimer()
  assertNull("Visible timer has no edge handle",overlayViewBounds("edge"))
  shell("am start -W -f 0x10008000 -n id.wynn.iwatchyou/.MainActivity")
  val settings=device.wait(Until.findObject(By.desc("Pengaturan")),20_000) ?: error("Missing Settings")
  settings.click()
  device.wait(Until.findObject(By.text("Ukuran timer")),8000)!!.click()
  val slider=device.wait(Until.findObject(By.desc("Ukuran timer")),8000) ?: error("Missing size slider")
  val sliderBounds=slider.visibleBounds
  device.click(sliderBounds.left+sliderBounds.height()/2,sliderBounds.centerY())
  waitFor { context.getSharedPreferences("overlay",Context.MODE_PRIVATE).getFloat("scale",1f)==0.5f }
  assertTrue(device.hasObject(By.text("50%")))
  captureScreen("timer-size-50")
  device.findObject(By.text("Selesai")).click()
  shell("am start -W -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.FocusProbe")
  waitFor { WatchService.instance?.current()?.foreground=="id.wynn.iwatchyou.test" }
  val small=editableTimer()
  captureScreen("timer-half-visible")
  assertTrue("Half size is available through the real slider",kotlin.math.abs(initial.width()/2-small.width())<=1)
  val before=WatchService.instance!!.current().apps["id.wynn.iwatchyou.test"]!!
  bubbleClick("Sembunyikan timer")
  assertTrue(device.wait(Until.hasObject(By.desc("Tampilkan timer")),8000))
  assertNull(overlayViewBounds("timer"))
  waitFor { WatchService.instance!!.current().apps["id.wynn.iwatchyou.test"]!!>before }
  device.pressHome()
  waitFor { WatchService.instance?.current()?.foreground==null }
  shell("am start -n id.wynn.iwatchyou.test/id.wynn.iwatchyou.FocusProbe")
  waitFor { WatchService.instance?.current()?.foreground=="id.wynn.iwatchyou.test" }
  assertTrue(device.wait(Until.hasObject(By.desc("Tampilkan timer")),8000))
  assertNull("Docked state survives app switches",overlayViewBounds("timer"))
  assertEquals(small.width(),editableTimer().width())
  assertNull("Restoring removes the edge handle",overlayViewBounds("edge"))
  InstrumentationRegistry.getInstrumentation().runOnMainSync { WatchService.instance!!.resetBubbleControls() }
  assertEquals(initial.width(),editableTimer().width())
 }
 @Test fun fadedBubbleForwardsTapsWithoutCreatingAnEdgeHandle() {
  startMonitoringFixture()
  editableTimer()
  SystemClock.sleep(3400)
  val bounds=editableTimer()
  assertNull("Dimming must not dock the timer",overlayViewBounds("edge"))
  captureScreen("timer-faded-expanded")
  val hide=overlayViewBounds("Sembunyikan timer") ?: error("Missing inline hide arrow")
  assertTrue("Hide arrow stays beside the timer",kotlin.math.abs(hide.centerY()-bounds.centerY())<=2)
  val label=device.findObject(By.textStartsWith("Touches: "))
  val before=label.text.substringAfter(": ").toInt()
  device.click(bounds.left+bounds.width()/3,bounds.centerY())
  waitFor { device.findObject(By.textStartsWith("Touches: ")).text.substringAfter(": ").toInt()>before }
  assertNull("Passing through a body tap cannot create an edge handle",overlayViewBounds("edge"))
  bubbleClick("Sembunyikan timer")
  assertNotNull("Edge handle appears only after hiding",overlayViewBounds("edge"))
  captureScreen("timer-hidden")
  assertNull(overlayViewBounds("Sembunyikan timer"))
  editableTimer()
  assertNotNull(overlayViewBounds("Sembunyikan timer"))
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
  assertTrue("Fixture enables unused-app restrictions ($enabled)",enabled in listOf(androidx.core.content.UnusedAppRestrictionsConstants.API_30,androidx.core.content.UnusedAppRestrictionsConstants.API_31))
  shell("am start -W -f 0x10008000 -n id.wynn.iwatchyou/.MainActivity")
  device.wait(Until.findObject(By.desc("Pemantauan")),20_000)!!.click()
  val blocked=device.wait(Until.findObject(By.desc("Atur aksesibilitas")),8000) ?: error("Missing accessibility action")
  assertFalse("Enabled unused-app restrictions block monitoring setup",blocked.parent.isEnabled)
  blocked.click()
  assertEquals("Blocked setup stays inside IWatchYou",context.packageName,focusedApplication())
  assertFalse(device.hasObject(By.textStartsWith("Stopwatch muncul otomatis")))
  captureScreen("permissions-compact")
  shell("settings put system font_scale 2.0")
  waitFor { context.resources.configuration.fontScale>=1.9f }
  shell("am start -W -f 0x10008000 -n id.wynn.iwatchyou/.MainActivity")
  device.wait(Until.findObject(By.desc("Pemantauan")),20_000)!!.click()
  assertNotNull(device.wait(Until.findObject(By.text("Selesai")),8000))
  captureScreen("permissions-large-text")
  device.findObject(By.text("Selesai")).click()
  shell("settings put system font_scale 1.0")
  device.pressHome()
  shell("cmd appops set --uid id.wynn.iwatchyou AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore")
  shell("am start -W -f 0x10008000 -n id.wynn.iwatchyou/.MainActivity")
  device.wait(Until.findObject(By.desc("Pemantauan")),20_000)!!.click()
  waitFor { device.findObject(By.desc("Atur aksesibilitas"))?.parent?.isEnabled==true }
  device.findObject(By.desc("Atur aksesibilitas")).click()
  waitFor { focusedApplication()=="com.android.settings" }
 }
 @Test fun legacyMigrationCannotReplaceANewerConcurrentSnapshot() {
  device.pressHome()
  val day="2000-12-30"
  val legacy=context.getSharedPreferences("times",Context.MODE_PRIVATE)
  legacy.edit().putString("day",day).putLong("screen",100).putString("apps","{\"legacy.app\":50}").commit()
  val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
  try {
   HistoryStore(context).use { current ->
    val db=current.writableDatabase
    db.execSQL("DELETE FROM days WHERE day=?",arrayOf(day))
    db.beginTransaction()
    val entered=java.util.concurrent.CountDownLatch(1)
    val migration=executor.submit {
     entered.countDown()
     HistoryStore(context).use { it.migrateLegacy(context) }
    }
    try {
     assertTrue(entered.await(3,java.util.concurrent.TimeUnit.SECONDS))
     SystemClock.sleep(200) // Migration waits behind the active SQLite writer.
     current.write(TimeLedger.Snapshot(day,9000,mapOf("new.app" to 8000L),null,List(24){if(it==4)9000L else 0L}))
     db.setTransactionSuccessful()
    } finally { db.endTransaction() }
    migration.get(10,java.util.concurrent.TimeUnit.SECONDS)
    val saved=current.read(day)!!
    assertEquals(9000L,saved.screenMs)
    assertEquals(mapOf("new.app" to 8000L),saved.apps)
    assertEquals(9000L,saved.hours[4])
    assertNull(legacy.getString("day",null))
   }
  } finally { executor.shutdownNow() }
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
  // Faded bodies pass through; the solid inline arrow still supports dragging.
  SystemClock.sleep(3300)
  assertNull("Dimming does not hide the timer",overlayViewBounds("edge"))
  val rect=editableTimer()
  // Drag the visible inline control instead of the pass-through body.
  val handle=overlayViewBounds("Sembunyikan timer") ?: error("Missing drag handle")
  val dragX=handle.centerX()
  device.swipe(dragX,handle.centerY(),dragX+100,handle.centerY()+90,12)
  assertEquals("Dragging keeps Settings focused", "com.android.settings", focusedApplication())
  SystemClock.sleep(200)
  val moved=editableTimer()
  assertTrue("Unlocked timer can move",moved.left>rect.left || moved.top>rect.top)
  val x=moved.left;val y=moved.top
  device.setOrientationLeft()
  waitFor { context.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE }
  assertNotNull("Timer returns after landscape rotation",editableTimer())
  device.setOrientationNatural()
  waitFor { context.resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT }
  val restoredTimer=editableTimer()
  assertNotNull("Timer returns after portrait rotation",restoredTimer)
  val restored=restoredTimer
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
    val displayed=timerReading()
    val before=service.current().apps["id.wynn.iwatchyou.test"] ?: 0L
    SystemClock.sleep(1100)
    assertTrue("External app time advances",service.current().apps["id.wynn.iwatchyou.test"]!!>before)
    // A native Chronometer callback may be delayed on a loaded emulator.
    // Wait for a real displayed tick; a frozen timer still fails this deadline.
    waitFor(8000) { timerReading()!=displayed }
    assertEquals("External app remains focused while the timer advances", "id.wynn.iwatchyou.test", service.current().foreground)
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
