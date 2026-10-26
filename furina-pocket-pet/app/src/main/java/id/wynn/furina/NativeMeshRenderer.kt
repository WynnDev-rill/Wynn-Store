package id.wynn.furina

import android.opengl.GLES20.*
import com.live2d.sdk.cubism.framework.model.CubismModel
import com.live2d.sdk.cubism.framework.rendering.CubismRenderer.CubismBlendMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/** Bounded ES2 adapter for standard Cubism ArtMeshes. Core, rig, motions and physics remain SDK-driven.
 * UV convention, premultiplied colors and clipping semantics follow the official Cubism Java shaders.
 * https://github.com/Live2D/CubismJavaFramework/tree/develop/framework/src/main/assets
 * Advanced Cubism 5.3 offscreen blend modes are rejected explicitly instead of being misrendered.
 */
class NativeMeshRenderer(private val model: CubismModel,private val textureIds: List<Int>) {
 private data class Mesh(val vertices: FloatBuffer,val uv: FloatBuffer,val indices: ShortBuffer,val count: Int)
 private val meshes=Array(model.drawableCount) { i ->
  val p=model.getDrawableVertexPositions(i); val uv=model.getDrawableVertexUvs(i); val indices=model.getDrawableVertexIndices(i)
  Mesh(floats(p),floats(uv),ByteBuffer.allocateDirect(indices.size*2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(indices);position(0) },indices.size)
 }
 private var program=0
 private var position=0; private var texCoord=0; private var matrix=0; private var texture=0; private var maskSampler=0
 private var useMask=0; private var invertMask=0; private var viewport=0; private var alpha=0; private var multiply=0; private var screen=0
 private var maskTexture=0; private var maskFbo=0; private var maskWidth=0; private var maskHeight=0
 private val masks=model.drawableMasks
 private val counts=model.drawableMaskCounts
 init {
  require(!model.isBlendModeEnabled && model.offscreenCount==0) { "Model memakai mode blending Cubism baru yang belum didukung. Gunakan model Cubism standar tanpa offscreen blending." }
  val vertex=compile(GL_VERTEX_SHADER,"""
   attribute vec2 a_position; attribute vec2 a_uv;
   uniform mat4 u_matrix; varying vec2 v_uv;
   void main(){ gl_Position=u_matrix*vec4(a_position,0.0,1.0); v_uv=vec2(a_uv.x,1.0-a_uv.y); }
  """.trimIndent())
  val fragment=compile(GL_FRAGMENT_SHADER,"""
   precision mediump float;
   varying vec2 v_uv;
   uniform sampler2D u_texture; uniform sampler2D u_mask;
   uniform float u_useMask; uniform float u_invert; uniform vec2 u_viewport;
   uniform float u_alpha; uniform vec4 u_multiply; uniform vec4 u_screen;
   void main(){
    vec4 color=texture2D(u_texture,v_uv);
    color.rgb*=u_multiply.rgb;
    color.rgb=color.rgb+u_screen.rgb*color.a-color.rgb*u_screen.rgb;
    color*=u_alpha;
    if(u_useMask>0.5){ float coverage=texture2D(u_mask,gl_FragCoord.xy/u_viewport).a; if(u_invert>0.5)coverage=1.0-coverage; color*=coverage; }
    gl_FragColor=color;
   }
  """.trimIndent())
  program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glLinkProgram(program)
  val linked=IntArray(1);glGetProgramiv(program,GL_LINK_STATUS,linked,0)
  require(linked[0]==GL_TRUE) { "GPU gagal menghubungkan shader: ${glGetProgramInfoLog(program)}" }
  glDeleteShader(vertex);glDeleteShader(fragment)
  position=glGetAttribLocation(program,"a_position");texCoord=glGetAttribLocation(program,"a_uv")
  matrix=glGetUniformLocation(program,"u_matrix");texture=glGetUniformLocation(program,"u_texture")
  maskSampler=glGetUniformLocation(program,"u_mask");useMask=glGetUniformLocation(program,"u_useMask")
  invertMask=glGetUniformLocation(program,"u_invert");viewport=glGetUniformLocation(program,"u_viewport")
  alpha=glGetUniformLocation(program,"u_alpha");multiply=glGetUniformLocation(program,"u_multiply");screen=glGetUniformLocation(program,"u_screen")
 }
 private fun ensureMask(w: Int,h: Int) {
  if(w==maskWidth && h==maskHeight) return
  if(maskTexture!=0) glDeleteTextures(1,intArrayOf(maskTexture),0)
  if(maskFbo!=0) glDeleteFramebuffers(1,intArrayOf(maskFbo),0)
  val ids=IntArray(1);glGenTextures(1,ids,0);maskTexture=ids[0]
  glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,maskTexture)
  glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,null)
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
  glGenFramebuffers(1,ids,0);maskFbo=ids[0];glBindFramebuffer(GL_FRAMEBUFFER,maskFbo)
  glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,maskTexture,0)
  require(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE) { "GPU tidak mendukung clipping mask" }
  glBindFramebuffer(GL_FRAMEBUFFER,0);maskWidth=w;maskHeight=h
 }
 fun draw(mvp: FloatArray,w: Int,h: Int) {
  ensureMask(w,h)
  glBindFramebuffer(GL_FRAMEBUFFER,0);glViewport(0,0,w,h)
  glDisable(GL_DEPTH_TEST);glDisable(GL_STENCIL_TEST);glDisable(GL_SCISSOR_TEST);glDisable(GL_CULL_FACE)
  glEnable(GL_BLEND);glColorMask(true,true,true,true)
  glBindBuffer(GL_ARRAY_BUFFER,0);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,0)
  glUseProgram(program);glUniformMatrix4fv(matrix,1,false,mvp,0)
  glUniform1i(texture,0);glUniform1i(maskSampler,1);glUniform2f(viewport,w.toFloat(),h.toFloat())
  glEnableVertexAttribArray(position);glEnableVertexAttribArray(texCoord)
  var previousMask=""
  val orders=model.renderOrders
  for(i in (0 until model.drawableCount).sortedBy { orders[it] }) {
   if(!model.getDrawableDynamicFlagIsVisible(i) || model.getDrawableOpacity(i)<=0f) continue
   val clipping=if(counts[i]>0) masks[i].take(counts[i]) else emptyList()
   if(clipping.isNotEmpty()) {
    val key=clipping.joinToString(",")
    if(key!=previousMask) {
     glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,0)
     glBindFramebuffer(GL_FRAMEBUFFER,maskFbo);glClearColor(0f,0f,0f,0f);glClear(GL_COLOR_BUFFER_BIT)
     glUniform1f(useMask,0f);glBlendFuncSeparate(GL_ONE,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA)
     clipping.forEach { source -> mesh(source,true) }
     glBindFramebuffer(GL_FRAMEBUFFER,0);previousMask=key
    }
    glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,maskTexture)
    glUniform1f(useMask,1f);glUniform1f(invertMask,if(model.getDrawableInvertedMask(i)) 1f else 0f)
   } else { glUniform1f(useMask,0f) }
   when(model.getDrawableBlendMode(i)) {
    CubismBlendMode.ADDITIVE -> glBlendFuncSeparate(GL_ONE,GL_ONE,GL_ZERO,GL_ONE)
    CubismBlendMode.MULTIPLICATIVE -> glBlendFuncSeparate(GL_DST_COLOR,GL_ONE_MINUS_SRC_ALPHA,GL_ZERO,GL_ONE)
    else -> glBlendFuncSeparate(GL_ONE,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA)
   }
   mesh(i,false)
  }
  glDisableVertexAttribArray(position);glDisableVertexAttribArray(texCoord);glUseProgram(0)
  glActiveTexture(GL_TEXTURE0)
 }
 private fun mesh(i: Int,mask: Boolean) {
  val part=meshes[i]
  val ti=model.getDrawableTextureIndex(i)
  require(ti in textureIds.indices) { "Indeks tekstur model tidak valid: $ti" }
  part.vertices.clear();part.vertices.put(model.getDrawableVertexPositions(i));part.vertices.position(0)
  glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,textureIds[ti])
  glVertexAttribPointer(position,2,GL_FLOAT,false,0,part.vertices)
  glVertexAttribPointer(texCoord,2,GL_FLOAT,false,0,part.uv)
  glUniform1f(alpha,if(mask) 1f else model.getDrawableOpacity(i))
  val m=if(mask) floatArrayOf(1f,1f,1f,1f) else model.getDrawableMultiplyColor(i)
  val s=if(mask) floatArrayOf(0f,0f,0f,0f) else model.getDrawableScreenColor(i)
  glUniform4fv(multiply,1,m,0);glUniform4fv(screen,1,s,0)
  glDrawElements(GL_TRIANGLES,part.count,GL_UNSIGNED_SHORT,part.indices)
 }
 fun close() { if(program!=0) glDeleteProgram(program); if(maskTexture!=0)glDeleteTextures(1,intArrayOf(maskTexture),0);if(maskFbo!=0)glDeleteFramebuffers(1,intArrayOf(maskFbo),0) }
 private fun compile(type: Int,source: String): Int {
  val shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader)
  val ok=IntArray(1);glGetShaderiv(shader,GL_COMPILE_STATUS,ok,0)
  require(ok[0]==GL_TRUE) { "GPU gagal mengompilasi shader: ${glGetShaderInfoLog(shader)}" };return shader
 }
 private fun floats(data: FloatArray): FloatBuffer=ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data);position(0) }
}
