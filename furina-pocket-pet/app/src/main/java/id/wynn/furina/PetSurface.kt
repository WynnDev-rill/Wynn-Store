package id.wynn.furina

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import com.live2d.sdk.cubism.framework.CubismFramework
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@android.annotation.SuppressLint("ViewConstructor") // Created programmatically with an explicit private model path.
class PetSurface(context: Context, private val file: File, private val failed: (String)->Unit) : GLSurfaceView(context) {
 @Volatile var frame = PetFrame()
 @Volatile var config = PetConfig()
 @Volatile var renderedFrames = 0L; private set
 @Volatile var ready = false; private set
 private var model: LivePetModel?=null
 private var w=1; private var h=1
 private var last=0L
 private var fingerprint: Double?=null
 private val main=Handler(Looper.getMainLooper())
 init {
  setEGLContextClientVersion(2)
  setEGLConfigChooser(8,8,8,8,16,0)
  holder.setFormat(PixelFormat.TRANSLUCENT)
  setZOrderOnTop(true)
  contentDescription="Peliharaan Live2D"
  isClickable=true
  preserveEGLContextOnPause=true
  setRenderer(object: Renderer {
   override fun onSurfaceCreated(gl: GL10?,egl: EGLConfig?) {
    ready=false
    // A context may be lost after screen-off or GPU pressure. CPU model resources must also be freed.
    model?.close(); model=null
    try {
     if(!CubismFramework.isStarted()) {
      val appAssets=context.applicationContext.assets
      val option=CubismFramework.Option().apply {
       if(BuildConfig.DEBUG) loggingLevel=CubismFramework.LogLevel.DEBUG
       loadFileFunction=com.live2d.sdk.cubism.framework.ICubismLoadFileFunction { path -> appAssets.open(path).use { it.readBytes() } }
       logFunction=com.live2d.sdk.cubism.core.ICubismLogger { message -> android.util.Log.d("Cubism",message) }
      }
      CubismFramework.startUp(option)
     }
     if(!CubismFramework.isInitialized()) CubismFramework.initialize()
     com.live2d.sdk.cubism.framework.rendering.android.CubismShaderAndroid.getInstance().releaseInvalidShaderProgram()
     // Official context-loss sequence: discard cached programs so the new EGL context recompiles them.
     com.live2d.sdk.cubism.framework.rendering.android.CubismShaderAndroid.deleteInstance()
    } catch(e: Exception) { report(e) }
    last=0
   }
   override fun onSurfaceChanged(gl: GL10?,width: Int,height: Int) {
    w=width; h=height; glViewport(0,0,w,h)
    try {
     if(model==null) model=LivePetModel(ModelStore(context).inspect(file),w,h)
     else model?.setRenderTargetSize(w,h)
     ready=true
     PetRuntime.update { it.copy(renderer="Cubism Java R5 · OpenGL ES 2",parameters=model?.parameterCount ?: 0,error="") }
    } catch(e: Exception) { report(e) }
   }
   override fun onDrawFrame(gl: GL10?) {
    glViewport(0,0,w,h); glClearColor(0f,0f,0f,0f); glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    val now=System.nanoTime(); val dt=if(last==0L) 1f/30 else ((now-last)/1e9).toFloat().coerceIn(0f,0.1f)
    last=now
    try {
     model?.draw(dt,frame,config,w,h); renderedFrames++
     // A one-time startup probe confirms actual GPU pixels, including transparent background.
     if(renderedFrames==8L && model!=null) {
      val data=java.nio.ByteBuffer.allocateDirect(w*h*4)
      glReadPixels(0,0,w,h,GL_RGBA,GL_UNSIGNED_BYTE,data)
      var opaque=0; var transparent=0
      for(i in 3 until w*h*4 step 4) { val alpha=data.get(i).toInt() and 255; if(alpha>16) opaque++ else transparent++ }
      val fbo=IntArray(1); glGetIntegerv(GL_FRAMEBUFFER_BINDING,fbo,0)
      android.util.Log.i("FurinaGPU","${w}x$h, pixels=$opaque/$transparent, fbo=${fbo[0]}, glError=${glGetError()}, ${model?.diagnostics()}")
      PetRuntime.update { it.copy(visiblePixels=opaque,transparentPixels=transparent) }
     }
     // Sample mesh deformation during startup only; no continuing pixel reads or profiling loop.
     if(renderedFrames%30L==0L && renderedFrames<=180L) {
      val next=model?.meshFingerprint()
      if(next!=null && fingerprint!=null && kotlin.math.abs(next-fingerprint!!)>0.00001)
       PetRuntime.update { it.copy(meshChanges=it.meshChanges+1) }
      fingerprint=next
     }
    }
    catch(e: Exception) { ready=false; report(e) }
   }
  })
  renderMode=RENDERMODE_WHEN_DIRTY
 }
 private fun report(e: Exception) {
  android.util.Log.e("FurinaRenderer","Render failed",e)
  PetRuntime.update { it.copy(error=e.message ?: "Model gagal dirender") }
  main.post { failed(e.message ?: "Model gagal dirender") }
 }
 fun release() {
  // Finish native/GL cleanup on its owning GL thread before removing the Surface.
  val done=CountDownLatch(1)
  onResume()
  queueEvent { try { model?.close(); model=null; ready=false } finally { done.countDown() } }
  requestRender(); done.await(400,TimeUnit.MILLISECONDS)
  onPause()
 }
 override fun performClick(): Boolean { super.performClick(); return true }
}
