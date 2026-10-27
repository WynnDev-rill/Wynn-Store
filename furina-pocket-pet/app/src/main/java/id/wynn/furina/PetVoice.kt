package id.wynn.furina

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONObject
import java.io.File

/** One short local clip at a time. No microphone, synthesis, network, or recurring idle speech. */
class PetVoice(private val context: Context) {
 private val audio=context.getSystemService(AudioManager::class.java)
 private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
 private val handler=Handler(Looper.getMainLooper())
 private val gate=VoiceGate()
 private var player: MediaPlayer?=null
 private var envelope=LipEnvelope(emptyList())
 private var preparing=false
 private var generation=0L
 private var lastClip=""
 private var focusHeld=false
 private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
  .setAudioAttributes(attributes).setOnAudioFocusChangeListener({change -> if(change<0) stop()},handler).build()
 private val manifest=runCatching {
  val clips=JSONObject(context.assets.open("personal/voice/manifest.json").bufferedReader().use {it.readText()}).getJSONArray("clips")
  (0 until clips.length()).map {clips.getJSONObject(it)}.filter {entry ->
   entry.optString("id").matches(Regex("[a-z0-9_-]{1,40}")) && entry.optInt("durationMs") in 50..6000
  }.take(32)
 }.getOrDefault(emptyList())
 val available: Boolean get()=manifest.isNotEmpty()
 val playing: Boolean get()=runCatching {player?.isPlaying==true}.getOrDefault(false)
 val pending: Boolean get()=preparing
 private val timeout=Runnable {stop();message("Pemutar audio melewati batas waktu; coba lagi")}
 private fun message(value: String) {PetRuntime.update {it.copy(voiceMessage=value)}}
 private fun audible(): Boolean = audio.getStreamVolume(AudioManager.STREAM_MUSIC)>0 &&
  context.getSystemService(NotificationManager::class.java).currentInterruptionFilter==NotificationManager.INTERRUPTION_FILTER_ALL
 fun play(cue: String,volume: Float,preview: Boolean=false): Boolean {
  if(preview) stop()
  if(!available) {message("Paket suara Jepang tidak tersedia");return false}
  if(volume<=0f || audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0) {message("Naikkan volume media dan volume Furina");return false}
  if(!audible()) {message("Suara dibatasi mode Jangan ganggu");return false}
  if(!volume.isFinite() || volume<=0f || (!preview && !gate.allowed(SystemClock.elapsedRealtime())) || player!=null || !audible()) return false
  val options=manifest.filter {it.optString("cue")==cue}
  val clip=options.filter {it.optString("id")!=lastClip}.ifEmpty {options}.randomOrNull() ?: return false
  return runCatching {
   val id=clip.getString("id")
   val digest=clip.getString("sha256").also {require(it.matches(Regex("[a-f0-9]{64}")))}
   val dir=File(context.cacheDir,"pet-voice").apply {mkdirs()};val file=File(dir,"$id-${digest.take(12)}.ogg")
   if(!file.isFile) context.assets.open("personal/voice/$id.ogg").use {src -> file.outputStream().use {src.copyTo(it)}}
   val json=JSONObject(context.assets.open("personal/voice/$id.json").bufferedReader().use {it.readText()})
   val values=json.getJSONArray("values");require(values.length()<=400)
   val e=LipEnvelope((0 until values.length()).map {values.getDouble(it).toFloat().coerceIn(0f,1f)},json.getInt("stepMs").also {require(it in 10..100)})
   playLocal(file,e,volume).also {if(it) lastClip=id}
  }.getOrElse {error -> stop();message("Suara gagal: ${error.message ?: "berkas tidak dapat dibaca"}");false}
 }
 /** Also used by instrumentation with a generated PCM fixture, through the real Android player. */
 internal fun playLocal(file: File,levels: LipEnvelope,volume: Float): Boolean {
  if(!volume.isFinite() || volume<=0f || player!=null || !audible() || !file.isFile || file.length()>2*1024*1024) return false
  if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {message("Audio sedang dipakai aplikasi lain; coba lagi");return false}
  focusHeld=true
  val token=++generation
  return try {
   envelope=levels;preparing=true
   val next=MediaPlayer();player=next
   next.setAudioAttributes(attributes);next.setVolume(volume.coerceIn(0f,1f),volume.coerceIn(0f,1f))
   next.setDataSource(file.path)
   next.setOnPreparedListener {p ->
    if(token!=generation || player!==p) return@setOnPreparedListener
    if(!audible()) {stop();return@setOnPreparedListener}
    preparing=false
    handler.removeCallbacks(timeout)
    runCatching {
     p.start();gate.started(SystemClock.elapsedRealtime())
     PetRuntime.update {it.copy(voicePlaying=true,voiceMessage="Memutar suara Jepang")}
     // Preparation has its own budget; never charge it against the clip duration.
     handler.postDelayed(timeout,p.duration.coerceIn(50,6000).toLong()+5000L)
    }.onFailure {error -> stop();message("Audio gagal dimulai: ${error.message ?: "coba lagi"}")}
   }
   next.setOnCompletionListener {if(token==generation) {stop();message("Suara selesai diputar")}}
   next.setOnErrorListener {_,what,extra -> if(token==generation) {stop();message("Pemutar audio gagal ($what/$extra)")};true}
   handler.postDelayed(timeout,10000);next.prepareAsync();true
  } catch(_: Exception) {stop();false}
 }
 fun level(): Float {
  // An idle companion must not poll three Android audio services on every animation frame.
  if(player==null) return 0f
  if(!audible()) {if(player!=null) stop();return 0f}
  val value=runCatching {if(playing) envelope.at(player!!.currentPosition) else 0f}.getOrDefault(0f)
  return value.coerceIn(0f,1f)
 }
 fun stop() {
  generation++;handler.removeCallbacks(timeout)
  player?.let {p -> runCatching {p.setOnPreparedListener(null);p.setOnCompletionListener(null);p.setOnErrorListener(null);p.release()}}
  player=null;preparing=false;envelope=LipEnvelope(emptyList())
  if(focusHeld) {audio.abandonAudioFocusRequest(focus);focusHeld=false}
  PetRuntime.update {it.copy(voicePlaying=false,lipLevel=0f)}
 }
}
