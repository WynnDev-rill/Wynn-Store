package id.wynn.furina

import kotlin.math.sqrt

/** Precomputed audio energy, sampled against actual player time, never a timer-based fake mouth. */
data class LipEnvelope(val values: List<Float>,val stepMs: Int=20) {
 fun at(milliseconds: Int): Float {
  if(milliseconds<0 || stepMs<=0) return 0f
  val i=milliseconds/stepMs
  if(i !in values.indices) return 0f
  val next=values.getOrElse(i+1) {0f}
  return (values[i]+(next-values[i])*(milliseconds%stepMs)/stepMs).coerceIn(0f,1f)
 }
 companion object {
  fun fromPcm(pcm: ShortArray,sampleRate: Int): LipEnvelope {
   require(sampleRate in 8000..192000)
   val chunk=(sampleRate/50).coerceAtLeast(1)
   val result=ArrayList<Float>()
   for(start in pcm.indices step chunk) {
    val end=minOf(start+chunk,pcm.size);var energy=0.0
    for(i in start until end) {val s=pcm[i]/32768.0;energy+=s*s}
    val rms=sqrt(energy/(end-start)).toFloat()
    result+=((rms-.012f)*3.5f).coerceIn(0f,1f)
   }
   return LipEnvelope(result)
  }
 }
}

class VoiceGate {
 private var last=Long.MIN_VALUE
 fun allowed(now: Long)=last==Long.MIN_VALUE || now-last>=7000
 fun started(now: Long) {last=now}
}
