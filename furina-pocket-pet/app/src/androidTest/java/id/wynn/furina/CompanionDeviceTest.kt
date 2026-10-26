package id.wynn.furina

import android.content.Intent
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CompanionDeviceTest {
 private val inst=InstrumentationRegistry.getInstrumentation()
 private val app=inst.targetContext
 private val device=UiDevice.getInstance(inst)
 private fun await(timeout: Long=20000,condition: ()->Boolean) {
  val end=SystemClock.elapsedRealtime()+timeout
  while(SystemClock.elapsedRealtime()<end) {if(condition()) return;SystemClock.sleep(100)}
  assertTrue("Timeout: ${PetRuntime.status.value}",condition())
 }
 private fun pet(): PetSurface {
  var result: PetSurface?=null
  inst.runOnMainSync {
   fun find(v: View): PetSurface? {
    if(v is PetSurface) return v
    if(v is ViewGroup) repeat(v.childCount) {find(v.getChildAt(it))?.let {p -> return p}}
    return null
   }
   result=WindowInspector.getGlobalWindowViews().firstNotNullOfOrNull {find(it)}
  }
  return requireNotNull(result)
 }
 private fun start(ghost: Boolean=false) {
  device.executeShellCommand("appops set ${app.packageName} SYSTEM_ALERT_WINDOW allow")
  app.startActivity(Intent(app,OverlayProbeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));device.waitForIdle()
  PetSettings(app).save(PetConfig(model=ModelStore(app).testModel().path,roam=false,jump=false,comments=false,voice=false,ghost=ghost,x=.5f,y=.5f))
  inst.runOnMainSync {PetService.command(app)}
  await(45000) {PetRuntime.status.value.running && PetRuntime.status.value.frames>20}
 }
 private fun stop() {inst.runOnMainSync {PetService.command(app,PetService.STOP)};await {!PetRuntime.status.value.running};PetSettings(app).save(PetConfig())}
 private fun command(action: String) {inst.runOnMainSync {PetService.command(app,action)}}
 @Test fun ghostReallyPassesTapsAndRestoresInteraction() {
  start(true)
  try {
   val view=pet();val point=IntArray(2)
   inst.runOnMainSync {
    val p=view.layoutParams as WindowManager.LayoutParams
    assertTrue(p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0);assertTrue(p.alpha<=.8f)
    view.getLocationOnScreen(point)
   }
   val before=OverlayProbeActivity.clicks
   device.click(point[0]+view.width/2,point[1]+view.height/2);await {OverlayProbeActivity.clicks>before}
   command(PetService.GHOST)
   await {!PetSettings(app).read().ghost}
   inst.runOnMainSync {assertEquals(0,(view.layoutParams as WindowManager.LayoutParams).flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)}
   val restored=OverlayProbeActivity.clicks
   device.click(point[0]+view.width/2,point[1]+view.height*2/3)
   await {PetRuntime.status.value.mode==PetMood.POKE.name}
   assertEquals(restored,OverlayProbeActivity.clicks)
  } finally {stop()}
 }
 @Test fun activityDrawsRealPropsAndTouchInterruptsIt() {
  start()
  try {
   val v=pet()
   fun whitePixels(): Int {
    val latch=CountDownLatch(1);var count=0
    v.queueEvent {
     try {
      val w=v.width;val h=v.height;val b=ByteBuffer.allocateDirect(w*h*4)
      android.opengl.GLES20.glBindFramebuffer(android.opengl.GLES20.GL_FRAMEBUFFER,0)
      android.opengl.GLES20.glReadPixels(0,0,w,h,android.opengl.GLES20.GL_RGBA,android.opengl.GLES20.GL_UNSIGNED_BYTE,b)
      for(y in (h*.18f).toInt() until (h*.48f).toInt()) for(x in (w*.3f).toInt() until (w*.7f).toInt()) {
       val i=(y*w+x)*4;val r=b.get(i).toInt() and 255;val g=b.get(i+1).toInt() and 255;val blue=b.get(i+2).toInt() and 255;val a=b.get(i+3).toInt() and 255
       if(r>190 && g>190 && blue>180 && a>120) count++
      }
     } finally {latch.countDown()}
    };v.requestRender();assertTrue(latch.await(5,TimeUnit.SECONDS));return count
   }
   val baseline=whitePixels()
   command(PetService.READ);await {PetRuntime.status.value.mode==PetMood.READ.name};SystemClock.sleep(1000)
   val book=whitePixels();assertTrue("Book must add visible page pixels: $baseline -> $book",book>baseline+100)
   inst.runOnMainSync {
    val now=SystemClock.uptimeMillis()
    fun send(a: Int,t: Long,x: Float) {val e=MotionEvent.obtain(now,t,a,x,v.height*.25f,0);try {v.dispatchTouchEvent(e)} finally {e.recycle()}}
    val x=v.width*.5f;val d=app.resources.displayMetrics.density
    send(MotionEvent.ACTION_DOWN,now,x)
    repeat(5) {send(MotionEvent.ACTION_MOVE,now+30L+it*30,x+(if(it%2==0) 13 else -13)*d)}
    send(MotionEvent.ACTION_UP,now+250,x)
   }
   await {PetRuntime.status.value.mode==PetMood.PETTED.name}
   command(PetService.CAKE);await {PetRuntime.status.value.mode==PetMood.CAKE.name}
   val prefs=PetSettings(app);prefs.save(prefs.read().copy(activities=false));command(PetService.REFRESH)
   await {PetRuntime.status.value.mode==PetMood.REST.name}
   assertEquals("",PetRuntime.status.value.error)
  } finally {stop()}
 }
 @Test fun actualAudioEnvelopeCompletionCancellationAndBadFile() {
  app.startActivity(Intent(app,OverlayProbeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));device.waitForIdle()
  val pcm=ShortArray(32000) {if(it in 5000..24000) (10000*kotlin.math.sin(it*.18)).toInt().toShort() else 0}
  val file=File(app.cacheDir,"voice-fixture.wav")
  val wav=ByteBuffer.allocate(44+pcm.size*2).order(ByteOrder.LITTLE_ENDIAN)
  wav.put("RIFF".toByteArray());wav.putInt(36+pcm.size*2);wav.put("WAVEfmt ".toByteArray());wav.putInt(16);wav.putShort(1);wav.putShort(1);wav.putInt(16000);wav.putInt(32000);wav.putShort(2);wav.putShort(16);wav.put("data".toByteArray());wav.putInt(pcm.size*2);pcm.forEach {wav.putShort(it)}
  file.writeBytes(wav.array());var voice: PetVoice?=null
  try {
   inst.runOnMainSync {
    voice=PetVoice(app)
    assertFalse(voice!!.playLocal(File(app.cacheDir,"missing.wav"),LipEnvelope(emptyList()),.4f))
    assertTrue(voice!!.playLocal(file,LipEnvelope.fromPcm(pcm,16000),.4f))
   }
   await {voice!!.playing}
   var peak=0f
   repeat(20) {inst.runOnMainSync {peak=maxOf(peak,voice!!.level())};SystemClock.sleep(50)}
   assertTrue("Real MediaPlayer must drive envelope",peak>.2f)
   await { !voice!!.playing && !voice!!.pending }
   inst.runOnMainSync {
    assertEquals(0f,voice!!.level(),0f)
    assertTrue(voice!!.playLocal(file,LipEnvelope.fromPcm(pcm,16000),.4f));voice!!.stop()
   }
   SystemClock.sleep(500);assertFalse(voice!!.playing);assertFalse(voice!!.pending)
   val bad=File(app.cacheDir,"bad-audio.ogg").apply {writeText("not audio")}
   inst.runOnMainSync {voice!!.playLocal(bad,LipEnvelope(emptyList()),.4f)}
   await {!voice!!.playing && !voice!!.pending};assertEquals(0f,voice!!.level(),0f);bad.delete()
  } finally {inst.runOnMainSync {voice?.stop()};file.delete()}
 }
}
