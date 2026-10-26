package id.wynn.furina

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class PetDeviceTest {
 private val inst=InstrumentationRegistry.getInstrumentation()
 private val app=inst.targetContext
 private val device=UiDevice.getInstance(inst)
 private fun await(timeout: Long=18000,test: ()->Boolean) {
  val end=SystemClock.elapsedRealtime()+timeout
  while(SystemClock.elapsedRealtime()<end) { if(test()) return; SystemClock.sleep(150) }
  assertTrue("Condition did not become true: ${PetRuntime.status.value}",test())
 }
 @Test fun nativeOfficialSdkSampleRenders() {
  val zip=File(app.cacheDir,"sdk-probe.zip")
  ZipOutputStream(zip.outputStream()).use { out ->
   fun pack(path: String,relative: String) {
    val children=app.assets.list(path) ?: emptyArray()
    if(children.isEmpty()) { out.putNextEntry(ZipEntry(relative)); app.assets.open(path).use { it.copyTo(out) }; out.closeEntry() }
    else children.forEach { pack("$path/$it","$relative/$it") }
   }
   pack("sdk-test/Haru","Haru")
  }
  val imported=ModelStore(app).importZip(android.net.Uri.fromFile(zip))
  device.executeShellCommand("appops set ${app.packageName} SYSTEM_ALERT_WINDOW allow")
  app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); device.waitForIdle()
  PetSettings(app).save(PetConfig(model=imported.file.path))
  inst.runOnMainSync { PetService.command(app) }
  try {
   await(50000) { PetRuntime.status.value.running && PetRuntime.status.value.frames>30 }
   android.util.Log.i("FurinaSDKProbe",PetRuntime.status.value.toString())
   assertTrue("Official SDK model must render: ${PetRuntime.status.value}",PetRuntime.status.value.visiblePixels>100)
  } finally {
   inst.runOnMainSync { PetService.command(app,PetService.STOP) }; await { !PetRuntime.status.value.running }
   PetSettings(app).save(PetConfig()); zip.delete()
  }
 }
 @Test fun nativePetAcrossAppsPauseScreenOffAndStop() {
  device.executeShellCommand("appops set ${app.packageName} SYSTEM_ALERT_WINDOW allow")
  app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  device.waitForIdle()
  val prefs=PetSettings(app); prefs.save(PetConfig())
  inst.runOnMainSync { PetService.command(app) }
  try {
   await(40000) { PetRuntime.status.value.frames>8 && PetRuntime.status.value.parameters>0 }
   await(20000) { PetRuntime.status.value.meshChanges>0 }
   val image=inst.uiAutomation.takeScreenshot()
   val probe=File(app.getExternalFilesDir(null),"live-overlay.png")
   probe.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
   assertTrue("GPU must draw model pixels: ${PetRuntime.status.value}",PetRuntime.status.value.visiblePixels>100)
   assertTrue("Background must remain transparent",PetRuntime.status.value.transparentPixels>100)
   assertEquals("",PetRuntime.status.value.error)
   val initial=PetRuntime.status.value.frames
   device.pressHome(); SystemClock.sleep(1500)
   app.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
   await { PetRuntime.status.value.frames>initial+15 }
   assertTrue(PetRuntime.status.value.running)
   // Return to our foreground activity before issuing a service command on target Android 16.
   app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); device.waitForIdle()
   inst.runOnMainSync { PetService.command(app,PetService.PAUSE) }
   await { PetRuntime.status.value.suspended }
   SystemClock.sleep(500); val paused=PetRuntime.status.value.frames
   SystemClock.sleep(1200); assertEquals(paused,PetRuntime.status.value.frames)
   inst.runOnMainSync { PetService.command(app,PetService.PAUSE) }
   await { PetRuntime.status.value.frames>paused+5 }
   inst.runOnMainSync { PetService.command(app,PetService.HIDE) }
   await { PetRuntime.status.value.suspended }
   inst.runOnMainSync { PetService.command(app,PetService.HIDE) }
   await { !PetRuntime.status.value.suspended }
   device.sleep(); await { PetRuntime.status.value.suspended }
   val screenOff=PetRuntime.status.value.frames
   SystemClock.sleep(1200); assertEquals(screenOff,PetRuntime.status.value.frames)
   device.wakeUp(); device.executeShellCommand("wm dismiss-keyguard")
   await { PetRuntime.status.value.frames>screenOff+5 }
   assertEquals("",PetRuntime.status.value.error)
   inst.runOnMainSync { PetService.command(app,PetService.STOP) }
   await { !PetRuntime.status.value.running }
   app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); device.waitForIdle()
   inst.runOnMainSync { PetService.command(app) }
   await(40000) { PetRuntime.status.value.running && PetRuntime.status.value.frames>8 && PetRuntime.status.value.visiblePixels>100 }
   assertTrue(PetRuntime.status.value.transparentPixels>100)
   assertEquals("",PetRuntime.status.value.error)
  } finally {
   inst.runOnMainSync { PetService.command(app,PetService.STOP) }
   await { !PetRuntime.status.value.running }
  }
 }
 @Test fun fastDragDizzyAndPositionSavePreserveModel() {
  device.executeShellCommand("appops set ${app.packageName} SYSTEM_ALERT_WINDOW allow")
  app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));device.waitForIdle()
  val prefs=PetSettings(app)
  val model=ModelStore(app).testModel().path
  prefs.save(PetConfig(model=model,roam=false,x=0.1f,y=0.5f,comments=true))
  prefs.savePosition(0.15f,0.5f);assertEquals(model,prefs.read().model)
  inst.runOnMainSync {PetService.command(app)}
  try {
   await {PetRuntime.status.value.running && PetRuntime.status.value.frames>15}
   SystemClock.sleep(5200)
   // Replay precise dp/time events through the real overlay view. UIAutomator swipe timing
   // can be throttled by the emulator, so it cannot establish a velocity threshold reliably.
   inst.runOnMainSync {
    fun find(v: android.view.View): PetSurface? {
     if(v is PetSurface) return v
     if(v is android.view.ViewGroup) repeat(v.childCount) {find(v.getChildAt(it))?.let {return it}}
     return null
    }
    val pet=android.view.inspector.WindowInspector.getGlobalWindowViews().firstNotNullOfOrNull {find(it)}
    assertNotNull(pet)
    val d=app.resources.displayMetrics.density
    val start=SystemClock.uptimeMillis()
    fun send(action: Int,time: Long,x: Float) {
     val e=android.view.MotionEvent.obtain(start,time,action,x*d,100*d,0)
     try {pet!!.dispatchTouchEvent(e)} finally {e.recycle()}
    }
    send(android.view.MotionEvent.ACTION_DOWN,start,20f)
    send(android.view.MotionEvent.ACTION_MOVE,start+20,100f)
    send(android.view.MotionEvent.ACTION_MOVE,start+40,20f)
    send(android.view.MotionEvent.ACTION_MOVE,start+60,100f)
    send(android.view.MotionEvent.ACTION_UP,start+65,100f)
   }
   await(4000) {PetRuntime.status.value.mode=="DIZZY"}
   fun bubbleVisible(): Boolean {
    var found=false
    inst.runOnMainSync {
     found=android.view.inspector.WindowInspector.getGlobalWindowViews().filterIsInstance<android.widget.TextView>().any {
      it.isAttachedToWindow && it.text.contains("🌀") &&
       ((it.layoutParams as? android.view.WindowManager.LayoutParams)?.flags ?: 0) and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0
     }
    }
    return found
   }
   await(2000) {bubbleVisible()}
   inst.runOnMainSync {PetService.command(app,PetService.HIDE)}
   await {PetRuntime.status.value.suspended}
   assertFalse(bubbleVisible())
  } finally {
   inst.runOnMainSync {PetService.command(app,PetService.STOP)};await {!PetRuntime.status.value.running}
   prefs.save(PetConfig())
  }
 }
 @Test fun importRealModelAndRejectCorruptZip() {
  val store=ModelStore(app); val f=store.testModel()
  val info=store.inspect(f)
  assertTrue(info.motions.size>=4)
  val zip=File(app.cacheDir,"device-model.zip")
  ZipOutputStream(zip.outputStream()).use { out -> f.parentFile!!.walkTopDown().filter { it.isFile }.forEach { file ->
   out.putNextEntry(ZipEntry("nested/モデル/"+file.relativeTo(f.parentFile!!).path)); file.inputStream().use { it.copyTo(out) }; out.closeEntry()
  } }
  val imported=store.importZip(android.net.Uri.fromFile(zip))
  assertTrue(imported.file.isFile); assertEquals(info.motions.size,imported.motions.size)
  assertTrue(imported.file.path.contains("モデル"))
  imported.file.parentFile!!.parentFile!!.parentFile!!.deleteRecursively()
  zip.delete()
  val bad=File(app.cacheDir,"bad.zip")
  ZipOutputStream(bad.outputStream()).use { out -> out.putNextEntry(ZipEntry("../outside")); out.write(byteArrayOf(1));out.closeEntry() }
  val rejected=assertThrows(Exception::class.java) { store.importZip(android.net.Uri.fromFile(bad)) }
  assertTrue(rejected is IllegalArgumentException || rejected is java.util.zip.ZipException)
  assertFalse(File(store.root.parentFile,"outside").exists())
  bad.delete()
 }
}
