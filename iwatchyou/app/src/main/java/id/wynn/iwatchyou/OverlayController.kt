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
import android.util.Log
import android.view.*
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.ImageButton
import kotlin.math.hypot
import kotlin.math.roundToInt

class OverlayController(private val context: AccessibilityService, private val snapshot: () -> TimeLedger.Snapshot) {
 private val window=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
 private val handler=Handler(Looper.getMainLooper())
 private val prefs=context.getSharedPreferences("overlay",Context.MODE_PRIVATE)
 private var view: FrameLayout?=null
 private var timer: Chronometer?=null
 private var params: WindowManager.LayoutParams?=null
 private var tab: ImageButton?=null
 private var tabParams: WindowManager.LayoutParams?=null
 private var pkg: String?=null
 private var positionKey="portrait"
 private var faded=false
 private var collapsed=prefs.getBoolean("collapsed",false)
 private var scale=BubbleSize.clamp(prefs.getFloat("scale",1f))
 private var maxWidth=0
 private var maxHeight=0
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
  val clock=Chronometer(context).apply {
   setTextColor(Color.WHITE)
   typeface=Typeface.create(context.resources.getFont(R.font.jakarta_medium),Typeface.NORMAL)
   fontFeatureSettings="tnum"; gravity=Gravity.CENTER_VERTICAL; includeFontPadding=false
   contentDescription="Waktu aplikasi"
   setOnChronometerTickListener { text=WatchService.format(SystemClock.elapsedRealtime()-base) }
  }
  clock.textSize=15f
  // Leave room for the two compact corner controls without enlarging the timer text.
  maxWidth=maxOf(dp(134),kotlin.math.ceil(clock.paint.measureText("00:00:00")).toInt()+dp(52))
  val metrics=clock.paint.fontMetrics
  maxHeight=maxOf(dp(44),kotlin.math.ceil(metrics.bottom-metrics.top).toInt()+dp(16))
  val root=FrameLayout(context).apply { elevation=dp(3).toFloat() }
  root.addView(clock,FrameLayout.LayoutParams(-1,-1))
  val collapse=control(R.drawable.ic_chevron,"Sembunyikan timer").apply {
   rotation=180f
   setOnClickListener { dock() }
  }
  val resize=control(R.drawable.ic_resize,"Perkecil timer").apply {
   setOnClickListener { resize(BubbleSize.smaller(scale)); saveSize(); scheduleFade() }
  }
  root.addView(collapse,FrameLayout.LayoutParams(dp(28),maxHeight/2,Gravity.TOP or Gravity.RIGHT))
  root.addView(resize,FrameLayout.LayoutParams(dp(28),maxHeight/2,Gravity.BOTTOM or Gravity.RIGHT))
  val w=WindowManager.LayoutParams(maxWidth,maxHeight,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
   PixelFormat.TRANSLUCENT).apply {
    gravity=Gravity.TOP or Gravity.LEFT
    x=prefs.getInt("x_$positionKey",dp(16)); y=prefs.getInt("y_$positionKey",dp(72))
   }
  var startX=0; var startY=0; var downX=0f; var downY=0f; var moved=false; var cancelled=false
  val slop=ViewConfiguration.get(context).scaledTouchSlop
  clock.setOnTouchListener { _,e ->
   when(e.actionMasked) {
    MotionEvent.ACTION_DOWN -> {
     handler.removeCallbacks(fadeTask)
     startX=w.x; startY=w.y; downX=e.rawX; downY=e.rawY; moved=false;cancelled=false; true
    }
    MotionEvent.ACTION_MOVE -> {
     if(e.pointerCount==1 && !cancelled) {
      val dx=e.rawX-downX; val dy=e.rawY-downY
      if(hypot(dx,dy)>slop) moved=true
      if(moved && !faded) { w.x=startX+dx.roundToInt(); w.y=startY+dy.roundToInt(); clamp(w); update(root,w) }
     }
     true
    }
    MotionEvent.ACTION_UP -> { if(!cancelled) { if(moved) savePosition(w) else clock.performClick() };scheduleFade();true }
    MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN -> { cancelled=true;scheduleFade();true }
    else -> true
   }
  }
  clock.setOnClickListener { scheduleFade() }
  var resizeStart=scale; var resizeX=0f; var resizing=false; var resizeCancelled=false
  resize.setOnTouchListener { v,e ->
   when(e.actionMasked) {
    MotionEvent.ACTION_DOWN -> { handler.removeCallbacks(fadeTask); resizeStart=scale; resizeX=e.rawX; resizing=false;resizeCancelled=false;true }
    MotionEvent.ACTION_MOVE -> {
     if(!resizeCancelled && e.pointerCount==1 && kotlin.math.abs(e.rawX-resizeX)>slop) {
      resizing=true; resize(resizeStart+(e.rawX-resizeX)/maxWidth)
     }; true
    }
    MotionEvent.ACTION_UP -> { if(!resizing && !resizeCancelled)v.performClick();saveSize();scheduleFade();true }
    MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN -> { resizeCancelled=true;saveSize();scheduleFade();true }
    else -> true
   }
  }
  view=root; timer=clock; params=w
  resize(scale); clamp(w)
  if(runCatching { window.addView(root,w) }.onFailure { Log.w("IWatchYou","Could not attach timer overlay",it) }.isFailure) {
   view=null; timer=null; params=null; pkg=null; return
  }
  setTime(); clock.start(); paint(); scheduleFade()
 }
 private fun control(icon: Int,label: String)=ImageButton(context).apply {
  setImageResource(icon);setColorFilter(Color.WHITE);contentDescription=label
  background=context.getDrawable(android.R.drawable.list_selector_background)
  setPadding(dp(6),dp(3),dp(6),dp(3))
 }
 private fun resize(value: Float) {
  scale=BubbleSize.clamp(value)
  val w=params ?: return
  w.width=(maxWidth*scale).roundToInt(); w.height=(maxHeight*scale).roundToInt()
  timer?.apply { textSize=15f*scale; setPadding(dp(10),0,dp(30),0) }
  view?.let { root ->
   for(i in 1 until root.childCount) {
    root.getChildAt(i).layoutParams=(root.getChildAt(i).layoutParams as FrameLayout.LayoutParams).apply { height=w.height/2 }
   }
  }
  clamp(w)
  view?.let { if(it.isAttachedToWindow)update(it,w) }
 }
 private fun saveSize() { prefs.edit().putFloat("scale",scale).apply() }
 private fun dock() {
  collapsed=true; prefs.edit().putBoolean("collapsed",true).apply()
  handler.removeCallbacks(fadeTask); paint()
 }
 private fun unlock() {
  collapsed=false; faded=false; prefs.edit().putBoolean("collapsed",false).apply()
  paint();scheduleFade()
 }
 /** Settings provides full-size alternatives to the compact bubble controls. */
 fun resetControls() {
  scale=1f;saveSize();resize(scale);unlock()
 }
 fun refresh() { if(view!=null)setTime() }
 private fun setTime() {
  val total=snapshot().apps[pkg] ?: 0L
  timer?.apply { base=SystemClock.elapsedRealtime()-total; text=WatchService.format(total) }
 }
 private fun clamp(w: WindowManager.LayoutParams) {
  val metrics=window.currentWindowMetrics
  val insets=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
  w.x=w.x.coerceIn(insets.left,(metrics.bounds.width()-insets.right-w.width).coerceAtLeast(insets.left))
  w.y=w.y.coerceIn(0,(metrics.bounds.height()-insets.top-insets.bottom-w.height).coerceAtLeast(0))
 }
 private fun update(v: View,w: WindowManager.LayoutParams) { runCatching { window.updateViewLayout(v,w) }.onFailure { Log.w("IWatchYou","Could not update timer overlay",it) } }
 private fun savePosition(w: WindowManager.LayoutParams) { prefs.edit().putInt("x_$positionKey",w.x).putInt("y_$positionKey",w.y).apply() }
 private fun scheduleFade() { handler.removeCallbacks(fadeTask);if(!collapsed)handler.postDelayed(fadeTask,3000) }
 private fun paint() {
  val root=view ?: return
  val w=params ?: return
  // Alpha alone does NOT make a window pass through. The whole faded timer,
  // including corner controls, must relinquish touch and accessibility actions.
  w.flags=if(faded || collapsed) w.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
          else w.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
  root.importantForAccessibility=if(faded || collapsed)View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
  root.visibility=if(collapsed)View.GONE else View.VISIBLE
  root.background=GradientDrawable().apply { cornerRadius=dp(28).toFloat();setColor(Color.rgb(26,36,51));setStroke(dp(1),Color.argb(40,255,255,255)) }
  root.animate().alpha(if(faded)0.68f else 0.94f).setDuration(220).start()
  if(root.isAttachedToWindow)update(root,w)
  if(collapsed || faded) showTab(w) else removeTab()
 }
 private fun showTab(anchor: WindowManager.LayoutParams) {
  val metrics=window.currentWindowMetrics
  val insets=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
  val left=anchor.x+anchor.width/2 < metrics.bounds.width()/2
  val button=tab ?: control(R.drawable.ic_chevron,"Tampilkan kontrol timer").apply {
   // A 48dp touch area surrounds the narrow, solid edge tab. It never becomes transparent.
   setPadding(dp(14),dp(14),dp(14),dp(14))
   background=GradientDrawable().apply { cornerRadius=dp(18).toFloat();setColor(Color.rgb(26,36,51)) }
   setOnClickListener { unlock() }
  }.also { tab=it }
  button.rotation=if(left)0f else 180f
  val p=tabParams ?: WindowManager.LayoutParams(dp(48),dp(48),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.LEFT }.also { tabParams=it }
  p.x=if(left)insets.left else metrics.bounds.width()-insets.right-p.width
  // Avoid covering the faded timer when it was dragged onto this edge.
  p.y=if(collapsed)anchor.y else anchor.y+anchor.height+dp(8);clamp(p)
  if(button.isAttachedToWindow)update(button,p) else runCatching { window.addView(button,p) }.onFailure { Log.w("IWatchYou","Could not attach timer handle",it) }
 }
 private fun removeTab() {
  tab?.let { if(it.isAttachedToWindow)runCatching { window.removeViewImmediate(it) } }
  tab=null;tabParams=null
 }
 fun recreate() {
  val previous=pkg;val dim=faded
  hide()
  if(previous!=null) { show(previous); faded=dim;paint() }
 }
 fun hide() {
  handler.removeCallbacksAndMessages(null);removeTab()
  timer?.stop()
  view?.let { it.animate().cancel();runCatching { window.removeViewImmediate(it) } }
  view=null;timer=null;params=null;pkg=null
 }
}
