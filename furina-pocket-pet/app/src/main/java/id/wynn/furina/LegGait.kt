package id.wynn.furina

import kotlin.math.*

/** Small frontal chibi steps applied only to explicitly labelled leg meshes.
 * Core vertex arrays stay untouched; the same offsets are used for visible meshes and clipping. */
object LegGait {
 fun offset(phase: Float, side: Float, weight: Float, length: Float, amount: Float, running: Boolean): Pair<Float,Float> {
  val cycle=sin(phase+if(side<0f) 0f else PI.toFloat())
  val w=weight.coerceIn(0f,1f);val a=amount.coerceIn(0f,1f)
  return cycle*length*(if(running) 0.16f else 0.09f)*w*a to
   max(0f,cycle)*length*(if(running) 0.13f else 0.07f)*w*w*a
 }
}
