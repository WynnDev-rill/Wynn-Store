package id.wynn.iwatchyou

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.os.*
import android.view.accessibility.AccessibilityEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WatchService : AccessibilityService() {
 companion object {
  @Volatile var instance: WatchService? = null
   private set
  private const val CHANNEL = "screen_time"
  private const val NOTICE = 1206
  fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  fun readSnapshot(context: Context): TimeLedger.Snapshot {
   instance?.let { return it.current() }
   val p = context.getSharedPreferences("times", MODE_PRIVATE)
   val day = today()
   if (p.getString("day", "") != day) return TimeLedger.Snapshot(day, 0, emptyMap(), null)
   val result = mutableMapOf<String, Long>()
   try {
    val obj = JSONObject(p.getString("apps", "{}") ?: "{}")
    obj.keys().forEach { result[it] = obj.optLong(it) }
   } catch (_: Exception) {}
   return TimeLedger.Snapshot(day, p.getLong("screen", 0), result, null)
  }
  fun format(ms: Long): String {
   val seconds = ms.coerceAtLeast(0) / 1000
   return "%02d:%02d:%02d".format(Locale.US, seconds/3600, (seconds/60)%60, seconds%60)
  }
  fun short(ms: Long): String {
   val seconds = ms.coerceAtLeast(0) / 1000
   val hours = seconds / 3600
   return if (hours > 0) "${hours}j ${(seconds/60)%60}m" else "${seconds/60}m ${seconds%60}d"
  }
 }
 private lateinit var ledger: TimeLedger
 private lateinit var overlay: OverlayController
 private val handler = Handler(Looper.getMainLooper())
 private val pm by lazy { getSystemService(POWER_SERVICE) as PowerManager }
 private val keys by lazy { getSystemService(KEYGUARD_SERVICE) as KeyguardManager }
 private val notices by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
 private var lastCheckpoint = 0L
 private var receiverReady = false
 private val receiver = object: BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
   val now = SystemClock.elapsedRealtime()
   rollover(now)
   when(intent.action) {
    Intent.ACTION_SCREEN_OFF -> {
     ledger.screenOff(now)
     overlay.hide()
     persist(now)
     notification()
    }
    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
     if (!keys.isKeyguardLocked && pm.isInteractive) ledger.screenOn(now)
     notification()
    }
    Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED -> {
     rollover(now); notification()
    }
   }
  }
 }
 override fun onServiceConnected() {
  super.onServiceConnected()
  val snapshot = readSnapshot(applicationContext)
  instance = this
  ledger = TimeLedger(today(), snapshot.apps, snapshot.screenMs)
  overlay = OverlayController(this) { ledger.snapshot(SystemClock.elapsedRealtime()) }
  val filter = IntentFilter().apply {
   addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
   addAction(Intent.ACTION_USER_PRESENT); addAction(Intent.ACTION_DATE_CHANGED)
   addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
  }
  if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
  else { @Suppress("DEPRECATION") registerReceiver(receiver, filter) }
  receiverReady = true
  if (pm.isInteractive && !keys.isKeyguardLocked) ledger.screenOn(SystemClock.elapsedRealtime())
  notices.createNotificationChannel(NotificationChannel(CHANNEL, "Waktu layar aktif", NotificationManager.IMPORTANCE_LOW))
  notification()
  handler.post(heartbeat)
 }
 private val heartbeat = object: Runnable {
  override fun run() {
   val now = SystemClock.elapsedRealtime()
   rollover(now)
   if (now-lastCheckpoint > 60000) { persist(now); lastCheckpoint=now }
   handler.postDelayed(this, 15000)
  }
 }
 private fun rollover(now: Long) {
  if (::ledger.isInitialized && ledger.day != today()) {
   ledger.nextDay(today(), now)
   persist(now)
   notification()
  }
 }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !::ledger.isInitialized) return
  val now = SystemClock.elapsedRealtime()
  rollover(now)
  if (!pm.isInteractive || keys.isKeyguardLocked) {
   if (ledger.isScreenRunning()) { ledger.screenOff(now); persist(now); notification() }
   overlay.hide()
   return
  }
  if (!ledger.isScreenRunning()) { ledger.screenOn(now); notification() }
  val packageName = event.packageName?.toString() ?: return
  val home = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
  val selected = if (packageName == this.packageName || packageName == "android" || packageName == "com.android.systemui" || packageName == home) null else packageName
  if (ledger.snapshot(now).foreground != selected) {
   ledger.appChanged(selected, now)
   persist(now)
  }
  if (selected != null) overlay.show(selected) else overlay.hide()
 }
 override fun onInterrupt() {}
 override fun onConfigurationChanged(newConfig: Configuration) {
  super.onConfigurationChanged(newConfig)
  if (::overlay.isInitialized) overlay.recreate()
 }
 private fun persist(now: Long) {
  ledger.checkpoint(now)
  val data = ledger.snapshot(now)
  val obj = JSONObject()
  data.apps.forEach { (k,v) -> obj.put(k,v) }
  getSharedPreferences("times", MODE_PRIVATE).edit()
   .putString("day",data.day).putString("apps",obj.toString()).putLong("screen",data.screenMs).apply()
 }
 fun current(): TimeLedger.Snapshot {
  val now = SystemClock.elapsedRealtime()
  rollover(now)
  return ledger.snapshot(now)
 }
 private fun notification() {
  if (!::ledger.isInitialized) return
  if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
  val snapshot = ledger.snapshot(SystemClock.elapsedRealtime())
  val running = ledger.isScreenRunning()
  val launch = PendingIntent.getActivity(this, 0, Intent(this,MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val builder = Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification)
   .setContentTitle("IWatchYou").setContentIntent(launch).setOngoing(true).setOnlyAlertOnce(true)
   .setVisibility(Notification.VISIBILITY_PUBLIC).setShowWhen(true)
  if (running) {
   builder.setContentText("Waktu layar aktif hari ini")
    .setUsesChronometer(true).setWhen(System.currentTimeMillis()-snapshot.screenMs)
  } else {
   builder.setUsesChronometer(false).setShowWhen(false)
    .setContentText("Waktu layar aktif hari ini · ${format(snapshot.screenMs)}")
  }
  notices.notify(NOTICE,builder.build())
 }
 override fun onDestroy() {
  handler.removeCallbacks(heartbeat)
  if (::ledger.isInitialized) persist(SystemClock.elapsedRealtime())
  if (::overlay.isInitialized) overlay.hide()
  if (receiverReady) unregisterReceiver(receiver)
  notices.cancel(NOTICE)
  instance = null
  super.onDestroy()
 }
}
