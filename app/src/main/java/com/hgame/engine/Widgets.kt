package com.hgame.engine

import android.graphics.Canvas
import kotlin.math.abs
import kotlin.math.min

/** A tappable rounded button drawn on the game canvas. */
class Btn(var label: String, var color: Int = Pal.ACCENT, var textColor: Int = Pal.WHITE) {
    var l = 0f; var t = 0f; var r = 0f; var b = 0f
    var enabled = true
    var pressed = false

    fun set(l: Float, t: Float, r: Float, b: Float): Btn { this.l = l; this.t = t; this.r = r; this.b = b; return this }
    fun setCenter(cx: Float, cy: Float, w: Float, h: Float) = set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
    fun hit(x: Float, y: Float) = enabled && x in l..r && y in t..b
    val cx get() = (l + r) / 2
    val cy get() = (t + b) / 2

    fun draw(c: Canvas, textSize: Float = (b - t) * 0.42f) {
        val col = if (enabled) color else Pal.SURFACE2
        val rad = (b - t) * 0.32f
        val dy = if (pressed) rad * 0.2f else 0f
        c.rrect(l, t + rad * 0.3f, r, b + rad * 0.3f, rad, Pal.shade(col, 0.55f))
        c.rrect(l, t + dy, r, b + dy, rad, col)
        c.text(label, cx, cy + dy, textSize, if (enabled) textColor else Pal.MUTED)
    }
}

/** Fits a cols x rows board of square cells into a rectangle, centred. */
class BoardFit {
    var cell = 0f; var x0 = 0f; var y0 = 0f; var cols = 1; var rows = 1

    fun fit(cols: Int, rows: Int, l: Float, t: Float, r: Float, b: Float): BoardFit {
        this.cols = cols; this.rows = rows
        cell = min((r - l) / cols, (b - t) / rows)
        x0 = l + ((r - l) - cell * cols) / 2
        y0 = t + ((b - t) - cell * rows) / 2
        return this
    }

    val right get() = x0 + cell * cols
    val bottom get() = y0 + cell * rows
    fun cx(col: Int) = x0 + (col + 0.5f) * cell
    fun cy(row: Int) = y0 + (row + 0.5f) * cell
    fun colAt(x: Float): Int = if (x < x0) -1 else ((x - x0) / cell).toInt().let { if (it >= cols) -1 else it }
    fun rowAt(y: Float): Int = if (y < y0) -1 else ((y - y0) / cell).toInt().let { if (it >= rows) -1 else it }
}

/** Detects swipe direction from down/up positions: 0=up,1=right,2=down,3=left, -1=tap. */
class Swipe(private val minDist: Float) {
    private var sx = 0f; private var sy = 0f
    var active = false
    fun down(x: Float, y: Float) { sx = x; sy = y; active = true }

    /** Returns a direction as soon as the finger moved far enough (for responsive controls). */
    fun move(x: Float, y: Float): Int {
        if (!active) return -1
        val d = dir(x, y)
        if (d >= 0) { sx = x; sy = y }
        return d
    }

    fun up(x: Float, y: Float): Int { val d = dir(x, y); active = false; return d }

    private fun dir(x: Float, y: Float): Int {
        val dx = x - sx; val dy = y - sy
        if (abs(dx) < minDist && abs(dy) < minDist) return -1
        return if (abs(dx) > abs(dy)) (if (dx > 0) 1 else 3) else (if (dy > 0) 2 else 0)
    }
}

/** On-screen QWERTY keyboard for word games. Keys report "A".."Z", "ENTER" or "DEL". */
class Keyboard {
    private val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "<ZXCVBNM>")
    private class Key(val code: String, var l: Float, var t: Float, var r: Float, var b: Float)
    private val keys = ArrayList<Key>()
    var showEnter = true
    /** Optional per-key colour (e.g. Wordle hints, used letters). */
    var colorOf: (String) -> Int? = { null }
    private var pressedKey: String? = null

    fun layout(l: Float, t: Float, r: Float, b: Float) {
        keys.clear()
        val gap = (r - l) * 0.012f
        val kh = (b - t - gap * 2) / 3
        val kw = (r - l - gap * 9) / 10
        rows.forEachIndexed { ri, row ->
            val y = t + ri * (kh + gap)
            if (ri < 2) {
                val total = row.length * kw + (row.length - 1) * gap
                var x = l + (r - l - total) / 2
                for (ch in row) { keys.add(Key(ch.toString(), x, y, x + kw, y + kh)); x += kw + gap }
            } else {
                val wide = kw * 1.5f + gap * 0.5f
                var x = l
                for (ch in row) {
                    val code = when (ch) { '<' -> "ENTER"; '>' -> "DEL"; else -> ch.toString() }
                    if (code == "ENTER" && !showEnter) { x += wide + gap; continue }
                    val ww = if (code.length > 1) wide else kw
                    keys.add(Key(code, x, y, x + ww, y + kh)); x += ww + gap
                }
            }
        }
    }

    fun draw(c: Canvas) {
        for (k in keys) {
            val col = colorOf(k.code) ?: Pal.SURFACE2
            val dy = if (pressedKey == k.code) 2f else 0f
            val rad = (k.b - k.t) * 0.16f
            c.rrect(k.l, k.t + 3f, k.r, k.b + 3f, rad, Pal.shade(col, 0.55f))
            c.rrect(k.l, k.t + dy, k.r, k.b + dy, rad, col)
            val label = when (k.code) { "ENTER" -> "OK"; "DEL" -> "⌫"; else -> k.code }
            c.text(label, (k.l + k.r) / 2, (k.t + k.b) / 2 + dy, (k.b - k.t) * 0.4f, Pal.TEXT)
        }
    }

    fun down(x: Float, y: Float): String? { pressedKey = keyAt(x, y); return pressedKey }
    fun up(x: Float, y: Float): String? {
        val k = keyAt(x, y)
        val res = if (k != null && k == pressedKey) k else null
        pressedKey = null
        return res
    }

    private fun keyAt(x: Float, y: Float) = keys.firstOrNull { x >= it.l && x <= it.r && y >= it.t && y <= it.b }?.code
}
