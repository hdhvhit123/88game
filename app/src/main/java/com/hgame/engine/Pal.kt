package com.hgame.engine

import android.graphics.Color

/** App colour palette and colour math. */
object Pal {
    const val BG = 0xFF0F1222.toInt()
    const val BG_TOP = 0xFF161A33.toInt()
    const val BG_BOTTOM = 0xFF0B0D1A.toInt()
    const val SURFACE = 0xFF1A1F38.toInt()
    const val SURFACE2 = 0xFF252B4D.toInt()
    const val LINE = 0xFF343B66.toInt()
    const val TEXT = 0xFFF1F3FF.toInt()
    const val MUTED = 0xFF9AA3C7.toInt()
    const val ACCENT = 0xFF7C5CFF.toInt()
    const val CYAN = 0xFF00C2FF.toInt()
    const val PINK = 0xFFFF5C8A.toInt()
    const val RED = 0xFFFF4D5E.toInt()
    const val ORANGE = 0xFFFF9F43.toInt()
    const val YELLOW = 0xFFFFC93C.toInt()
    const val GREEN = 0xFF3DDC97.toInt()
    const val TEAL = 0xFF1DD3B0.toInt()
    const val BLUE = 0xFF4D7CFF.toInt()
    const val PURPLE = 0xFFB15CFF.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val BLACK = 0xFF000000.toInt()
    const val WOOD = 0xFFE3B778.toInt()
    const val WOOD_DARK = 0xFF9C6B3A.toInt()
    const val FELT = 0xFF17694A.toInt()

    /** Bright distinct colours for pieces, balls, liquids, ... */
    val VIVID = intArrayOf(
        RED, BLUE, GREEN, YELLOW, PURPLE, ORANGE, CYAN, PINK,
        0xFF8BC34A.toInt(), 0xFF795548.toInt(), 0xFF9E9E9E.toInt(), 0xFF3F51B5.toInt(),
    )

    fun alpha(color: Int, a: Float): Int = (color and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)

    fun shade(color: Int, f: Float): Int = Color.argb(
        Color.alpha(color), (Color.red(color) * f).toInt().coerceIn(0, 255),
        (Color.green(color) * f).toInt().coerceIn(0, 255), (Color.blue(color) * f).toInt().coerceIn(0, 255),
    )

    fun tint(color: Int, f: Float): Int = Color.argb(
        Color.alpha(color), (Color.red(color) + (255 - Color.red(color)) * f).toInt(),
        (Color.green(color) + (255 - Color.green(color)) * f).toInt(),
        (Color.blue(color) + (255 - Color.blue(color)) * f).toInt(),
    )

    fun mix(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * u).toInt(),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt(),
        )
    }

    fun hsv(h: Float, s: Float, v: Float): Int = Color.HSVToColor(floatArrayOf(((h % 360) + 360) % 360, s, v))
}
