package id.wynn.roadtoimmortal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Light =
    lightColorScheme(
        primary = Color(0xFF4E51D8),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5E4FF),
        onPrimaryContainer = Color(0xFF292A83),
        secondary = Color(0xFF916615),
        secondaryContainer = Color(0xFFFFEAC3),
        onSecondaryContainer = Color(0xFF5F420B),
        tertiary = Color(0xFF147665),
        background = Color(0xFFF8F8FC),
        onBackground = Color(0xFF1A1B2E),
        surface = Color(0xFFF8F8FC),
        onSurface = Color(0xFF1A1B2E),
        onSurfaceVariant = Color(0xFF606276),
        surfaceContainer = Color(0xFFF0F0F8),
        surfaceContainerLow = Color.White,
        surfaceContainerHigh = Color(0xFFECECF5),
        outline = Color(0xFF77798B),
        outlineVariant = Color(0xFFDEDFEB),
        error = Color(0xFFB43344),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFFB9B7FF),
        onPrimary = Color(0xFF24256E),
        primaryContainer = Color(0xFF333479),
        onPrimaryContainer = Color(0xFFE6E4FF),
        secondary = Color(0xFFE7BD70),
        secondaryContainer = Color(0xFF4A371A),
        onSecondaryContainer = Color(0xFFFFEAC3),
        tertiary = Color(0xFF7CDBC3),
        background = Color(0xFF11121C),
        onBackground = Color(0xFFE9E9F5),
        surface = Color(0xFF11121C),
        onSurface = Color(0xFFE9E9F5),
        onSurfaceVariant = Color(0xFFB0B1C6),
        surfaceContainer = Color(0xFF1B1C2A),
        surfaceContainerLow = Color(0xFF1D1E2C),
        surfaceContainerHigh = Color(0xFF292A3A),
        outline = Color(0xFF85869C),
        outlineVariant = Color(0xFF3C3D50),
        error = Color(0xFFFFB1BE),
    )
private val Type =
    Typography(
        displayLarge =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
                lineHeight = 70.sp,
                letterSpacing = (-3).sp,
                fontFeatureSettings = "tnum",
            ),
        headlineLarge =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-1).sp,
            ),
        headlineMedium =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = (-.6).sp,
            ),
        titleLarge =
            TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
        titleMedium =
            TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge =
            TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )

@Composable
fun ImmortalTheme(mode: String, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            if (mode == "dark" || mode == "system" && isSystemInDarkTheme()) Dark else Light,
        typography = Type,
        content = content,
    )
}
