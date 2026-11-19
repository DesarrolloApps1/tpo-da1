package com.takeabreak.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.takeabreak.R

// Tokens del sistema de diseño (docs/figma/build.py)
object Palette {
    val Forest = Color(0xFF0B3B2C)
    val Green = Color(0xFF136B4A)
    val Lime = Color(0xFFE4F56A)
    val Cream = Color(0xFFF7F3EC)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF16211C)
    val Muted = Color(0xFF5F6B64)
    val Line = Color(0xFFE7E1D6)
    val Red = Color(0xFFC2412D)
    val Amber = Color(0xFFC98A12)
    val AmberBg = Color(0xFFFFF3D6)
    val SandInk = Color(0xFF6B4A0E)
    val Pink = Color(0xFFFFD3F2)
    val PinkInk = Color(0xFF6A1B54)
}

private val colors = lightColorScheme(
    primary = Palette.Green,
    onPrimary = Palette.Paper,
    primaryContainer = Palette.Forest,
    onPrimaryContainer = Palette.Lime,
    // Lima solo como fondo de acento (indicador de pestaña, chips), nunca como texto sobre claro
    secondary = Palette.Forest,
    onSecondary = Palette.Lime,
    secondaryContainer = Palette.Lime,
    onSecondaryContainer = Palette.Forest,
    background = Palette.Cream,
    surfaceContainer = Palette.Paper,
    onBackground = Palette.Ink,
    surface = Palette.Paper,
    onSurface = Palette.Ink,
    onSurfaceVariant = Palette.Muted,
    outline = Palette.Line,
    error = Palette.Red,
)

// Píldoras y sin sombras: la jerarquía sale del color y el tamaño
val PillShape = RoundedCornerShape(percent = 50)

private val shapes = Shapes(
    small = PillShape,
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

// Fuentes variables (un archivo con todos los pesos): cada peso fija su eje "wght"
private fun variableFamily(resId: Int, vararg weights: FontWeight) = FontFamily(
    weights.map { weight -> Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))) },
)

// Bricolage Grotesque para títulos y DM Sans para texto (licencias en third_party/fonts)
private val Bricolage = variableFamily(R.font.bricolage_grotesque, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
private val DmSans = variableFamily(R.font.dm_sans, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private val typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        displayMedium = displayMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        displaySmall = displaySmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = DmSans, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = DmSans, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = DmSans),
        bodyMedium = bodyMedium.copy(fontFamily = DmSans),
        bodySmall = bodySmall.copy(fontFamily = DmSans),
        labelLarge = labelLarge.copy(fontFamily = DmSans, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = DmSans, fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(fontFamily = DmSans, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun TakeABreakTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, shapes = shapes, typography = typography, content = content)
}
