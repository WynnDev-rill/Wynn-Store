package id.wynn.furina

import kotlin.math.hypot

enum class TouchKind { NONE, HEAD, BODY, PET, DRAG, HOLD, CANCEL }

/** Coordinates are dp and timestamps are MotionEvent eventTime. No velocity assumptions. */
class PetTouch {
 private var sx=0f;private var sy=0f;private var px=0f;private var py=0f
 private var started=0L;private var path=0f;private var head=false;private var dragging=false;private var stroked=false
 fun begin(x: Float,y: Float,time: Long,onHead: Boolean) {
  sx=x;sy=y;px=x;py=y;started=time;head=onHead;path=0f;dragging=false;stroked=false
 }
 fun move(x: Float,y: Float,time: Long): TouchKind {
  if(time<started) return TouchKind.NONE
  path+=hypot(x-px,y-py);px=x;py=y
  val distance=hypot(x-sx,y-sy)
  if(distance>if(head) 30f else 8f) dragging=true
  if(dragging) return TouchKind.DRAG
  if(head && path>=40f && time-started>=80) {stroked=true;return TouchKind.PET}
  return TouchKind.NONE
 }
 fun finish(time: Long,cancelled: Boolean): TouchKind = when {
  cancelled -> TouchKind.CANCEL
  dragging -> TouchKind.DRAG
  stroked -> TouchKind.PET
  time-started>=650 -> TouchKind.HOLD
  head -> TouchKind.HEAD
  else -> TouchKind.BODY
 }
}
