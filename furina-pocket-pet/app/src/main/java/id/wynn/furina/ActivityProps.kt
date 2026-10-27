package id.wynn.furina

import android.content.Context
import android.graphics.*
import android.opengl.GLES20.*
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** Tiny GPU props composed with the existing model, in the same transparent surface/window. */
class ActivityProps(context: Context) {
 private val program: Int
 private val textures=IntArray(7)
 private val vertices=ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
 private val position: Int;private val uv: Int;private val opacity: Int;private val matrix: Int
 init {
  fun shader(type: Int,source: String)=glCreateShader(type).also {s ->
   glShaderSource(s,source);glCompileShader(s);val status=IntArray(1);glGetShaderiv(s,GL_COMPILE_STATUS,status,0)
   require(status[0]==GL_TRUE) {glGetShaderInfoLog(s)}
  }
  val vs=shader(GL_VERTEX_SHADER,"attribute vec2 a_position;attribute vec2 a_uv;varying vec2 v_uv;uniform mat4 u_matrix;void main(){gl_Position=u_matrix*vec4(a_position,0.,1.);v_uv=a_uv;}")
  val fs=shader(GL_FRAGMENT_SHADER,"precision mediump float;varying vec2 v_uv;uniform sampler2D u_texture;uniform float u_alpha;void main(){gl_FragColor=texture2D(u_texture,v_uv)*u_alpha;}")
  program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program)
  glDeleteShader(vs);glDeleteShader(fs)
  val status=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,status,0);require(status[0]==GL_TRUE)
  position=glGetAttribLocation(program,"a_position");uv=glGetAttribLocation(program,"a_uv");opacity=glGetUniformLocation(program,"u_alpha");matrix=glGetUniformLocation(program,"u_matrix")
  glGenTextures(textures.size,textures,0)
  val book=BitmapFactory.decodeResource(context.resources,R.drawable.activity_book)
  val cake=BitmapFactory.decodeResource(context.resources,R.drawable.activity_cake)
  val page=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).also {b ->
   val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG);p.color=Color.rgb(244,249,255);c.drawRoundRect(3f,5f,61f,59f,4f,4f,p)
   p.color=Color.rgb(148,172,204);p.strokeWidth=2f
   repeat(5) {i -> c.drawLine(13f,17f+i*7f,51f,17f+i*7f,p)}
  }
  val fork=Bitmap.createBitmap(32,96,Bitmap.Config.ARGB_8888).also {b ->
   val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(163,184,213);strokeWidth=5f;strokeCap=Paint.Cap.ROUND}
   c.drawLine(16f,24f,16f,89f,p);c.drawLine(6f,30f,26f,30f,p)
   for(x in listOf(6f,16f,26f)) c.drawLine(x,5f,x,30f,p)
  }
  val sparkle=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).also {b ->
   val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(139,199,255)}
   val path=Path().apply {moveTo(32f,2f);lineTo(40f,24f);lineTo(62f,32f);lineTo(40f,40f);lineTo(32f,62f);lineTo(24f,40f);lineTo(2f,32f);lineTo(24f,24f);close()}
   Canvas(b).drawPath(path,p)
  }
  listOf(book,cake,page,fork,sparkle,StageFurniture.chair(),StageFurniture.swing()).forEachIndexed {i,b ->
   try {
    glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,textures[i])
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
    GLUtils.texImage2D(GL_TEXTURE_2D,0,b,0)
   } finally {b.recycle()}
  }
 }
 /** NDC positions deliberately stay inside the pet's original rectangular window. */
 private fun sprite(texture: Int,x: Float,y: Float,w: Float,h: Float,angle: Float,alpha: Float,uvRight: Float=1f) {
  val c=cos(angle);val s=sin(angle)
  val corners=floatArrayOf(-w/2,-h/2,w/2,-h/2,-w/2,h/2,w/2,h/2)
  val tex=floatArrayOf(0f,1f,uvRight,1f,0f,0f,uvRight,0f)
  vertices.clear()
  for(i in 0..3) {val px=corners[i*2];val py=corners[i*2+1];vertices.put(x+px*c-py*s);vertices.put(y+px*s+py*c);vertices.put(tex[i*2]);vertices.put(tex[i*2+1])}
  vertices.position(0);glVertexAttribPointer(position,2,GL_FLOAT,false,16,vertices)
  vertices.position(2);glVertexAttribPointer(uv,2,GL_FLOAT,false,16,vertices)
  glBindTexture(GL_TEXTURE_2D,textures[texture]);glUniform1f(opacity,alpha);glDrawArrays(GL_TRIANGLE_STRIP,0,4)
 }
 private val identity=floatArrayOf(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f)
 private fun begin(projection: FloatArray=identity) {
  glBindFramebuffer(GL_FRAMEBUFFER,0);glDisable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glDisable(GL_SCISSOR_TEST)
  glBindBuffer(GL_ARRAY_BUFFER,0);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,0)
  glEnable(GL_BLEND);glBlendFuncSeparate(GL_ONE,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA)
  glUseProgram(program);glActiveTexture(GL_TEXTURE0);glUniform1i(glGetUniformLocation(program,"u_texture"),0)
  glUniformMatrix4fv(matrix,1,false,projection,0)
  glEnableVertexAttribArray(position);glEnableVertexAttribArray(uv)
 }
 private fun end() {glDisableVertexAttribArray(position);glDisableVertexAttribArray(uv);glUseProgram(0)}
 fun drawBack(scene: PoseScene,projection: FloatArray) {
  if(scene.amount<=0f) return
  begin(projection)
  if(scene.mood==PetMood.SWING) sprite(6,-.015f,.18f,.9f,1.64f,0f,scene.amount)
  else sprite(5,-.015f,-.38f,.78f,.98f,0f,scene.amount)
  end()
 }
 private fun drawHeld(scene: PoseScene,projection: FloatArray) {
  begin(projection)
  val a=scene.amount
  when(scene.mood) {
   PetMood.READ -> {
    sprite(0,-.015f,-.29f,.51f,.32f,0f,a)
    val turn=scene.progress*5f%1f
    if(turn>.78f) {val t=(turn-.78f)/.22f*PI.toFloat();sprite(2,-.015f+sin(t)*.05f,-.26f,max(.006f,abs(cos(t))*.20f),.20f,.02f,a)}
   }
   PetMood.CAKE -> {
    val remaining=(1f-(scene.progress*4).toInt()*.16f).coerceAtLeast(.35f)
    sprite(1,-.14f,-.28f,.29f*remaining,.25f,0f,a,remaining)
    // Fork handle at the same smoothed wrist used by the IK bones.
    sprite(3,scene.right.x,scene.right.y+.045f,.035f,.14f,-.18f,a)
   }
   else -> Unit
  }
  end()
 }
 fun draw(frame: PetFrame,scene: PoseScene?=null,projection: FloatArray?=null) {
  if(scene!=null && scene.amount>0f && projection!=null) {drawHeld(scene,projection);return}
  if(frame.effect<=0f) return
  if(frame.mood!=PetMood.READ && frame.mood!=PetMood.CAKE && frame.mood!=PetMood.PETTED) return
  begin()
  val a=frame.effect
  when(frame.mood) {
   PetMood.READ -> {
    sprite(0,0f,-.35f,.82f,.62f,sin(frame.progress*22f)*.025f,a)
    val turn=(frame.progress*5f)%1f
    if(turn>.78f) sprite(2,sin((turn-.78f)/.22f*PI.toFloat())*.1f,-.29f,max(.02f,abs(cos((turn-.78f)/.22f*PI.toFloat()))*.35f),.36f,.07f,a)
   }
   PetMood.CAKE -> {
    val remaining=(1f-(frame.progress*4).toInt()*.16f).coerceAtLeast(.35f)
    sprite(1,.43f,-.5f,.55f*remaining,.55f,.03f,a,remaining)
    val bite=((frame.progress*7)%1f).let {max(0f,sin(it*PI.toFloat()))}
    sprite(3,.43f-bite*.3f,-.39f+bite*.48f,.065f,.29f,-.5f-bite*.8f,a)
   }
   PetMood.PETTED -> repeat(3) {i -> sprite(4,-.52f+i*.5f,.42f+sin(frame.progress*9f+i)*.1f,.13f,.16f,frame.progress*.4f,a*.8f)}
   else -> Unit
  }
  end()
 }
 fun close() {glDeleteTextures(textures.size,textures,0);glDeleteProgram(program)}
}
