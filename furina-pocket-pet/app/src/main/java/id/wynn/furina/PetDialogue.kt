package id.wynn.furina

import kotlin.random.Random
import kotlin.math.*

enum class PetCue { HELLO, TAP, DRAG, DIZZY, HOP, ROAM, RUN, NAP, STRETCH, IDLE, BATTERY, WAKE }

/** Local reactive dialogue, not AI, OCR, screen recording, or app surveillance. */
class PetDialogue(private val random: Random=Random.Default) {
 private var lastAt=-100_000L
 private var lastLine=""
 private val cueTimes=mutableMapOf<PetCue,Long>()
 fun line(cue: PetCue,now: Long): String? {
  val gap=if(cue==PetCue.DIZZY) 5000 else 15000
  if(now-lastAt<gap || now-(cueTimes[cue] ?: -100_000L)<35000) return null
  val choices=when(cue) {
   PetCue.HELLO -> listOf("Panggung kecilku sudah siap!","Aku ikut menemanimu, ya.","Furina hadir. Mulai pertunjukannya!")
   PetCue.TAP -> listOf("Hm? Memanggilku?","Aku di sini, kok.","Hehe, ketahuan sedang melamun.")
   PetCue.DRAG -> listOf("Mau pindah ke mana?","Pelan-pelan, topiku bisa jatuh!","Baiklah, aku ikut.")
   PetCue.DIZZY -> listOf("🌀 Tunggu... lantainya berputar!","🌀 Pelan-pelan! Aku pusing...","🌀 Beri aku waktu menyeimbangkan diri.")
   PetCue.HOP -> listOf("Hop! Pendaratan yang anggun.","Lihat, masih lincah!","Satu lompatan kecil!")
   PetCue.RUN -> listOf("Permisi, aku lewat!","Kejar aku kalau bisa!","Aku ke sebelah sana sebentar!")
   PetCue.ROAM -> listOf("Aku jalan-jalan sebentar.","Sudut sana kelihatannya nyaman.","Jangan khawatir, aku masih di sini.")
   PetCue.NAP -> listOf("Aku istirahat sebentar, ya...","Hanya memejamkan mata sebentar.","Zzz... pertunjukannya nanti lagi.")
   PetCue.STRETCH -> listOf("Ahh, regangkan badan dulu.","Berdiri terus juga melelahkan.","Siap untuk babak berikutnya!")
   PetCue.BATTERY -> listOf("Baterainya mulai lelah. Aku ikut tenang dulu.","Mungkin sebentar lagi waktunya mengisi daya.")
   PetCue.WAKE -> listOf("Selamat datang kembali!","Oh, layarnya menyala lagi.","Aku sudah siap menemanimu lagi.")
   PetCue.IDLE -> listOf("Aku masih di sini. Santai saja.","Tidak setiap saat harus ada pertunjukan.","Sesekali istirahatkan mata juga, ya.")
  }
  val result=choices.filter { it!=lastLine }.random(random)
  lastAt=now;lastLine=result;cueTimes[cue]=now
  return result
 }
}

/** Velocity is measured in dp/s with time and travel thresholds, so event rate/density do not change sensitivity. */
class ShakeDetector {
 private var lastX=0f;private var lastY=0f;private var lastTime=0L;private var energy=0f
 fun begin(x: Float,y: Float,time: Long) {lastX=x;lastY=y;lastTime=time;energy=0f}
 fun move(x: Float,y: Float,time: Long,density: Float) {
  val dt=(time-lastTime).coerceAtLeast(1)/1000f
  val distance=hypot(x-lastX,y-lastY)/density.coerceAtLeast(0.5f)
  val velocity=distance/dt
  energy=max(0f,energy-dt*300f)+if(velocity>850f && distance>5f) distance else 0f
  lastX=x;lastY=y;lastTime=time
 }
 fun isDizzy()=energy>=140f
}
