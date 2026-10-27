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
 private var locomotion=0f
 private var wasRoaming=false
 private val lipIds=mutableListOf<Int>()
 private var mouthLevel=0f
 private var previousMood=PetMood.REST
 private var activityFile: File?=null
 private val bodyPresentation=BodyPresentation()
 var stageScene=PoseScene();private set
 var stageMatrix=FloatArray(16);private set
 private var bounds=floatArrayOf(-1f,1f,-1f,1f)
 val parameterCount: Int get() = model.parameterCount
 init {
  try {
   val setting=CubismModelSettingJson(info.file.readBytes())
   val dir=info.file.parentFile!!
   loadModel(File(dir,setting.modelFileName).readBytes(),true)
   require(model != null) { "MOC3 gagal dimuat oleh Cubism Core" }
   repeat(model.parameterCount) { params[model.getParameterId(it).string]=it }
   repeat(setting.lipSyncParameterCount) {i -> params[setting.getLipSyncParameterId(i).string]?.let {lipIds+=it}}
   if(lipIds.isEmpty()) params["ParamMouthOpenY"]?.let {lipIds+=it}
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
   val legParts=mutableSetOf<String>()
   setting.displayInfoFileName.takeIf {it.isNotEmpty()}?.let { name ->
    val display=JSONObject(File(dir,name).readText()).optJSONArray("Parts")
    if(display!=null) repeat(display.length()) { i ->
     val item=display.getJSONObject(i); val label=item.optString("Name").lowercase()
     if(label=="腿" || label=="足" || label=="legs" || label=="leg" || label=="left leg" || label=="right leg") legParts+=item.getString("Id")
    }
   }
   meshRenderer=NativeMeshRenderer(model,textures,legParts,info.bodyRig)
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
  if(previousMood in ActivityCatalog.sustained || frame.mood in ActivityCatalog.sustained || frame.mood in setOf(PetMood.PETTED,PetMood.POKE)) motionManager.stopAllMotions()
  previousMood=frame.mood
  val selected=when(frame.mood) {PetMood.SIT -> c.sitMotion;PetMood.SWING -> c.swingMotion;else -> ""}
  val activity=info.motions.firstOrNull {selected.isNotEmpty() && it.first==selected} ?:
   ActivityCatalog.motion(info.motions.map {"${it.first} ${it.second.name}" to it.second},frame.mood)
  activityFile=activity?.second
  activityFile?.let {motion(it)}
  if(!c.reactions) return
  val wave=info.motions.firstOrNull { it.first==c.waveMotion } ?: info.motions.firstOrNull {
   listOf("wave","手を振","挥手","揮手").any { word -> it.first.contains(word,true) || it.second.name.contains(word,true) }
  }
  when(frame.mood) {
   PetMood.REACT -> {
    val selected=info.motions.firstOrNull { it.first==c.tapMotion } ?: wave ?:
     info.motions.firstOrNull { it.first.contains("nod",true) || it.first.contains("tap",true) }
    selected?.let { motion(it.second) }
    val e=info.expressions.firstOrNull { it.first==c.expression } ?: info.expressions.firstOrNull { it.first.contains("挥手") || it.first.contains("wave",true) } ?: info.expressions.randomOrNull()
    e?.let { expression(it.second) }
   }
   PetMood.EXPRESSION -> if(Random.nextBoolean() && wave!=null) motion(wave.second)
    else info.expressions.randomOrNull()?.let { expression(it.second) }
   PetMood.PETTED,PetMood.POKE -> {
    val words=if(frame.mood==PetMood.PETTED) listOf("heart","blush","爱心","脸红","照れ") else listOf("angry","生气","怒")
    info.expressions.firstOrNull {entry -> words.any {entry.first.contains(it,true)}}?.let {expression(it.second)}
   }
   else -> Unit
  }
 }
 fun draw(dt: Float,frame: PetFrame,c: PetConfig,width: Int,height: Int,voiceLevel: Float=0f,speaking: Boolean=false,beforeBody: ((PoseScene,FloatArray)->Unit)?=null) {
  elapsed+=dt
  if(wasRoaming != (frame.mood==PetMood.ROAM)) motionManager.stopAllMotions()
  wasRoaming=frame.mood==PetMood.ROAM
  event(frame,c)
  stageScene=bodyPresentation.step(dt,frame,info.bodyRig && c.bodyMotion && c.activities && activityFile==null)
  model.loadParameters()
  val walk=MotionCatalog.walk(info.motions,c.walkMotion)
  if(frame.mood==PetMood.ROAM && walk!=null) {
   if(motionManager.isFinished) motion(walk.second)
  } else if(frame.mood in ActivityCatalog.sustained && motionManager.isFinished) {
   activityFile?.let {motion(it)}
  } else if(frame.mood !in ActivityCatalog.sustained && motionManager.isFinished && elapsed>=idleAt) {
   info.motions.filter { it.first.contains("idle",true) }.randomOrNull()?.let { motion(it.second) }
   idleAt=elapsed+3f+Random.nextFloat()*4f
  }
  motionManager.updateMotion(model,dt)
  model.saveParameters()
  if(c.reactions && expressionUntil>elapsed) expressionManager.updateMotion(model,dt)
  else expressionManager.stopAllMotions()
  locomotion+=(frame.movement-locomotion)*(1f-exp(-dt*7f))
  add("ParamBodyAngleZ",(frame.stride*1.5f+frame.dragLean*7f)*(1f-stageScene.amount))
  if(frame.mood==PetMood.DIZZY) {
   add("ParamAngleZ",sin(elapsed*9f)*9f*frame.effect)
   add("ParamEyeBallX",sin(elapsed*12f)*0.7f*frame.effect)
   add("ParamEyeBallY",cos(elapsed*12f)*0.5f*frame.effect)
   add("ParamMouthOpenY",0.2f*frame.effect)
  }
  if(frame.mood==PetMood.NAP) {
   multiply("ParamEyeLOpen",1f-frame.effect);multiply("ParamEyeROpen",1f-frame.effect)
   add("ParamAngleZ",frame.effect*9f)
  }
  if(frame.mood==PetMood.STRETCH) {add("ParamBodyAngleY",frame.effect*4f);add("ParamAngleY",frame.effect*12f)}
  if(frame.mood==PetMood.READ) {
   add("ParamBrowLY",-.25f*frame.effect);add("ParamBrowRY",-.25f*frame.effect)
   add("ParamBrowLForm",-.35f*frame.effect);add("ParamBrowRForm",-.35f*frame.effect)
   add("ParamAngleZ",sin(elapsed*.55f)*4f*frame.effect)
  }
  if(frame.mood==PetMood.CAKE) {
   add("ParamMouthOpenY",(.12f+max(0f,sin(frame.progress*44f))*.3f)*frame.effect)
   add("ParamMouthForm",.6f*frame.effect);add("ParamEyeLSmile",.4f*frame.effect);add("ParamEyeRSmile",.4f*frame.effect)
  }
  if(frame.mood==PetMood.PETTED) {
   add("ParamMouthForm",.75f*frame.effect);add("ParamEyeLSmile",.6f*frame.effect);add("ParamEyeRSmile",.6f*frame.effect)
   multiply("ParamEyeLOpen",1f-.25f*frame.effect);multiply("ParamEyeROpen",1f-.25f*frame.effect)
   add("ParamAngleZ",sin(elapsed*2f)*5f*frame.effect)
  }
  if(frame.mood==PetMood.POKE) {add("ParamBrowLForm",-.6f*frame.effect);add("ParamBrowRForm",-.6f*frame.effect);add("ParamMouthForm",-.6f*frame.effect);add("ParamAngleZ",-8f*frame.effect)}
  if(frame.mood==PetMood.EDGE) add("ParamAngleZ",-sign(frame.x-.5f)*8f*frame.effect)
  add("ParamBodyAngleY",-frame.crouch*5f)
  // Only existing rig parameters are touched; no generated fake limbs or image-based blinking.
  add("ParamAngleX",frame.gazeX*22f)
  add("ParamAngleY",frame.gazeY*15f+sin(elapsed*0.65f)*2f)
  add("ParamAngleZ",sin(elapsed*0.9f)*1.5f-frame.lean*3f)
  add("ParamBodyAngleX",frame.gazeX*3.5f*(1f-stageScene.amount))
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
  val wasSpeaking=mouthLevel>.001f
  mouthLevel+=((if(speaking) voiceLevel.coerceIn(0f,1f) else 0f)-mouthLevel)*(1f-exp(-dt*22f))
  if(speaking || wasSpeaking) lipIds.forEach {id -> model.setParameterValue(id,mouthLevel)}
  model.update()
  val aspect=width.toFloat()/height.coerceAtLeast(1)
  val s=BodyPose.scale(stageScene)*min(1.8f/(bounds[1]-bounds[0]),1.8f/((bounds[3]-bounds[2])*aspect))
  val matrix=CubismMatrix44.create()
  val bob=if(frame.mood==PetMood.ROAM && walk==null) abs(frame.stride)*0.025f else 0f
  matrix.scale(s,s*aspect)
  matrix.translate(-(bounds[0]+bounds[1])*0.5f*s+frame.peek,-(bounds[2]+bounds[3])*0.5f*s*aspect+bob-frame.crouch*0.025f)
  meshRenderer?.gaitFrame=frame
  meshRenderer?.gaitEnabled=c.legMotion && walk==null
  stageMatrix=BodyPose.projection(matrix.array,PosePoint(-.015f,.94f),stageScene.swing)
  meshRenderer?.present(stageScene)
  beforeBody?.invoke(stageScene,stageMatrix)
  meshRenderer?.draw(stageMatrix,width,height)
 }
 fun drawForeground(width: Int,height: Int) {
  meshRenderer?.draw(stageMatrix,width,height,true)
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
  return "legMeshes=${meshRenderer?.animatedLegMeshes}, drawables=${model.drawableCount}, visible=$visible, opacity=${opacity.minOrNull()}..${opacity.maxOrNull()}, modelAlpha=${model.modelOpacity}, bounds=${bounds.toList()}, points=$points, textures=$textures, blend=${model.isBlendModeEnabled}, masks=${model.isUsingMasking}"
 }
}
