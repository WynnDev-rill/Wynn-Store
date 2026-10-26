package id.wynn.furina

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class CoreTest {
 @Test fun longSimulationRemainsBoundedAndVaried() {
  val pet=BehaviorEngine(Random(123)); val states=mutableSetOf<PetMood>(); var hops=0
  repeat(36000) {
   val f=pet.step(1f/60,1f,0.7f,true,true)
   assertTrue(f.x in 0f..1f && f.y in 0f..1f); assertTrue(f.gazeX in -1f..1f)
   assertTrue(f.bounce in 0f..1f); states+=f.mood
   if(f.bounce>0.9f) hops++
  }
  assertTrue(states.containsAll(listOf(PetMood.REST,PetMood.ROAM,PetMood.LOOK,PetMood.HOP,PetMood.EXPRESSION)))
  assertTrue(hops>0)
 }
 @Test fun disabledBehaviorsNeverRun() {
  val pet=BehaviorEngine(Random(10)); pet.place(0.2f,0.7f)
  repeat(18000) {
   val f=pet.step(1f/30,1f,0.9f,false,false)
   assertNotEquals(PetMood.ROAM,f.mood); assertNotEquals(PetMood.HOP,f.mood)
   assertEquals(0.2f,f.x,0f); assertEquals(0.7f,f.y,0f)
  }
 }
 @Test fun gesturesClampAndReactImmediately() {
  val pet=BehaviorEngine(Random(4)); pet.place(-5f,100f)
  assertEquals(0f,pet.frame.x,0f); assertEquals(1f,pet.frame.y,0f)
  pet.react(); assertEquals(PetMood.REACT,pet.frame.mood)
 }
 @Test fun movementIsContinuous() {
  val pet=BehaviorEngine(Random(1)); var old=pet.frame
  repeat(36000) {
   val f=pet.step(1f/60,2f,1f,true,true)
   assertTrue(kotlin.math.abs(f.x-old.x)<0.04f)
   assertTrue(kotlin.math.abs(f.y-old.y)<0.04f); old=f
  }
 }
 @Test fun traversalIsRejected() {
  val root=Files.createTempDirectory("zip-safe").toFile()
  try {
   listOf("../escape","/tmp/escape","a/../../escape","C:/escape","a\\escape").forEach { path ->
    assertThrows(IllegalArgumentException::class.java) { SafeArchive.safeChild(root,path) }
   }
   assertEquals(root.resolve("a/model.moc3").canonicalFile,SafeArchive.safeChild(root,"a/model.moc3"))
  } finally { root.deleteRecursively() }
 }
 @Test fun normalArchiveExtractsAndTraversalDoesNotWrite() {
  fun zip(name: String): ByteArray = ByteArrayOutputStream().apply {
   ZipOutputStream(this).use { out -> out.putNextEntry(ZipEntry(name)); out.write("data".toByteArray()); out.closeEntry() }
  }.toByteArray()
  val root=Files.createTempDirectory("extract").toFile()
  try {
   SafeArchive.extract(ByteArrayInputStream(zip("model/a.txt")),root)
   assertEquals("data",root.resolve("model/a.txt").readText())
   assertThrows(IllegalArgumentException::class.java) { SafeArchive.extract(ByteArrayInputStream(zip("../escape")),root) }
  } finally { root.deleteRecursively() }
 }
 @Test fun largeTextureAboveOldLimitExtractsWithoutLoadingWholeFile() {
  val root=Files.createTempDirectory("large-model").toFile()
  try {
   val archive=root.resolve("input.zip")
   val size=255L*1024*1024
   ZipOutputStream(archive.outputStream().buffered()).use { zip ->
    zip.putNextEntry(ZipEntry("芙宁娜/textures/texture_00.png"))
    val block=ByteArray(65536); var remaining=size
    while(remaining>0) {
     val n=minOf(remaining,block.size.toLong()).toInt()
     zip.write(block,0,n); remaining-=n
    }
    zip.closeEntry()
   }
   archive.inputStream().use { SafeArchive.extract(it,root.resolve("output")) }
   assertEquals(size,root.resolve("output/芙宁娜/textures/texture_00.png").length())
  } finally { root.deleteRecursively() }
 }
 @Test fun oversizedArchiveEntryIsRejectedWithSpecificMessage() {
  val root=Files.createTempDirectory("oversized-model").toFile()
  try {
   // A stored entry exposes its size before data is read: no 501 MB allocation needed.
   val header=ByteArrayOutputStream().apply {
    fun le(value: Long, count: Int) { repeat(count) { write((value shr (it*8)).toInt() and 255) } }
    val name="large.png".toByteArray()
    le(0x04034b50,4); le(20,2); le(0,2); le(0,2); le(0,2); le(0,2)
    le(0,4); le(SafeArchive.MAX_FILE+1,4); le(SafeArchive.MAX_FILE+1,4)
    le(name.size.toLong(),2); le(0,2); write(name)
   }.toByteArray()
   val error=assertThrows(IllegalArgumentException::class.java) {
    SafeArchive.extract(ByteArrayInputStream(header),root)
   }
   assertTrue(error.message!!.contains("500 MB"))
   assertTrue(error.message!!.contains("large.png"))
   assertFalse(root.resolve("large.png").exists())
  } finally { root.deleteRecursively() }
 }
 @Test fun disablingMovementTakesEffectDuringRoam() {
  val engine=BehaviorEngine(Random(12)); engine.action(PetMood.ROAM)
  engine.step(0.1f,1f,1f,true,true)
  val stopped=engine.step(0.1f,1f,1f,false,false)
  assertEquals(PetMood.REST,stopped.mood)
  val x=stopped.x;val y=stopped.y
  repeat(100) { val f=engine.step(0.016f,1f,1f,false,false);assertEquals(x,f.x,0f);assertEquals(y,f.y,0f) }
 }
 @Test fun dragAndTouchAreBoundedAndDecay() {
  val engine=BehaviorEngine(Random(12));engine.drag(-3f,4f,9f)
  assertEquals(0f,engine.frame.x,0f);assertEquals(1f,engine.frame.y,0f)
  assertEquals(1f,engine.frame.dragLean,0f)
  engine.place(0.5f,0.5f);engine.lookAt(1f,-1f)
  repeat(40) {engine.step(0.016f,1f,0f,false,false)}
  assertTrue(engine.frame.gazeX>0.5f);assertTrue(engine.frame.dragLean<0.2f)
 }
 @Test fun locomotionUsesOwnModelMotionAndExplicitSelection() {
  val motions=listOf("Idle" to 1,"Walk cycle" to 2,"Wave" to 3)
  assertEquals(2,MotionCatalog.walk(motions,"")!!.second)
  assertEquals(3,MotionCatalog.walk(motions,"Wave")!!.second)
  assertNull(MotionCatalog.walk(listOf("Idle" to 1),""))
 }
 @Test fun releaseParserSeparatesMissingChannelAndNoUpdate() {
  fun release(tag: String,draft: Boolean=false,apk: Boolean=true)=
   """{"tag_name":"$tag","draft":$draft,"assets":[{"name":"${if(apk) "Furina-Pocket-Pet-2.0.0.apk" else "other.apk"}"}]}"""
  assertFalse(UpdateChecker.parse("[]","1.1.0").published)
  assertFalse(UpdateChecker.parse("[${release("screen-v9.0.0")}]","1.1.0").published)
  assertFalse(UpdateChecker.parse("[${release("furina-v9.0.0",true)}]","1.1.0").published)
  assertFalse(UpdateChecker.parse("[${release("furina-v9.0.0",apk=false)}]","1.1.0").published)
  assertTrue(UpdateChecker.parse("[${release("furina-v1.1.0")}]","1.1.0").published)
  assertNull(UpdateChecker.parse("[${release("furina-v1.1.0")}]","1.1.0").update)
  assertEquals("2.0.0",UpdateChecker.parse("[${release("furina-v1.2.0")},${release("furina-v2.0.0")}]","1.1.0").update!!.version)
 }
 @Test fun hopHasAnticipationFlightAndLanding() {
  val engine=BehaviorEngine(Random(2));engine.action(PetMood.HOP)
  val frames=(0 until 50).map {engine.step(0.016f,1f,1f,true,true)}
  assertTrue(frames.take(8).any {it.crouch>0.3f && it.bounce==0f})
  assertTrue(frames.any {it.bounce>0.95f})
  assertTrue(frames.takeLast(6).any {it.crouch>0f && it.bounce==0f})
 }
 @Test fun roamingExploresBothAxes() {
  val engine=BehaviorEngine(Random(22));var minY=1f;var maxY=0f
  repeat(18000) {
   val f=engine.step(1f/30,1f,1f,true,true)
   minY=minOf(minY,f.y);maxY=maxOf(maxY,f.y)
   assertTrue(f.movement in 0f..1f && f.stride in -1f..1f && f.crouch in 0f..1f)
  }
  assertTrue(maxY-minY>0.5f)
 }
 @Test fun dizzyInterruptsRoamingAndRecovers() {
  val e=BehaviorEngine(Random(3));e.action(PetMood.ROAM);e.step(0.1f,1f,1f,true,true)
  e.dizzy();val x=e.frame.x;val y=e.frame.y
  repeat(40) {val f=e.step(0.1f,1f,1f,true,true);assertEquals(PetMood.DIZZY,f.mood);assertEquals(x,f.x,0f);assertEquals(y,f.y,0f)}
  repeat(5) {e.step(0.1f,1f,1f,true,true)}
  assertNotEquals(PetMood.DIZZY,e.frame.mood)
 }
 @Test fun shakeUsesDensityAndTimeAndIgnoresNormalDragging() {
  val slow=ShakeDetector();slow.begin(0f,0f,1000)
  repeat(10) {slow.move(it*10f,0f,1100L+it*100,1f)}
  assertFalse(slow.isDizzy())
  for(density in listOf(1f,3f)) {
   val fast=ShakeDetector();fast.begin(0f,0f,1000)
   repeat(5) {fast.move((if(it%2==0) 80f else 0f)*density,0f,1020L+it*20,density)}
   assertTrue(fast.isDizzy())
   fast.move(0f,0f,5000,density);assertFalse(fast.isDizzy())
  }
 }
 @Test fun localCommentsAreCausalVariedAndRateLimited() {
  val d=PetDialogue(Random(2));val first=d.line(PetCue.TAP,1000)
  assertNotNull(first);assertNull(d.line(PetCue.TAP,2000))
  assertNull(d.line(PetCue.HOP,3000))
  val next=d.line(PetCue.TAP,50000);assertNotNull(next);assertNotEquals(first,next)
  assertNotNull(d.line(PetCue.DIZZY,56000))
 }
 @Test fun chibiStepKeepsHipFixedAlternatesFeetAndScalesForRun() {
  val phase=kotlin.math.PI.toFloat()/2
  val hip=LegGait.offset(phase,-1f,0f,1f,1f,false);assertEquals(0f,hip.first,0f);assertEquals(0f,hip.second,0f)
  val l=LegGait.offset(phase,-1f,1f,1f,1f,false)
  val r=LegGait.offset(phase,1f,1f,1f,1f,false)
  assertTrue(l.first>0 && r.first<0);assertTrue(l.second>0 && r.second==0f)
  val run=LegGait.offset(phase,-1f,1f,1f,1f,true);assertTrue(run.first>l.first && run.second>l.second)
  assertEquals(0f,LegGait.offset(phase,1f,1f,1f,0f,true).first,0f)
 }
 @Test fun versionComparisonIsNumericAndChannelSpecific() {
  assertTrue(UpdateChecker.newer("furina-v1.10.0","1.9.9"))
  assertFalse(UpdateChecker.newer("furina-v1.0.0","1.0.0"))
  assertFalse(UpdateChecker.newer("furina-v0.9.9","1.0.0"))
 }
}
