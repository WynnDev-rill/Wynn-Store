package id.wynn.arithmemo.ui

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.arithmemo.R

private val Jakarta =
    FontFamily(
        Font(R.font.jakarta_regular),
        Font(R.font.jakarta_medium, FontWeight.Medium),
        Font(R.font.jakarta_semibold, FontWeight.SemiBold),
        Font(R.font.jakarta_bold, FontWeight.Bold),
    )

private fun TextStyle.branded() = copy(fontFamily = Jakarta)

private val Type =
    Typography().let { t ->
        Typography(
            displayLarge = t.displayLarge.branded(),
            displayMedium = t.displayMedium.branded(),
            displaySmall = t.displaySmall.branded(),
            headlineLarge = t.headlineLarge.branded(),
            headlineMedium = t.headlineMedium.branded(),
            headlineSmall = t.headlineSmall.branded(),
            titleLarge = t.titleLarge.branded(),
            titleMedium = t.titleMedium.branded(),
            titleSmall = t.titleSmall.branded(),
            bodyLarge = t.bodyLarge.branded(),
            bodyMedium = t.bodyMedium.branded(),
            bodySmall = t.bodySmall.branded(),
            labelLarge = t.labelLarge.branded(),
            labelMedium = t.labelMedium.branded(),
            labelSmall = t.labelSmall.branded(),
        )
    }
private val Light =
    lightColorScheme(
        primary = Color(0xFF245BE8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEAF0FF),
        onPrimaryContainer = Color(0xFF153B92),
        secondary = Color(0xFF23764C),
        secondaryContainer = Color(0xFFE4F4E9),
        onSecondaryContainer = Color(0xFF135630),
        background = Color(0xFFF6F8FD),
        onBackground = Color(0xFF17243B),
        surface = Color.White,
        onSurface = Color(0xFF17243B),
        surfaceVariant = Color(0xFFEDF1F9),
        onSurfaceVariant = Color(0xFF52627A),
        outline = Color(0xFF738198),
        outlineVariant = Color(0xFFDDE3EF),
        error = Color(0xFFB3261E),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFFAFC6FF),
        onPrimary = Color(0xFF082D84),
        primaryContainer = Color(0xFF183B83),
        onPrimaryContainer = Color(0xFFDCE7FF),
        secondary = Color(0xFF93D7AB),
        secondaryContainer = Color(0xFF194D30),
        onSecondaryContainer = Color(0xFFC6F2D6),
        background = Color(0xFF101722),
        onBackground = Color(0xFFE6EDFA),
        surface = Color(0xFF192230),
        onSurface = Color(0xFFE6EDFA),
        surfaceVariant = Color(0xFF252F40),
        onSurfaceVariant = Color(0xFFBDC8DC),
        outline = Color(0xFF8B9AB3),
        outlineVariant = Color(0xFF354258),
    )

@Composable
fun ArithMemoTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark =
        when (theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
    val view = LocalView.current
    val activity =
        generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<ComponentActivity>()
            .firstOrNull()
    SideEffect {
        val transparent = android.graphics.Color.TRANSPARENT
        val style =
            if (dark) SystemBarStyle.dark(transparent)
            else SystemBarStyle.light(transparent, transparent)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = Type,
        shapes =
            Shapes(
                extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            ),
        content = content,
    )
}
