package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.Checkers
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.min

class CheckersGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Đấu với máy", "2 người")),
        GameOption("level", "Độ khó", listOf("Dễ", "Thường", "Khó"), 1),
    )
    override val help = "Cờ đam (luật Anh): quân đi chéo về phía trước một ô. Bắt buộc phải ăn khi có thể, " +
        "ăn liên tiếp nhiều quân trong một lượt. Quân đến hàng cuối thành Vua, đi được cả tiến và lùi. Hết quân hoặc hết nước là thua."

    private var g = Checkers()
    private val fit = BoardFit()
    private var side = 1          // 1 bottom (red, human), -1 top
    private var sel = -1
    private var thinking = false
    private var moves: List<Checkers.Move> = emptyList()
    private var anim: Checkers.Move? = null
    private var animT = 1f
    private var animPiece = 0

    override fun newGame() {
        g = Checkers(); side = 1; sel = -1; thinking = false; anim = null; animT = 1f
        moves = g.moves(side)
        status()
    }

    override fun layout() {
        val pad = 10 * dp
        val size = min(w - pad * 2, h - 110 * dp)
        fit.fit(8, 8, (w - size) / 2, 70 * dp, (w + size) / 2, 70 * dp + size)
    }

    private fun status() = info("🔴 ${g.b.count { it > 0 }}  ·  ⚫ ${g.b.count { it < 0 }}")

    override fun up(x: Float, y: Float) {
        if (over || thinking || animT < 1f) return
        if (opt("mode") == 0 && side != 1) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        val sq = r * 8 + c
        if (sel >= 0) {
            val m = moves.filter { it.from == sel && it.to == sq }
            if (m.isNotEmpty()) { play(m.maxByOrNull { it.captured.size }!!); return }
        }
        if (moves.any { it.from == sq }) { sel = sq; sfx(Sfx.CLICK) }
        else if (g.b[sq] * side > 0) { sfx(Sfx.ERROR); info(if (moves.any { it.captured.isNotEmpty() }) "Bắt buộc phải ăn quân!" else "Quân này không đi được") }
        else sel = -1
        redraw()
    }

    private fun play(m: Checkers.Move) {
        animPiece = g.b[m.from]
        g.play(m)
        anim = m; animT = 0f
        animate(0.25f * (m.path.size - 1) + 0.1f)
        sel = -1
        sfx(if (m.captured.isNotEmpty()) Sfx.HIT else Sfx.TAP)
        side = -side
        moves = g.moves(side)
        status()
        if (moves.isEmpty()) {
            val vsAi = opt("mode") == 0
            val winner = -side
            val title = if (!vsAi) (if (winner == 1) "Đỏ thắng!" else "Đen thắng!") else if (winner == 1) "Bạn thắng!" else "Máy thắng!"
            gameOver(!vsAi || winner == 1, title, "Đối phương hết nước đi")
            return
        }
        if (opt("mode") == 0 && side == -1) aiMove()
    }

    private fun aiMove() {
        thinking = true
        val copy = g.copy()
        val depth = intArrayOf(1, 4, 7)[opt("level")]
        background({ Thread.sleep(300); copy.bestMove(-1, depth) }) { m ->
            thinking = false
            val real = m?.let { mm -> moves.firstOrNull { it.path == mm.path } }
            if (real != null) after(0.05f) { if (animT >= 1f) play(real) else after(0.4f) { play(real) } }
        }
    }

    override fun update(dt: Float) { animT = min(1f, animT + dt / (0.22f * ((anim?.path?.size ?: 2) - 1))) }

    private fun drawPiece(c: Canvas, p: Int, cx: Float, cy: Float, r: Float) {
        val col = if (p > 0) 0xFFE53950.toInt() else 0xFF2E3142.toInt()
        c.circle(cx, cy + r * 0.18f, r, Pal.shade(col, 0.5f))
        c.ball(cx, cy, r, col)
        c.ring(cx, cy, r * 0.7f, r * 0.08f, Pal.alpha(Pal.WHITE, 0.25f))
        if (abs(p) == 2) c.text("♛", cx, cy, r * 1.0f, Pal.YELLOW)
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.text(when { over -> ""; thinking -> "Máy đang nghĩ…"; side == 1 -> "Lượt Đỏ"; else -> "Lượt Đen" }, w / 2, 35 * dp, 18 * dp, Pal.TEXT)
        c.rrect(fit.x0 - 8 * dp, fit.y0 - 8 * dp, fit.right + 8 * dp, fit.bottom + 8 * dp, 12 * dp, 0xFF4A2F1D.toInt())
        val movable = if (!thinking && !over) moves.map { it.from }.toSet() else emptySet()
        for (sq in 0 until 64) {
            val r = sq / 8; val col = sq % 8
            val x = fit.x0 + col * cs; val y = fit.y0 + r * cs
            c.rect(x, y, x + cs, y + cs, if ((r + col) % 2 == 1) 0xFF7A5134.toInt() else 0xFFEED9B7.toInt())
            if (sq == sel) c.rect(x, y, x + cs, y + cs, Pal.alpha(Pal.GREEN, 0.45f))
        }
        val a = anim
        val animating = a != null && animT < 1f
        for (sq in 0 until 64) {
            val p = g.b[sq]
            if (p == 0 || (animating && sq == a!!.to)) continue
            drawPiece(c, p, fit.cx(sq % 8), fit.cy(sq / 8), cs * 0.38f)
            if (sq in movable && sel < 0 && (opt("mode") == 1 || side == 1)) c.ring(fit.cx(sq % 8), fit.cy(sq / 8), cs * 0.44f, 2 * dp, Pal.alpha(Pal.YELLOW, 0.7f))
        }
        if (animating) {
            val segs = a!!.path.size - 1
            val pos = animT * segs
            val k = min(segs - 1, pos.toInt())
            val e = pos - k
            val f = a.path[k]; val t = a.path[k + 1]
            val x = fit.cx(f % 8) + (fit.cx(t % 8) - fit.cx(f % 8)) * e
            val y = fit.cy(f / 8) + (fit.cy(t / 8) - fit.cy(f / 8)) * e - kotlin.math.sin(e * Math.PI).toFloat() * cs * 0.3f
            drawPiece(c, g.b[a.to], x, y, cs * 0.38f)
        }
        if (sel >= 0) for (m in moves) if (m.from == sel) {
            c.circle(fit.cx(m.to % 8), fit.cy(m.to / 8), cs * 0.16f, Pal.alpha(Pal.GREEN, 0.8f))
        }
    }
}
