package id.wynn.iwatchyou

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.*
import android.widget.Chronometer
import kotlin.math.hypot
import kotlin.math.roundToInt

class OverlayController(private val context: AccessibilityService, private val snapshot: () -> TimeLedger.Snapshot) {
 private val window=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
 private val handler=Handler(Looper.getMainLooper())
 private val prefs=context.getSharedPreferences("overlay",Context.MODE_PRIVATE)
 private var view: Chronometer?=null
 private var params: WindowManager.LayoutParams?=null
 private var pkg: String?=null
 private var positionKey="portrait"
 private var faded=false
 private val density get()=context.resources.displayMetrics.density
 private fun dp(n: Int)=(n*density).roundToInt()
 private fun orientation()=if(context.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
 private val fadeTask=Runnable { faded=true; paint() }
 fun show(packageName: String) {
  if(view!=null) {
   if(pkg!=packageName) { pkg=packageName; setTime() }
   return
  }
  pkg=packageName; positionKey=orientation(); faded=false
  val timer=object: Chronometer(context) {
   override fun performClick(): Boolean { super.performClick(); return true }
  }.apply {
   setTextColor(Color.WHITE); textSize=15f
   typeface=Typeface.create(context.resources.getFont(R.font.jakarta_medium),Typeface.NORMAL)
   fontFeatureSettings="tnum"
   gravity=Gravity.CENTER; includeFontPadding=false
   setPadding(dp(12),dp(8),dp(12),dp(8))
   elevation=dp(3).toFloat()
   val clock=context.getDrawable(R.drawable.ic_notification)?.mutate()
   clock?.setBounds(0,0,dp(17),dp(17))
   setCompoundDrawables(clock,null,null,null);compoundDrawablePadding=dp(6)
   contentDescription="Waktu aplikasi. Ketuk untuk membuka kunci posisi, lalu geser."
   setOnChronometerTickListener { text=WatchService.format(SystemClock.elapsedRealtime()-base) }
  }
  val w=WindowManager.LayoutParams(dp(134),dp(44),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
   PixelFormat.TRANSLUCENT).apply {
    gravity=Gravity.TOP or Gravity.LEFT
    x=prefs.getInt("x_$positionKey",dp(16)); y=prefs.getInt("y_$positionKey",dp(72))
   }
  var startX=0; var startY=0; var downX=0f; var downY=0f
  var moved=false; var lockedGesture=false
  val slop=ViewConfiguration.get(context).scaledTouchSlop
  timer.setOnTouchListener { _,e ->
   when(e.actionMasked) {
    MotionEvent.ACTION_DOWN -> {
     handler.removeCallbacks(fadeTask)
     lockedGesture=faded; startX=w.x; startY=w.y; downX=e.rawX; downY=e.rawY; moved=false
     true
    }
    MotionEvent.ACTION_MOVE -> {
     val dx=e.rawX-downX; val dy=e.rawY-downY
     if(hypot(dx,dy)>slop) moved=true
     if(moved && !lockedGesture) { w.x=startX+dx.roundToInt(); w.y=startY+dy.roundToInt(); clamp(w); update(timer,w) }
     true
    }
    MotionEvent.ACTION_UP -> {
     // A swipe beginning while dimmed never moves OR unlocks the timer.
     if(lockedGesture && !moved) { faded=false; paint(); timer.performClick() }
     if(moved && !lockedGesture) savePosition(w)
     if(!moved && !lockedGesture) timer.performClick()
     scheduleFade(); true
    }
    MotionEvent.ACTION_CANCEL -> { scheduleFade(); true }
    else -> true
   }
  }
  timer.setOnClickListener { if(faded) { faded=false; paint() }; scheduleFade() }
  view=timer; params=w
  clamp(w)
  if(runCatching { window.addView(timer,w) }.isFailure) { view=null; params=null; pkg=null; return }
  setTime(); paint(); timer.start(); scheduleFade()
 }
 fun refresh() { if(view!=null) setTime() }
 private fun setTime() {
  val total=snapshot().apps[pkg] ?: 0L
  view?.apply { base=SystemClock.elapsedRealtime()-total; text=WatchService.format(total) }
 }
 private fun clamp(w: WindowManager.LayoutParams) {
  val metrics=window.currentWindowMetrics
  val insets=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
  w.x=w.x.coerceIn(insets.left,(metrics.bounds.width()-insets.right-w.width).coerceAtLeast(insets.left))
  // Window coordinates already start below the top system inset without FLAG_LAYOUT_IN_SCREEN.
  w.y=w.y.coerceIn(0,(metrics.bounds.height()-insets.top-insets.bottom-w.height).coerceAtLeast(0))
 }
 private fun update(v: View,w: WindowManager.LayoutParams) { runCatching { window.updateViewLayout(v,w) } }
 private fun savePosition(w: WindowManager.LayoutParams) { prefs.edit().putInt("x_$positionKey",w.x).putInt("y_$positionKey",w.y).apply() }
 private fun scheduleFade() { handler.removeCallbacks(fadeTask); handler.postDelayed(fadeTask,3000) }
 private fun paint() {
  view?.apply {
   background=GradientDrawable().apply { cornerRadius=dp(28).toFloat(); setColor(Color.rgb(26,36,51)); setStroke(dp(1),Color.argb(40,255,255,255)) }
   // Dark backing is kept for legibility over bright games. Text and pill dim together.
   animate().alpha(if(faded) 0.68f else 0.94f).setDuration(220).start()
  }
 }
 fun recreate() {
  val previous=pkg; val dim=faded
  hide()
  if(previous!=null) { show(previous); faded=dim; paint() }
 }
 fun hide() {
  handler.removeCallbacksAndMessages(null)
  view?.let { it.stop(); it.animate().cancel(); runCatching { window.removeViewImmediate(it) } }
  view=null; params=null; pkg=null
 }
}
