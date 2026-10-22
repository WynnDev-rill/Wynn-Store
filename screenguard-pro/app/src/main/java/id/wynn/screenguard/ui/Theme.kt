package id.wynn.screenguard.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Blue = Color(0xFF1E73DF)
val PaleBlue = Color(0xFFEAF3FF)
val Ink = Color(0xFF142137)
val Muted = Color(0xFF58677C)
val Green = Color(0xFF158453)
val Success: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < .2f) Color(0xFF6CDDAB) else Green
private val LightColors = lightColorScheme(primary = Blue, onPrimary = Color.White,
    primaryContainer = PaleBlue, onPrimaryContainer = Color(0xFF1459AD),
    background = Color(0xFFF6F9FD), onBackground = Ink, surface = Color.White,
    onSurface = Ink, surfaceVariant = Color(0xFFEFF3F8), onSurfaceVariant = Muted,
    outline = Color(0xFF8493A7), outlineVariant = Color(0xFFE5ECF5), error = Color(0xFFB3261E))
private val DarkColors = darkColorScheme(primary = Color(0xFF9EC8FF), onPrimary = Color(0xFF003367),
    primaryContainer = Color(0xFF173B65), onPrimaryContainer = Color(0xFFD4E5FF),
    background = Color(0xFF0E1726), onBackground = Color(0xFFE8EFFB), surface = Color(0xFF172336),
    onSurface = Color(0xFFE8EFFB), surfaceVariant = Color(0xFF243248), onSurfaceVariant = Color(0xFFB6C4D8),
    outlineVariant = Color(0xFF35465E))
@Composable fun GuardTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = base.copy(
        headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-1).sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 23.sp, letterSpacing = (-.6).sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-.5).sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
        bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    ), content = content)
}

fun duration(ms: Long): String {
    val minutes = (ms.coerceAtLeast(0) / 60_000).toInt()
    return when { minutes == 0 -> "<1 mnt"; minutes < 60 -> "$minutes mnt"; else -> "${minutes / 60}j ${minutes % 60}m" }
}
fun timeLabel(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
