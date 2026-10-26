package id.wynn.furina

import kotlin.math.*
import kotlin.random.Random

enum class PetMood { REST, ROAM, LOOK, HOP, REACT, EXPRESSION }
data class PetFrame(val x: Float = 0.5f, val y: Float = 0.8f, val gazeX: Float = 0f,
 val gazeY: Float = 0f, val bounce: Float = 0f, val lean: Float = 0f,
 val mood: PetMood = PetMood.REST, val event: Long = 0)

/** Time-based movement, independent of refresh rate. No synthetic walking on an unsupported rig. */
class BehaviorEngine(private val random: Random = Random.Default) {
 var frame = PetFrame(); private set
 private var age = 0f
 private var duration = 3f
 private var targetX = 0.5f
 private var targetY = 0.8f
 private var startX = 0.5f
 private var startY = 0.8f
 private var gazeX = 0f
 private var gazeY = 0f
 private var event = 0L
 private var previous = PetMood.REST
 fun place(x: Float, y: Float) {
  frame = frame.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f))
  enter(PetMood.REST, 2f)
 }
 fun react() { enter(PetMood.REACT, 2.8f) }
 fun action(mood: PetMood) { enter(mood, if (mood == PetMood.HOP) 0.85f else 4f) }
 fun step(delta: Float, speed: Float, activity: Float, roam: Boolean, jump: Boolean): PetFrame {
  val dt = delta.coerceIn(0f, 0.1f)
  age += dt
  if (age >= duration) {
   val candidates = mutableListOf(PetMood.REST, PetMood.LOOK, PetMood.EXPRESSION)
   if (roam) { candidates += PetMood.ROAM; if (activity > 0.6f) candidates += PetMood.ROAM }
   if (jump && activity > 0.2f) candidates += PetMood.HOP
   val pick = candidates.filter { it != previous }.random(random)
   enter(pick, when (pick) {
    PetMood.ROAM -> random.nextFloat() * 5f + 5f
    PetMood.HOP -> 0.85f
    else -> (random.nextFloat() * 5f + 2.5f) / (0.6f + activity)
   } / speed.coerceIn(0.4f, 2f))
  }
  val t = (age / duration).coerceIn(0f, 1f)
  val eased = t * t * (3f - 2f * t)
  val mode = frame.mood
  val gx = if (mode == PetMood.ROAM) sign(targetX - startX) * 0.65f else gazeX
  val damping = 1f - exp(-dt * 3f)
  frame = frame.copy(
   x = if (mode == PetMood.ROAM) startX + (targetX - startX) * eased else frame.x,
   y = if (mode == PetMood.ROAM) startY + (targetY - startY) * eased else frame.y,
   gazeX = frame.gazeX + (gx - frame.gazeX) * damping,
   gazeY = frame.gazeY + (gazeY - frame.gazeY) * damping,
   bounce = if (mode == PetMood.HOP) sin(PI.toFloat() * t).coerceAtLeast(0f) else 0f,
   lean = if (mode == PetMood.ROAM) sin(PI.toFloat() * t) * sign(targetX - startX) else 0f,
   event = event
  )
  return frame
 }
 private fun enter(mood: PetMood, seconds: Float) {
  previous = mood; age = 0f; duration = seconds.coerceAtLeast(0.4f); event++
  startX = frame.x; startY = frame.y
  targetX = random.nextFloat() * 0.9f + 0.05f
  targetY = (frame.y + (random.nextFloat() - 0.5f) * 0.3f).coerceIn(0.08f, 0.95f)
  gazeX = (random.nextFloat() * 2 - 1) * 0.8f
  gazeY = (random.nextFloat() * 2 - 1) * 0.5f
  frame = frame.copy(mood = mood, bounce = 0f, event = event)
 }
}
