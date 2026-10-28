package id.wynn.iwatchyou

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.os.*
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import java.time.*
import java.util.Locale

class WatchService : AccessibilityService() {
 companion object {
  @Volatile var instance: WatchService?=null
   private set
  private const val CHANNEL="screen_time"
  private const val NOTICE=1206
  fun today()=LocalDate.now().toString()
  fun readSnapshot(context: Context): TimeLedger.Snapshot = instance?.current() ?: TimeLedger.Snapshot(today(),0,emptyMap(),null)
  fun format(ms: Long): String {
   val s=ms.coerceAtLeast(0)/1000
   return "%02d:%02d:%02d".format(Locale.US,s/3600,(s/60)%60,s%60)
  }
  fun short(ms: Long): String {
   val s=ms.coerceAtLeast(0)/1000
   return if(s>=3600) "${s/3600}j ${(s/60)%60}m ${s%60}d" else "${s/60}m ${s%60}d"
  }
 }
 private val main=Handler(Looper.getMainLooper())
 private val thread=HandlerThread("IWatchYou-ledger")
 private lateinit var worker: Handler
 @Volatile private var ledger: TimeLedger?=null
 private lateinit var store: HistoryStore
 private lateinit var overlay: OverlayController
 private val power by lazy { getSystemService(PowerManager::class.java) }
 private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }
 private val displays by lazy { getSystemService(DisplayManager::class.java) }
 private val notices by lazy { getSystemService(NotificationManager::class.java) }
 private var registered=false
 private var displayInteractive=true
 private val displayListener=object: DisplayManager.DisplayListener {
  override fun onDisplayAdded(id: Int)=Unit
  override fun onDisplayRemoved(id: Int)=Unit
  override fun onDisplayChanged(id: Int) {
   if(id!=Display.DEFAULT_DISPLAY || !::worker.isInitialized || closing)return
   val on=displays.getDisplay(id)?.state==Display.STATE_ON && power.isInteractive
   if(on==displayInteractive)return
   displayInteractive=on;screenOffSignal=!on
   val elapsed=SystemClock.elapsedRealtime();val wall=System.currentTimeMillis()
   worker.post {
    advanceDay(elapsed,wall)
    val t=ledger ?: return@post
    if(on && unlocked())t.screenOn(elapsed) else t.screenOff(elapsed)
    persist(elapsed);notification(elapsed);scheduleMaintenance()
    main.post { if(on) resolveForeground() else overlay.hide() }
   }
  }
 }
 @Volatile private var closing=false
 @Volatile private var screenOffSignal=false
 private var midnightWall=0L
 private var lastWall=0L
 private var lastElapsed=0L
 private var zone=ZoneId.systemDefault()
 private var excluded=emptySet<String>()
 private var inputMethods=emptySet<String>()
 private val windowPackages=linkedMapOf<Int,String>()
 private val windowReader=java.util.concurrent.Executors.newSingleThreadExecutor()
 @Volatile private var focusGeneration=0L
 private var retryBudget=0
 private var activityHint: String?=null
 private var hasActivityHint=false
 private val resolveTask=Runnable { resolveForeground() }
 private fun rememberWindow(windowId: Int,pkg: String,replace: Boolean=false) {
  if(windowId<0)return
  val known=windowPackages[windowId]
  if(known!=null && known!=pkg && !replace)return
  windowPackages.remove(windowId);windowPackages[windowId]=pkg
  if(windowPackages.size>32)windowPackages.remove(windowPackages.keys.first())
 }
 private fun unlocked()=!screenOffSignal && power.isInteractive && !keyguard.isKeyguardLocked
 private val receiver=object: BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
   if(intent.action==Intent.ACTION_SCREEN_OFF) screenOffSignal=true
   if(intent.action==Intent.ACTION_SCREEN_ON || intent.action==Intent.ACTION_USER_PRESENT) screenOffSignal=false
   val elapsed=SystemClock.elapsedRealtime(); val wall=System.currentTimeMillis()
   val isOn=unlocked()
   worker.post {
    advanceDay(elapsed,wall)
    val t=ledger ?: return@post
    if (intent.action==Intent.ACTION_TIME_CHANGED || intent.action==Intent.ACTION_TIMEZONE_CHANGED) {
     zone=ZoneId.systemDefault(); t.reanchor(elapsed,wall,zone)
     switchCalendarDay(elapsed,wall); planMidnight(wall)
    }
    if (!isOn) t.screenOff(elapsed) else t.screenOn(elapsed)
    persist(elapsed); notification(elapsed)
    scheduleMaintenance()
    main.post { if(isOn) resolveForeground() else overlay.hide() }
   }
  }
 }
 override fun onServiceConnected() {
  super.onServiceConnected()
  // A service object can reconnect after an interruption. Do not duplicate workers/receivers.
  if (::worker.isInitialized) return
  thread.start(); worker=Handler(thread.looper)
  overlay=OverlayController(this) { current() }
  val homes=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0)
   .filterNot { it.activityInfo.name.endsWith(".FallbackHome") }
   .map { it.activityInfo.packageName }
  excluded=setOf(packageName,"android","com.android.systemui")+homes
  inputMethods=runCatching {
   getSystemService(android.view.inputmethod.InputMethodManager::class.java).inputMethodList.map { it.packageName }.toSet()
  }.getOrDefault(emptySet())
  notices.createNotificationChannel(NotificationChannel(CHANNEL,"Waktu layar aktif",NotificationManager.IMPORTANCE_LOW).apply {
   setSound(null,null); enableVibration(false); setShowBadge(false)
  })
  val filter=IntentFilter().apply {
   addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT)
   addAction(Intent.ACTION_DATE_CHANGED); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
  }
  if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,filter,RECEIVER_NOT_EXPORTED)
  else { @Suppress("DEPRECATION") registerReceiver(receiver,filter) }
  registered=true
  displayInteractive=displays.getDisplay(Display.DEFAULT_DISPLAY)?.state==Display.STATE_ON && power.isInteractive
  displays.registerDisplayListener(displayListener,main)
  worker.post {
   store=HistoryStore(applicationContext); store.migrateLegacy(applicationContext)
   val elapsed=SystemClock.elapsedRealtime(); val wall=System.currentTimeMillis()
   val day=Instant.ofEpochMilli(wall).atZone(zone).toLocalDate().toString()
   val saved=store.read(day)
   val t=TimeLedger(day,saved?.apps ?: emptyMap(),saved?.screenMs ?: 0L,saved?.hours ?: List(24){0L},wall-elapsed,zone)
   ledger=t; lastElapsed=elapsed; lastWall=wall; planMidnight(wall)
   if(unlocked()) t.screenOn(elapsed)
   if (!closing) instance=this
   notification(elapsed); scheduleMaintenance()
   main.post { if(!closing) resolveForeground() }
  }
 }
 private fun planMidnight(wall: Long) {
  midnightWall=Instant.ofEpochMilli(wall).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
 }
 /** Split at the actual midnight, even if this callback was delayed by Doze or scheduling. */
 private fun advanceDay(elapsed: Long, wall: Long) {
  val t=ledger ?: return
  val wallJump=kotlin.math.abs((wall-lastWall)-(elapsed-lastElapsed))>2000
  if(wallJump) {
   t.reanchor(elapsed,wall,zone)
   switchCalendarDay(elapsed,wall); planMidnight(wall)
  } else {
   while(wall>=midnightWall) {
    val boundary=(elapsed-(wall-midnightWall)).coerceAtLeast(lastElapsed)
    val day=Instant.ofEpochMilli(midnightWall).atZone(zone).toLocalDate().toString()
    val old=t.nextDay(day,boundary,store.read(day))
    store.write(old)
    main.post { overlay.refresh() }
    planMidnight(midnightWall)
   }
  }
  lastElapsed=elapsed; lastWall=wall
 }
 private fun switchCalendarDay(elapsed: Long, wall: Long) {
  val t=ledger ?: return
  val day=Instant.ofEpochMilli(wall).atZone(zone).toLocalDate().toString()
  if(day!=t.day) { store.write(t.nextDay(day,elapsed,store.read(day))); main.post { overlay.refresh() } }
 }
 private val checkpoint=object: Runnable {
  override fun run() {
   val now=SystemClock.elapsedRealtime(); val wall=System.currentTimeMillis()
   val before=ledger?.day
   advanceDay(now,wall)
   // Screen-off receivers normally do this; this guard handles a missed vendor broadcast.
   if(!unlocked()) { ledger?.screenOff(now); main.post { overlay.hide() }; notification(now) }
   persist(now)
   if(before!=ledger?.day) notification(now)
   scheduleMaintenance()
  }
 }
 private val midnight=Runnable {
  val now=SystemClock.elapsedRealtime()
  advanceDay(now,System.currentTimeMillis()); persist(now); notification(now); scheduleMaintenance()
 }
 private fun scheduleMaintenance() {
  worker.removeCallbacks(checkpoint); worker.removeCallbacks(midnight)
  if(ledger?.isScreenRunning()==true && !closing) {
   worker.postDelayed(checkpoint,60_000)
   worker.postDelayed(midnight,(midnightWall-System.currentTimeMillis()).coerceAtLeast(1))
  }
 }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  if(event==null || !::worker.isInitialized || closing) return
  if(event.eventType!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.eventType!=AccessibilityEvent.TYPE_WINDOWS_CHANGED) return
  // Invalidate a read as soon as newer window information arrives, including
  // structural events whose reconciliation is deliberately delayed.
  focusGeneration++
  // Some OEMs expose a window before its accessibility root (or never expose that root).
  // Keep package hints by window so focus metadata works without reading text nodes.
  if(event.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
   val pkg=event.packageName?.toString()
   val cls=event.className?.toString().orEmpty()
   // An Activity can have any class name and need not have a launcher entry.
   // Keep IME window events out of the hint too: rootless reconciliation cannot
   // subsequently distinguish a keyboard from an app using window metadata.
   val inputWindow=cls.endsWith("SoftInputWindow") || cls.endsWith("InputMethodService")
   if(pkg!=null && (pkg!=packageName || cls.endsWith("Activity")) && !cls.contains("Dialog") && !cls.contains("Popup") && !inputWindow) {
    rememberWindow(event.windowId,pkg,cls.endsWith("Activity") || cls.endsWith("Launcher"))
    activityHint=pkg.takeUnless { it in excluded };hasActivityHint=true
   }
  }
  // A state event can also come from a background pane. Resolve current identity
  // before committing it; cached per-window packages avoid blocking root reads.
  main.removeCallbacks(resolveTask)
  retryBudget=2
  if(event.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) resolveForeground()
  else main.postDelayed(resolveTask,35)
 }
 private fun resolveForeground() {
  if(ledger==null || closing) return
  val ticket=++focusGeneration
  val on=unlocked()
  val hint=activityHint;val hadHint=hasActivityHint
  val owners=windowPackages.toMap();val auxiliaryPackages=inputMethods+packageName
  // OEM accessibility root retrieval may block. Keep both UI and time/storage worker free.
  windowReader.execute {
   if(ticket!=focusGeneration || closing)return@execute
   var needsRetry=false
   var rememberFocus=false
   var rootIdentity: Pair<Int,String>?=null
   val selected=if(!on)null else runCatching {
    val observed=windows.map { w ->
     val pkg=if(w.type==AccessibilityWindowInfo.TYPE_APPLICATION) owners[w.id] else null
     ForegroundPolicy.Window(pkg,w.type==AccessibilityWindowInfo.TYPE_APPLICATION,w.isFocused,w.isActive,
      w.isInPictureInPictureMode,w.type==AccessibilityWindowInfo.TYPE_INPUT_METHOD,
      w.type==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY,w.layer,w.id)
    }
    needsRetry=ForegroundPolicy.needsRoot(observed)
    val resolved=ForegroundPolicy.select(observed,excluded)
    // A background window can send late state events. When interactive-window
    // metadata is absent, query the current active root rather than retaining
    // that last event's package indefinitely. Only identity metadata is read.
    val activePackage=if(needsRetry) runCatching {
     val root=rootInActiveWindow
     try { root?.let {
      ForegroundPolicy.rootPackage(it.windowId,it.packageName?.toString(),owners,auxiliaryPackages,observed)
       ?.also { pkg -> rootIdentity=it.windowId to pkg }
     } }
     finally { @Suppress("DEPRECATION") root?.recycle() }
    }.getOrNull() else null
    if(!needsRetry) { rememberFocus=true;resolved }
    else if(activePackage!=null) { rememberFocus=true;activePackage.takeUnless { it in excluded } }
    else if(hadHint) ForegroundPolicy.unresolvedHint(hint,observed)
    else { rememberFocus=true;resolved }
   }.getOrElse { if(hadHint)hint else null }
   main.post {
    if(closing || ticket!=focusGeneration)return@post
    if(rememberFocus) {
     activityHint=selected;hasActivityHint=true
     rootIdentity?.let { rememberWindow(it.first,it.second) }
    }
    if(on && needsRetry && retryBudget>0) { retryBudget--;main.removeCallbacks(resolveTask);main.postDelayed(resolveTask,100) }
    commitSelection(selected,on && unlocked())
   }
  }
 }
 private fun commitSelection(selected: String?,on: Boolean) {
  val elapsed=SystemClock.elapsedRealtime();val wall=System.currentTimeMillis()
  worker.post {
   advanceDay(elapsed,wall)
   val t=ledger ?: return@post
   val wasOn=t.isScreenRunning()
   if(on) t.screenOn(elapsed) else t.screenOff(elapsed)
   val previous=t.snapshot(elapsed).foreground
   t.appChanged(selected,elapsed)
   main.post { if(!closing && unlocked() && selected!=null) overlay.show(selected) else overlay.hide() }
   if(previous!=selected || wasOn!=on) persist(elapsed)
   if(wasOn!=on) { notification(elapsed);scheduleMaintenance() }
  }
 }
 private fun persist(now: Long) {
  ledger?.let { it.checkpoint(now); store.write(it.snapshot(now)) }
 }
 fun current(): TimeLedger.Snapshot=ledger?.snapshot(SystemClock.elapsedRealtime()) ?: TimeLedger.Snapshot(today(),0,emptyMap(),null)
 fun ownActivityVisible() {
  activityHint=null;hasActivityHint=true;focusGeneration++
  if(ledger!=null && !closing)commitSelection(null,unlocked())
 }
 fun resetBubbleControls() { if(::overlay.isInitialized)overlay.resetControls() }
 fun refreshNotification() { if(::worker.isInitialized) worker.post { notification(SystemClock.elapsedRealtime()) } }
 private fun notification(now: Long) {
  if(closing) return
  val t=ledger ?: return
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) return
  val s=t.snapshot(now)
  val launch=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val builder=Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification)
   .setContentTitle("IWatchYou").setContentIntent(launch).setOngoing(true).setOnlyAlertOnce(true)
   .setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_SERVICE)
  if(t.isScreenRunning()) builder.setContentText("Waktu layar aktif hari ini").setShowWhen(true).setUsesChronometer(true).setWhen(System.currentTimeMillis()-s.screenMs)
  else builder.setContentText("Waktu layar aktif hari ini · ${format(s.screenMs)}").setShowWhen(false).setUsesChronometer(false)
  runCatching { notices.notify(NOTICE,builder.build()) }
 }
 override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); if(::overlay.isInitialized) overlay.recreate() }
 override fun onInterrupt() { if(::worker.isInitialized) worker.post { val now=SystemClock.elapsedRealtime(); ledger?.appChanged(null,now); persist(now); main.post { overlay.hide() } } }
 override fun onDestroy() {
  closing=true; instance=null
  focusGeneration++;windowReader.shutdownNow()
  main.removeCallbacksAndMessages(null)
  if(::overlay.isInitialized) overlay.hide()
  if(registered) { unregisterReceiver(receiver);displays.unregisterDisplayListener(displayListener) }
  if(::worker.isInitialized) {
   worker.removeCallbacksAndMessages(null)
   worker.post {
    val now=SystemClock.elapsedRealtime(); ledger?.screenOff(now)
    if(::store.isInitialized) { persist(now); store.close() }
    thread.quitSafely()
   }
  }
  notices.cancel(NOTICE)
  super.onDestroy()
 }
}
