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
}
