package id.wynn.iwatchyou

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.*
import kotlin.math.hypot
import kotlin.math.roundToInt

/** One small accessibility window. Hiding its clock never pauses the ledger. */
class OverlayController(private val context: AccessibilityService, private val snapshot: () -> TimeLedger.Snapshot) {
 private enum class Mode { COMPACT, EXPANDED, EDGE }
 private val window=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
 private val handler=Handler(Looper.getMainLooper())
 private val metadata=java.util.concurrent.Executors.newSingleThreadExecutor()
 private val prefs=context.getSharedPreferences("overlay",Context.MODE_PRIVATE)
 private var view: FrameLayout?=null
 private var timer: Chronometer?=null
 private var params: WindowManager.LayoutParams?=null
 private var pkg: String?=null
 private var positionKey="portrait"
 private var mode=if(prefs.getBoolean("edge",false))Mode.EDGE else Mode.COMPACT
 private var right=prefs.getBoolean("right",true)
 private var faded=false
 private var resize: ValueAnimator?=null
 private val density get()=context.resources.displayMetrics.density
 private fun dp(n: Int)=(n*density).roundToInt()
 private fun orientation()=if(context.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
 private val fadeTask=Runnable { faded=true; paint() }
 private fun backing(radius: Int=24)=GradientDrawable().apply { cornerRadius=dp(radius).toFloat();setColor(Color.rgb(20,23,34)) }
 fun show(packageName: String) {
  if(view!=null) {
   if(pkg!=packageName) { pkg=packageName;setTime();if(mode==Mode.EXPANDED)render(false) }
   return
  }
  pkg=packageName;positionKey=orientation();faded=false
  val root=FrameLayout(context).apply { clipChildren=true;clipToPadding=true }
  val w=WindowManager.LayoutParams(dp(128),dp(48),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT).apply {
   gravity=Gravity.TOP or Gravity.LEFT;x=prefs.getInt("x_$positionKey",dp(16));y=prefs.getInt("y_$positionKey",dp(72))
  }
  view=root;params=w;render(false);clamp(w)
  if(runCatching { window.addView(root,w) }.onFailure { Log.w("IWatchYou","Could not attach the timer overlay",it) }.isFailure) {
   timer?.stop();view=null;timer=null;params=null;pkg=null;return
  }
  scheduleFade()
 }
 private fun clock(size: Float)=object: Chronometer(context) {
  override fun performClick(): Boolean { super.performClick();return true }
 }.apply {
  setTextColor(Color.WHITE);textSize=size;includeFontPadding=false;gravity=Gravity.CENTER
  typeface=Typeface.create(context.resources.getFont(R.font.jakarta_medium),Typeface.NORMAL);fontFeatureSettings="tnum"
  setOnChronometerTickListener { text=WatchService.format(SystemClock.elapsedRealtime()-base) }
 }
 private fun action(description: String,kind: Int,onClick:()->Unit)=object: View(context) {
  private val pen=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE;strokeWidth=dp(2).toFloat();style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND }
  override fun onDraw(canvas: Canvas) {
   val x=width/2f;val y=height/2f;val d=dp(4).toFloat()
   val path=Path()
   if(kind==0) { path.moveTo(x-d,y-d/2);path.lineTo(x,y+d/2);path.lineTo(x+d,y-d/2) }
   else { val sign=if((mode==Mode.EDGE && this@OverlayController.right) || (mode!=Mode.EDGE && !this@OverlayController.right)) -1 else 1
    path.moveTo(x-sign*d/2,y-d);path.lineTo(x+sign*d/2,y);path.lineTo(x-sign*d/2,y+d) }
   canvas.drawPath(path,pen)
  }
 }.apply { contentDescription=description;isClickable=true;isFocusable=true;setOnClickListener { onClick() } }
 private fun render(animated: Boolean) {
  val root=view ?: return;val w=params ?: return
  resize?.cancel();timer?.stop();timer=null;root.removeAllViews()
  val font=context.resources.configuration.fontScale
  val compactHeight=maxOf(dp(48),dp((32*font).roundToInt()))
  var width=dp(128);var height=compactHeight
  if(mode==Mode.EDGE) {
   width=dp(48);height=dp(48);root.background=null
   val handle=FrameLayout(context).apply { background=backing(12) }
   root.addView(handle,FrameLayout.LayoutParams(dp(16),dp(36),Gravity.CENTER_VERTICAL or if(right)Gravity.RIGHT else Gravity.LEFT))
   root.addView(action("Tampilkan timer",1) { change(Mode.COMPACT) },FrameLayout.LayoutParams(-1,-1))
  } else {
   val body=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(8),0,0,0) }
   root.background=if(mode==Mode.COMPACT)android.graphics.drawable.InsetDrawable(backing(),0,dp(6),0,dp(6)) else backing()
   root.addView(body,FrameLayout.LayoutParams(-1,-1))
   if(mode==Mode.EXPANDED) {
    width=minOf(dp(288),window.currentWindowMetrics.bounds.width()-dp(16));height=dp((128*maxOf(1f,font)).roundToInt())
    val header=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(8),dp(8),0) }
    val icon=ImageView(context)
    header.addView(icon,LinearLayout.LayoutParams(dp(28),dp(28)))
    val label=TextView(context).apply { text="Aplikasi";setTextColor(Color.WHITE);textSize=14f;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(dp(10),0,0,0) }
    header.addView(label,LinearLayout.LayoutParams(0,dp(40),1f));body.addView(header)
    val identity=pkg
    metadata.execute {
     val info=runCatching { val app=context.packageManager.getApplicationInfo(identity!!,0);context.packageManager.getApplicationLabel(app) to context.packageManager.getApplicationIcon(app) }.getOrNull()
     handler.post { if(pkg==identity && label.isAttachedToWindow && info!=null) { label.text=info.first;icon.setImageDrawable(info.second) } }
    }
   }
   val row=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL }
   val time=clock(if(mode==Mode.EXPANDED)24f else 13f);timer=time
   if(mode==Mode.COMPACT) width=maxOf(dp(128),time.paint.measureText("00:00:00").roundToInt()+dp(64))
   val largeExpanded=mode==Mode.EXPANDED && font>1.3f
   if(largeExpanded)body.addView(time,LinearLayout.LayoutParams(-1,0,1f))
   else row.addView(time,LinearLayout.LayoutParams(0,-1,1f))
   if(mode==Mode.COMPACT) {
    val dot=GradientDrawable().apply { shape=GradientDrawable.OVAL;setColor(Color.rgb(94,222,166));setBounds(0,0,dp(5),dp(5)) }
    time.setCompoundDrawables(dot,null,null,null);time.compoundDrawablePadding=dp(4)
   }
   if(mode==Mode.EXPANDED) row.addView(action("Ringkas timer",0) { change(Mode.COMPACT) },LinearLayout.LayoutParams(dp(48),dp(48)))
   row.addView(action("Sembunyikan timer ke tepi",1) { dock() },LinearLayout.LayoutParams(dp(48),maxOf(dp(48),compactHeight)))
   body.addView(row,if(largeExpanded)LinearLayout.LayoutParams(-1,dp(48)) else LinearLayout.LayoutParams(-1,0,1f))
   // The entire 48dp clock target accepts drag; a tap expands the compact island.
   drag(time) { if(mode==Mode.COMPACT)change(Mode.EXPANDED) }
   setTime();time.start()
  }
  val oldWidth=w.width;val oldHeight=w.height;val oldX=w.x
  w.width=width;w.height=height
  if(mode==Mode.EDGE) w.x=if(right)window.currentWindowMetrics.bounds.width()-width else 0
  else if(prefs.getBoolean("edge",false)) w.x=if(right)window.currentWindowMetrics.bounds.width()-width-dp(8) else dp(8)
  clamp(w);val finalX=w.x
  if(animated && root.isAttachedToWindow && ValueAnimator.areAnimatorsEnabled()) {
   resize=ValueAnimator.ofFloat(0f,1f).apply {
    duration=220;interpolator=android.view.animation.DecelerateInterpolator()
    addUpdateListener { val f=it.animatedValue as Float;w.width=(oldWidth+(width-oldWidth)*f).roundToInt();w.height=(oldHeight+(height-oldHeight)*f).roundToInt();w.x=(oldX+(finalX-oldX)*f).roundToInt();update(root,w) };start()
   }
  } else update(root,w)
  paint()
 }
 private fun change(next: Mode) {
  faded=false;mode=next;render(true);prefs.edit().putBoolean("edge",next==Mode.EDGE).apply();scheduleFade()
 }
 private fun dock() {
  val w=params ?: return
  right=w.x+w.width/2>=window.currentWindowMetrics.bounds.width()/2
  savePosition(w);prefs.edit().putBoolean("right",right).apply();change(Mode.EDGE)
 }
 private fun drag(target: View,onTap:()->Unit) {
  var startX=0;var startY=0;var downX=0f;var downY=0f;var moved=false
  val slop=ViewConfiguration.get(context).scaledTouchSlop
  target.setOnClickListener { onTap() }
  target.setOnTouchListener { _,e ->
   val w=params ?: return@setOnTouchListener false
   when(e.actionMasked) {
    MotionEvent.ACTION_DOWN -> { resize?.end();handler.removeCallbacks(fadeTask);faded=false;paint();startX=w.x;startY=w.y;downX=e.rawX;downY=e.rawY;moved=false }
    MotionEvent.ACTION_MOVE -> {
     val dx=e.rawX-downX;val dy=e.rawY-downY
     if(hypot(dx,dy)>slop)moved=true
     if(moved) { w.x=startX+dx.roundToInt();w.y=startY+dy.roundToInt();clamp(w);view?.let { update(it,w) } }
    }
    MotionEvent.ACTION_UP -> {
     if(moved)savePosition(w) else target.performClick()
     scheduleFade()
    }
    MotionEvent.ACTION_CANCEL -> scheduleFade()
   };true
  }
 }
 fun refresh() { if(view!=null)setTime() }
 private fun setTime() { val total=snapshot().apps[pkg] ?: 0L;timer?.apply { base=SystemClock.elapsedRealtime()-total;text=WatchService.format(total) } }
 private fun clamp(w: WindowManager.LayoutParams) {
  val metrics=window.currentWindowMetrics
  val insets=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
  w.x=w.x.coerceIn(insets.left,(metrics.bounds.width()-insets.right-w.width).coerceAtLeast(insets.left))
  w.y=w.y.coerceIn(0,(metrics.bounds.height()-insets.top-insets.bottom-w.height).coerceAtLeast(0))
 }
 private fun update(v: View,w: WindowManager.LayoutParams) { if(v.isAttachedToWindow)runCatching { window.updateViewLayout(v,w) } }
 private fun savePosition(w: WindowManager.LayoutParams) { prefs.edit().putInt("x_$positionKey",w.x).putInt("y_$positionKey",w.y).apply() }
 private fun scheduleFade() { handler.removeCallbacks(fadeTask);handler.postDelayed(fadeTask,3000) }
 private fun paint() { view?.apply { val target=if(faded)0.76f else 0.98f
  if(ValueAnimator.areAnimatorsEnabled())animate().alpha(target).setDuration(180).start() else { animate().cancel();alpha=target }
 } }
 fun recreate() { val previous=pkg;hide();if(previous!=null)show(previous) }
 fun hide() {
  resize?.cancel();resize=null;handler.removeCallbacksAndMessages(null);timer?.stop()
  view?.let { it.animate().cancel();runCatching { window.removeViewImmediate(it) } }
  view=null;timer=null;params=null;pkg=null
 }
 fun close() { hide();metadata.shutdownNow() }
}
