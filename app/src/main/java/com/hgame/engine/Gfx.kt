package com.hgame.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/** Shared paints and drawing helpers. All drawing happens on the UI thread. */
object Gfx {
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val rect = RectF()
    val path = Path()
    val bold: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val regular: Typeface = Typeface.DEFAULT
    val mono: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    fun fillPaint(color: Int): Paint = fill.apply { shader = null; this.color = color }
}

fun Canvas.rect(l: Float, t: Float, r: Float, b: Float, color: Int) {
    drawRect(l, t, r, b, Gfx.fillPaint(color))
}

fun Canvas.rrect(l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int) {
    Gfx.rect.set(l, t, r, b)
    drawRoundRect(Gfx.rect, rad, rad, Gfx.fillPaint(color))
}

fun Canvas.rrectStroke(l: Float, t: Float, r: Float, b: Float, rad: Float, width: Float, color: Int) {
    Gfx.rect.set(l, t, r, b)
    val p = Gfx.stroke.apply { shader = null; strokeWidth = width; this.color = color }
    drawRoundRect(Gfx.rect, rad, rad, p)
}

/** Rounded rect with a vertical gradient from [top] to [bottom]. */
fun Canvas.gradRect(l: Float, t: Float, r: Float, b: Float, rad: Float, top: Int, bottom: Int) {
    Gfx.rect.set(l, t, r, b)
    val p = Gfx.fill
    p.color = Color.WHITE
    p.shader = LinearGradient(0f, t, 0f, b, top, bottom, Shader.TileMode.CLAMP)
    drawRoundRect(Gfx.rect, rad, rad, p)
    p.shader = null
}

/** Soft drop shadow followed by a filled rounded rect: a raised "tile". */
fun Canvas.tile(l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int, lift: Float = rad * 0.35f) {
    rrect(l, t + lift, r, b + lift, rad, Pal.shade(color, 0.55f))
    rrect(l, t, r, b, rad, color)
}

fun Canvas.circle(x: Float, y: Float, r: Float, color: Int) {
    drawCircle(x, y, r, Gfx.fillPaint(color))
}

/** Circle shaded with a radial highlight, looks like a ball. */
fun Canvas.ball(x: Float, y: Float, r: Float, color: Int) {
    val p = Gfx.fill
    p.color = Color.WHITE
    p.shader = RadialGradient(x - r * 0.35f, y - r * 0.4f, r * 1.4f,
        intArrayOf(Pal.tint(color, 0.55f), color, Pal.shade(color, 0.6f)), floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP)
    drawCircle(x, y, r, p)
    p.shader = null
}

fun Canvas.ring(x: Float, y: Float, r: Float, width: Float, color: Int) {
    drawCircle(x, y, r, Gfx.stroke.apply { shader = null; strokeWidth = width; this.color = color })
}

fun Canvas.line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
    drawLine(x1, y1, x2, y2, Gfx.stroke.apply { shader = null; strokeWidth = width; this.color = color })
}

/** Draws [s] centred on ([x],[y]) (vertically centred too, unless [align] is not CENTER). */
fun Canvas.text(
    s: String, x: Float, y: Float, size: Float, color: Int,
    align: Paint.Align = Paint.Align.CENTER, bold: Boolean = true,
) {
    val p = Gfx.text
    p.textSize = size
    p.color = color
    p.textAlign = align
    p.typeface = if (bold) Gfx.bold else Gfx.regular
    val fm = p.fontMetrics
    drawText(s, x, y - (fm.ascent + fm.descent) / 2f, p)
}

fun textWidth(s: String, size: Float, bold: Boolean = true): Float {
    val p = Gfx.text
    p.textSize = size
    p.typeface = if (bold) Gfx.bold else Gfx.regular
    return p.measureText(s)
}

/** Polygon from flat x,y pairs. */
fun Canvas.poly(color: Int, vararg pts: Float) {
    val p = Gfx.path
    p.reset()
    p.moveTo(pts[0], pts[1])
    var i = 2
    while (i < pts.size) { p.lineTo(pts[i], pts[i + 1]); i += 2 }
    p.close()
    drawPath(p, Gfx.fillPaint(color))
}

/** Full-view vertical background gradient. */
fun Canvas.background(w: Float, h: Float, top: Int = Pal.BG_TOP, bottom: Int = Pal.BG_BOTTOM) {
    val p = Gfx.fill
    p.color = Color.WHITE
    p.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
    drawRect(0f, 0f, w, h, p)
    p.shader = null
}
