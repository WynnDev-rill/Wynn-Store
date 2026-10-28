package id.wynn.iwatchyou

/** Keep the shipped timer as the maximum; never shrink text below 12sp. */
object BubbleSize {
 const val MIN = 0.8f
 const val MAX = 1f
 fun clamp(value: Float) = if (value.isFinite()) value.coerceIn(MIN, MAX) else MAX
 fun smaller(value: Float) = clamp(value - 0.1f)
}
