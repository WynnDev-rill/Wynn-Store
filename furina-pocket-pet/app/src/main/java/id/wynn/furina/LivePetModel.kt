package id.wynn.furina

import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import com.live2d.sdk.cubism.framework.CubismModelSettingJson
import com.live2d.sdk.cubism.framework.math.CubismMatrix44
import com.live2d.sdk.cubism.framework.model.CubismUserModel
import com.live2d.sdk.cubism.framework.motion.CubismMotion
import com.live2d.sdk.cubism.framework.rendering.android.CubismRendererAndroid
import org.json.JSONObject
import java.io.File
import kotlin.math.*
import kotlin.random.Random

/** Native implementation of the official Cubism Java sample loading/update/render pipeline.
 * https://github.com/Live2D/CubismJavaSamples/tree/develop/Sample/src/minimum */
class LivePetModel(private val info: ModelInfo, width: Int, height: Int) : CubismUserModel() {
 private val textures=mutableListOf<Int>()
 private var meshRenderer: NativeMeshRenderer?=null
 private val params=mutableMapOf<String,Int>()
 private val cache=object : LinkedHashMap<String,CubismMotion>(8,0.75f,true) {
  override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String,CubismMotion>?): Boolean = size>8
 }
 private var elapsed=0f
 private var nextBlink=2f+Random.nextFloat()*3
 private var blinkAge=-1f
 private var lastEvent=-1L
 private var expressionUntil=0f
 private var idleAt=0f
 private var bounds=floatArrayOf(-1f,1f,-1f,1f)
 val parameterCount: Int get() = model.parameterCount
 init {
  try {
   val setting=CubismModelSettingJson(info.file.readBytes())
   val dir=info.file.parentFile!!
   loadModel(File(dir,setting.modelFileName).readBytes(),true)
   require(model != null) { "MOC3 gagal dimuat oleh Cubism Core" }
   repeat(model.parameterCount) { params[model.getParameterId(it).string]=it }
   setting.physicsFileName.takeIf { it.isNotEmpty() }?.let { loadPhysics(File(dir,it).readBytes()) }
   setting.poseFileName.takeIf { it.isNotEmpty() }?.let { loadPose(File(dir,it).readBytes()) }
   model.saveParameters(); model.update()
   var minX=Float.POSITIVE_INFINITY; var maxX=Float.NEGATIVE_INFINITY
   var minY=Float.POSITIVE_INFINITY; var maxY=Float.NEGATIVE_INFINITY
   repeat(model.drawableCount) { i ->
    if(model.getDrawableOpacity(i)>0.01f) {
     val v=model.getDrawableVertexPositions(i)
     for(j in v.indices step 2) { minX=min(minX,v[j]); maxX=max(maxX,v[j]); minY=min(minY,v[j+1]); maxY=max(maxY,v[j+1]) }
    }
   }
   if(minX.isFinite() && maxX>minX && maxY>minY) bounds=floatArrayOf(minX,maxX,minY,maxY)
   repeat(setting.textureCount) { i ->
    val path=File(dir,setting.getTextureFileName(i)).path
    val opts=BitmapFactory.Options().apply { inJustDecodeBounds=true }
    BitmapFactory.decodeFile(path,opts)
    var sample=1
    while(max(opts.outWidth,opts.outHeight)/sample>2048) sample*=2
    val bitmap=BitmapFactory.decodeFile(path,BitmapFactory.Options().apply { inSampleSize=sample }) ?: error("Tekstur gagal dibaca")
    try {
     val ids=IntArray(1); glGenTextures(1,ids,0); textures+=ids[0]
     glActiveTexture(GL_TEXTURE0)
     glBindTexture(GL_TEXTURE_2D,ids[0]); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR)
     glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR)
     glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE)
     GLUtils.texImage2D(GL_TEXTURE_2D,0,bitmap,0)
    } finally { bitmap.recycle() }
   }
   glBindTexture(GL_TEXTURE_2D,0)
   meshRenderer=NativeMeshRenderer(model,textures)
  } catch(e: Exception) { close(); throw e }
 }
 private fun add(name: String, value: Float) { params[name]?.let { model.addParameterValue(it,value) } }
 private fun multiply(name: String, value: Float) { params[name]?.let { model.multiplyParameterValue(it,value) } }
 private fun motion(file: File) {
  val m=cache.getOrPut(file.path) { loadMotion(file.readBytes()).apply { fadeInTime=0.45f; fadeOutTime=0.55f } }
  motionManager.startMotionPriority(m,3)
 }
 private fun expression(file: File) {
  expressionManager.startMotionPriority(loadExpression(file.readBytes()),3)
  expressionUntil=elapsed+3.5f
 }
 private fun event(frame: PetFrame,c: PetConfig) {
  if(frame.event==lastEvent) return
  lastEvent=frame.event
  if(!c.reactions) return
  val wave=info.motions.firstOrNull { it.first==c.waveMotion } ?: info.motions.firstOrNull {
   listOf("wave","手を振","挥手","揮手").any { word -> it.first.contains(word,true) || it.second.name.contains(word,true) }
  }
  when(frame.mood) {
   PetMood.REACT -> {
    val selected=info.motions.firstOrNull { it.first==c.tapMotion } ?: wave ?:
     info.motions.firstOrNull { it.first.contains("nod",true) || it.first.contains("tap",true) }
    selected?.let { motion(it.second) }
    val e=info.expressions.firstOrNull { it.first==c.expression } ?: info.expressions.randomOrNull()
    e?.let { expression(it.second) }
   }
   PetMood.EXPRESSION -> if(Random.nextBoolean() && wave!=null) motion(wave.second)
    else info.expressions.randomOrNull()?.let { expression(it.second) }
   else -> Unit
  }
 }
 fun draw(dt: Float,frame: PetFrame,c: PetConfig,width: Int,height: Int) {
  elapsed+=dt
  event(frame,c)
  model.loadParameters()
  if(motionManager.isFinished && elapsed>=idleAt) {
   info.motions.filter { it.first.contains("idle",true) }.randomOrNull()?.let { motion(it.second) }
   idleAt=elapsed+3f+Random.nextFloat()*4f
  }
  motionManager.updateMotion(model,dt)
  model.saveParameters()
  if(c.reactions && expressionUntil>elapsed) expressionManager.updateMotion(model,dt)
  else expressionManager.stopAllMotions()
  // Only existing rig parameters are touched; no generated fake limbs or image-based blinking.
  add("ParamAngleX",frame.gazeX*22f)
  add("ParamAngleY",frame.gazeY*15f+sin(elapsed*0.65f)*2f)
  add("ParamAngleZ",sin(elapsed*0.9f)*1.5f-frame.lean*3f)
  add("ParamBodyAngleX",frame.gazeX*3.5f)
  add("ParamEyeBallX",frame.gazeX*0.65f); add("ParamEyeBallY",frame.gazeY*0.6f)
  add("ParamBreath",sin(elapsed*1.6f)*0.25f)
  if(frame.mood==PetMood.REACT) { add("ParamMouthForm",0.5f); add("ParamEyeLSmile",0.35f); add("ParamEyeRSmile",0.35f) }
  if(elapsed>=nextBlink) { blinkAge=0f; nextBlink=elapsed+2.5f+Random.nextFloat()*4f }
  if(blinkAge>=0f) {
   blinkAge+=dt
   val open=when { blinkAge<0.06f -> 1-blinkAge/0.06f; blinkAge<0.11f -> 0f; blinkAge<0.24f -> (blinkAge-0.11f)/0.13f; else -> 1f }
   multiply("ParamEyeLOpen",open); multiply("ParamEyeROpen",open)
   if(blinkAge>=0.24f) blinkAge=-1f
  }
  physics?.evaluate(model,dt); pose?.updateParameters(model,dt)
  model.update()
  val aspect=width.toFloat()/height.coerceAtLeast(1)
  val s=min(1.8f/(bounds[1]-bounds[0]),1.8f/((bounds[3]-bounds[2])*aspect))
  val matrix=CubismMatrix44.create()
  matrix.scale(s,s*aspect)
  matrix.translate(-(bounds[0]+bounds[1])*0.5f*s,-(bounds[2]+bounds[3])*0.5f*s*aspect)
  meshRenderer?.draw(matrix.array,width,height)
 }
 fun close() {
  meshRenderer?.close();meshRenderer=null
  if(textures.isNotEmpty()) glDeleteTextures(textures.size,textures.toIntArray(),0)
  textures.clear(); cache.clear(); delete()
 }
 fun meshFingerprint(): Double {
  var sum=0.0
  repeat(model.drawableCount) { i ->
   val vertices=model.getDrawableVertexPositions(i)
   for(j in vertices.indices step 6) sum+=vertices[j].toDouble()*(i+1)*(j+1)
  }
  return sum
 }
 fun diagnostics(): String {
  val visible=(0 until model.drawableCount).count { model.getDrawableDynamicFlagIsVisible(it) }
  val opacity=(0 until model.drawableCount).map { model.getDrawableOpacity(it) }
  val points=if(model.drawableCount>0) model.getDrawableVertexPositions(0).take(8) else emptyList()
  return "drawables=${model.drawableCount}, visible=$visible, opacity=${opacity.minOrNull()}..${opacity.maxOrNull()}, modelAlpha=${model.modelOpacity}, bounds=${bounds.toList()}, points=$points, textures=$textures, blend=${model.isBlendModeEnabled}, masks=${model.isUsingMasking}"
 }
}
