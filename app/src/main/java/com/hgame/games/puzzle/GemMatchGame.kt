package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Match3
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.min

class GemMatchGame : Game() {
    override val help = "Đổi chỗ hai viên đá quý cạnh nhau để tạo hàng 3 viên cùng màu trở lên. " +
        "Chuỗi phản ứng liên tiếp được nhân điểm. Bạn có 30 lượt đổi, hãy ghi điểm cao nhất!"

    private val colors = intArrayOf(Pal.RED, Pal.BLUE, Pal.GREEN, Pal.YELLOW, Pal.PURPLE, Pal.ORANGE)
    private var m = Match3()
    private val fit = BoardFit()
    private var sel = -1
    private var downCell = -1; private var downX = 0f; private var downY = 0f
    private var busy = false
    private val offY = FloatArray(64)      // vertical offset in cells (animating towards 0)
    private val pop = FloatArray(64)
    private var swapA = -1; private var swapB = -1; private var swapT = 1f; private var swapBack = false
    private var points = 0
    private var moves = 30
    private var chain = 0

    override fun newGame() {
        m = Match3(); sel = -1; busy = false; points = 0; moves = 30; chain = 0
        offY.fill(0f); pop.fill(0f); swapT = 1f
        for (i in 0 until 64) offY[i] = -(8 - i / 8 + 2).toFloat()
        animate(1f)
        score(0); info("Lượt còn: $moves")
    }

