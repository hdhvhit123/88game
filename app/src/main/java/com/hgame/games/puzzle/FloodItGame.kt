package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.FloodIt
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.rect
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min

class FloodItGame : Game() {
    override val bestMode = BestMode.HIGH
    override val options = listOf(GameOption("size", "Kích thước", listOf("14 × 14", "10 × 10", "18 × 18")))
    override val help = "Bắt đầu từ ô góc trên bên trái. Mỗi lần chọn một màu, cả vùng đã chiếm sẽ đổi sang màu đó " +
        "và nuốt các ô cùng màu kề bên. Phủ kín bảng bằng một màu trong số bước cho phép."

    private val palette = intArrayOf(Pal.RED, Pal.BLUE, Pal.GREEN, Pal.YELLOW, Pal.PURPLE, Pal.ORANGE)
    private lateinit var f: FloodIt
    private val fit = BoardFit()
    private var moves = 0
    private var limit = 25
    private val btnX = FloatArray(6)
    private var btnY = 0f
    private var btnR = 0f
    private var wave = 1f

    override fun newGame() {
        val n = intArrayOf(14, 10, 18)[opt("size")]
        f = FloodIt(n, 6)
        limit = f.greedyMoves()
        moves = 0
        info("Bước: 0/$limit")
    }

    override fun layout() {
        val pad = 14 * dp
        val size = min(w - pad * 2, h - 120 * dp)
        fit.fit(f.n, f.n, (w - size) / 2, pad, (w + size) / 2, pad + size)
        btnR = min(w / 15f, 28 * dp)
        btnY = min(h - btnR - 20 * dp, fit.bottom + 30 * dp + btnR)
        for (i in 0 until 6) btnX[i] = w * (i + 0.5f) / 6
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        for (i in 0 until 6) {
            val dx = x - btnX[i]; val dy = y - btnY
            if (dx * dx + dy * dy < btnR * btnR * 1.6f) { choose(i); return }
        }
        // tapping a board cell picks its colour too
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c >= 0 && r >= 0) choose(f.cells[r * f.n + c])
    }

    private fun choose(col: Int) {
        if (!f.flood(col)) return
        moves++
        sfx(Sfx.POP)
        wave = 0f; animate(0.4f)
        info("Bước: $moves/$limit")
        if (f.solved()) {
            score((limit - moves + 1) * 10)
            gameOver(true, "Phủ kín!", "Dùng $moves/$limit bước")
        } else if (moves >= limit) {
            gameOver(false, "Hết bước", "Còn ${f.n * f.n - f.region().size} ô chưa phủ")
        }
    }

    override fun update(dt: Float) { wave = min(1f, wave + dt * 3) }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.rrect(fit.x0 - 6 * dp, fit.y0 - 6 * dp, fit.right + 6 * dp, fit.bottom + 6 * dp, 12 * dp, Pal.SURFACE)
        val region = if (wave < 1f) f.region() else emptySet()
        for (i in f.cells.indices) {
            val x = fit.x0 + (i % f.n) * cs; val y = fit.y0 + (i / f.n) * cs
            var col = palette[f.cells[i]]
            if (i in region) col = Pal.tint(col, 0.35f * (1 - wave))
            c.rect(x, y, x + cs + 0.5f, y + cs + 0.5f, col)
        }
        for (i in 0 until 6) {
            c.ball(btnX[i], btnY, btnR, palette[i])
            if (i == f.cells[0]) c.ring(btnX[i], btnY, btnR + 5 * dp, 3 * dp, Pal.WHITE)
        }
        c.text("Chọn màu", w / 2, btnY + btnR + 16 * dp, 12 * dp, Pal.MUTED, bold = false)
    }
}
