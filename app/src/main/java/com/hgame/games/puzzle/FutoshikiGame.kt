package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Futoshiki
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class FutoshikiGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("size", "Kích thước", listOf("4 × 4", "5 × 5", "6 × 6"), 1))
    override val help = "Điền các số từ 1 đến N sao cho mỗi hàng và mỗi cột không có số lặp lại, " +
        "đồng thời thoả mãn các dấu < và > giữa các ô kề nhau. Ô sai sẽ chuyển đỏ."

    private var f = Futoshiki(5)
    private var grid = IntArray(25)
    private var sel = -1
    private val fit = BoardFit()
    private var numBtns = listOf<Btn>()
    private val eraseBtn = Btn("⌫", Pal.SURFACE2)

    override fun newGame() {
        val n = intArrayOf(4, 5, 6)[opt("size")]
        f = Futoshiki(n)
        f.generate(Random.Default, 0.35f, n / 2)
        grid = f.givens.copyOf(); sel = -1
        numBtns = (1..n).map { Btn(it.toString(), Pal.SURFACE2) }
        startClock(); score(0)
    }

    override fun layout() {
        val n = f.n
        val size = min(w - 30 * dp, h - 170 * dp)
        // each cell is followed by a gap for the sign: use 2n-1 slots, signs are 0.5 cell
        fit.fit(n, n, (w - size) / 2, 20 * dp, (w + size) / 2, 20 * dp + size)
        val top = fit.bottom + 24 * dp
        val bw = min(56 * dp, (w - 30 * dp) / (n + 1))
        val total = bw * (n + 1)
        numBtns.forEachIndexed { i, b -> b.set((w - total) / 2 + i * bw + 3 * dp, top, (w - total) / 2 + (i + 1) * bw - 3 * dp, top + bw) }
        eraseBtn.set((w - total) / 2 + n * bw + 3 * dp, top, (w - total) / 2 + (n + 1) * bw - 3 * dp, top + bw)
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c >= 0 && r >= 0) {
            val i = r * f.n + c
            if (f.givens[i] == 0) { sel = i; sfx(Sfx.CLICK) }
            redraw(); return
        }
        if (sel < 0) return
        numBtns.forEachIndexed { k, b -> if (b.hit(x, y)) { grid[sel] = k + 1; sfx(Sfx.TAP); check(); redraw(); return } }
        if (eraseBtn.hit(x, y)) { grid[sel] = 0; redraw() }
    }

    private fun check() {
        if (grid.all { it != 0 } && grid.indices.all { conflictFree(it) }) gameOver(true, "Hoàn thành!", "", clockSeconds)
    }

    private fun conflictFree(i: Int): Boolean {
        val v = grid[i]
        if (v == 0) return true
        grid[i] = 0
        val ok = f.ok(grid, i, v)
        grid[i] = v
        return ok
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val n = f.n
        val cs = fit.cell
        val box = cs * 0.68f
        for (i in 0 until n * n) {
            val cx = fit.cx(i % n); val cy = fit.cy(i / n)
            val given = f.givens[i] != 0
            val bad = !conflictFree(i)
            val col = when { i == sel -> 0xFF5B47D6.toInt(); given -> 0xFF2A2F55.toInt(); else -> 0xFF353B69.toInt() }
            c.rrect(cx - box / 2, cy - box / 2, cx + box / 2, cy + box / 2, box * 0.18f, col)
            if (grid[i] != 0) c.text(grid[i].toString(), cx, cy, box * 0.55f, if (bad) Pal.RED else if (given) Pal.TEXT else Pal.CYAN, bold = given)
        }
        for (r in 0 until n) for (col in 0 until n - 1) {
            val s = f.hSign[r * (n - 1) + col]
            if (s != 0) c.text(if (s == 1) "<" else ">", fit.x0 + (col + 1) * cs, fit.cy(r), cs * 0.32f, Pal.YELLOW)
        }
        for (r in 0 until n - 1) for (col in 0 until n) {
            val s = f.vSign[r * n + col]
            if (s != 0) {
                c.save(); c.rotate(90f, fit.cx(col), fit.y0 + (r + 1) * cs)
                c.text(if (s == 1) "<" else ">", fit.cx(col), fit.y0 + (r + 1) * cs, cs * 0.32f, Pal.YELLOW)
                c.restore()
            }
        }
        numBtns.forEach { it.draw(c, (it.b - it.t) * 0.45f) }
        eraseBtn.draw(c, (eraseBtn.b - eraseBtn.t) * 0.4f)
    }
}
