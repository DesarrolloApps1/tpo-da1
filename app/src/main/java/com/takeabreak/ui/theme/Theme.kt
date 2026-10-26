package com.takeabreak.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

// TODO: Bricolage Grotesque para títulos y DM Sans para texto (res/font)
private val typography = Typography()

@Composable
fun TakeABreakTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, shapes = shapes, typography = typography, content = content)
}
