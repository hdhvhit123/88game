package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Binary
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class BinaryPuzzleGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("size", "Kích thước", listOf("6 × 6", "8 × 8", "10 × 10")))
    override val help = "Điền 0 (xanh) hoặc 1 (cam) vào mọi ô. Không được có 3 ô cùng số liền nhau, mỗi hàng/cột có số 0 và 1 bằng nhau, " +
        "và không có hai hàng (hoặc hai cột) giống hệt nhau. Chạm để đổi: trống → 0 → 1 → trống."

    private var b = Binary(6)
    private var grid = IntArray(36)
    private val fit = BoardFit()

    override fun newGame() {
        val n = intArrayOf(6, 8, 10)[opt("size")]
        b = Binary(n); b.generate(Random.Default)
        grid = b.givens.copyOf()
        startClock(); score(0)
    }

    override fun layout() {
        val size = min(w, h) - 28 * dp
        fit.fit(b.n, b.n, (w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2)
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        val i = r * b.n + c
        if (b.givens[i] >= 0) return
        grid[i] = when (grid[i]) { -1 -> 0; 0 -> 1; else -> -1 }
        sfx(Sfx.CLICK)
        if (grid.all { it >= 0 } && grid.indices.all { b.valid(grid, it) }) gameOver(true, "Hoàn thành!", "", clockSeconds)
        redraw()
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        for (i in grid.indices) {
            val cx = fit.cx(i % b.n); val cy = fit.cy(i / b.n)
            val v = grid[i]
            val given = b.givens[i] >= 0
            val bad = v >= 0 && !b.valid(grid, i)
            val col = when (v) { 0 -> 0xFF3D7BFF.toInt(); 1 -> 0xFFFF9F43.toInt(); else -> 0xFF2A2F55.toInt() }
            c.rrect(cx - cs * 0.45f, cy - cs * 0.45f, cx + cs * 0.45f, cy + cs * 0.45f, cs * 0.18f, if (given) Pal.shade(col, 0.8f) else col)
            if (v >= 0) c.text(v.toString(), cx, cy, cs * 0.5f, if (bad) Pal.RED else Pal.WHITE, bold = given)
            if (given) c.circle(cx + cs * 0.3f, cy - cs * 0.3f, cs * 0.05f, Pal.alpha(Pal.WHITE, 0.6f))
        }
    }
}
