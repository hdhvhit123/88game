package com.hgame.games.casual

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.Gfx
import com.hgame.engine.text
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

/** Repeat the growing sequence of coloured pads. */
class ColorMemoryGame : Game() {
    override val help = "Quan sát chuỗi màu sáng lên, sau đó chạm lại đúng thứ tự. Mỗi vòng chuỗi dài thêm một màu."

    private val colors = intArrayOf(Pal.GREEN, Pal.RED, Pal.BLUE, Pal.YELLOW)
    private val seq = ArrayList<Int>()
    private var input = 0
    private var showing = false
    private var lit = -1
    private var cx = 0f; private var cy = 0f; private var r = 0f
    private val sounds = arrayOf(Sfx.TAP, Sfx.POP, Sfx.CLICK, Sfx.COIN)

    override fun newGame() {
        seq.clear(); input = 0; lit = -1
        score(0)
        after(0.6f) { nextRound() }
    }

    override fun layout() {
        cx = w / 2; cy = h / 2 + 10 * dp
        r = min(w, h) * 0.42f
    }

    private fun nextRound() {
        seq.add(Random.nextInt(4))
        input = 0
        showing = true
        info("Ghi nhớ…")
        val step = maxOf(0.28f, 0.6f - seq.size * 0.02f)
        seq.forEachIndexed { i, p ->
            after(0.4f + i * step) { lit = p; sfx(sounds[p]) }
            after(0.4f + i * step + step * 0.7f) { lit = -1 }
        }
        after(0.4f + seq.size * step) { showing = false; info("Đến lượt bạn") }
    }

    private fun padAt(x: Float, y: Float): Int {
        val d = hypot(x - cx, y - cy)
        if (d < r * 0.3f || d > r) return -1
        val a = Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat()
        // 0: top-left, 1: top-right, 2: bottom-right, 3: bottom-left
        return when {
            a < -90 -> 0; a < 0 -> 1; a < 90 -> 2; else -> 3
        }
    }

    override fun down(x: Float, y: Float) {
        if (showing || over) return
        lit = padAt(x, y)
        if (lit >= 0) sfx(sounds[lit])
    }

    override fun up(x: Float, y: Float) {
        if (showing || over) return
        val p = lit
        lit = -1
        if (p < 0) return
        if (p == seq[input]) {
            input++
            if (input == seq.size) {
                score(seq.size)
                showing = true
                after(0.7f) { nextRound() }
            }
        } else {
            buzz(80)
            gameOver(false, "Sai rồi!", "Bạn nhớ được ${seq.size - 1} màu", seq.size - 1)
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val starts = floatArrayOf(180f, 270f, 0f, 90f)
        for (i in 0 until 4) {
            val on = lit == i
            val col = if (on) Pal.tint(colors[i], 0.45f) else Pal.shade(colors[i], 0.55f)
            if (on) c.circle(cx, cy, r * 1.04f, Pal.alpha(colors[i], 0.12f))
            Gfx.rect.set(cx - r, cy - r, cx + r, cy + r)
            c.drawArc(Gfx.rect, starts[i] + 2f, 86f, true, Gfx.fillPaint(col))
        }
        c.circle(cx, cy, r * 0.34f, Pal.BG)
        c.circle(cx, cy, r * 0.3f, Pal.SURFACE)
        c.text(seq.size.toString(), cx, cy - 6 * dp, r * 0.2f, Pal.TEXT)
        c.text(if (showing) "Xem" else "Chạm", cx, cy + r * 0.13f, r * 0.08f, Pal.MUTED, bold = false)
    }
}
