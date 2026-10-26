package com.hgame.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.circle
import com.hgame.engine.rrect
import com.hgame.engine.text
import com.hgame.engine.textWidth
import com.hgame.games.GameInfo

/** Home grid tile: gradient card with the game's emoji, title, tagline and favourite star. */
@SuppressLint("ViewConstructor")
class GameCard(context: Context) : View(context) {
    private val d = resources.displayMetrics.density
    var game: GameInfo? = null
    var favorite = false
    var best: String = ""
    private var press = 0f
    private var target = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (w * 1.08f).toInt())
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        target = if (pressed) 1f else 0f
        invalidate()
    }

    /** True if ([x],[y]) is on the favourite star. */
    private val clipPath = android.graphics.Path()

    fun hitStar(x: Float, y: Float) = x > width - 44 * d && y < 44 * d

    override fun onDraw(c: Canvas) {
        val g = game ?: return
        if (press != target) {
            press += (target - press) * 0.35f
            if (kotlin.math.abs(press - target) < 0.02f) press = target
            postInvalidateOnAnimation()
        }
        val w = width.toFloat(); val h = height.toFloat()
        val s = 1f - press * 0.04f
        c.save()
        c.scale(s, s, w / 2, h / 2)
        val rad = 22 * d
        c.rrect(4 * d, 8 * d, w - 4 * d, h - 2 * d, rad, Pal.alpha(g.color2.toInt(), 0.25f))
        val p = Gfx.fill
        p.color = Pal.WHITE
        p.shader = LinearGradient(0f, 0f, w, h, g.color1.toInt(), g.color2.toInt(), Shader.TileMode.CLAMP)
        Gfx.rect.set(4 * d, 4 * d, w - 4 * d, h - 6 * d)
        c.drawRoundRect(Gfx.rect, rad, rad, p)
        p.shader = null
        // glossy highlight, clipped to the rounded card
        c.save()
        clipPath.reset(); clipPath.addRoundRect(Gfx.rect, rad, rad, android.graphics.Path.Direction.CW)
        c.clipPath(clipPath)
        c.circle(w * 0.85f, h * 0.05f, w * 0.45f, Pal.alpha(Pal.WHITE, 0.10f))
        c.circle(w * 0.1f, h * 0.95f, w * 0.3f, Pal.alpha(Pal.BLACK, 0.08f))
        c.restore()

        val iconY = h * 0.38f
        c.circle(w / 2, iconY, w * 0.22f, Pal.alpha(Pal.WHITE, 0.22f))
        c.text(g.emoji, w / 2, iconY + 2 * d, w * 0.24f, Pal.WHITE)

        var title = g.title
        val ts = 15.5f * d
        while (textWidth(title, ts) > w - 24 * d && title.length > 3) title = title.dropLast(2) + "…"
        c.text(title, w / 2, h * 0.72f, ts, Pal.WHITE)
        var tag = if (best.isNotEmpty()) "★ $best" else g.tagline
        val ss = 11.5f * d
        while (textWidth(tag, ss, false) > w - 20 * d && tag.length > 3) tag = tag.dropLast(2) + "…"
        c.text(tag, w / 2, h * 0.84f, ss, Pal.alpha(Pal.WHITE, 0.85f), bold = false)

        // favourite star
        drawStar(c, w - 24 * d, 25 * d, 9 * d, favorite)
        c.restore()
    }

    private fun drawStar(c: Canvas, cx: Float, cy: Float, r: Float, filled: Boolean) {
        val path = Gfx.path
        path.reset()
        for (i in 0 until 10) {
            val a = Math.PI / 2 + i * Math.PI / 5
            val rr = if (i % 2 == 0) r else r * 0.45f
            val x = cx + (rr * kotlin.math.cos(a)).toFloat()
            val y = cy - (rr * kotlin.math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) {
            c.drawPath(path, Gfx.fillPaint(Pal.YELLOW))
        } else {
            val st = Gfx.stroke.apply { strokeWidth = 1.6f * d; color = Pal.alpha(Pal.WHITE, 0.75f); shader = null }
            c.drawPath(path, st)
        }
    }
}
