package id.wynn.furina

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CompanionTest {
 @Test fun strokeIsDistinctFromDragAndCancellation() {
  val s=PetTouch();s.begin(0f,0f,1000,true)
  repeat(5) {s.move(if(it%2==0) 13f else -13f,0f,1020L+it*30)}
  assertEquals(TouchKind.PET,s.finish(1250,false))
  val d=PetTouch();d.begin(0f,0f,1000,true)
  assertEquals(TouchKind.DRAG,d.move(60f,0f,1040))
  assertEquals(TouchKind.DRAG,d.finish(1080,false))
  val c=PetTouch();c.begin(0f,0f,1000,false)
  assertEquals(TouchKind.CANCEL,c.finish(1300,true))
 }
 @Test fun touchZonesHoldAndQuickTapAreCausal() {
  val t=PetTouch();t.begin(0f,0f,1000,true);assertEquals(TouchKind.HEAD,t.finish(1100,false))
  t.begin(0f,0f,1200,false);assertEquals(TouchKind.BODY,t.finish(1250,false))
  t.begin(0f,0f,1500,true);assertEquals(TouchKind.HOLD,t.finish(2300,false))
 }
 @Test fun sustainedActivitiesInterruptImmediatelyAndDisableCleanly() {
  val e=BehaviorEngine(Random(4));e.action(PetMood.READ)
  repeat(50) {assertEquals(PetMood.READ,e.step(.1f,1f,.5f,false,false,true).mood)}
  e.pet();assertEquals(PetMood.PETTED,e.frame.mood)
  e.action(PetMood.CAKE)
  assertEquals(PetMood.REST,e.step(.1f,1f,.5f,false,false,false).mood)
 }
 @Test fun lifeHasVarietyWithoutUnsupportedSittingOrPositionJumps() {
  val e=BehaviorEngine(Random(73));val seen=mutableSetOf<PetMood>();var prev=e.frame
  repeat(72000) {
   val f=e.step(1f/30,1f,.5f,true,true,true,true)
   seen+=f.mood
   assertTrue(f.x in 0f..1f && f.y in 0f..1f)
   assertTrue(kotlin.math.abs(f.x-prev.x)<.05f && kotlin.math.abs(f.y-prev.y)<.05f)
   assertTrue(f.effect in 0f..1f && f.progress in 0f..1f)
   assertFalse(f.mood==PetMood.SIT || f.mood==PetMood.SWING);prev=f
  }
  assertTrue(seen.containsAll(listOf(PetMood.READ,PetMood.CAKE,PetMood.EDGE)))
 }
 @Test fun edgeDisabledMidMoveStopsAndDoesNotLeavePeek() {
  val e=BehaviorEngine(Random(8));e.action(PetMood.EDGE)
  repeat(70) {e.step(.1f,1f,.5f,true,true,true,true)}
  assertTrue(kotlin.math.abs(e.frame.peek)>0f)
  val f=e.step(.1f,1f,.5f,true,true,true,false)
  assertEquals(PetMood.REST,f.mood);assertEquals(0f,f.peek,0f)
 }
 @Test fun activityMatcherDoesNotInventPoseOrMatchVisit() {
  assertNull(ActivityCatalog.motion(listOf("Visit home" to 1),PetMood.SIT))
  assertEquals(2,ActivityCatalog.motion(listOf("Sit loop" to 2),PetMood.SIT)!!.second)
  assertEquals(3,ActivityCatalog.motion(listOf("読書" to 3),PetMood.READ)!!.second)
  assertNull(ActivityCatalog.motion(listOf("wave" to 1),PetMood.SWING))
 }
 @Test fun envelopeTracksSilencePeaksAndBounds() {
  val pcm=ShortArray(16000) {if(it in 4800..9600) (12000*kotlin.math.sin(it*.2)).toInt().toShort() else 0}
  val e=LipEnvelope.fromPcm(pcm,16000)
  assertEquals(0f,e.at(100),.001f);assertTrue(e.at(450)>.2f)
  assertEquals(0f,e.at(-1),0f);assertEquals(0f,e.at(1200),0f)
  assertTrue(e.values.all {it in 0f..1f})
  assertEquals(0f,LipEnvelope.fromPcm(ShortArray(1000),16000).at(10),0f)
 }
 @Test fun voiceGateHonorsCooldownAndFailedPlaybackDoesNotConsumeIt() {
  val g=VoiceGate()
  assertTrue(g.allowed(1000));assertTrue(g.allowed(1100))
  g.started(1200);assertFalse(g.allowed(2000));assertTrue(g.allowed(9000))
 }
}
