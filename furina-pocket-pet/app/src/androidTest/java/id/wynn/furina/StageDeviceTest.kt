package id.wynn.furina

import android.opengl.EGL14
import android.opengl.GLES20.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

/** Real ES2 framebuffer tests for model-space furniture/held props, without private art. */
@RunWith(AndroidJUnit4::class)
class StageDeviceTest {
 @Test fun furnitureHeldPropsSwingAndCleanupRenderOnRealGpu() {
  val display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
  assertTrue(EGL14.eglInitialize(display,IntArray(2),0,IntArray(2),1))
  val configs=arrayOfNulls<android.opengl.EGLConfig>(1);val count=IntArray(1)
  val spec=intArrayOf(EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_NONE)
  assertTrue(EGL14.eglChooseConfig(display,spec,0,configs,0,1,count,0))
  val context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
  val surface=EGL14.eglCreatePbufferSurface(display,configs[0],intArrayOf(EGL14.EGL_WIDTH,256,EGL14.EGL_HEIGHT,320,EGL14.EGL_NONE),0)
  assertTrue(EGL14.eglMakeCurrent(display,surface,surface,context))
  var props: ActivityProps?=null
  try {
   glViewport(0,0,256,320)
   props=ActivityProps(InstrumentationRegistry.getInstrumentation().targetContext)
   val projection=floatArrayOf(1.05f,0f,0f,0f,0f,1.05f*.8f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f)
   fun pixels(mood: PetMood,phase: Float,amount: Float=1f): ByteArray {
    glClearColor(0f,0f,0f,0f);glClear(GL_COLOR_BUFFER_BIT)
    val scene=PoseScene(mood,phase,amount,BodyPose.target(-1,mood,phase),BodyPose.target(1,mood,phase),if(mood==PetMood.SWING) BodyPose.swing(phase) else 0f)
    val m=BodyPose.projection(projection,PosePoint(-.015f,.94f),scene.swing)
    props!!.drawBack(scene,m);props!!.draw(PetFrame(),scene,m)
    val b=ByteBuffer.allocateDirect(256*320*4);glReadPixels(0,0,256,320,GL_RGBA,GL_UNSIGNED_BYTE,b)
    assertEquals(GL_NO_ERROR,glGetError());return ByteArray(b.capacity()).also {b.get(it)}
   }
   fun visible(p: ByteArray)=(3 until p.size step 4).count {(p[it].toInt() and 255)>16}
   val chair=pixels(PetMood.SIT,.2f)
   assertTrue("Chair must have actual pixels",visible(chair)>1500)
   assertTrue("Book must change framebuffer",!chair.contentEquals(pixels(PetMood.READ,.2f)))
   val cake=pixels(PetMood.CAKE,0f)
   assertFalse("Fork must travel with hand between phases",cake.contentEquals(pixels(PetMood.CAKE,.1f)))
   val swing=pixels(PetMood.SWING,.04f)
   assertTrue(visible(swing)>1000)
   assertFalse("Swing must change projected pixels",swing.contentEquals(pixels(PetMood.SWING,.14f)))
   assertEquals("A fully exited scene leaves no furniture or prop pixels",0,visible(pixels(PetMood.SIT,.2f,0f)))
   // Releasing/recreating textures must not leave stale GL names or state.
   props.close();props=ActivityProps(InstrumentationRegistry.getInstrumentation().targetContext)
   assertEquals(visible(chair),visible(pixels(PetMood.SIT,.2f)))
  } finally {
   props?.close();EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
   EGL14.eglDestroySurface(display,surface);EGL14.eglDestroyContext(display,context);EGL14.eglTerminate(display)
  }
 }
}
