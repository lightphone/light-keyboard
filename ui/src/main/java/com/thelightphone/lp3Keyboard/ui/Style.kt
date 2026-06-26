package com.thelightphone.lp3Keyboard.ui

import android.content.Context
import android.graphics.fonts.SystemFonts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Resolves the Akkurat font family at runtime — the .ttf/.otf files are
 * license-restricted, so we can't ship them in this library's resources.
 * Lookup order:
 *   1. System fonts on the host device (LP3 hardware ships with Akkurat).
 *   2. A res/font copy in the consumer's app if they have one locally
 *      (resolved via getIdentifier so a missing copy is a runtime miss,
 *      not a compile error).
 *   3. FontFamily.Default.
 */
fun lightFontFamily(context: Context): FontFamily {
    systemAkkuratFonts()?.let { return it }
    bundledAkkuratFonts(context)?.let { return it }
    return FontFamily.Default
}

private fun systemAkkuratFonts(): FontFamily? {
    val fonts = SystemFonts.getAvailableFonts()
        .filter { it.file?.name?.startsWith("Akkurat", ignoreCase = true) == true }
        .mapNotNull { font ->
            val file = font.file ?: return@mapNotNull null
            val weight = FontWeight(font.style.weight)
            val style = if (font.style.slant != 0) FontStyle.Italic else FontStyle.Normal
            Font(file = file, weight = weight, style = style)
        }
    return if (fonts.isNotEmpty()) FontFamily(fonts) else null
}

private fun bundledAkkuratFonts(context: Context): FontFamily? {
    val res = context.resources
    val pkg = context.packageName
    fun fontId(name: String): Int = res.getIdentifier(name, "font", pkg)

    val fonts = buildList {
        fontId("akkuratll_light").takeIf { it != 0 }
            ?.let { add(Font(it, FontWeight.Light)) }
        fontId("akkuratll_regular").takeIf { it != 0 }
            ?.let { add(Font(it, FontWeight.Normal)) }
        fontId("akkuratpro_bold").takeIf { it != 0 }
            ?.let { add(Font(it, FontWeight.Bold)) }
    }
    return if (fonts.isNotEmpty()) FontFamily(fonts) else null
}

@Immutable
data class Lp3KeyboardColors(
    val background: Color,
    val foreground: Color,
)

val DarkKeyboardColors = Lp3KeyboardColors(
    background = Color.Black,
    foreground = Color.White,
)

val LightKeyboardColors = Lp3KeyboardColors(
    background = Color.White,
    foreground = Color.Black,
)

val LocalKeyboardColors = staticCompositionLocalOf { DarkKeyboardColors }

/**
 * Provided by [Lp3Keyboard] after one runtime lookup; key composables read
 * from it instead of calling [lightFontFamily] themselves so the system-font
 * scan only happens once per keyboard, not once per key.
 */
internal val LocalAkkuratFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

@Composable
fun Lp3KeyboardTheme(
    colors: Lp3KeyboardColors = DarkKeyboardColors,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalKeyboardColors provides colors) {
        content()
    }
}
