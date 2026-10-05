package com.adityakulkarni.haircare

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surfaceLip: Color, // darker bottom edge that gives cards their chunky 3D look
    val muted: Color, // empty tracks, idle cells
    val text: Color,
    val textMuted: Color,
    val amber: Color,
    val amberLip: Color,
    val green: Color,
    val greenLip: Color,
    val greenTint: Color,
    val morningTint: Color,
    val scalpTint: Color,
    val scalp: Color,
)

private val Light = Palette(
    bg = Color(0xFFFFFBEB), surface = Color.White, surfaceLip = Color(0xFFF1E3CF), muted = Color(0xFFF5EBDD),
    text = Color(0xFF1C1917), textMuted = Color(0xFF6B5E52),
    amber = Color(0xFFF59E0B), amberLip = Color(0xFFD97706),
    green = Color(0xFF22C55E), greenLip = Color(0xFF15803D), greenTint = Color(0xFFECFDF3),
    morningTint = Color(0xFFFEF3C7), scalpTint = Color(0xFFCCFBF1), scalp = Color(0xFF0D9488),
)

private val Dark = Palette(
    bg = Color(0xFF15110D), surface = Color(0xFF241D16), surfaceLip = Color(0xFF0E0B08), muted = Color(0xFF332920),
    text = Color(0xFFFFF7ED), textMuted = Color(0xFFC9B8A5),
    amber = Color(0xFFFBBF24), amberLip = Color(0xFFB45309),
    green = Color(0xFF4ADE80), greenLip = Color(0xFF166534), greenTint = Color(0xFF16291D),
    morningTint = Color(0xFF3A2C12), scalpTint = Color(0xFF113330), scalp = Color(0xFF2DD4BF),
)

val LocalPalette = staticCompositionLocalOf { Light }

/** Hero/widget colour per urgency: (top of gradient, bottom of gradient). White text stays readable on all. */
fun heroColors(u: Urgency): Pair<Color, Color> = when (u) {
    Urgency.DONE -> Color(0xFF16A34A) to Color(0xFF15803D)
    Urgency.CALM -> Color(0xFFF97316) to Color(0xFFEA580C)
    Urgency.WARN -> Color(0xFFEA580C) to Color(0xFFC2410C)
    Urgency.PANIC -> Color(0xFFDC2626) to Color(0xFF991B1B)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Nunito = FontFamily(nunito(400), nunito(600), nunito(700), nunito(800), nunito(900))

@Composable
fun HairCareTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) Dark else Light
    val base = TextStyle(fontFamily = Nunito)
    val type = Typography(
        displayLarge = base.copy(fontSize = 56.sp, fontWeight = FontWeight.Black, lineHeight = 60.sp),
        headlineMedium = base.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
        titleMedium = base.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
        titleSmall = base.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
        bodyMedium = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
        bodySmall = base.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        labelLarge = base.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
        labelSmall = base.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
    )
    val scheme = if (dark) darkColorScheme(background = p.bg, surface = p.surface, primary = p.amber)
    else lightColorScheme(background = p.bg, surface = p.surface, primary = p.amberLip)
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
