package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.Reversi
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.min

class ReversiGame : Game() {
    override val bestMode = BestMode.HIGH
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Đấu với máy", "2 người")),
        GameOption("level", "Độ khó", listOf("Dễ", "Thường", "Khó"), 1),
    )
    override val help = "Đặt quân sao cho kẹp được quân đối phương giữa quân mới và một quân của bạn theo hàng, cột hoặc chéo. " +
        "Các quân bị kẹp sẽ lật sang màu của bạn. Ai nhiều quân hơn khi hết nước sẽ thắng. Bạn cầm quân Đen, đi trước."

    private var r = Reversi()
    private val fit = BoardFit()
    private var turn = 1
    private var thinking = false
    private val flipT = FloatArray(64) { 1f }
    private var last = -1

    override fun newGame() {
        r = Reversi(); turn = 1; thinking = false; flipT.fill(1f); last = -1
        status()
    }

    override fun layout() {
        val pad = 12 * dp
        val size = min(w - pad * 2, h - 130 * dp)
        fit.fit(8, 8, (w - size) / 2, 80 * dp, (w + size) / 2, 80 * dp + size)
    }

    private fun status() = info("⚫ ${r.count(1)}  ·  ⚪ ${r.count(2)}")

    override fun up(x: Float, y: Float) {
        if (over || thinking) return
        if (opt("mode") == 0 && turn != 1) return
        val c = fit.colAt(x); val row = fit.rowAt(y)
        if (c < 0 || row < 0) return
        val sq = row * 8 + c
        if (r.flips(sq, turn).isEmpty()) { sfx(Sfx.ERROR); return }
        place(sq)
    }

    private fun place(sq: Int) {
        val flipped = r.play(sq, turn)
        last = sq
        flipT[sq] = 1f
        flipped.forEachIndexed { i, s -> flipT[s] = -i * 0.06f }
        animate(0.6f + flipped.size * 0.06f)
        sfx(Sfx.TAP)
        status()
        next()
    }

    private fun next() {
        val other = 3 - turn
        when {
            r.moves(other).isNotEmpty() -> turn = other
            r.moves(turn).isNotEmpty() -> info("${if (other == 1) "Đen" else "Trắng"} không có nước, mất lượt")
            else -> { finish(); return }
        }
        if (opt("mode") == 0 && turn == 2) aiMove()
    }

    private fun finish() {
        val b = r.count(1); val wC = r.count(2)
        val vsAi = opt("mode") == 0
        val title = when {
            b == wC -> "Hoà!"
            !vsAi -> if (b > wC) "Đen thắng!" else "Trắng thắng!"
            b > wC -> "Bạn thắng!"
            else -> "Máy thắng!"
        }
        gameOver(b > wC || (!vsAi && b != wC), title, "Đen $b – $wC Trắng", if (vsAi) b else null)
    }

    private fun aiMove() {
        thinking = true
        val copy = r.copy()
        val depth = intArrayOf(1, 3, 5)[opt("level")]
        background({ Thread.sleep(350); copy.bestMove(2, depth) }) { m -> thinking = false; if (m >= 0) place(m) }
    }

    override fun update(dt: Float) { for (i in 0 until 64) if (flipT[i] < 1f) flipT[i] = min(1f, flipT[i] + dt * 4f) }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.text(when { over -> ""; thinking -> "Máy đang nghĩ…"; turn == 1 -> "Lượt Đen ⚫"; else -> "Lượt Trắng ⚪" }, w / 2, 40 * dp, 18 * dp, Pal.TEXT)
        c.rrect(fit.x0 - 8 * dp, fit.y0 - 8 * dp, fit.right + 8 * dp, fit.bottom + 8 * dp, 14 * dp, 0xFF0B4A33.toInt())
        c.rrect(fit.x0, fit.y0, fit.right, fit.bottom, 4 * dp, 0xFF1B8A5A.toInt())
        for (k in 0..8) {
            c.line(fit.x0 + k * cs, fit.y0, fit.x0 + k * cs, fit.bottom, 1.2f * dp, 0xFF0F6B44.toInt())
            c.line(fit.x0, fit.y0 + k * cs, fit.right, fit.y0 + k * cs, 1.2f * dp, 0xFF0F6B44.toInt())
        }
        val showHints = !over && !thinking && (opt("mode") == 1 || turn == 1)
        val hints = if (showHints) r.moves(turn).toSet() else emptySet()
        for (sq in 0 until 64) {
            val cx = fit.cx(sq % 8); val cy = fit.cy(sq / 8)
            val p = r.cells[sq]
            if (p == 0) {
                if (sq in hints) c.circle(cx, cy, cs * 0.1f, Pal.alpha(Pal.BLACK, 0.25f))
                continue
            }
            val t = flipT[sq].coerceIn(0f, 1f)
            // mid-flip shows the previous colour first, then the new one
            val sx = abs(1 - 2 * t).let { if (flipT[sq] >= 1f) 1f else it }
            val showColor = if (t < 0.5f && flipT[sq] < 1f) 3 - p else p
            val rad = cs * 0.4f
            c.save(); c.scale(sx.coerceAtLeast(0.05f), 1f, cx, cy)
            c.circle(cx, cy + 2 * dp, rad, Pal.alpha(Pal.BLACK, 0.35f))
            c.circle(cx, cy, rad, if (showColor == 1) 0xFF1E1F26.toInt() else 0xFFF4F4F8.toInt())
            c.circle(cx - rad * 0.25f, cy - rad * 0.3f, rad * 0.3f, Pal.alpha(Pal.WHITE, if (showColor == 1) 0.12f else 0.5f))
            c.restore()
            if (sq == last) c.circle(cx, cy, cs * 0.07f, Pal.RED)
        }
    }
}
