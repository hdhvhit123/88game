package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.TicTacToe
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.line
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class TicTacToeGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Đấu với máy", "2 người")),
        GameOption("level", "Độ khó", listOf("Dễ", "Thường", "Bất bại"), 1),
    )
    override val help = "Lần lượt đánh X và O vào bàn 3×3. Ai có 3 ký hiệu thẳng hàng (ngang, dọc, chéo) trước sẽ thắng."

    private val b = IntArray(9)
    private var turn = 1
    private var thinking = false
    private val fit = BoardFit()
    private val anim = FloatArray(9)
    private var wins = 0; private var losses = 0; private var draws = 0
    private var humanFirst = false

    override fun newGame() {
        b.fill(0); anim.fill(0f); turn = 1; thinking = false
        humanFirst = !humanFirst
        status()
        if (opt("mode") == 0 && !humanFirst) aiMove()
    }

    override fun layout() {
        val size = min(w, h - 120 * dp) - 40 * dp
        fit.fit(3, 3, (w - size) / 2, 90 * dp, (w + size) / 2, 90 * dp + size)
    }

    private fun status() {
        val vsAi = opt("mode") == 0
        info(if (vsAi) "Thắng $wins · Hoà $draws · Thua $losses" else "Lượt: ${if (turn == 1) "X" else "O"}")
    }

    override fun up(x: Float, y: Float) {
        if (over || thinking) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        val i = r * 3 + c
        if (b[i] != 0) return
        play(i)
        if (!over && opt("mode") == 0) aiMove()
    }

    private fun aiMove() {
        thinking = true
        after(0.45f) {
            thinking = false
            val level = opt("level")
            val empty = (0 until 9).filter { b[it] == 0 }
            val mistake = when (level) { 0 -> 0.6f; 1 -> 0.2f; else -> 0f }
            val i = if (Random.nextFloat() < mistake) empty.random() else TicTacToe.bestMove(b, turn)
            play(i)
        }
    }

    private fun play(i: Int) {
        b[i] = turn
        anim[i] = 0f
        animate(0.4f)
        sfx(Sfx.TAP)
        turn = 3 - turn
        status()
        val win = TicTacToe.winner(b)
        if (win != 0) {
            val vsAi = opt("mode") == 0
            val humanMark = if (humanFirst) 1 else 2
            when {
                win == 3 -> { draws++; gameOver(false, "Hoà!", "Không ai thắng ván này") }
                !vsAi -> gameOver(true, "${if (win == 1) "X" else "O"} thắng!")
                win == humanMark -> { wins++; gameOver(true, "Bạn thắng!") }
                else -> { losses++; gameOver(false, "Máy thắng!") }
            }
            status()
        }
    }

    override fun update(dt: Float) { for (i in 0 until 9) if (b[i] != 0) anim[i] = min(1f, anim[i] + dt * 4f) }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val vsAi = opt("mode") == 0
        val label = when {
            over -> ""
            vsAi -> if (thinking) "Máy đang nghĩ…" else "Lượt của bạn (${if (humanFirst) "X" else "O"})"
            else -> "Lượt của ${if (turn == 1) "X" else "O"}"
        }
        c.text(label, w / 2, 50 * dp, 20 * dp, Pal.TEXT)
        val cs = fit.cell
        c.rrect(fit.x0 - 10 * dp, fit.y0 - 10 * dp, fit.right + 10 * dp, fit.bottom + 10 * dp, 24 * dp, Pal.SURFACE)
        for (k in 1..2) {
            c.line(fit.x0 + k * cs, fit.y0 + 10 * dp, fit.x0 + k * cs, fit.bottom - 10 * dp, 5 * dp, Pal.LINE)
            c.line(fit.x0 + 10 * dp, fit.y0 + k * cs, fit.right - 10 * dp, fit.y0 + k * cs, 5 * dp, Pal.LINE)
        }
        for (i in 0 until 9) {
            val cx = fit.cx(i % 3); val cy = fit.cy(i / 3)
            val a = if (b[i] != 0) anim[i].coerceAtLeast(0.01f) else 0f
            val r = cs * 0.28f
            if (b[i] == 1) {
                val e = min(1f, a * 2); val e2 = ((a - 0.5f) * 2).coerceIn(0f, 1f)
                c.line(cx - r, cy - r, cx - r + 2 * r * e, cy - r + 2 * r * e, cs * 0.1f, Pal.PINK)
                if (e2 > 0) c.line(cx + r, cy - r, cx + r - 2 * r * e2, cy - r + 2 * r * e2, cs * 0.1f, Pal.PINK)
            } else if (b[i] == 2) {
                c.ring(cx, cy, r * (0.5f + 0.5f * a), cs * 0.1f, Pal.CYAN)
            }
        }
        TicTacToe.winLine(b)?.let { l ->
            c.line(fit.cx(l[0] % 3), fit.cy(l[0] / 3), fit.cx(l[2] % 3), fit.cy(l[2] / 3), cs * 0.08f, Pal.alpha(Pal.YELLOW, 0.9f))
        }
    }
}
