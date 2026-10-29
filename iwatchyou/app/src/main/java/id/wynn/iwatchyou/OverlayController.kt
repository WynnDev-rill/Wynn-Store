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
 private var hideButton: ImageButton?=null
 private var hideParams: WindowManager.LayoutParams?=null
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
   if(pkg!=packageName) { pkg=packageName;setTime() }
   return
  }
  pkg=packageName;positionKey=orientation();faded=false
  val clock=Chronometer(context).apply {
   setTextColor(Color.WHITE)
   typeface=Typeface.create(context.resources.getFont(R.font.jakarta_medium),Typeface.NORMAL)
   fontFeatureSettings="tnum";gravity=Gravity.CENTER_VERTICAL;includeFontPadding=false
   contentDescription="Waktu aplikasi";textSize=15f
   setOnChronometerTickListener { text=WatchService.format(SystemClock.elapsedRealtime()-base) }
  }
  maxWidth=maxOf(dp(134),kotlin.math.ceil(clock.paint.measureText("00:00:00")).toInt()+dp(52))
  val metrics=clock.paint.fontMetrics
  maxHeight=maxOf(dp(44),kotlin.math.ceil(metrics.bottom-metrics.top).toInt()+dp(16))
  val root=FrameLayout(context).apply { elevation=dp(3).toFloat() }
  root.addView(clock,FrameLayout.LayoutParams(-1,-1))
  val w=layout(maxWidth,maxHeight).apply {
   x=prefs.getInt("x_$positionKey",dp(16));y=prefs.getInt("y_$positionKey",dp(72))
  }
  view=root;timer=clock;params=w
  drag(clock,w) { scheduleFade() }
  resize()
  if(runCatching { window.addView(root,w) }.onFailure { Log.w("IWatchYou","Could not attach timer overlay",it) }.isFailure) {
   view=null;timer=null;params=null;pkg=null;return
  }
  setTime();clock.start();paint();scheduleFade()
 }
 private fun layout(width: Int,height: Int)=WindowManager.LayoutParams(width,height,
  WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
  PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.LEFT }
 private fun control(label: String)=ImageButton(context).apply {
  setImageResource(R.drawable.ic_chevron);setColorFilter(Color.WHITE);contentDescription=label
  val surface=GradientDrawable().apply { cornerRadius=dp(28).toFloat();setColor(Color.rgb(26,36,51)) }
  background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(50,255,255,255)),surface,null)
 }
 private fun drag(target: View,w: WindowManager.LayoutParams,tap: ()->Unit) {
  var startX=0;var startY=0;var downX=0f;var downY=0f;var moved=false;var cancelled=false
  val slop=ViewConfiguration.get(context).scaledTouchSlop
  target.setOnClickListener { tap() }
  target.setOnTouchListener { v,e ->
   when(e.actionMasked) {
    MotionEvent.ACTION_DOWN -> {
     handler.removeCallbacks(fadeTask);v.isPressed=true;v.drawableHotspotChanged(e.x,e.y)
     startX=w.x;startY=w.y;downX=e.rawX;downY=e.rawY;moved=false;cancelled=false
     true
    }
    MotionEvent.ACTION_MOVE -> {
     if(e.pointerCount==1 && !cancelled) {
      val dx=e.rawX-downX;val dy=e.rawY-downY
      if(hypot(dx,dy)>slop)moved=true
      if(moved) {
       v.isPressed=false
       w.x=startX+dx.roundToInt();w.y=startY+dy.roundToInt();clamp(w)
       view?.let { update(it,w) };showHideControl(w)
      }
     }
     true
    }
    MotionEvent.ACTION_UP -> {
     v.isPressed=false
     if(!cancelled) { if(moved)savePosition(w) else v.performClick() }
     scheduleFade();true
    }
    MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN -> { v.isPressed=false;cancelled=true;scheduleFade();true }
    else -> true
   }
  }
 }
 private fun resize() {
  val w=params ?: return
  w.width=(maxWidth*scale).roundToInt();w.height=(maxHeight*scale).roundToInt()
  timer?.apply { textSize=15f*scale;setPadding((dp(10)*scale).roundToInt(),0,(dp(10)*scale).roundToInt(),0) }
  clamp(w)
  view?.let { if(it.isAttachedToWindow)update(it,w) }
 }
 fun setSize(value: Float) {
  scale=BubbleSize.clamp(value);prefs.edit().putFloat("scale",scale).apply()
  resize();paint()
 }
 private fun dock() {
  collapsed=true;prefs.edit().putBoolean("collapsed",true).apply()
  handler.removeCallbacks(fadeTask);paint()
 }
 private fun unlock() {
  collapsed=false;faded=false;prefs.edit().putBoolean("collapsed",false).apply()
  paint();scheduleFade()
 }
 fun resetControls() { setSize(1f);unlock() }
 fun refresh() { if(view!=null)setTime() }
 private fun setTime() {
  val total=snapshot().apps[pkg] ?: 0L
  timer?.apply { base=SystemClock.elapsedRealtime()-total;text=WatchService.format(total) }
 }
 private fun clamp(w: WindowManager.LayoutParams) {
  val metrics=window.currentWindowMetrics
  val insets=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
  w.x=w.x.coerceIn(insets.left,(metrics.bounds.width()-insets.right-w.width).coerceAtLeast(insets.left))
  w.y=w.y.coerceIn(0,(metrics.bounds.height()-insets.top-insets.bottom-w.height).coerceAtLeast(0))
 }
 private fun update(v: View,w: WindowManager.LayoutParams) {
  runCatching { window.updateViewLayout(v,w) }.onFailure { Log.w("IWatchYou","Could not update timer overlay",it) }
 }
 private fun savePosition(w: WindowManager.LayoutParams) { prefs.edit().putInt("x_$positionKey",w.x).putInt("y_$positionKey",w.y).apply() }
 private fun scheduleFade() {
  handler.removeCallbacks(fadeTask)
  if(!collapsed)handler.postDelayed(fadeTask,context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
   .getRecommendedTimeoutMillis(3000,android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS).toLong())
 }
 private fun paint() {
  val root=view ?: return
  val w=params ?: return
  // The faded body passes touches through. The single solid inline arrow remains an explicit control.
  w.flags=if(faded || collapsed)w.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
   else w.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
  root.importantForAccessibility=if(faded || collapsed)View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
  root.visibility=if(collapsed)View.GONE else View.VISIBLE
  root.background=GradientDrawable().apply { cornerRadius=dp(28).toFloat();setColor(Color.rgb(26,36,51));setStroke(dp(1),Color.argb(40,255,255,255)) }
  root.animate().alpha(if(faded)0.68f else 0.94f).setDuration(220).start()
  if(root.isAttachedToWindow)update(root,w)
  if(collapsed) { removeHideControl();showTab(w) } else { removeTab();showHideControl(w) }
 }
 private fun showHideControl(anchor: WindowManager.LayoutParams) {
  if(collapsed || view?.isAttachedToWindow!=true)return
  val button=hideButton ?: control("Sembunyikan timer").also {
   hideButton=it;drag(it,anchor) { dock() }
  }
  val p=hideParams ?: layout(dp(32),dp(32)).also { hideParams=it }
  val metrics=window.currentWindowMetrics
  val insets=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
  val right=anchor.x+anchor.width+dp(4)
  p.x=if(right+p.width<=metrics.bounds.width()-insets.right)right else anchor.x-p.width-dp(4)
  p.y=anchor.y+(anchor.height-p.height)/2;clamp(p)
  val left=anchor.x+anchor.width/2 < metrics.bounds.width()/2
  button.setImageResource(if(left)R.drawable.ic_chevron_left else R.drawable.ic_chevron)
  button.setPadding(dp(9),dp(9),dp(9),dp(9))
  if(button.isAttachedToWindow)update(button,p) else runCatching { window.addView(button,p) }
   .onFailure { Log.w("IWatchYou","Could not attach hide control",it) }
 }
 private fun showTab(anchor: WindowManager.LayoutParams) {
  val metrics=window.currentWindowMetrics
  val insets=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
  val left=anchor.x+anchor.width/2 < metrics.bounds.width()/2
  val button=tab ?: control("Tampilkan timer").apply { setPadding(dp(10),dp(14),dp(10),dp(14));setOnClickListener { unlock() } }.also { tab=it }
  button.setImageResource(if(left)R.drawable.ic_chevron else R.drawable.ic_chevron_left)
  val p=tabParams ?: layout(dp(32),dp(48)).also { tabParams=it }
  p.x=if(left)insets.left else metrics.bounds.width()-insets.right-p.width;p.y=anchor.y;clamp(p)
  if(button.isAttachedToWindow)update(button,p) else runCatching { window.addView(button,p) }
   .onFailure { Log.w("IWatchYou","Could not attach timer handle",it) }
 }
 private fun removeHideControl() {
  hideButton?.let { if(it.isAttachedToWindow)runCatching { window.removeViewImmediate(it) } }
  hideButton=null;hideParams=null
 }
 private fun removeTab() {
  tab?.let { if(it.isAttachedToWindow)runCatching { window.removeViewImmediate(it) } }
  tab=null;tabParams=null
 }
 fun recreate() {
  val previous=pkg;val dim=faded
  hide()
  if(previous!=null) { show(previous);faded=dim;paint() }
 }
 fun hide() {
  handler.removeCallbacksAndMessages(null);removeTab();removeHideControl()
  timer?.stop()
  view?.let { it.animate().cancel();runCatching { window.removeViewImmediate(it) } }
  view=null;timer=null;params=null;pkg=null
 }
}
