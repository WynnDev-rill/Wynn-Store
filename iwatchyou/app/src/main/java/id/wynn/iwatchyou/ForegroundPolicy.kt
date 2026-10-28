package id.wynn.iwatchyou

/** Platform-neutral selection rule, independently testable for multi-window/PiP. */
object ForegroundPolicy {
 fun rootPackage(windowId: Int, nodePackage: String?, owners: Map<Int,String>, auxiliaryPackages: Set<String>, windows: List<Window> = emptyList()): String? {
  val selected=target(windows)
  if(selected!=null && selected.id>=0 && selected.id!=windowId)return null
  val window=windows.firstOrNull { it.id>=0 && it.id==windowId }
  if(window!=null) {
   if(!window.application || window.pip || window.keyboard || window.overlay)return null
   return owners[windowId] ?: nodePackage
  }
  return owners[windowId] ?: nodePackage?.takeUnless { it in auxiliaryPackages }
 }
 data class Window(val pkg: String?, val application: Boolean, val focused: Boolean, val active: Boolean, val pip: Boolean=false, val keyboard: Boolean=false, val overlay: Boolean=false, val layer: Int=0, val id: Int=-1)
 private fun target(windows: List<Window>): Window? {
  val candidates=windows.filterNot { it.keyboard || it.overlay }
  val focus=candidates.firstOrNull { it.focused }
  return focus ?: candidates.filter { it.application && it.active && !it.pip }.maxByOrNull { it.layer }
 }
 fun needsRoot(windows: List<Window>): Boolean {
  target(windows)?.let { return it.application && !it.pip && it.pkg==null }
  return windows.isEmpty() || windows.all { it.keyboard || it.overlay } ||
   windows.any { it.application && !it.pip && !it.keyboard && !it.overlay }
 }
 fun unresolvedHint(hint: String?, windows: List<Window>): String? =
  if(target(windows)?.id?.let { it>=0 }==true) null else hint
 fun select(windows: List<Window>, excluded: Set<String>): String? {
  val target=target(windows) ?: return null
  if(!target.application || target.pip)return null
  return target.pkg?.takeUnless { it in excluded }
 }
}
