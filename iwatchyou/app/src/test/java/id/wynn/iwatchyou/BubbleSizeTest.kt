package id.wynn.iwatchyou

import org.junit.Assert.*
import org.junit.Test

class BubbleSizeTest {
 @Test fun repeatedShrinkStopsAtHalfSize() {
  var size=1f
  repeat(100) { size=BubbleSize.smaller(size) }
  assertEquals(0.5f,size,0f)
 }
 @Test fun restoredAndDraggedSizesCannotExceedEitherBound() {
  assertEquals(1f,BubbleSize.clamp(5f),0f)
  assertEquals(0.5f,BubbleSize.clamp(-5f),0f)
  assertEquals(1f,BubbleSize.clamp(Float.NaN),0f)
  assertEquals(1f,BubbleSize.clamp(Float.POSITIVE_INFINITY),0f)
  assertEquals(0.9f,BubbleSize.clamp(0.9f),0f)
 }
}
