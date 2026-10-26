package com.hgame.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.hgame.engine.Btn
import com.hgame.engine.Pal
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min

/** Modal panel over the game: result screen or pause screen. */
@SuppressLint("ViewConstructor")
class Overlay(context: Context, private val accent: Int) : View(context) {
    private val d = resources.displayMetrics.density
    private var emoji = ""
    private var title = ""
    private var lines: List<String> = emptyList()
    private val primary = Btn("", accent)
    private val secondary = Btn("", Pal.SURFACE2)
    private var onPrimary: () -> Unit = {}
    private var onSecondary: () -> Unit = {}
    private var appear = 0f

    init { visibility = GONE }

    fun show(emoji: String, title: String, lines: List<String>, primary: String, secondary: String,
             onPrimary: () -> Unit, onSecondary: () -> Unit) {
        this.emoji = emoji; this.title = title; this.lines = lines
        this.primary.label = primary; this.secondary.label = secondary
        this.onPrimary = onPrimary; this.onSecondary = onSecondary
        appear = 0f
        visibility = VISIBLE
        invalidate()
    }

    fun hide() { visibility = GONE }
    val showing get() = visibility == VISIBLE

    override fun onDraw(c: Canvas) {
        if (appear < 1f) { appear = min(1f, appear + 0.12f); postInvalidateOnAnimation() }
        val e = 1 - (1 - appear) * (1 - appear)
        c.drawColor(Pal.alpha(0xFF05060F.toInt(), 0.72f * e))
        val cw = min(width - 40 * d, 340 * d)
        val ch = (210 + lines.size * 24) * d
        val l = (width - cw) / 2
        val t = (height - ch) / 2 + (1 - e) * 40 * d
        c.rrect(l, t + 6 * d, l + cw, t + ch + 6 * d, 24 * d, Pal.alpha(Pal.BLACK, 0.4f * e))
        c.rrect(l, t, l + cw, t + ch, 24 * d, Pal.alpha(Pal.SURFACE, e))
        c.text(emoji, width / 2f, t + 42 * d, 40 * d, Pal.WHITE)
        c.text(title, width / 2f, t + 92 * d, 24 * d, Pal.alpha(Pal.TEXT, e))
        lines.forEachIndexed { i, s ->
            c.text(s, width / 2f, t + (122 + i * 24) * d, 15 * d, Pal.alpha(Pal.MUTED, e), bold = i == 0 && lines.size > 1)
        }
        val by = t + ch - 46 * d
        val bw = (cw - 52 * d) / 2
        secondary.set(l + 20 * d, by - 22 * d, l + 20 * d + bw, by + 22 * d)
        primary.set(l + cw - 20 * d - bw, by - 22 * d, l + cw - 20 * d, by + 22 * d)
        secondary.draw(c, 15 * d)
        primary.draw(c, 15 * d)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                primary.pressed = primary.hit(e.x, e.y); secondary.pressed = secondary.hit(e.x, e.y); invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val p = primary.pressed && primary.hit(e.x, e.y)
                val s = secondary.pressed && secondary.hit(e.x, e.y)
                primary.pressed = false; secondary.pressed = false; invalidate()
                if (p) onPrimary() else if (s) onSecondary()
            }
        }
        return true
    }
}
