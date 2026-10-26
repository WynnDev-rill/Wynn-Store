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
 @Test fun versionComparisonIsNumericAndChannelSpecific() {
  assertTrue(UpdateChecker.newer("furina-v1.10.0","1.9.9"))
  assertFalse(UpdateChecker.newer("furina-v1.0.0","1.0.0"))
  assertFalse(UpdateChecker.newer("furina-v0.9.9","1.0.0"))
 }
}
