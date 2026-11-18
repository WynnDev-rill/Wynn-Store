package id.wynn.vocamemo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val Light =
    lightColorScheme(
        primary = Color(0xFF5943A8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEBE1FF),
        onPrimaryContainer = Color(0xFF302064),
        secondary = Color(0xFF864966),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF9E0EB),
        onSecondaryContainer = Color(0xFF532740),
        tertiary = Color(0xFF35665A),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD7F0E8),
        onTertiaryContainer = Color(0xFF17483E),
        background = Color(0xFFF8F5FC),
        onBackground = Color(0xFF261F30),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF261F30),
        surfaceVariant = Color(0xFFEEE7F4),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF8F5FC),
        surfaceContainer = Color(0xFFF2EDF8),
        surfaceContainerHigh = Color(0xFFEEE7F4),
        surfaceContainerHighest = Color(0xFFEEE7F4),
        onSurfaceVariant = Color(0xFF5B5165),
        outline = Color(0xFF786D83),
        outlineVariant = Color(0xFFE6DDED),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFFD2BFFF),
        onPrimary = Color(0xFF362167),
        primaryContainer = Color(0xFF493568),
        onPrimaryContainer = Color(0xFFECE0FF),
        secondary = Color(0xFFF6B9D4),
        onSecondary = Color(0xFF4E263D),
        secondaryContainer = Color(0xFF59344B),
        onSecondaryContainer = Color(0xFFFAD9EA),
        tertiary = Color(0xFFA4DFD0),
        onTertiary = Color(0xFF16382F),
        tertiaryContainer = Color(0xFF294C44),
        onTertiaryContainer = Color(0xFFC7F4E8),
        background = Color(0xFF1D1921),
        onBackground = Color(0xFFF1E8F4),
        surface = Color(0xFF27212D),
        onSurface = Color(0xFFF1E8F4),
        surfaceVariant = Color(0xFF3B3142),
        surfaceContainerLowest = Color(0xFF17141B),
        surfaceContainerLow = Color(0xFF221C27),
        surfaceContainer = Color(0xFF27212D),
        surfaceContainerHigh = Color(0xFF322938),
        surfaceContainerHighest = Color(0xFF3B3142),
        onSurfaceVariant = Color(0xFFCDBFD3),
        outline = Color(0xFFA596AE),
        outlineVariant = Color(0xFF493F50),
    )

@Composable
fun VocaTheme(preference: String, content: @Composable () -> Unit) {
    val font =
        FontFamily(
            Font(R.font.nunito, FontWeight.Normal),
            Font(R.font.nunito_medium, FontWeight.Medium),
            Font(R.font.nunito_semibold, FontWeight.SemiBold),
            Font(R.font.nunito_bold, FontWeight.Bold),
            Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
        )
    val headings = FontFamily(
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
    )
    val t = Typography()
    MaterialTheme(
        colorScheme =
            if (preference == "Gelap" || preference == "Sistem" && isSystemInDarkTheme()) Dark
            else Light,
        typography =
            t.copy(
                displayLarge = t.displayLarge.copy(fontFamily = headings, fontWeight = FontWeight.SemiBold),
                displayMedium = t.displayMedium.copy(fontFamily = headings, fontWeight = FontWeight.SemiBold),
                displaySmall = t.displaySmall.copy(fontFamily = headings, fontWeight = FontWeight.SemiBold),
                headlineLarge = t.headlineLarge.copy(fontFamily = headings, fontWeight = FontWeight.SemiBold),
                headlineMedium = t.headlineMedium.copy(fontFamily = headings, fontWeight = FontWeight.SemiBold),
                headlineSmall = t.headlineSmall.copy(fontFamily = headings, fontWeight = FontWeight.SemiBold),
                titleLarge = t.titleLarge.copy(fontFamily = font),
                titleMedium = t.titleMedium.copy(fontFamily = font),
                titleSmall = t.titleSmall.copy(fontFamily = font),
                bodyLarge = t.bodyLarge.copy(fontFamily = font),
                bodyMedium = t.bodyMedium.copy(fontFamily = font),
                bodySmall = t.bodySmall.copy(fontFamily = font),
                labelLarge = t.labelLarge.copy(fontFamily = font),
                labelMedium = t.labelMedium.copy(fontFamily = font),
                labelSmall = t.labelSmall.copy(fontFamily = font),
            ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(30.dp),
        ),
        content = content,
    )
}
