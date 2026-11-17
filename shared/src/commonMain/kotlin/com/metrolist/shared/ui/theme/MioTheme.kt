/**
 * MiMetrolist: colores, formas y tipografía del estilo personal.
 */

package com.metrolist.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.metrolist.shared.settings.MioAccent
import com.metrolist.shared.settings.Settings

private val MioBackground = Color(0xFF121212)
private val MioOledBackground = Color(0xFF000000)

/** Color de acento elegido; CANCION usa el color de la carátula si existe. */
fun MioAccent.resolve(songColor: Color?): Color {
    if (this != MioAccent.CANCION || songColor == null) return Color(argb)
    // Aclara colores muy oscuros para que el acento se lea sobre el fondo negro.
    return if (songColor.luminance() < 0.25f) lerp(songColor, Color.White, 0.45f) else songColor
}

fun mioColorScheme(
    accent: Color,
    oled: Boolean,
): ColorScheme {
    val bg = if (oled) MioOledBackground else MioBackground
    val onAccent = if (accent.luminance() > 0.4f) Color.Black else Color.White
    return darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = lerp(bg, accent, 0.32f),
        onPrimaryContainer = Color.White,
        inversePrimary = lerp(accent, Color.Black, 0.35f),
        secondary = Color(0xFFEDEDED),
        onSecondary = Color.Black,
        secondaryContainer = if (oled) Color(0xFF1F1F1F) else Color(0xFF2A2A2A),
        onSecondaryContainer = Color.White,
        tertiary = lerp(accent, Color.White, 0.3f),
        onTertiary = Color.Black,
        tertiaryContainer = lerp(bg, accent, 0.22f),
        onTertiaryContainer = Color.White,
        background = bg,
        onBackground = Color.White,
        surface = bg,
        onSurface = Color.White,
        surfaceVariant = if (oled) Color(0xFF1A1A1A) else Color(0xFF2A2A2A),
        onSurfaceVariant = Color(0xFFB3B3B3),
        surfaceTint = bg,
        inverseSurface = Color(0xFFF2F2F2),
        inverseOnSurface = Color(0xFF121212),
        error = Color(0xFFFF6B6B),
        onError = Color.Black,
        errorContainer = Color(0xFF5C1F24),
        onErrorContainer = Color(0xFFFFDADA),
        outline = Color(0xFF6A6A6A),
        outlineVariant = if (oled) Color(0xFF262626) else Color(0xFF3A3A3A),
        scrim = Color.Black,
        surfaceBright = if (oled) Color(0xFF1F1F1F) else Color(0xFF2E2E2E),
        surfaceDim = bg,
        surfaceContainerLowest = if (oled) Color.Black else Color(0xFF0D0D0D),
        surfaceContainerLow = if (oled) Color(0xFF0A0A0A) else Color(0xFF181818),
        surfaceContainer = if (oled) Color(0xFF111111) else Color(0xFF1E1E1E),
        surfaceContainerHigh = if (oled) Color(0xFF181818) else Color(0xFF262626),
        surfaceContainerHighest = if (oled) Color(0xFF202020) else Color(0xFF2F2F2F),
    )
}

/** Escala de esquinas activa (1 cuando el estilo personal está apagado). */
fun mioCornerScale(): Float = if (Settings.shared.mioStyle) Settings.shared.mioCorners.scale else 1f

/** Aplica la escala de esquinas del estilo personal a un radio. */
fun mioCorner(radius: Dp): Dp = radius * mioCornerScale()

fun mioShapes(): Shapes {
    val scale = mioCornerScale()
    return Shapes(
        extraSmall = RoundedCornerShape(16.dp * scale),
        small = RoundedCornerShape(20.dp * scale),
        medium = RoundedCornerShape(24.dp * scale),
        large = RoundedCornerShape(28.dp * scale),
        extraLarge = RoundedCornerShape(32.dp * scale),
    )
}

private fun TextStyle.mioHeavy(weight: FontWeight): TextStyle = copy(fontWeight = weight, letterSpacing = (-0.02).em)

/** Títulos más gruesos y compactos, como en las apps de streaming modernas. */
fun Typography.mioBold(): Typography =
    copy(
        displayLarge = displayLarge.mioHeavy(FontWeight.Black),
        displayMedium = displayMedium.mioHeavy(FontWeight.Black),
        displaySmall = displaySmall.mioHeavy(FontWeight.ExtraBold),
        headlineLarge = headlineLarge.mioHeavy(FontWeight.ExtraBold),
        headlineMedium = headlineMedium.mioHeavy(FontWeight.ExtraBold),
        headlineSmall = headlineSmall.mioHeavy(FontWeight.Bold),
        titleLarge = titleLarge.mioHeavy(FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold),
    )
