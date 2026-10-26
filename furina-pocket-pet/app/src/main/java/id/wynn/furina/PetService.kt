package id.wynn.furina

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlin.math.*

/** A bounded overlay, never a full-screen touch-capturing window.
 * https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_TOUCH_MODAL
 * https://developer.android.com/develop/background-work/services/fgs/service-types#special-use */
class PetService : Service() {
 private lateinit var prefs: PetSettings
 private lateinit var wm: WindowManager
 private val handler=Handler(Looper.getMainLooper())
 private val engine=BehaviorEngine()
 private var view: PetSurface?=null
 private var config=PetConfig()
 private var params=WindowManager.LayoutParams()
 private var screenAwake=true
 private var dragging=false
 private var lastTick=0L
 private var lastStatus=0L
 private var visible=false
 private var receiverRegistered=false
 private var density=1f
 private var left=0; private var top=0; private var right=0; private var bottom=0
 private var baseX=0f; private var baseY=0f
 private var downX=0f; private var downY=0f
 private var downAt=0L
 private val receiver=object: BroadcastReceiver() {
  override fun onReceive(context: Context,intent: Intent) {
   when(intent.action) {
    Intent.ACTION_SCREEN_OFF -> screenAwake=false
    Intent.ACTION_SCREEN_ON,Intent.ACTION_USER_PRESENT -> screenAwake=(getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked)
   }
   schedule()
  }
 }
 private val tick=object: Runnable {
  override fun run() {
   val v=view ?: return
   if(!screenAwake || config.hidden || config.paused || !visible) return
   val now=SystemClock.elapsedRealtime()
   val dt=if(lastTick==0L) 1f/30 else (now-lastTick)/1000f
   lastTick=now
   val frame=if(dragging) engine.frame else engine.step(dt,config.speed,config.activity,config.roam,config.jump)
   v.frame=frame; v.config=config
   if(!dragging) {
    val nx=(left+frame.x*(right-left)-0f).roundToInt()
    val ny=(top+frame.y*(bottom-top)-frame.bounce*42*density).roundToInt().coerceAtLeast(top)
    if(nx!=params.x || ny!=params.y) { params.x=nx; params.y=ny; if(!updateWindow()) return }
   }
   v.requestRender()
   if(now-lastStatus>=1000) {
    lastStatus=now
    PetRuntime.update { it.copy(frames=v.renderedFrames,mode=frame.mood.name,suspended=false) }
   }
   val batterySaver=getSystemService(PowerManager::class.java).isPowerSaveMode
   val fps=if(batterySaver) 15 else config.fps
   handler.postDelayed(this,(1000L/fps-(SystemClock.elapsedRealtime()-now)).coerceAtLeast(1))
  }
 }
 override fun onCreate() {
  super.onCreate()
  prefs=PetSettings(this); wm=getSystemService(WindowManager::class.java)
  density=resources.displayMetrics.density
  val power=getSystemService(PowerManager::class.java)
  screenAwake=power.isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked
  val filter=IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT); addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) }
  ContextCompat.registerReceiver(this,receiver,filter,ContextCompat.RECEIVER_NOT_EXPORTED)
  receiverRegistered=true
  getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Peliharaan aktif",NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
 }
 override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
  if(intent?.action==STOP) { stopSelf(); return START_NOT_STICKY }
  try {
   config=prefs.read()
   when(intent?.action) {
    PAUSE -> config=config.copy(paused=!config.paused)
    HIDE -> config=config.copy(hidden=!config.hidden)
    HOME -> config=config.copy(x=0.5f,y=0.8f)
   }
   prefs.save(config)
   ServiceCompat.startForeground(this,NOTIFICATION,notification(),if(Build.VERSION.SDK_INT>=34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
   if(!Settings.canDrawOverlays(this)) { fail("Izin tampil di atas aplikasi lain belum aktif"); return START_NOT_STICKY }
   val selected=ModelStore(this).selected(config)
   if(view==null || view?.tag!=selected.path) {
    removePet()
    PetRuntime.update { PetStatus(running=true) }
    view=PetSurface(this,selected) { fail(it) }.apply { tag=selected.path }
    params=WindowManager.LayoutParams(dp(config.size),dp(config.size*1.22f),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
     WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
     PixelFormat.TRANSLUCENT).apply {
     gravity=Gravity.TOP or Gravity.START
     softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
     if(Build.VERSION.SDK_INT>=28) layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
    }
    bounds()
    engine.place(config.x,config.y)
    params.x=(left+config.x*(right-left)).roundToInt(); params.y=(top+config.y*(bottom-top)).roundToInt()
    view!!.setOnTouchListener { _,event -> touch(event) }
    view!!.setOnClickListener { if(config.reactions) { engine.react(); view?.frame=engine.frame; view?.requestRender() } }
    wm.addView(view,params)
    visible=true
   } else {
    params.width=dp(config.size); params.height=dp(config.size*1.22f)
    bounds()
    if(intent?.action==HOME) engine.place(config.x,config.y)
    params.x=(left+engine.frame.x*(right-left)).roundToInt(); params.y=(top+engine.frame.y*(bottom-top)).roundToInt()
    updateWindow()
   }
   view?.config=config
   PetRuntime.update { it.copy(running=true,error="") }
   schedule()
  } catch(e: Exception) { fail(e.message ?: "Overlay tidak dapat dimulai") }
  return START_NOT_STICKY // Never silently restart a user-stopped pet or start a restricted FGS in background.
 }
 private fun touch(e: MotionEvent): Boolean {
  when(e.actionMasked) {
   MotionEvent.ACTION_DOWN -> {
    downX=e.rawX; downY=e.rawY; baseX=params.x.toFloat(); baseY=params.y.toFloat(); downAt=SystemClock.elapsedRealtime(); dragging=true
   }
   MotionEvent.ACTION_MOVE -> {
    params.x=(baseX+e.rawX-downX).roundToInt().coerceIn(left,right)
    params.y=(baseY+e.rawY-downY).roundToInt().coerceIn(top,bottom)
    updateWindow()
   }
   MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
    val moved=hypot(e.rawX-downX,e.rawY-downY)>ViewConfiguration.get(this).scaledTouchSlop
    dragging=false
    val nx=(params.x-left).toFloat()/(right-left).coerceAtLeast(1)
    val ny=(params.y-top).toFloat()/(bottom-top).coerceAtLeast(1)
    engine.place(nx,ny)
    config=config.copy(x=nx,y=ny)
    // Read latest UI settings before saving only position.
    prefs.save(prefs.read().copy(x=nx,y=ny))
    if(e.actionMasked==MotionEvent.ACTION_UP && !moved) {
     if(SystemClock.elapsedRealtime()-downAt>650) {
      startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
     } else view?.performClick()
    }
    view?.frame=engine.frame; view?.requestRender()
   }
  }
  return true
 }
 private fun dp(value: Float)= (value*density).roundToInt()
 private fun bounds() {
  if(Build.VERSION.SDK_INT>=30) {
   val metrics=wm.currentWindowMetrics
   val inset=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
   left=inset.left+dp(12f); top=inset.top+dp(14f)
   right=(metrics.bounds.width()-inset.right-params.width-dp(12f)).coerceAtLeast(left)
   bottom=(metrics.bounds.height()-inset.bottom-params.height-dp(24f)).coerceAtLeast(top)
  } else {
   val metrics=resources.displayMetrics
   left=dp(12f); top=dp(40f)
   right=(metrics.widthPixels-params.width-dp(12f)).coerceAtLeast(left)
   bottom=(metrics.heightPixels-params.height-dp(60f)).coerceAtLeast(top)
  }
 }
 private fun updateWindow(): Boolean = try { view?.let { wm.updateViewLayout(it,params) }; true }
  catch(e: Exception) { fail(e.message ?: "Izin overlay dicabut"); false }
 private fun schedule() {
  handler.removeCallbacks(tick); lastTick=0L
  val v=view ?: return
  val show=screenAwake && !config.hidden
  v.visibility=if(show) View.VISIBLE else View.INVISIBLE
  visible=show
  val active=show && !config.paused
  if(active) { v.onResume(); handler.post(tick) } else { v.onPause() }
  PetRuntime.update { it.copy(suspended=!active) }
 }
 override fun onConfigurationChanged(newConfig: Configuration) {
  super.onConfigurationChanged(newConfig)
  density=resources.displayMetrics.density
  bounds()
  params.x=(left+engine.frame.x*(right-left)).roundToInt(); params.y=(top+engine.frame.y*(bottom-top)).roundToInt()
  updateWindow()
 }
 private fun notification(): Notification {
  fun pending(action: String)=PendingIntent.getService(this,action.hashCode(),Intent(this,PetService::class.java).setAction(action),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_pet)
   .setContentTitle("Furina Pocket Pet")
   .setContentText(when { config.hidden -> "Karakter disembunyikan"; config.paused -> "Animasi dijeda"; else -> "Ketuk karakter untuk bereaksi · tahan untuk pengaturan" })
   .setContentIntent(open).setOngoing(true).setSilent(true).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
   .addAction(0,if(config.paused) "Lanjutkan" else "Jeda",pending(PAUSE))
   .addAction(0,if(config.hidden) "Tampilkan" else "Sembunyikan",pending(HIDE))
   .addAction(0,"Hentikan",pending(STOP)).build()
 }
 private fun fail(message: String) { PetRuntime.update { it.copy(error=message) }; stopSelf() }
 private fun removePet() {
  handler.removeCallbacks(tick)
  view?.let { v -> v.release(); if(v.isAttachedToWindow) runCatching { wm.removeViewImmediate(v) } }
  view=null; visible=false
 }
 override fun onDestroy() {
  prefs.save(prefs.read().copy(x=engine.frame.x,y=engine.frame.y))
  removePet()
  if(receiverRegistered) unregisterReceiver(receiver)
  stopForeground(STOP_FOREGROUND_REMOVE)
  PetRuntime.update { it.copy(running=false,suspended=true) }
  super.onDestroy()
 }
 override fun onBind(intent: Intent?) = null
 companion object {
  private const val CHANNEL="pet-active"
  private const val NOTIFICATION=41
  const val START="start"; const val STOP="stop"; const val PAUSE="pause"; const val HIDE="hide"; const val HOME="home"; const val REFRESH="refresh"
  fun command(context: Context,action: String=START) {
   val intent=Intent(context,PetService::class.java).setAction(action)
   if(action==STOP) context.stopService(intent)
   else ContextCompat.startForegroundService(context,intent)
  }
 }
}
