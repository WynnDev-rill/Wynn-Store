package id.wynn.furina

import java.nio.FloatBuffer
import kotlin.math.*

data class PosePoint(val x: Float,val y: Float)
data class PoseScene(val mood: PetMood=PetMood.REST,val progress: Float=0f,val amount: Float=0f,
 val left: PosePoint=PosePoint(-.36f,-.33f),val right: PosePoint=PosePoint(.36f,-.33f),val swing: Float=0f)
/** Two affine bone maps in the model's original coordinate system. Immutable per frame. */
class BonePose(private val upper: FloatArray,private val lower: FloatArray) {
 private fun x(m: FloatArray,x: Float,y: Float)=m[0]*x+m[1]*y+m[2]
 private fun y(m: FloatArray,x: Float,y: Float)=m[3]*x+m[4]*y+m[5]
 fun point(x: Float,y: Float,weight: Float,amount: Float): PosePoint {
  if(amount<=0f) return PosePoint(x,y)
  val w=weight.coerceIn(0f,1f);val a=amount.coerceIn(0f,1f)
  return PosePoint(x+(x(upper,x,y)*(1-w)+x(lower,x,y)*w-x)*a,
   y+(y(upper,x,y)*(1-w)+y(lower,x,y)*w-y)*a)
 }
 fun write(out: FloatBuffer,x: Float,y: Float,weight: Float,amount: Float) {
  val w=weight.coerceIn(0f,1f);val a=amount.coerceIn(0f,1f)
  out.put(x+(x(upper,x,y)*(1-w)+x(lower,x,y)*w-x)*a)
  out.put(y+(y(upper,x,y)*(1-w)+y(lower,x,y)*w-y)*a)
 }
}
object BodyPose {
 private const val ORIGINAL_MOC="0f7d1b0e70e9a27cf7afd3851f7dada2858472c3970d791de9fe483874b7f944"
 fun supports(sha: String)=sha==ORIGINAL_MOC
 val leftArms=setOf("ArtMesh18","ArtMesh51","ArtMesh52","ArtMesh53","ArtMesh59")
 val rightArms=setOf("ArtMesh182","ArtMesh183","ArtMesh184","ArtMesh185","ArtMesh186")
 val leftLegs=setOf("ArtMesh108","ArtMesh110")
 val rightLegs=setOf("ArtMesh109","ArtMesh111")
 fun target(side: Int,mood: PetMood,progress: Float): PosePoint = when(mood) {
  PetMood.READ -> PosePoint(side*.20f-.015f,-.27f)
  PetMood.SWING -> PosePoint(side*.29f-.015f,-.13f)
  PetMood.CAKE -> if(side<0) PosePoint(-.17f,-.31f) else {
   val t=max(0f,sin((progress*5f%1f)*PI.toFloat())).let {it*it*(3-2*it)}
   PosePoint(.19f+(-.015f-.19f)*t,-.29f+(.075f+.29f)*t)
  }
  else -> PosePoint(side*.36f,-.33f)
 }
 private fun affine(sx: Float,sy: Float,tx: Float,ty: Float,angle: Float,scaleX: Float=1f,scaleY: Float=1f): FloatArray {
  val c=cos(angle);val s=sin(angle)
  return floatArrayOf(c*scaleX,-s*scaleY,tx-c*scaleX*sx+s*scaleY*sy,
   s*scaleX,c*scaleY,ty-s*scaleX*sx-c*scaleY*sy)
 }
 fun arm(side: Int,target: PosePoint): BonePose {
  val sx=side*.14f;val sy=-.025f;val ex=side*.245f;val ey=-.16f;val wx=side*.36f;val wy=-.33f
  val l1=hypot(ex-sx,ey-sy);val l2=hypot(wx-ex,wy-ey)
  val dx=target.x-sx;val dy=target.y-sy
  val d=hypot(dx,dy).coerceIn(abs(l1-l2)+.00001f,l1+l2-.00001f)
  val base=atan2(dy,dx);val bend=acos(((l1*l1+d*d-l2*l2)/(2*l1*d)).coerceIn(-1f,1f))*side
  val nx=sx+cos(base+bend)*l1;val ny=sy+sin(base+bend)*l1
  val tx=sx+cos(base)*d;val ty=sy+sin(base)*d
  val upperAngle=atan2(ny-sy,nx-sx)-atan2(ey-sy,ex-sx)
  val lowerAngle=atan2(ty-ny,tx-nx)-atan2(wy-ey,wx-ex)
  return BonePose(affine(sx,sy,sx,sy,upperAngle),affine(ex,ey,nx,ny,lowerAngle))
 }
 fun leg(side: Int,progress: Float): BonePose {
  val x=side*.09f-.015f;val ky=-.57f;val hip=-.29f
  val nx=x+side*.018f;val ny=-.46f
  val lowerAngle=sin(progress*PI.toFloat()*10f+side*.6f)*.09f
  val upperAngle=atan2(ny-hip,nx-x)+PI.toFloat()/2f
  return BonePose(affine(x,hip,x,hip,upperAngle,1f,hypot(nx-x,ny-hip)/(hip-ky)),
   affine(x,ky,nx,ny,lowerAngle))
 }
 fun projection(base: FloatArray,pivot: PosePoint,angle: Float): FloatArray {
  val c=cos(angle);val s=sin(angle)
  val tx=pivot.x-c*pivot.x+s*pivot.y;val ty=pivot.y-s*pivot.x-c*pivot.y
  return base.copyOf().also {out -> for(i in 0..3) {
   out[i]=base[i]*c+base[4+i]*s
   out[4+i]=-base[i]*s+base[4+i]*c
   out[12+i]=base[i]*tx+base[4+i]*ty+base[12+i]
  }}
 }
 fun swing(progress: Float)=sin(progress*PI.toFloat()*8f)*.09f
}
class BodyPresentation {
 var scene=PoseScene();private set
 fun step(dt: Float,frame: PetFrame,enabled: Boolean): PoseScene {
  if(!enabled) {scene=PoseScene();return scene}
  val active=frame.mood in ActivityCatalog.sustained
  val mood=if(active) frame.mood else scene.mood
  val progress=if(active) frame.progress else scene.progress
  val k=1f-exp(-dt.coerceIn(0f,.1f)*10f)
  val amount=(scene.amount+((if(active) frame.effect else 0f)-scene.amount)*k).let {if(!active && it<.001f) 0f else it}
  fun smooth(a: PosePoint,b: PosePoint): PosePoint {
   val dx=(b.x-a.x)*k;val dy=(b.y-a.y)*k;val distance=hypot(dx,dy)
   val limit=dt.coerceIn(0f,.1f)*2.4f
   val factor=if(distance>limit && distance>0f) limit/distance else 1f
   return PosePoint(a.x+dx*factor,a.y+dy*factor)
  }
  val angle=if(mood==PetMood.SWING) BodyPose.swing(progress)*amount else 0f
  scene=PoseScene(if(amount==0f) PetMood.REST else mood,progress,amount,
   smooth(scene.left,BodyPose.target(-1,mood,progress)),smooth(scene.right,BodyPose.target(1,mood,progress)),
   scene.swing+(angle-scene.swing)*k)
  return scene
 }
}
