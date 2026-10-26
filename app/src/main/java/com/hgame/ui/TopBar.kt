package com.hgame.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.text
import com.hgame.engine.textWidth

/** Game screen header: back, title, score line and action icons, all drawn by hand. */
@SuppressLint("ViewConstructor")
class TopBar(context: Context, private val title: String, private val accent: Int, hasOptions: Boolean) : View(context) {
    enum class Action { BACK, RESTART, OPTIONS, HELP }

    var onAction: (Action) -> Unit = {}
    var score: String = ""
        set(v) { field = v; invalidate() }
    var best: String = ""
        set(v) { field = v; invalidate() }
    var info: String = ""
        set(v) { field = v; invalidate() }

    private val d = resources.displayMetrics.density
    private val actions = buildList {
        add(Action.HELP)
        if (hasOptions) add(Action.OPTIONS)
        add(Action.RESTART)
    }
    private var pressed: Action? = null
    private val path = Path()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (64 * d).toInt())
    }

    private fun iconX(i: Int) = width - (28 + i * 44) * d
    private val cy get() = height / 2f

    override fun onDraw(c: Canvas) {
        c.drawColor(Pal.BG)
        // back
        drawIconBg(c, 28 * d, Action.BACK)
        c.line(32 * d, cy - 8 * d, 24 * d, cy, 2.6f * d, Pal.TEXT)
        c.line(24 * d, cy, 32 * d, cy + 8 * d, 2.6f * d, Pal.TEXT)

        val left = 56 * d
        val right = iconX(actions.size - 1) - 26 * d
        var t = title
        val ts = 18 * d
        while (textWidth(t, ts) > right - left && t.length > 3) t = t.dropLast(2) + "…"
        val hasSub = score.isNotEmpty() || best.isNotEmpty() || info.isNotEmpty()
        c.text(t, left, if (hasSub) cy - 9 * d else cy, ts, Pal.TEXT, Paint.Align.LEFT)
        if (hasSub) {
            var x = left
            val sy = cy + 12 * d
            val ss = 12.5f * d
            if (score.isNotEmpty()) {
                c.text(score, x, sy, ss, accent, Paint.Align.LEFT); x += textWidth(score, ss) + 12 * d
            }
            if (best.isNotEmpty()) {
                val b = "★ $best"
                c.text(b, x, sy, ss, Pal.YELLOW, Paint.Align.LEFT); x += textWidth(b, ss) + 12 * d
            }
            if (info.isNotEmpty()) {
                var s = info
                while (textWidth(s, ss, false) > right - x && s.length > 3) s = s.dropLast(2) + "…"
                c.text(s, x, sy, ss, Pal.MUTED, Paint.Align.LEFT, bold = false)
            }
        }
        actions.forEachIndexed { i, a ->
            val x = iconX(i)
            drawIconBg(c, x, a)
            when (a) {
                Action.RESTART -> drawRestart(c, x)
                Action.HELP -> c.text("?", x, cy, 18 * d, Pal.TEXT)
                Action.OPTIONS -> drawSliders(c, x)
                else -> {}
            }
        }
    }

    private fun drawIconBg(c: Canvas, x: Float, a: Action) {
        c.circle(x, cy, 18 * d, if (pressed == a) Pal.LINE else Pal.SURFACE)
    }

    private fun drawRestart(c: Canvas, x: Float) {
        val r = 8 * d
        val p = Gfx.stroke.apply { strokeWidth = 2.4f * d; color = Pal.TEXT; shader = null }
        Gfx.rect.set(x - r, cy - r, x + r, cy + r)
        c.drawArc(Gfx.rect, -60f, 290f, false, p)
        path.reset()
        val ax = x + r * 0.5f; val ay = cy - r * 0.87f
        path.moveTo(ax + 5 * d, ay - 4 * d); path.lineTo(ax, ay); path.lineTo(ax + 4.5f * d, ay + 4 * d)
        c.drawPath(path, p)
    }

    private fun drawSliders(c: Canvas, x: Float) {
        for (i in -1..1) {
            val y = cy + i * 6 * d
            c.line(x - 8 * d, y, x + 8 * d, y, 2.2f * d, Pal.TEXT)
            c.circle(x + i * 4 * d, y, 3 * d, Pal.TEXT)
        }
    }

    private fun actionAt(x: Float): Action? {
        if (x < 52 * d) return Action.BACK
        actions.forEachIndexed { i, a -> if (kotlin.math.abs(x - iconX(i)) < 22 * d) return a }
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { pressed = actionAt(e.x); invalidate() }
            MotionEvent.ACTION_UP -> {
                val a = actionAt(e.x)
                if (a != null && a == pressed) { performHapticFeedback(HAPTIC_FEEDBACK_ENABLED); onAction(a) }
                pressed = null; invalidate()
            }
            MotionEvent.ACTION_CANCEL -> { pressed = null; invalidate() }
        }
        return true
    }
}
