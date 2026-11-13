package com.samlytics.lastmark.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Brand {
    val Cream = Color(0xFFFEFFF8)
    val Blue = Color(0xFF4392F1)
    val Navy = Color(0xFF00212B)
    val Lime = Color(0xFFDCFF38)
    val LimeDark = Color(0xFFC2E812)
    val BlueTint = Color(0xFFEAF3FE)
    val Muted = Color(0xFF5B6B73)
    val Line = Color(0xFFE3E8EE)
}

// Swap to Montserrat once the TTF files are in res/font (see README):
// val Montserrat = FontFamily(
//     Font(R.font.montserrat_regular, FontWeight.Normal),
//     Font(R.font.montserrat_medium, FontWeight.Medium),
//     Font(R.font.montserrat_bold, FontWeight.Bold)
// )
val Montserrat = FontFamily.SansSerif

private val LastmarkColours = lightColorScheme(
    primary = Brand.Blue,
    onPrimary = Brand.Cream,
    secondary = Brand.Lime,
    onSecondary = Brand.Navy,
    background = Brand.Cream,
    onBackground = Brand.Navy,
    surface = Color.White,
    onSurface = Brand.Navy,
    outline = Brand.Line
)

private val LastmarkType = Typography(
    headlineSmall = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = Montserrat, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)

@Composable
fun LastmarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LastmarkColours,
        typography = LastmarkType,
        content = content
    )
}
