package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Board2048
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.Swipe
import com.hgame.engine.background
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min

class Game2048 : Game() {
    override val options = listOf(GameOption("size", "Kích thước", listOf("4 × 4", "5 × 5", "6 × 6", "3 × 3")))
    override val help = "Vuốt để trượt tất cả các ô. Hai ô cùng số chạm nhau sẽ gộp lại thành một. " +
        "Hãy tạo ra ô 2048! Trò chơi kết thúc khi không còn nước đi."

    private lateinit var board: Board2048
    private val fit = BoardFit()
    private lateinit var swipe: Swipe
    private var t = 1f          // slide animation progress 0..1
    private var won = false

    override fun newGame() {
        val n = intArrayOf(4, 5, 6, 3)[opt("size")]
        board = Board2048(n)
        board.reset()
        won = false
        t = 1f
        score(0)
        animate()
    }

    override fun layout() {
        swipe = Swipe(24 * dp)
        val pad = 16 * dp
        val size = min(w - pad * 2, h - pad * 2)
        fit.fit(board.n, board.n, (w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2)
    }

    override fun update(dt: Float) { t = min(1f, t + dt / 0.11f) }

    override fun down(x: Float, y: Float) = swipe.down(x, y)
    override fun move(x: Float, y: Float) { val d = swipe.move(x, y); if (d >= 0) { swipe.active = false; doMove(d) } }
    override fun up(x: Float, y: Float) { val d = swipe.up(x, y); if (d >= 0) doMove(d) }

    private fun doMove(dir: Int) {
        if (over) return
        if (!board.move(dir)) return
        t = 0f
        animate(0.3f)
        sfx(if (board.absorbed.isNotEmpty()) Sfx.MERGE else Sfx.TAP)
        score(board.score)
        if (!won && board.maxTile() >= 2048) {
            won = true
            info("Bạn đã đạt 2048! Tiếp tục để lấy điểm cao hơn")
            sfx(Sfx.WIN)
        }
        if (!board.canMove()) gameOver(won, if (won) "Tuyệt vời!" else "Hết nước đi", "Ô lớn nhất: ${board.maxTile()}", board.score)
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val gap = fit.cell * 0.1f
        val rad = fit.cell * 0.14f
        c.rrect(fit.x0 - gap, fit.y0 - gap, fit.right + gap, fit.bottom + gap, rad * 1.4f, 0xFF2A2F55.toInt())
        for (r in 0 until board.n) for (col in 0 until board.n) {
            val x = fit.x0 + col * fit.cell; val y = fit.y0 + r * fit.cell
            c.rrect(x + gap / 2, y + gap / 2, x + fit.cell - gap / 2, y + fit.cell - gap / 2, rad, 0xFF353B69.toInt())
        }
        val e = 1 - (1 - t) * (1 - t)
        if (t < 1f) for (tile in board.absorbed) drawTile(c, tile, e, gap, rad, tile.value)
        for (tile in board.tiles()) drawTile(c, tile, e, gap, rad, tile.value)
    }

    private fun drawTile(c: Canvas, tile: Board2048.Tile, e: Float, gap: Float, rad: Float, value: Int) {
        val col = tile.pc + (tile.c - tile.pc) * e
        val row = tile.pr + (tile.r - tile.pr) * e
        var scale = 1f
        if (tile.isNew) scale = if (t < 1f) e else 1f
        if (tile.merged && t < 1f) scale = 1f + 0.15f * (1 - kotlin.math.abs(2 * e - 1))
        val cx = fit.x0 + (col + 0.5f) * fit.cell
        val cy = fit.y0 + (row + 0.5f) * fit.cell
        val half = (fit.cell - gap) / 2 * scale
        val color = colorOf(value)
        c.rrect(cx - half, cy - half + 3 * dp, cx + half, cy + half + 3 * dp, rad, Pal.shade(color, 0.6f))
        c.rrect(cx - half, cy - half, cx + half, cy + half, rad, color)
        val s = value.toString()
        val size = fit.cell * when (s.length) { 1, 2 -> 0.42f; 3 -> 0.34f; 4 -> 0.27f; else -> 0.22f } * scale
        c.text(s, cx, cy, size, if (value <= 4) 0xFF3B3F5C.toInt() else Pal.WHITE)
    }

    private fun colorOf(v: Int): Int = when (v) {
        2 -> 0xFFEDEBFF.toInt(); 4 -> 0xFFD9D2FF.toInt(); 8 -> 0xFFFFA45C.toInt(); 16 -> 0xFFFF8547.toInt()
        32 -> 0xFFFF6B6B.toInt(); 64 -> 0xFFFF4D6D.toInt(); 128 -> 0xFFFFD166.toInt(); 256 -> 0xFFFFC23D.toInt()
        512 -> 0xFF5EE2A0.toInt(); 1024 -> 0xFF35C9E8.toInt(); 2048 -> 0xFF8A5CFF.toInt()
        else -> 0xFF2E2A4F.toInt()
    }
}
