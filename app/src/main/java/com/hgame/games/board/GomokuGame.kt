package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.Gomoku
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min

class GomokuGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Đấu với máy", "2 người")),
        GameOption("level", "Độ khó", listOf("Dễ", "Thường", "Khó"), 1),
        GameOption("size", "Bàn cờ", listOf("15 × 15", "13 × 13", "19 × 19")),
    )
    override val help = "Cờ ca-rô: lần lượt đặt quân lên giao điểm. Ai có 5 quân liên tiếp theo hàng ngang, dọc hoặc chéo trước sẽ thắng. " +
        "Chạm lần đầu để xem trước, chạm lại cùng chỗ để đặt."

    private var g = Gomoku(15)
    private var turn = 1
    private var thinking = false
    private var preview = -1
    private var last = -1
    private var win: List<Int>? = null
    private var cell = 0f; private var x0 = 0f; private var y0 = 0f
    private val history = ArrayList<Int>()

    override fun newGame() {
        g = Gomoku(intArrayOf(15, 13, 19)[opt("size")])
        turn = 1; thinking = false; preview = -1; last = -1; win = null; history.clear()
        info(if (opt("mode") == 0) "Bạn: ⚫ đi trước" else "Lượt: ⚫")
    }

    override fun layout() {
        cell = min((w - 24 * dp) / (g.n - 1 + 1f), (h - 90 * dp) / (g.n - 1 + 1f))
        x0 = (w - cell * (g.n - 1)) / 2
        y0 = 60 * dp + ((h - 60 * dp) - cell * (g.n - 1)) / 2
    }

    private fun at(x: Float, y: Float): Int {
        val c = ((x - x0) / cell + 0.5f).toInt(); val r = ((y - y0) / cell + 0.5f).toInt()
        if (x < x0 - cell / 2 || y < y0 - cell / 2 || !g.inside(r, c)) return -1
        return r * g.n + c
    }

    override fun up(x: Float, y: Float) {
        if (over || thinking) return
        val i = at(x, y)
        if (i < 0 || g.cells[i] != 0) return
        if (preview != i) { preview = i; sfx(Sfx.CLICK); redraw(); return }
        place(i)
        if (!over && opt("mode") == 0) aiMove()
    }

    private fun place(i: Int) {
        g.cells[i] = turn; last = i; preview = -1; history.add(i)
        sfx(Sfx.TAP)
        win = g.winLine(i)
        val vsAi = opt("mode") == 0
        if (win != null) {
            val title = if (!vsAi) (if (turn == 1) "⚫ Đen thắng!" else "⚪ Trắng thắng!") else if (turn == 1) "Bạn thắng!" else "Máy thắng!"
            gameOver(!vsAi || turn == 1, title, "5 quân thẳng hàng")
            return
        }
        if (g.cells.none { it == 0 }) { gameOver(false, "Hoà!"); return }
        turn = 3 - turn
        if (!vsAi) info("Lượt: ${if (turn == 1) "⚫" else "⚪"}")
    }

    private fun aiMove() {
        thinking = true
        val copy = Gomoku(g.n).also { g.cells.copyInto(it.cells) }
        val skill = opt("level")
        background({ Thread.sleep(300); copy.bestMove(2, skill) }) { m -> thinking = false; if (m >= 0) place(m) }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.text(when { over -> ""; thinking -> "Máy đang nghĩ…"; turn == 1 -> "Lượt ⚫"; else -> "Lượt ⚪" }, w / 2, 30 * dp, 18 * dp, Pal.TEXT)
        val n = g.n
        val pad = cell * 0.7f
        c.rrect(x0 - pad, y0 - pad, x0 + cell * (n - 1) + pad, y0 + cell * (n - 1) + pad, 10 * dp, 0xFFE3B778.toInt())
        for (k in 0 until n) {
            c.line(x0, y0 + k * cell, x0 + (n - 1) * cell, y0 + k * cell, 1f * dp, 0xFF8A5A2B.toInt())
            c.line(x0 + k * cell, y0, x0 + k * cell, y0 + (n - 1) * cell, 1f * dp, 0xFF8A5A2B.toInt())
        }
        val star = if (n >= 15) listOf(3, n / 2, n - 4) else listOf(3, n / 2, n - 4)
        for (a in star) for (b in star) c.circle(x0 + a * cell, y0 + b * cell, 3 * dp, 0xFF8A5A2B.toInt())
        for (i in g.cells.indices) {
            val p = g.cells[i]
            if (p == 0) continue
            val cx = x0 + (i % n) * cell; val cy = y0 + (i / n) * cell
            c.circle(cx + 1.5f * dp, cy + 2 * dp, cell * 0.44f, 0x55000000)
            c.ball(cx, cy, cell * 0.44f, if (p == 1) 0xFF2A2B33.toInt() else 0xFFF7F7FA.toInt())
            if (i == last) c.circle(cx, cy, cell * 0.1f, Pal.RED)
        }
        if (preview >= 0) {
            val cx = x0 + (preview % n) * cell; val cy = y0 + (preview / n) * cell
            c.circle(cx, cy, cell * 0.44f, Pal.alpha(if (turn == 1) Pal.BLACK else Pal.WHITE, 0.45f))
            c.ring(cx, cy, cell * 0.5f, 2 * dp, Pal.ACCENT)
        }
        win?.let { l ->
            for (i in l) c.ring(x0 + (i % n) * cell, y0 + (i / n) * cell, cell * 0.48f, 3 * dp, Pal.RED)
        }
    }
}
