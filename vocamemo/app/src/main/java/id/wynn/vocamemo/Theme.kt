package id.wynn.vocamemo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF13584C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9DF),
    onPrimaryContainer = Color(0xFF123D32),
    secondary = Color(0xFF745115),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E3C7),
    onSecondaryContainer = Color(0xFF4E3917),
    tertiary = Color(0xFF35634A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9EBDD),
    onTertiaryContainer = Color(0xFF143F2B),
    background = Color(0xFFF4F6F0),
    onBackground = Color(0xFF1C2923),
    surface = Color(0xFFFBFCF8),
    onSurface = Color(0xFF1C2923),
    surfaceVariant = Color(0xFFE9EEE6),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F8F2),
    surfaceContainer = Color(0xFFEEF2EA),
    surfaceContainerHigh = Color(0xFFE8EDE5),
    surfaceContainerHighest = Color(0xFFE0E8DE),
    onSurfaceVariant = Color(0xFF4C5B51),
    outline = Color(0xFF77877C),
    outlineVariant = Color(0xFFCDD7CD),
    surfaceTint = Color(0xFF13584C),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA3D3BB),
    onPrimary = Color(0xFF17382C),
    primaryContainer = Color(0xFF293B32),
    onPrimaryContainer = Color(0xFFD6EFE0),
    secondary = Color(0xFFE0BF81),
    onSecondary = Color(0xFF3C2909),
    secondaryContainer = Color(0xFF3D3426),
    onSecondaryContainer = Color(0xFFF0E0BF),
    tertiary = Color(0xFFADD5AE),
    onTertiary = Color(0xFF203823),
    tertiaryContainer = Color(0xFF2B3A2A),
    onTertiaryContainer = Color(0xFFD4EAD2),
    background = Color(0xFF191A18),
    onBackground = Color(0xFFF2F1E9),
    surface = Color(0xFF222421),
    onSurface = Color(0xFFF2F1E9),
    surfaceVariant = Color(0xFF30332D),
    surfaceContainerLowest = Color(0xFF171815),
    surfaceContainerLow = Color(0xFF1E201C),
    surfaceContainer = Color(0xFF262823),
    surfaceContainerHigh = Color(0xFF2D302A),
    surfaceContainerHighest = Color(0xFF363932),
    onSurfaceVariant = Color(0xFFC0C3B7),
    outline = Color(0xFF888E81),
    outlineVariant = Color(0xFF42483F),
    surfaceTint = Color(0xFFA3D3BB),
)

private val InterfaceFont = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_bold, FontWeight.ExtraBold),
)

private val EntryFont = FontFamily(Font(R.font.literata_medium, FontWeight.Medium))

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = InterfaceFont, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

private val VocaType = Typography(
    headlineLarge = type(34, 42, FontWeight.Bold),
    headlineMedium = type(28, 36, FontWeight.Bold),
    headlineSmall = type(24, 32, FontWeight.Bold),
    titleLarge = type(20, 28, FontWeight.Bold),
    titleMedium = type(16, 23, FontWeight.SemiBold),
    titleSmall = type(14, 20, FontWeight.SemiBold),
    bodyLarge = type(16, 24),
    bodyMedium = type(14, 21),
    bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.SemiBold),
    labelMedium = type(12, 17, FontWeight.SemiBold),
    labelSmall = type(11, 16, FontWeight.SemiBold),
)

internal fun entryType(size: Int, line: Int) =
    TextStyle(fontFamily = EntryFont, fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight.Medium)

private val LocalDarkGlass = staticCompositionLocalOf { false }

@Composable
internal fun usesDarkTheme(preference: String): Boolean =
    preference == "Gelap" || preference == "Sistem" && isSystemInDarkTheme()

internal fun windowColor(dark: Boolean): Int = (if (dark) Dark else Light).background.toArgb()

@Composable
fun VocaTheme(preference: String, content: @Composable () -> Unit) {
    val dark = usesDarkTheme(preference)
    CompositionLocalProvider(LocalDarkGlass provides dark) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = VocaType,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp),
            ),
            content = content,
        )
    }
}

@Composable
internal fun Modifier.glass(shape: Shape): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = LocalDarkGlass.current
    val face = if (dark) Color(0xFF373D34) else Color.White
    return clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(face.copy(alpha = if (dark) .84f else .94f), colors.surfaceContainer.copy(alpha = .88f))
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(if (dark) Color(0xFF535B4F) else Color.White, colors.outlineVariant)
            ),
            shape = shape,
        )
}

@Composable
internal fun ReadingBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = LocalDarkGlass.current
    val top = if (dark) Color(0xFF292D26) else Color(0xFFE3EEE4)
    Box(
        modifier.background(Brush.verticalGradient(listOf(top, colors.background, colors.background))),
    ) { content() }
}