    override fun layout() {
        val size = min(w, h) - 20 * dp
        fit.fit(8, 8, (w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2)
    }

    private fun cellAt(x: Float, y: Float): Int { val c = fit.colAt(x); val r = fit.rowAt(y); return if (c < 0 || r < 0) -1 else r * 8 + c }

    override fun down(x: Float, y: Float) { if (!busy && !over) { downCell = cellAt(x, y); downX = x; downY = y } }

    override fun move(x: Float, y: Float) {
        if (downCell < 0 || busy) return
        val dx = x - downX; val dy = y - downY
        if (abs(dx) < fit.cell * 0.4f && abs(dy) < fit.cell * 0.4f) return
        val target = if (abs(dx) > abs(dy)) downCell + (if (dx > 0) 1 else -1) else downCell + (if (dy > 0) 8 else -8)
        val from = downCell
        downCell = -1
        if (target in 0 until 64 && m.adjacent(from, target)) trySwap(from, target)
    }

    override fun up(x: Float, y: Float) {
        if (busy || over) return
        val c = cellAt(x, y)
        if (downCell >= 0 && c == downCell) {
            if (sel >= 0 && m.adjacent(sel, c)) { val s = sel; sel = -1; trySwap(s, c) }
            else { sel = if (sel == c) -1 else c; sfx(Sfx.CLICK) }
        }
        downCell = -1
        redraw()
    }

    private fun trySwap(a: Int, b: Int) {
        sel = -1
        busy = true
        m.swap(a, b)
        val matched = m.findMatches()
        swapA = a; swapB = b; swapT = 0f; swapBack = matched.isEmpty()
        sfx(Sfx.TAP)
        animate(0.4f)
        if (swapBack) {
            m.swap(a, b)
            after(0.35f) { busy = false; sfx(Sfx.ERROR) }
        } else {
            moves--
            info("Lượt còn: $moves")
            chain = 0
            after(0.2f) { resolve() }
        }
    }

    private fun resolve() {
        val matched = m.findMatches()
        if (matched.isEmpty()) {
            busy = false
            if (moves <= 0) { gameOver(true, "Hết lượt!", "Bạn đạt $points điểm", points); return }
            if (!m.hasMove()) { m.shuffle(); info("Không còn nước – xáo lại bàn") }
            return
        }
        chain++
        points += matched.size * 10 * chain
        score(points)
        if (chain > 1) info("Chuỗi ×$chain!")
        sfx(if (chain > 1) Sfx.COIN else Sfx.MERGE)
        for (i in matched) pop[i] = 1f
        animate(0.3f)
        after(0.22f) {
            val fall = m.collapse(matched)
            for (i in 0 until 64) { offY[i] = -fall[i].toFloat(); pop[i] = 0f }
            animate(0.6f)
            after(0.38f) { resolve() }
        }
    }

    override fun update(dt: Float) {
        for (i in 0 until 64) {
            if (offY[i] < 0f) offY[i] = min(0f, offY[i] + dt * 16f)
            if (pop[i] > 0f) pop[i] = maxOf(0f, pop[i] - dt * 4f)
        }
        if (swapT < 1f) swapT = min(1f, swapT + dt * 6f)
    }

    private fun gem(c: Canvas, x: Float, y: Float, s: Float, type: Int, scale: Float) {
        val r = s * 0.4f * scale
        val col = colors[type]
        val p = Gfx.path
        p.reset()
        when (type % 3) {
            0 -> { c.circle(x, y + r * 0.1f, r, Pal.shade(col, 0.5f)); c.circle(x, y, r, col) }
            1 -> { p.moveTo(x, y - r); p.lineTo(x + r, y); p.lineTo(x, y + r); p.lineTo(x - r, y); p.close(); c.drawPath(p, Gfx.fillPaint(col)) }
            else -> { c.rrect(x - r * 0.85f, y - r * 0.85f, x + r * 0.85f, y + r * 0.85f, r * 0.3f, col) }
        }
        if (type >= 3) c.circle(x, y, r * 0.35f, Pal.alpha(Pal.WHITE, 0.35f))
        c.circle(x - r * 0.3f, y - r * 0.35f, r * 0.2f, Pal.alpha(Pal.WHITE, 0.55f))
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.rrect(fit.x0 - 6 * dp, fit.y0 - 6 * dp, fit.right + 6 * dp, fit.bottom + 6 * dp, 14 * dp, Pal.SURFACE)
        for (i in 0 until 64) {
            val x = fit.x0 + (i % 8) * cs; val y = fit.y0 + (i / 8) * cs
            c.rrect(x + 2, y + 2, x + cs - 2, y + cs - 2, cs * 0.18f, if ((i / 8 + i % 8) % 2 == 0) 0xFF262B4F.toInt() else 0xFF2C3259.toInt())
        }
        c.save()
        c.clipRect(fit.x0, fit.y0, fit.right, fit.bottom)
        for (i in 0 until 64) {
            val t = m.cells[i]
            if (t < 0) continue
            var cx = fit.cx(i % 8); var cy = fit.cy(i / 8) + offY[i] * cs
            if (swapT < 1f && (i == swapA || i == swapB)) {
                val other = if (i == swapA) swapB else swapA
                val e = if (swapBack) 1 - abs(1 - 2 * swapT) else 1 - swapT
                // the model already holds the swapped state (or was swapped back), so draw from the other cell
                val fx = fit.cx(other % 8); val fy = fit.cy(other / 8)
                if (!swapBack) { cx = cx + (fx - cx) * e; cy = cy + (fy - cy) * e }
                else { cx = cx + (fx - cx) * e; cy = cy + (fy - cy) * e }
            }
            val scale = if (pop[i] > 0f) 1f + (1 - pop[i]) * 0.3f else 1f
            if (pop[i] > 0f) c.circle(cx, cy, cs * 0.45f * scale, Pal.alpha(Pal.WHITE, pop[i] * 0.5f))
            gem(c, cx, cy, cs, t, if (pop[i] > 0f) pop[i] else 1f)
            if (i == sel) c.rrect(cx - cs * 0.48f, cy - cs * 0.48f, cx + cs * 0.48f, cy + cs * 0.48f, cs * 0.2f, Pal.alpha(Pal.WHITE, 0.18f))
        }
        c.restore()
        if (moves <= 5 && !over) c.text("Còn $moves lượt!", w / 2, fit.bottom + 24 * dp, 16 * dp, Pal.ORANGE)
    }
}
