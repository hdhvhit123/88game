package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.TicTacToe
import com.hgame.core.UltimateTTT
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.line
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.rrectStroke
import com.hgame.engine.text
import kotlin.math.min

class UltimateTicTacToeGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Đấu với máy", "2 người")),
        GameOption("level", "Độ khó", listOf("Dễ", "Thường", "Khó"), 1),
    )
    override val help = "Bàn lớn gồm 9 bàn ca-rô nhỏ. Ô bạn đánh trong bàn nhỏ quyết định bàn nhỏ mà đối thủ phải đánh tiếp. " +
        "Thắng một bàn nhỏ để chiếm ô đó trên bàn lớn. Chiếm 3 bàn nhỏ thẳng hàng để thắng."

    private var g = UltimateTTT()
    private var thinking = false
    private var size = 0f; private var x0 = 0f; private var y0 = 0f
    private var last = -1

    override fun newGame() { g = UltimateTTT(); thinking = false; last = -1; status() }

    override fun layout() {
        size = min(w, h - 80 * dp) - 24 * dp
        x0 = (w - size) / 2; y0 = 60 * dp + (h - 60 * dp - size) / 2
    }

    private fun status() = info(if (g.turn == 1) "Lượt X" else "Lượt O")

    private fun cellRect(i: Int): FloatArray {
        val big = i / 9; val s = i % 9
        val bs = size / 3; val pad = bs * 0.06f
        val cs = (bs - pad * 2) / 3
        val bx = x0 + (big % 3) * bs + pad; val by = y0 + (big / 3) * bs + pad
        return floatArrayOf(bx + (s % 3) * cs, by + (s / 3) * cs, cs)
    }

    override fun up(x: Float, y: Float) {
        if (over || thinking) return
        if (opt("mode") == 0 && g.turn != 1) return
        val legal = g.legal().toSet()
        for (i in legal) {
            val r = cellRect(i)
            if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[2]) { play(i); return }
        }
    }

    private fun play(i: Int) {
        val before = g.boards.copyOf()
        g.play(i); last = i
        sfx(if (!g.boards.contentEquals(before)) Sfx.COIN else Sfx.TAP)
        status()
        val w = g.winner()
        if (w != 0) {
            val vsAi = opt("mode") == 0
            gameOver(w == 1 || (!vsAi && w == 2), when { w == 3 -> "Hoà!"; !vsAi -> if (w == 1) "X thắng!" else "O thắng!"; w == 1 -> "Bạn thắng!"; else -> "Máy thắng!" })
            return
        }
        if (opt("mode") == 0 && g.turn == 2) {
            thinking = true
            val copy = g.copy()
            val n = intArrayOf(60, 600, 3000)[opt("level")]
            background({ Thread.sleep(200); copy.bestMove(n) }) { m -> thinking = false; play(m) }
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.text(when { over -> ""; thinking -> "Máy đang nghĩ…"; g.turn == 1 -> "Lượt X"; else -> "Lượt O" }, w / 2, 30 * dp, 18 * dp, Pal.TEXT)
        val bs = size / 3
        val legal = if (!over) g.legal().map { it / 9 }.toSet() else emptySet()
        for (big in 0 until 9) {
            val bx = x0 + (big % 3) * bs; val by = y0 + (big / 3) * bs
            val active = big in legal && g.boards[big] == 0
            c.rrect(bx + 3 * dp, by + 3 * dp, bx + bs - 3 * dp, by + bs - 3 * dp, 12 * dp, if (active) 0xFF2E3570.toInt() else Pal.SURFACE)
            if (active && !thinking) run { c.rrectStroke(bx + 3 * dp, by + 3 * dp, bx + bs - 3 * dp, by + bs - 3 * dp, 12 * dp, 2.5f * dp, Pal.alpha(Pal.YELLOW, 0.7f)) }
        }
        for (i in 0 until 81) {
            val r = cellRect(i)
            val cx = r[0] + r[2] / 2; val cy = r[1] + r[2] / 2
            val s = i % 9
            if (s % 3 != 2) c.line(r[0] + r[2], r[1] + r[2] * 0.15f, r[0] + r[2], r[1] + r[2] * 0.85f, 1.5f * dp, Pal.LINE)
            if (s / 3 != 2) c.line(r[0] + r[2] * 0.15f, r[1] + r[2], r[0] + r[2] * 0.85f, r[1] + r[2], 1.5f * dp, Pal.LINE)
            val p = g.cells[i]
            val k = r[2] * 0.28f
            if (p == 1) {
                c.line(cx - k, cy - k, cx + k, cy + k, r[2] * 0.12f, Pal.PINK)
                c.line(cx + k, cy - k, cx - k, cy + k, r[2] * 0.12f, Pal.PINK)
            } else if (p == 2) c.ring(cx, cy, k, r[2] * 0.12f, Pal.CYAN)
            if (i == last) c.ring(cx, cy, r[2] * 0.45f, 1.5f * dp, Pal.alpha(Pal.YELLOW, 0.8f))
        }
        for (big in 0 until 9) {
            val win = g.boards[big]
            if (win == 0) continue
            val bx = x0 + (big % 3) * bs; val by = y0 + (big / 3) * bs
            c.rrect(bx + 3 * dp, by + 3 * dp, bx + bs - 3 * dp, by + bs - 3 * dp, 12 * dp, Pal.alpha(Pal.BG, 0.75f))
            val cx = bx + bs / 2; val cy = by + bs / 2; val k = bs * 0.3f
            when (win) {
                1 -> { c.line(cx - k, cy - k, cx + k, cy + k, bs * 0.1f, Pal.PINK); c.line(cx + k, cy - k, cx - k, cy + k, bs * 0.1f, Pal.PINK) }
                2 -> c.ring(cx, cy, k, bs * 0.1f, Pal.CYAN)
                else -> c.text("=", cx, cy, bs * 0.5f, Pal.MUTED)
            }
        }
        val bw = IntArray(9) { if (g.boards[it] == 3) 0 else g.boards[it] }
        TicTacToe.winLine(bw)?.let { l ->
            val p = { b: Int -> floatArrayOf(x0 + (b % 3 + 0.5f) * bs, y0 + (b / 3 + 0.5f) * bs) }
            val a = p(l[0]); val b = p(l[2])
            c.line(a[0], a[1], b[0], b[1], 8 * dp, Pal.alpha(Pal.YELLOW, 0.9f))
        }
    }
}
