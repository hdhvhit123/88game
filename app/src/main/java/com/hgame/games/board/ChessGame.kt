package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.Chess
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.min

class ChessGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Cầm quân Trắng", "Cầm quân Đen", "2 người")),
        GameOption("level", "Độ khó máy", listOf("Mới chơi", "Dễ", "Trung bình", "Khó"), 1),
    )
    override val help = "Cờ vua theo luật quốc tế: nhập thành, bắt tốt qua đường, phong cấp (tự động thành Hậu). " +
        "Chạm quân để xem các nước đi hợp lệ, chạm ô đích để đi. Chiếu hết vua đối phương để thắng. Nút ↶ để đi lại."

    private var chess = Chess()
    private val undoStack = ArrayList<Chess>()
    private val fit = BoardFit()
    private var sel = -1
    private var targets: List<Int> = emptyList()
    private var lastFrom = -1; private var lastTo = -1
    private var thinking = false
    private var flip = false
    private var animMove = 0; private var animT = 1f; private var animPiece = 0
    private val undoBtn = Btn("↶ Đi lại", Pal.SURFACE2)
    private val capturedW = ArrayList<Int>(); private val capturedB = ArrayList<Int>()

    private val humanWhite get() = opt("mode") != 1
    private val vsAi get() = opt("mode") != 2

    override fun newGame() {
        chess = Chess(); undoStack.clear()
        sel = -1; targets = emptyList(); lastFrom = -1; lastTo = -1; thinking = false; animT = 1f
        capturedW.clear(); capturedB.clear()
        flip = opt("mode") == 1
        status()
        if (vsAi && !humanWhite) aiMove()
    }

    override fun layout() {
        val pad = 8 * dp
        val size = min(w - pad * 2, h - 150 * dp)
        fit.fit(8, 8, (w - size) / 2, (h - size) / 2 - 10 * dp, (w + size) / 2, (h + size) / 2 - 10 * dp)
        undoBtn.setCenter(w / 2, h - 32 * dp, 150 * dp, 44 * dp)
    }

    private fun sqAt(x: Float, y: Float): Int {
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return -1
        return if (flip) r * 8 + (7 - c) else (7 - r) * 8 + c
    }

    private fun sqX(sq: Int) = fit.cx(if (flip) 7 - sq % 8 else sq % 8)
    private fun sqY(sq: Int) = fit.cy(if (flip) sq / 8 else 7 - sq / 8)

    private fun status() {
        val side = if (chess.white) "Trắng" else "Đen"
        info(when {
            thinking -> "Máy đang nghĩ…"
            chess.inCheck() -> "$side đang bị chiếu!"
            else -> "Lượt: $side"
        })
    }

    override fun up(x: Float, y: Float) {
        if (undoBtn.hit(x, y)) { undo(); return }
        if (over || thinking || animT < 1f) return
        if (vsAi && chess.white != humanWhite) return
        val sq = sqAt(x, y)
        if (sq < 0) return
        val p = chess.b[sq]
        if (sel >= 0 && sq in targets) {
            val m = chess.legalMoves().filter { Chess.from(it) == sel && Chess.dest(it) == sq }
                .maxByOrNull { Chess.promo(it) }!! // promote to queen (Q=5) when several
            doMove(m)
            if (!over && vsAi) aiMove()
            return
        }
        if (p != 0 && (p > 0) == chess.white) {
            sel = sq
            targets = chess.legalMoves().filter { Chess.from(it) == sq }.map { Chess.dest(it) }
            sfx(Sfx.CLICK)
        } else { sel = -1; targets = emptyList() }
        redraw()
    }

    private fun undo() {
        if (thinking || undoStack.isEmpty()) return
        chess = undoStack.removeAt(undoStack.size - 1)
        if (vsAi) while (chess.white != humanWhite && undoStack.isNotEmpty()) chess = undoStack.removeAt(undoStack.size - 1)
        sel = -1; targets = emptyList(); lastFrom = -1; lastTo = -1; animT = 1f
        recountCaptures()
        if (over) resumePlay()
        sfx(Sfx.TAP); status(); redraw()
        if (vsAi && chess.white != humanWhite) aiMove()
    }

    private fun recountCaptures() {
        capturedW.clear(); capturedB.clear()
        val start = intArrayOf(0, 8, 2, 2, 2, 1, 1)
        for (t in 1..5) {
            val wc = chess.b.count { it == t }; val bc = chess.b.count { it == -t }
            repeat(maxOf(0, start[t] - wc)) { capturedW.add(t) }
            repeat(maxOf(0, start[t] - bc)) { capturedB.add(t) }
        }
    }

    private fun doMove(m: Int) {
        undoStack.add(chess.copy())
        animPiece = chess.b[Chess.from(m)]
        val cap = chess.play(m)
        if (cap != 0) (if (cap > 0) capturedW else capturedB).add(abs(cap))
        lastFrom = Chess.from(m); lastTo = Chess.dest(m)
        animMove = m; animT = 0f; animate(0.3f)
        sel = -1; targets = emptyList()
        sfx(if (cap != 0) Sfx.HIT else Sfx.TAP)
        val r = chess.result()
        if (r != 0) {
            val title = when (r) {
                3 -> "Hoà cờ"
                1 -> if (!vsAi) "Trắng thắng!" else if (humanWhite) "Chiếu hết! Bạn thắng" else "Máy thắng"
                else -> if (!vsAi) "Đen thắng!" else if (!humanWhite) "Chiếu hết! Bạn thắng" else "Máy thắng"
            }
            val win = r != 3 && (!vsAi || (r == 1) == humanWhite)
            gameOver(win, title, if (r == 3) "Không còn nước thắng" else "Chiếu hết")
        } else if (chess.inCheck()) sfx(Sfx.ERROR)
        status()
    }

    private fun aiMove() {
        thinking = true; status()
        val pos = chess.copy()
        val (depth, ms, rnd) = when (opt("level")) { 0 -> Triple(1, 300L, 120); 1 -> Triple(2, 700L, 40); 2 -> Triple(3, 1500L, 10); else -> Triple(5, 3000L, 0) }
        background({ pos.bestMove(depth, ms, rnd) }) { m ->
            thinking = false
            if (m != 0) doMove(m)
            status()
        }
    }

    override fun update(dt: Float) { animT = min(1f, animT + dt / 0.18f) }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.rrect(fit.x0 - 6 * dp, fit.y0 - 6 * dp, fit.right + 6 * dp, fit.bottom + 6 * dp, 10 * dp, 0xFF5A3E2B.toInt())
        val kingInCheck = if (chess.inCheck()) chess.kingSquare(chess.white) else -1
        for (sq in 0 until 64) {
            val x = sqX(sq) - cs / 2; val y = sqY(sq) - cs / 2
            val light = (sq / 8 + sq % 8) % 2 == 1
            var col = if (light) 0xFFF0D9B5.toInt() else 0xFFB58863.toInt()
            if (sq == lastFrom || sq == lastTo) col = Pal.mix(col, 0xFFF6E05E.toInt(), 0.5f)
            if (sq == sel) col = Pal.mix(col, 0xFF7FD36B.toInt(), 0.6f)
            c.rect(x, y, x + cs, y + cs, col)
            if (sq == kingInCheck) c.circle(x + cs / 2, y + cs / 2, cs * 0.48f, Pal.alpha(Pal.RED, 0.6f))
        }
        // coordinates
        for (i in 0 until 8) {
            val file = if (flip) 7 - i else i
            c.text(('a' + file).toString(), fit.x0 + (i + 0.9f) * cs, fit.bottom - cs * 0.12f, cs * 0.17f, 0xFF6B4A33.toInt())
            val rank = if (flip) i + 1 else 8 - i
            c.text(rank.toString(), fit.x0 + cs * 0.1f, fit.y0 + (i + 0.15f) * cs, cs * 0.17f, 0xFF6B4A33.toInt())
        }
        val animTo = if (animT < 1f) Chess.dest(animMove) else -1
        for (sq in 0 until 64) {
            val p = chess.b[sq]
            if (p == 0 || sq == animTo) continue
            ChessPieces.draw(c, abs(p), p > 0, sqX(sq), sqY(sq), cs * 0.9f)
        }
        if (animTo >= 0) {
            val f = Chess.from(animMove)
            val e = 1 - (1 - animT) * (1 - animT)
            val x = sqX(f) + (sqX(animTo) - sqX(f)) * e
            val y = sqY(f) + (sqY(animTo) - sqY(f)) * e
            val p = chess.b[animTo]
            ChessPieces.draw(c, abs(p), p > 0, x, y, cs * 0.9f)
        }
        for (t in targets) {
            if (chess.b[t] != 0) c.ring(sqX(t), sqY(t), cs * 0.44f, cs * 0.07f, Pal.alpha(0xFF1E5631.toInt(), 0.6f))
            else c.circle(sqX(t), sqY(t), cs * 0.15f, Pal.alpha(0xFF1E5631.toInt(), 0.5f))
        }
        // captured pieces rows
        val topCaps = if (flip) capturedB else capturedW
        val botCaps = if (flip) capturedW else capturedB
        val small = cs * 0.45f
        topCaps.sortedDescending().forEachIndexed { i, t -> ChessPieces.draw(c, t, !flip, fit.x0 + small * (i * 0.6f + 0.5f), fit.y0 - small * 0.75f, small) }
        botCaps.sortedDescending().forEachIndexed { i, t -> ChessPieces.draw(c, t, flip, fit.x0 + small * (i * 0.6f + 0.5f), fit.bottom + small * 0.75f, small) }
        undoBtn.enabled = undoStack.isNotEmpty() && !thinking
        undoBtn.draw(c, 15 * dp)
    }
}
