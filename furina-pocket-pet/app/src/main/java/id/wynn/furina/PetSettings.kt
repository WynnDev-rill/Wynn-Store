package id.wynn.furina

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PetConfig(val size: Float = 170f, val speed: Float = 1f, val activity: Float = 0.55f,
 val roam: Boolean = true, val jump: Boolean = true, val reactions: Boolean = true,
 val fps: Int = 30, val paused: Boolean = false, val hidden: Boolean = false,
 val x: Float = 0.5f, val y: Float = 0.8f, val model: String = "", val tapMotion: String = "",
 val waveMotion: String = "", val expression: String = "", val walkMotion: String = "", val comments: Boolean = true, val legMotion: Boolean = true,
 val activities: Boolean=true,val edges: Boolean=true,val ghost: Boolean=false,val voice: Boolean=true,val voiceVolume: Float=.55f,
 val sitMotion: String="",val swingMotion: String="",val bodyMotion: Boolean=true)

class PetSettings(context: Context) {
 private val p = context.applicationContext.getSharedPreferences("pet", Context.MODE_PRIVATE)
 fun read() = PetConfig(p.getFloat("size",170f), p.getFloat("speed",1f), p.getFloat("activity",0.55f),
  p.getBoolean("roam",true), p.getBoolean("jump",true), p.getBoolean("reactions",true),
  p.getInt("fps",30), p.getBoolean("paused",false), p.getBoolean("hidden",false),
  p.getFloat("x",0.5f),p.getFloat("y",0.8f),p.getString("model","") ?: "",
  p.getString("tapMotion","") ?: "",p.getString("waveMotion","") ?: "",p.getString("expression","") ?: "",p.getString("walkMotion","") ?: "",p.getBoolean("comments",true),p.getBoolean("legMotion",true),
  p.getBoolean("activities",true),p.getBoolean("edges",true),p.getBoolean("ghost",false),p.getBoolean("voice",true),p.getFloat("voiceVolume",.55f),p.getString("sitMotion","") ?: "",p.getString("swingMotion","") ?: "",p.getBoolean("bodyMotion",true))
 fun savePosition(x: Float,y: Float) { p.edit().putFloat("x",x.coerceIn(0f,1f)).putFloat("y",y.coerceIn(0f,1f)).apply() }
 fun save(c: PetConfig) { p.edit().putFloat("size",c.size.coerceIn(100f,280f)).putFloat("speed",c.speed.coerceIn(0.4f,2f))
  .putFloat("activity",c.activity.coerceIn(0f,1f)).putBoolean("roam",c.roam).putBoolean("jump",c.jump)
  .putBoolean("reactions",c.reactions).putInt("fps",if (c.fps == 60) 60 else 30)
  .putBoolean("paused",c.paused).putBoolean("hidden",c.hidden).putFloat("x",c.x.coerceIn(0f,1f))
  .putFloat("y",c.y.coerceIn(0f,1f)).putString("model",c.model).putString("tapMotion",c.tapMotion)
  .putString("waveMotion",c.waveMotion).putString("expression",c.expression).putString("walkMotion",c.walkMotion).putBoolean("comments",c.comments).putBoolean("legMotion",c.legMotion)
  .putBoolean("activities",c.activities).putBoolean("edges",c.edges).putBoolean("ghost",c.ghost).putBoolean("voice",c.voice).putFloat("voiceVolume",c.voiceVolume.coerceIn(0f,1f))
  .putString("sitMotion",c.sitMotion).putString("swingMotion",c.swingMotion).putBoolean("bodyMotion",c.bodyMotion).apply() }
}

data class PetStatus(val running: Boolean = false, val renderer: String = "Belum dimulai", val error: String = "",
 val frames: Long = 0, val mode: String = "", val parameters: Int = 0, val suspended: Boolean = false,
 val visiblePixels: Int = 0, val transparentPixels: Int = 0, val meshChanges: Int = 0,
 val voicePlaying: Boolean=false,val lipLevel: Float=0f,val ghost: Boolean=false,val voiceMessage: String="")
object PetRuntime {
 private val value = MutableStateFlow(PetStatus())
 val status = value.asStateFlow()
 @Synchronized fun update(block: (PetStatus) -> PetStatus) { value.value = block(value.value) }
}
