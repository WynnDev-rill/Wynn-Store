package id.wynn.iwatchyou

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

class OverlayController(
 private val context: AccessibilityService,
 private val snapshot: () -> TimeLedger.Snapshot
) {
 private val window = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
 private val handler = Handler(Looper.getMainLooper())
 private val prefs = context.getSharedPreferences("overlay",Context.MODE_PRIVATE)
 private var textView: TextView? = null
 private var params: WindowManager.LayoutParams? = null
 private var packageName: String? = null
 private var faded = false
 private val density = context.resources.displayMetrics.density
 private val fadeTask = Runnable { faded=true; paint() }
 private val ticker = object: Runnable {
  override fun run() {
   textView?.text = WatchService.format(snapshot().apps[packageName] ?: 0L)
   if (textView != null) handler.postDelayed(this,1000)
  }
 }
 private fun orientation(): String = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
 private fun dp(v:Float) = (v*density).roundToInt()
 fun show(pkg:String) {
  if (textView != null) {
   packageName=pkg
   textView?.text=WatchService.format(snapshot().apps[pkg] ?: 0L)
   return
  }
  packageName=pkg
  faded=false
  val view=TextView(context).apply {
   setTextColor(Color.WHITE)
   textSize=16f
   typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)
   setShadowLayer(2f,0f,1f,Color.BLACK)
   gravity=Gravity.CENTER
   setPadding(dp(18f),dp(10f),dp(18f),dp(10f))
  }
  val key=orientation()
  val w=WindowManager.LayoutParams(
   WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
   WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
   PixelFormat.TRANSLUCENT
  ).apply {
   gravity=Gravity.TOP or Gravity.START
   x=prefs.getInt("x_$key",dp(20f))
   y=prefs.getInt("y_$key",dp(96f))
  }
  var x0=0
  var y0=0
  var downX=0f
  var downY=0f
  var moved=false
  view.setOnTouchListener { _,event ->
   when(event.actionMasked) {
    MotionEvent.ACTION_DOWN -> {
     x0=w.x; y0=w.y; downX=event.rawX; downY=event.rawY; moved=false
     true
    }
    MotionEvent.ACTION_MOVE -> {
     if (!faded) {
      val dx=event.rawX-downX
      val dy=event.rawY-downY
      if (abs(dx)>dp(4f) || abs(dy)>dp(4f)) moved=true
      w.x=(x0+dx).roundToInt().coerceAtLeast(0)
      w.y=(y0+dy).roundToInt().coerceAtLeast(0)
      try { window.updateViewLayout(view,w) } catch (_:Exception) {}
     }
     true
    }
    MotionEvent.ACTION_UP -> {
     if (faded) { faded=false; paint() }
     else if (moved) prefs.edit().putInt("x_$key",w.x).putInt("y_$key",w.y).apply()
     scheduleFade()
     true
    }
    else -> true
   }
  }
  textView=view
  params=w
  try { window.addView(view,w) } catch (_:Exception) { textView=null; params=null; return }
  paint()
  handler.post(ticker)
  scheduleFade()
 }
 private fun scheduleFade() {
  handler.removeCallbacks(fadeTask)
  handler.postDelayed(fadeTask,3000)
 }
 private fun paint() {
  val bg=GradientDrawable().apply {
   cornerRadius=dp(40f).toFloat()
   setColor(if(faded) Color.argb(83,17,27,44) else Color.argb(226,18,28,46))
  }
  textView?.background=bg
  textView?.alpha=1f
 }
 fun recreate() {
  val pkg=packageName
  hide()
  if (pkg!=null) show(pkg)
 }
 fun hide() {
  handler.removeCallbacks(fadeTask)
  handler.removeCallbacks(ticker)
  textView?.let { try { window.removeView(it) } catch (_:Exception){} }
  textView=null
  params=null
  packageName=null
 }
}
