package id.wynn.furina

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/** Separate, small, non-touchable window: text never expands the pet's input rectangle. */
class SpeechBubble(private val context: Context,private val wm: WindowManager) {
 private val d=context.resources.displayMetrics.density
 private var until=0L
 private var attached=false
 private val text=TextView(context).apply {
  setTextColor(Color.rgb(27,44,70));textSize=13f;maxLines=3
  setPadding((14*d).toInt(),(10*d).toInt(),(14*d).toInt(),(10*d).toInt())
  background=GradientDrawable().apply {setColor(Color.rgb(241,245,255));cornerRadius=18*d;setStroke((d).toInt().coerceAtLeast(1),Color.rgb(206,222,249))}
  importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
 }
 private val p=WindowManager.LayoutParams((220*d).toInt(),WindowManager.LayoutParams.WRAP_CONTENT,
  WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
  PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START;alpha=0.8f }
 fun show(line: String) {
  text.text=line;until=SystemClock.elapsedRealtime()+6500
  if(!attached) {wm.addView(text,p);attached=true}
  text.alpha=0f;text.animate().alpha(1f).setDuration(180).start()
 }
 fun position(x: Int,y: Int,petWidth: Int,petHeight: Int,minX: Int,minY: Int,maxX: Int,maxY: Int) {
  if(!attached) return
  if(SystemClock.elapsedRealtime()>=until) { hide();return }
  text.measure(View.MeasureSpec.makeMeasureSpec(p.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
  val nx=(x+petWidth/2-p.width/2).coerceIn(minX,(maxX-p.width).coerceAtLeast(minX))
  val above=y-text.measuredHeight-(8*d).toInt()
  val ny=(if(above>=minY) above else y+petHeight+(8*d).toInt()).coerceIn(minY,(maxY-text.measuredHeight).coerceAtLeast(minY))
  if(p.x!=nx || p.y!=ny) {p.x=nx;p.y=ny;wm.updateViewLayout(text,p)}
 }
 fun hide() {if(attached) {text.animate().cancel();runCatching {wm.removeViewImmediate(text)};attached=false};until=0L}
}
