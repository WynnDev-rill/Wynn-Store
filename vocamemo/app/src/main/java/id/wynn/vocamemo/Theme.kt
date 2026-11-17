package id.wynn.vocamemo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val Light =
    lightColorScheme(
        primary = Color(0xFF245CCA),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDEE9FF),
        onPrimaryContainer = Color(0xFF102F68),
        secondary = Color(0xFF806000),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE8A6),
        onSecondaryContainer = Color(0xFF513A00),
        tertiary = Color(0xFF006C58),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFC2F0DE),
        onTertiaryContainer = Color(0xFF004D3D),
        background = Color(0xFFF4F7FD),
        onBackground = Color(0xFF172842),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF172842),
        surfaceVariant = Color(0xFFE7EDF8),
        onSurfaceVariant = Color(0xFF475770),
        outline = Color(0xFF718099),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFFAFC9FF),
        onPrimary = Color(0xFF002D70),
        primaryContainer = Color(0xFF183E7B),
        onPrimaryContainer = Color(0xFFDEE9FF),
        secondary = Color(0xFFEAC45F),
        secondaryContainer = Color(0xFF594300),
        onSecondaryContainer = Color(0xFFFFE8A6),
        tertiary = Color(0xFF83D7B7),
        tertiaryContainer = Color(0xFF005340),
        onTertiaryContainer = Color(0xFFC2F0DE),
        background = Color(0xFF101B2D),
        onBackground = Color(0xFFE5EDFF),
        surface = Color(0xFF18253A),
        onSurface = Color(0xFFE5EDFF),
        surfaceVariant = Color(0xFF28364D),
        onSurfaceVariant = Color(0xFFC1CDE2),
        outline = Color(0xFF91A1BC),
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
    val t = Typography()
    MaterialTheme(
        colorScheme =
            if (preference == "Gelap" || preference == "Sistem" && isSystemInDarkTheme()) Dark
            else Light,
        typography =
            t.copy(
                displayLarge = t.displayLarge.copy(fontFamily = font),
                displayMedium = t.displayMedium.copy(fontFamily = font),
                displaySmall = t.displaySmall.copy(fontFamily = font),
                headlineLarge = t.headlineLarge.copy(fontFamily = font),
                headlineMedium = t.headlineMedium.copy(fontFamily = font),
                headlineSmall = t.headlineSmall.copy(fontFamily = font),
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
        content = content,
    )
}
