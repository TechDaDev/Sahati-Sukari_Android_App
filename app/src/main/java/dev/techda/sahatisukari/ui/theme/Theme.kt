package dev.techda.sahatisukari.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.techda.sahatisukari.R

private val SahatiColors = lightColorScheme(
    primary = Color(0xFF087E8B), onPrimary = Color.White,
    secondary = Color(0xFF168AAD), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF4F8), onSecondaryContainer = Color(0xFF087E8B),
    background = Color(0xFFF4F9FC), onBackground = Color(0xFF16324F),
    surface = Color.White, onSurface = Color(0xFF16324F),
    surfaceVariant = Color(0xFFEAF4F8), outline = Color(0xFFD5E7EF),
    error = Color(0xFFB42318)
)

private val Cairo = FontFamily(Font(R.font.cairo))
private val DefaultTypography = Typography()
private val CairoTypography = DefaultTypography.copy(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = Cairo),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = Cairo),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = Cairo),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = Cairo),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = Cairo),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = Cairo),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = Cairo),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = Cairo),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = Cairo),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = Cairo),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = Cairo),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = Cairo),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = Cairo),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = Cairo),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = Cairo),
)

@Composable
fun SahatiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SahatiColors, typography = CairoTypography, content = content)
}
