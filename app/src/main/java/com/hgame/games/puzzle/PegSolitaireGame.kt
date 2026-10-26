package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.ring
import kotlin.math.abs
import kotlin.math.min

class PegSolitaireGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(GameOption("board", "Bàn", listOf("Kiểu Anh (33 lỗ)", "Kiểu Pháp (37 lỗ)")))
    override val help = "Nhảy một quân qua quân kề bên (ngang hoặc dọc) vào lỗ trống phía sau; quân bị nhảy qua sẽ bị loại. " +
        "Mục tiêu: chỉ còn lại 1 quân, tốt nhất là ở chính giữa."

    private val n = 7
    private var valid = BooleanArray(49)
    private var peg = BooleanArray(49)
    private var sel = -1
    private val fit = BoardFit()
    private val history = ArrayList<BooleanArray>()
    private val undoBtn = Btn("↶ Đi lại", Pal.SURFACE2)
    private var dragX = 0f; private var dragY = 0f; private var dragging = false

    override fun newGame() {
        val french = opt("board") == 1
        valid = BooleanArray(49) { i ->
            val r = i / n; val c = i % n
            val cross = (r in 2..4) || (c in 2..4)
            cross || (french && r in 1..5 && c in 1..5)
        }
        peg = BooleanArray(49) { valid[it] && it != 24 }
        if (french) { peg[24] = true; peg[2 * n + 3 - n] = false }
        sel = -1; history.clear()
        status()
    }

    override fun layout() {
        val size = (min(w, h - 80 * dp) - 24 * dp) * 0.93f
        fit.fit(n, n, (w - size) / 2, (h - 70 * dp - size) / 2, (w + size) / 2, (h - 70 * dp + size) / 2)
        undoBtn.setCenter(w / 2, h - 34 * dp, 150 * dp, 44 * dp)
    }

    private fun status() = info("Còn ${peg.count { it }} quân")

    private fun jumpTarget(from: Int, to: Int): Int {
        val fr = from / n; val fc = from % n; val tr = to / n; val tc = to % n
        if (!(fr == tr && abs(fc - tc) == 2 || fc == tc && abs(fr - tr) == 2)) return -1
        val mid = ((fr + tr) / 2) * n + (fc + tc) / 2
        return if (valid[to] && !peg[to] && peg[mid]) mid else -1
    }

    private fun canMove(i: Int) = peg[i] && listOf(i - 2, i + 2, i - 2 * n, i + 2 * n).any { it in 0..48 && (it / n == i / n || it % n == i % n) && jumpTarget(i, it) >= 0 }

    override fun down(x: Float, y: Float) {
        if (over) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        val i = r * n + c
        if (peg[i]) { sel = i; dragging = true; dragX = x; dragY = y; sfx(Sfx.CLICK) }
    }

    override fun move(x: Float, y: Float) { if (dragging) { dragX = x; dragY = y } }

    override fun up(x: Float, y: Float) {
        if (undoBtn.hit(x, y) && history.isNotEmpty()) {
            peg = history.removeAt(history.size - 1); sel = -1; dragging = false
            if (over) resumePlay()
            status(); sfx(Sfx.TAP); return
        }
        dragging = false
        if (over || sel < 0) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) { return }
        val to = r * n + c
        if (to == sel) return
        val mid = jumpTarget(sel, to)
        if (mid < 0) { if (!peg[to]) sfx(Sfx.ERROR); sel = if (peg[to]) to else -1; return }
        history.add(peg.copyOf())
        peg[sel] = false; peg[mid] = false; peg[to] = true
        sel = -1
        sfx(Sfx.POP)
        status()
        if ((0 until 49).none { canMove(it) }) {
            val left = peg.count { it }
            when {
                left == 1 && peg[24] -> gameOver(true, "Hoàn hảo!", "Quân cuối cùng ở chính giữa")
                left == 1 -> gameOver(true, "Xuất sắc!", "Chỉ còn 1 quân")
                else -> gameOver(left <= 3, if (left <= 3) "Khá lắm!" else "Hết nước đi", "Còn lại $left quân")
            }
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.circle(w / 2, fit.y0 + fit.cell * 3.5f, cs * 3.75f, 0xFF6D4C33.toInt())
        c.circle(w / 2, fit.y0 + fit.cell * 3.5f, cs * 3.6f, 0xFF8D6446.toInt())
        for (i in 0 until 49) {
            if (!valid[i]) continue
            val cx = fit.cx(i % n); val cy = fit.cy(i / n)
            c.circle(cx, cy, cs * 0.2f, 0xFF4A3121.toInt())
            if (sel >= 0 && jumpTarget(sel, i) >= 0) c.ring(cx, cy, cs * 0.32f, 3 * dp, Pal.GREEN)
            if (peg[i] && !(dragging && i == sel)) {
                c.ball(cx, cy - 2 * dp, cs * 0.34f, if (i == sel) Pal.YELLOW else 0xFF4FC3F7.toInt())
            }
        }
        if (dragging && sel >= 0) c.ball(dragX, dragY, cs * 0.38f, Pal.YELLOW)
        undoBtn.enabled = history.isNotEmpty()
        undoBtn.draw(c, 15 * dp)
    }
}
