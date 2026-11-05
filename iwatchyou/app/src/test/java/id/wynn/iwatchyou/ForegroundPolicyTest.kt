package id.wynn.iwatchyou
import org.junit.Assert.*
import org.junit.Test
class ForegroundPolicyTest {
 private val ignored=setOf("self","home","system")
 private fun app(p:String,f:Boolean=false,a:Boolean=false,pip:Boolean=false,layer:Int=0)=ForegroundPolicy.Window(p,true,f,a,pip,layer=layer)
 @Test fun splitScreenCountsOnlyFocusedApp() { assertEquals("b",ForegroundPolicy.select(listOf(app("a",a=true),app("b",f=true)),ignored)) }
 @Test fun pipIsNotCountedAsBackgroundPlayback() { assertEquals("chat",ForegroundPolicy.select(listOf(app("video",pip=true,layer=9),app("chat",f=true)),ignored)) }
 @Test fun pipOnlyWithoutFocusedAppStopsTimer() { assertNull(ForegroundPolicy.select(listOf(app("video",a=true,pip=true)),ignored)) }
 @Test fun keyboardDoesNotStealApp() { assertEquals("chat",ForegroundPolicy.select(listOf(ForegroundPolicy.Window("keyboard",false,true,true,keyboard=true),app("chat",a=true)),ignored)) }
 @Test fun notificationShadePausesApp() { assertNull(ForegroundPolicy.select(listOf(ForegroundPolicy.Window("system",false,true,true),app("game",a=true)),ignored)) }
 @Test fun accessibilityOverlayDoesNotStealApp() { assertEquals("game",ForegroundPolicy.select(listOf(ForegroundPolicy.Window("self",false,true,true,overlay=true),app("game",a=true)),ignored)) }
 @Test fun launcherIsNotAnAppSession() { assertNull(ForegroundPolicy.select(listOf(app("home",f=true)),ignored)) }
 @Test fun selfIsNotAnAppSession() { assertNull(ForegroundPolicy.select(listOf(app("self",f=true)),ignored)) }
 @Test fun noWindowsStopsTimer() { assertNull(ForegroundPolicy.select(emptyList(),ignored)) }
 @Test fun unknownWindowNeverReusesPreviousApp() { assertNull(ForegroundPolicy.select(listOf(ForegroundPolicy.Window(null,true,true,true)),ignored)) }
 @Test fun windowOwnerWinsOverStaleOrDelegatedNodePackage() {
  assertEquals("fixture",ForegroundPolicy.rootPackage(42,"settings",mapOf(42 to "fixture"),emptySet()))
 }
 @Test fun launcherWindowDoesNotBecomeItsEmbeddedSearchPackage() {
  val pkg=ForegroundPolicy.rootPackage(7,"search",mapOf(7 to "home"),emptySet())
  assertNull(ForegroundPolicy.select(listOf(ForegroundPolicy.Window(pkg,true,true,true)),ignored))
 }
 @Test fun knownWindowOwnerSurvivesMissingNodePackage() {
  assertEquals("fixture",ForegroundPolicy.rootPackage(42,null,mapOf(42 to "fixture"),emptySet()))
 }
 @Test fun unknownKeyboardRootCannotReplaceTheApplicationHint() {
  assertNull(ForegroundPolicy.rootPackage(9,"ime",emptyMap(),setOf("ime")))
 }
 @Test fun keyboardPackageCanStillOwnARealSettingsActivity() {
  assertEquals("ime",ForegroundPolicy.rootPackage(10,"ime",mapOf(10 to "ime"),setOf("ime")))
 }
 @Test fun ordinaryActiveRootIsUsableWithoutAnEventOwner() {
  assertEquals("fixture",ForegroundPolicy.rootPackage(42,"fixture",emptyMap(),emptySet()))
 }
 @Test fun absentRootAndOwnerProvideNoSelection() {
  assertNull(ForegroundPolicy.rootPackage(42,null,emptyMap(),emptySet()))
 }
 @Test fun knownFocusedAppDoesNotNeedFallbackForAnotherUnknownActiveWindow() {
  assertFalse(ForegroundPolicy.needsRoot(listOf(app("chat",f=true),ForegroundPolicy.Window(null,true,false,true))))
 }
 @Test fun knownLauncherFocusIsNotReplacedByAnotherUnknownWindow() {
  assertFalse(ForegroundPolicy.needsRoot(listOf(app("home",f=true),ForegroundPolicy.Window(null,true,false,true))))
 }
 @Test fun systemFocusDoesNotNeedAnApplicationRootFallback() {
  assertFalse(ForegroundPolicy.needsRoot(listOf(ForegroundPolicy.Window("system",false,true,true),ForegroundPolicy.Window(null,true,false,true))))
 }
 @Test fun focusedPipDoesNotNeedAnApplicationRootFallback() {
  assertFalse(ForegroundPolicy.needsRoot(listOf(app("video",f=true,pip=true),ForegroundPolicy.Window(null,true,false,true))))
 }
 @Test fun unknownFocusedApplicationNeedsCurrentRoot() {
  assertTrue(ForegroundPolicy.needsRoot(listOf(ForegroundPolicy.Window(null,true,true,true))))
 }
 @Test fun auxiliaryWindowsAloneDoNotIdentifyTheApplication() {
  assertTrue(ForegroundPolicy.needsRoot(listOf(ForegroundPolicy.Window(null,false,true,true,keyboard=true))))
 }
 @Test fun absentWindowsNeedCurrentRoot() { assertTrue(ForegroundPolicy.needsRoot(emptyList())) }
 @Test fun knownKeyboardWindowCannotBeSelectedEvenWithAnOwnerHint() {
  assertNull(ForegroundPolicy.rootPackage(9,"ime",mapOf(9 to "ime"),emptySet(),listOf(ForegroundPolicy.Window(null,false,true,true,keyboard=true,id=9))))
 }
 @Test fun knownOverlayWindowCannotBeSelectedEvenWithAnOwnerHint() {
  assertNull(ForegroundPolicy.rootPackage(9,"overlay.app",mapOf(9 to "overlay.app"),emptySet(),listOf(ForegroundPolicy.Window(null,false,true,true,overlay=true,id=9))))
 }
 @Test fun knownPipWindowCannotBeSelectedEvenWithAnOwnerHint() {
  assertNull(ForegroundPolicy.rootPackage(9,"video",mapOf(9 to "video"),emptySet(),listOf(ForegroundPolicy.Window(null,true,true,true,pip=true,id=9))))
 }
 @Test fun keyboardSettingsWindowIsAnAppEvenBeforeAnOwnerEvent() {
  assertEquals("ime",ForegroundPolicy.rootPackage(9,"ime",emptyMap(),setOf("ime"),listOf(ForegroundPolicy.Window(null,true,true,true,id=9))))
 }
 @Test fun unownedSelfOverlayRootCannotReplaceApplicationHint() {
  assertNull(ForegroundPolicy.rootPackage(9,"self",emptyMap(),setOf("self")))
 }
 @Test fun nonFocusedRootCannotStandForAnUnknownFocusedWindow() {
  val windows=listOf(ForegroundPolicy.Window(null,true,true,true,id=42),ForegroundPolicy.Window("other",true,false,true,id=43))
  assertNull(ForegroundPolicy.rootPackage(43,"other",mapOf(43 to "other"),emptySet(),windows))
 }
 @Test fun globalHintCannotIdentifyAnUnresolvedKnownWindow() {
  assertNull(ForegroundPolicy.unresolvedHint("other",listOf(ForegroundPolicy.Window(null,true,true,true,id=42))))
 }
 @Test fun hintRemainsLastResortWhenAllWindowMetadataIsMissing() {
  assertEquals("fixture",ForegroundPolicy.unresolvedHint("fixture",emptyList()))
 }
 @Test fun hintIsUsableWhenWindowIdentityIsUnavailable() {
  assertEquals("fixture",ForegroundPolicy.unresolvedHint("fixture",listOf(ForegroundPolicy.Window(null,true,true,true))))
 }
}
