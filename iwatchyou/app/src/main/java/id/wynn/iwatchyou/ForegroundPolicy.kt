package id.wynn.iwatchyou

/** Platform-neutral selection rule, independently testable for multi-window/PiP. */
object ForegroundPolicy {
 data class Window(val pkg: String?, val application: Boolean, val focused: Boolean, val active: Boolean, val pip: Boolean=false, val keyboard: Boolean=false, val overlay: Boolean=false, val layer: Int=0)
 fun select(windows: List<Window>, excluded: Set<String>): String? {
  val candidates=windows.filterNot { it.keyboard || it.overlay }
  val focus=candidates.firstOrNull { it.focused }
  if (focus!=null && (!focus.application || focus.pip)) return null
  val target=focus ?: candidates.filter { it.application && it.active && !it.pip }.maxByOrNull { it.layer }
  return target?.pkg?.takeUnless { it in excluded }
 }
}
