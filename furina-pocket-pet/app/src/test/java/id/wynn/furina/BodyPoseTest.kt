package id.wynn.furina

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class BodyPoseTest {
 @Test fun wristReachesBookForkAndRopeTargets() {
  for(mood in listOf(PetMood.READ,PetMood.CAKE,PetMood.SWING)) for(side in listOf(-1,1)) for(t in listOf(0f,.15f,.5f,.8f)) {
   val target=BodyPose.target(side,mood,t)
   val arm=BodyPose.arm(side,target)
   val p=arm.point(side*.36f,-.33f,1f,1f)
   assertEquals("$mood $side x",target.x,p.x,.0001f)
   assertEquals("$mood $side y",target.y,p.y,.0001f)
  }
 }
 @Test fun bothArmBonesMeetAtTheSameElbow() {
  for(side in listOf(-1,1)) {
   val b=BodyPose.arm(side,BodyPose.target(side,PetMood.READ,.4f))
   val a=b.point(side*.245f,-.16f,0f,1f);val c=b.point(side*.245f,-.16f,1f,1f)
   assertEquals(a.x,c.x,.00001f);assertEquals(a.y,c.y,.00001f)
  }
 }
 @Test fun zeroBlendIsExactlyTheOriginalMeshPosition() {
  val b=BodyPose.arm(-1,BodyPose.target(-1,PetMood.CAKE,.5f))
  for(x in listOf(-.4f,-.2f,0f)) for(y in listOf(-.4f,-.2f,0f)) {
   val p=b.point(x,y,.6f,0f);assertEquals(x,p.x,0f);assertEquals(y,p.y,0f)
   val q=BodyPose.leg(-1,.4f).point(x,y,.4f,0f);assertEquals(x,q.x,0f);assertEquals(y,q.y,0f)
  }
 }
 @Test fun seatedLegKeepsHipAndRaisesKneeAndBoot() {
  for(side in listOf(-1,1)) {
   val b=BodyPose.leg(side,.2f);val x=side*.09f-.015f
   val hip=b.point(x,-.29f,0f,1f);assertEquals(x,hip.x,.00001f);assertEquals(-.29f,hip.y,.00001f)
   val knee=b.point(x,-.57f,0f,1f);val joint=b.point(x,-.57f,1f,1f)
   assertEquals(knee.x,joint.x,.00001f);assertEquals(knee.y,joint.y,.00001f)
   assertTrue(knee.y>-.48f)
   assertTrue(b.point(x,-.83f,1f,1f).y>-.75f)
  }
 }
 @Test fun swingHasBoundedAlternatingPhases() {
  val a=(0..100).map {BodyPose.swing(it/100f)}
  assertTrue(a.max()>.07f);assertTrue(a.min()<-.07f)
  assertTrue(a.all {abs(it)<=.10f});assertEquals(0f,a.first(),.00001f)
 }
 @Test fun onlyTheVerifiedExactModelEnablesTheNativeRig() {
  assertTrue(BodyPose.supports("0f7d1b0e70e9a27cf7afd3851f7dada2858472c3970d791de9fe483874b7f944"))
  assertFalse(BodyPose.supports("whalegirl"));assertFalse(BodyPose.supports(""))
 }
 @Test fun interruptedPresentationFadesOutAndRapidTargetsRemainContinuous() {
  val p=BodyPresentation();var old=PosePoint(0f,0f)
  repeat(120) {i ->
   val f=PetFrame(mood=if(i<60) PetMood.READ else PetMood.CAKE,effect=1f,progress=i/200f)
   val s=p.step(1f/60,f,true)
   if(i>0) assertTrue(hypot(s.right.x-old.x,s.right.y-old.y)<.05f)
   old=s.right
  }
  repeat(80) {p.step(1f/60,PetFrame(mood=PetMood.POKE),true)}
  assertEquals(0f,p.scene.amount,.002f)
 }
 @Test fun unsupportedRigDoesNotCreateASeatScene() {
  val p=BodyPresentation();repeat(100) {p.step(.016f,PetFrame(mood=PetMood.SIT,effect=1f),false)}
  assertEquals(0f,p.scene.amount,0f)
 }
 @Test fun swingProjectionKeepsPivotAndDoesNotMutateBase() {
  val base=floatArrayOf(1.2f,0f,0f,0f,0f,1.5f,0f,0f,0f,0f,1f,0f,.04f,-.1f,0f,1f)
  val copy=base.copyOf();val pivot=PosePoint(-.015f,.94f)
  fun project(m: FloatArray,p: PosePoint)=PosePoint(m[0]*p.x+m[4]*p.y+m[12],m[1]*p.x+m[5]*p.y+m[13])
  val left=BodyPose.projection(base,pivot,-.09f);val right=BodyPose.projection(base,pivot,.09f)
  val original=project(base,pivot)
  for(m in listOf(left,right)) {val p=project(m,pivot);assertEquals(original.x,p.x,.00001f);assertEquals(original.y,p.y,.00001f)}
  assertTrue(project(left,PosePoint(0f,-.7f)).x<project(right,PosePoint(0f,-.7f)).x)
  assertArrayEquals(copy,base,0f)
 }
 @Test fun activitySwitchFadesOldFurnitureAndScaleBeforeReplacingIt() {
  for((old,next) in listOf(PetMood.SWING to PetMood.READ,PetMood.SWING to PetMood.SIT,PetMood.READ to PetMood.SWING)) {
   val p=BodyPresentation();repeat(100) {p.step(1f/60,PetFrame(mood=old,effect=1f,progress=.16f),true)}
   val before=p.scene
   p.step(1f/60,PetFrame(mood=next,effect=1f,progress=.1f),true)
   assertEquals("Outgoing furniture must remain while fading",old,p.scene.mood)
   assertTrue(p.scene.amount<before.amount)
   assertTrue("First transition frame remains smooth",abs(BodyPose.scale(p.scene)-BodyPose.scale(before))<.02f)
   var previous=p.scene;var changed=false
   repeat(140) {
    val scene=p.step(1f/60,PetFrame(mood=next,effect=1f,progress=.1f),true)
    assertTrue("No one-frame scale jump",abs(BodyPose.scale(scene)-BodyPose.scale(previous))<.02f)
    if(scene.mood!=previous.mood) assertTrue("Swap only when old furniture is invisible",previous.amount<.002f)
    if(scene.mood==next && scene.amount>.9f) changed=true
    previous=scene
   }
   assertTrue("Requested activity must eventually enter",changed)
  }
 }
 @Test fun forkRemainsAtTheRenderedWristDuringPartialAndInterruptedBites() {
  val target=PosePoint(-.015f,.075f)
  for(amount in listOf(0f,.25f,.5f,.9f,1f)) {
   val scene=PoseScene(PetMood.CAKE,.1f,amount,right=target)
   val point=BodyPose.wrist(scene,1)
   assertEquals(.36f+(-.015f-.36f)*amount,point.x,.0001f)
   assertEquals(-.33f+(.075f+.33f)*amount,point.y,.0001f)
  }
 }
 @Test fun pageTurnMovesTheRightHandAcrossTheBookWhileLeftSupportsIt() {
  val a=BodyPose.target(1,PetMood.READ,.144f);val b=BodyPose.target(1,PetMood.READ,.182f)
  assertTrue(a.x-b.x>.15f)
  assertEquals(BodyPose.target(-1,PetMood.READ,.144f),BodyPose.target(-1,PetMood.READ,.182f))
 }
 @Test fun eatingHandDoesNotCrossTheFaceAndHasAStableMouthHold() {
  for(i in 0..1000) {val p=BodyPose.target(1,PetMood.CAKE,i/1000f);assertTrue(p.x>=.069f);assertTrue(p.y<=.001f)}
  assertEquals(1f,BodyPose.bite(.14f),.001f)
  assertEquals(1f,BodyPose.bite(.16f),.001f)
 }
}
