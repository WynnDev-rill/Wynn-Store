package id.wynn.furina

import kotlin.math.*
import kotlin.random.Random

enum class PetMood { REST, ROAM, LOOK, HOP, REACT, EXPRESSION, DIZZY, NAP, STRETCH, READ, CAKE, SIT, SWING, PETTED, POKE, EDGE }
data class PetFrame(val x: Float = 0.5f, val y: Float = 0.8f, val gazeX: Float = 0f,
 val gazeY: Float = 0f, val bounce: Float = 0f, val lean: Float = 0f,
 val mood: PetMood = PetMood.REST, val event: Long = 0,
 val movement: Float = 0f, val stride: Float = 0f, val crouch: Float = 0f, val dragLean: Float = 0f, val phase: Float = 0f, val running: Boolean = false, val effect: Float = 0f,
 val progress: Float=0f,val peek: Float=0f)

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
 private var phase = 0f
 private var touchAge = 99f
 private var touchX = 0f
 private var touchY = 0f
 var supportedActivities: Set<PetMood> = setOf(PetMood.READ,PetMood.CAKE)
 private var energy=.75f
 private var curiosity=.5f
 fun pet() {energy=(energy+.025f).coerceAtMost(1f);enter(PetMood.PETTED,3f)}
 fun poke() {enter(PetMood.POKE,2f)}
 fun lookAt(x: Float,y: Float) { touchX=x.coerceIn(-1f,1f); touchY=y.coerceIn(-1f,1f); touchAge=0f }
 fun drag(x: Float,y: Float,lean: Float) {
  frame=frame.copy(x=x.coerceIn(0f,1f),y=y.coerceIn(0f,1f),dragLean=lean.coerceIn(-1f,1f),movement=0f,bounce=0f,stride=0f,effect=0f,peek=0f,mood=PetMood.REST,gazeX=frame.gazeX+(lean.coerceIn(-1f,1f)-frame.gazeX)*0.3f,gazeY=-0.3f)
  lookAt(lean,-0.3f)
 }
 fun place(x: Float, y: Float) {
  frame = frame.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f))
  enter(PetMood.REST, 2f)
 }
 fun dizzy() { enter(PetMood.DIZZY,4.2f) }
 fun react() { enter(PetMood.REACT, 2.8f) }
 fun action(mood: PetMood) { enter(mood, when(mood) { PetMood.HOP -> .85f;PetMood.EDGE -> 12f;in ActivityCatalog.sustained -> 22f;else -> 4f }) }
 fun step(delta: Float, speed: Float, activity: Float, roam: Boolean, jump: Boolean, activities: Boolean=false, edges: Boolean=false, quiet: Boolean=false): PetFrame {
  val dt = delta.coerceIn(0f, 0.1f)
  age += dt; touchAge += dt
  energy=(energy+dt*(if(frame.mood==PetMood.NAP || frame.mood==PetMood.REST || frame.mood==PetMood.READ) .003f else -.0012f)).coerceIn(.1f,1f)
  curiosity=(curiosity+dt*(if(frame.mood in ActivityCatalog.sustained) -.006f else .002f)).coerceIn(0f,1f)
  if(!roam && frame.mood==PetMood.ROAM || !jump && frame.mood==PetMood.HOP) enter(PetMood.REST,1.5f)
  if(!activities && frame.mood in ActivityCatalog.sustained || !edges && frame.mood==PetMood.EDGE) enter(PetMood.REST,2f)
  if (age >= duration) {
   val candidates = mutableListOf(PetMood.REST, PetMood.LOOK, PetMood.EXPRESSION,PetMood.STRETCH)
   if(activity<0.7f || quiet || energy<.35f) candidates += PetMood.NAP
   if(activities) {candidates+=supportedActivities; if(curiosity>.65f) candidates+=supportedActivities}
   if(edges && roam) candidates+=PetMood.EDGE
   if (roam) { candidates += PetMood.ROAM; if (activity > 0.6f) candidates += PetMood.ROAM }
   if (jump && activity > 0.2f) candidates += PetMood.HOP
   val pick = candidates.filter { it != previous }.random(random)
   enter(pick, when (pick) {
    PetMood.ROAM -> random.nextFloat() * 5f + 5f
    PetMood.NAP -> 8f+random.nextFloat()*6f
    PetMood.STRETCH -> 3.2f
    in ActivityCatalog.sustained -> 16f+random.nextFloat()*16f
    PetMood.EDGE -> 12f+random.nextFloat()*6f
    PetMood.HOP -> 0.85f
    else -> (random.nextFloat() * 5f + 2.5f) / (0.6f + activity)
   }.let {if(pick in ActivityCatalog.sustained || pick==PetMood.NAP || pick==PetMood.EDGE) it else it/speed.coerceIn(.4f,2f)})
  }
  val t = (age / duration).coerceIn(0f, 1f)
  val eased = t * t * (3f - 2f * t)
  val mode = frame.mood
  val gx = if(touchAge<2.5f) touchX else when(mode) {PetMood.ROAM -> sign(targetX-startX)*.65f;PetMood.READ -> sin(age*.75f)*.24f;PetMood.CAKE -> .1f;else -> gazeX}
  val gy = if(touchAge<2.5f) touchY else when(mode) {PetMood.READ -> -.65f;PetMood.CAKE -> -.4f;else -> gazeY}
  val movement=if(mode==PetMood.ROAM) (6f*t*(1f-t)).coerceAtMost(1f) else 0f
  phase += dt * (5f+speed*3f) * movement
  val jumpT=((t-0.18f)/0.64f).coerceIn(0f,1f)
  val damping = 1f - exp(-dt * 3f)
  val edgeT=(age/2.8f).coerceIn(0f,1f).let {it*it*(3-2*it)}
  val strength=min(age/.65f,(duration-age)/.75f).coerceIn(0f,1f)
  frame = frame.copy(
   x = when(mode) {PetMood.ROAM -> startX+(targetX-startX)*eased;PetMood.EDGE -> startX+(targetX-startX)*edgeT;else -> frame.x},
   y = if (mode == PetMood.ROAM) startY + (targetY - startY) * eased else frame.y,
   gazeX = frame.gazeX + (gx - frame.gazeX) * damping,
   gazeY = frame.gazeY + (gy - frame.gazeY) * damping,
   bounce = if (mode == PetMood.HOP) (4f*jumpT*(1f-jumpT)).coerceAtLeast(0f) else 0f,
   lean = if (mode == PetMood.ROAM) sin(PI.toFloat() * t) * sign(targetX - startX) else 0f,
   event = event, movement=movement, stride=sin(phase)*movement,
   crouch=if(mode==PetMood.HOP) (if(t<0.18f) sin(PI.toFloat()*t/0.18f) else if(t>0.82f) sin(PI.toFloat()*(t-0.82f)/0.18f)*0.6f else 0f) else 0f,
   dragLean=frame.dragLean*(1f-damping),phase=phase,running=mode==PetMood.ROAM && speed>=1.4f,
   effect=when(mode) {PetMood.DIZZY,PetMood.NAP,PetMood.STRETCH -> sin(PI.toFloat()*t).coerceAtLeast(0f);in ActivityCatalog.sustained,PetMood.PETTED,PetMood.POKE,PetMood.EDGE -> strength;else -> 0f},
   progress=t,peek=if(mode==PetMood.EDGE) sign(targetX-.5f)*.38f*((age-4)/2).coerceIn(0f,1f)*strength else 0f
  )
  return frame
 }
 private fun enter(mood: PetMood, seconds: Float) {
  previous = mood; age = 0f; duration = seconds.coerceAtLeast(0.4f); event++
  startX = frame.x; startY = frame.y
  targetX = if(frame.x>0.5f) random.nextFloat()*0.42f+0.04f else 0.54f+random.nextFloat()*0.42f
  targetY = random.nextFloat()*0.84f+0.08f
  gazeX = (random.nextFloat() * 2 - 1) * 0.8f
  gazeY = (random.nextFloat() * 2 - 1) * 0.5f
  if(mood==PetMood.EDGE) targetX=if(frame.x<.5f) 0f else 1f
  frame = frame.copy(mood = mood, bounce = 0f, event = event,effect=0f,peek=0f,progress=0f,movement=0f,stride=0f,crouch=0f)
 }
}
