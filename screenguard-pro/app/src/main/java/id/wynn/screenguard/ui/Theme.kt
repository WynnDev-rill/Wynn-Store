package id.wynn.screenguard.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import id.wynn.screenguard.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

val Blue = Color(0xFF2366D1)
val PaleBlue = Color(0xFFEAF3FF)
val Ink = Color(0xFF142137)
val Muted = Color(0xFF58677C)
val Green = Color(0xFF158453)
val Success: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < .2f) Color(0xFF6CDDAB) else Green
private val LightColors = lightColorScheme(primary = Blue, onPrimary = Color.White,
    primaryContainer = PaleBlue, onPrimaryContainer = Color(0xFF1459AD),
    secondary = Blue, onSecondary = Color.White, secondaryContainer = PaleBlue, onSecondaryContainer = Color(0xFF1459AD),
    tertiary = Green, onTertiary = Color.White, tertiaryContainer = Color(0xFFE5F5EC), onTertiaryContainer = Color(0xFF075D37),
    background = Color(0xFFF6F9FD), onBackground = Ink, surface = Color.White,
    onSurface = Ink, surfaceVariant = Color(0xFFEFF3F8), onSurfaceVariant = Muted,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F5FC),
    surfaceContainer = Color(0xFFEDF2FA), surfaceContainerHigh = Color(0xFFE8EFF9), surfaceContainerHighest = Color(0xFFE0E9F5),
    outline = Color(0xFF8493A7), outlineVariant = Color(0xFFE5ECF5), error = Color(0xFFB3261E))
private val DarkColors = darkColorScheme(primary = Color(0xFF9EC8FF), onPrimary = Color(0xFF003367),
    primaryContainer = Color(0xFF173B65), onPrimaryContainer = Color(0xFFD4E5FF),
    secondary = Color(0xFF9EC8FF), onSecondary = Color(0xFF003367), secondaryContainer = Color(0xFF173B65), onSecondaryContainer = Color(0xFFD4E5FF),
    tertiary = Color(0xFF6CDDAB), onTertiary = Color(0xFF003922), tertiaryContainer = Color(0xFF114D34), onTertiaryContainer = Color(0xFFC0F5D9),
    background = Color(0xFF0E1726), onBackground = Color(0xFFE8EFFB), surface = Color(0xFF172336),
    onSurface = Color(0xFFE8EFFB), surfaceVariant = Color(0xFF243248), onSurfaceVariant = Color(0xFFB6C4D8),
    surfaceContainerLowest = Color(0xFF0A111D), surfaceContainerLow = Color(0xFF142033),
    surfaceContainer = Color(0xFF1B293E), surfaceContainerHigh = Color(0xFF243248), surfaceContainerHighest = Color(0xFF2D3E55),
    outlineVariant = Color(0xFF35465E))
private val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold)
)
private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) =
    TextStyle(fontFamily = Jakarta, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight, letterSpacing = tracking.sp)
private val GuardTypography = Typography(
    displayLarge = type(48, 56, FontWeight.Bold, -1.6f), displayMedium = type(40, 48, FontWeight.Bold, -1.2f),
    displaySmall = type(36, 44, FontWeight.Bold, -1f), headlineLarge = type(32, 40, FontWeight.Bold, -1f),
    headlineMedium = type(28, 36, FontWeight.Bold, -.8f), headlineSmall = type(24, 32, FontWeight.Bold, -.6f),
    titleLarge = type(20, 28, FontWeight.Bold, -.5f), titleMedium = type(15, 22, FontWeight.SemiBold, -.2f),
    titleSmall = type(14, 20, FontWeight.SemiBold), bodyLarge = type(16, 24), bodyMedium = type(14, 21),
    bodySmall = type(12, 18), labelLarge = type(13, 20, FontWeight.SemiBold),
    labelMedium = type(12, 18, FontWeight.SemiBold), labelSmall = type(11, 16, FontWeight.Medium)
)
@Composable fun GuardTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = GuardTypography,
        shapes = Shapes(extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)), content = content)
}

fun duration(ms: Long): String {
    val minutes = (ms.coerceAtLeast(0) / 60_000).toInt()
    return when { minutes == 0 -> "<1 mnt"; minutes < 60 -> "$minutes mnt"; else -> "${minutes / 60}j ${minutes % 60}m" }
}
fun timeLabel(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
