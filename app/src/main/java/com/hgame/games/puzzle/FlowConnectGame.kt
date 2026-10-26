package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.FlowGen
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class FlowConnectGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("size", "Kích thước", listOf("5 × 5", "6 × 6", "7 × 7", "8 × 8"), 1))
    override val help = "Kéo từ một chấm màu để vẽ đường nối tới chấm cùng màu. Các đường không được cắt nhau. " +
        "Nối tất cả các cặp và phủ kín toàn bộ bảng để hoàn thành."

    private val colors = intArrayOf(Pal.RED, Pal.BLUE, Pal.GREEN, Pal.YELLOW, Pal.ORANGE, Pal.CYAN, Pal.PINK, Pal.PURPLE,
        0xFF8BC34A.toInt(), 0xFF795548.toInt(), 0xFFE0E0E0.toInt(), 0xFF3F51B5.toInt(), 0xFF009688.toInt(), 0xFFCDDC39.toInt(),
        0xFFFF7043.toInt(), 0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(), 0xFFFFEB3B.toInt())
    private lateinit var gen: FlowGen
    private val fit = BoardFit()
    private var paths = ArrayList<ArrayList<Int>>()
    private var endOf = IntArray(0)
    private var active = -1
    private var n = 6

    override fun newGame() {
        n = intArrayOf(5, 6, 7, 8)[opt("size")]
        do { gen = FlowGen(n, Random.Default) } while (gen.ends.size > colors.size || gen.ends.size < n - 2)
        paths = ArrayList(gen.ends.map { ArrayList<Int>() })
        endOf = IntArray(n * n) { -1 }
        gen.ends.forEachIndexed { k, (a, b) -> endOf[a] = k; endOf[b] = k }
        active = -1
        startClock(); score(0)
        status()
    }

    override fun layout() {
        val size = min(w, h) - 24 * dp
        fit.fit(n, n, (w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2)
    }

    private fun status() {
        val done = paths.indices.count { connected(it) }
        val filled = paths.sumOf { it.size }
        info("Nối $done/${paths.size} · Phủ ${filled * 100 / (n * n)}%")
    }

    private fun connected(k: Int): Boolean {
        val p = paths[k]
        if (p.size < 2) return false
        val (a, b) = gen.ends[k]
        return (p.first() == a && p.last() == b) || (p.first() == b && p.last() == a)
    }

    private fun owner(cell: Int): Int = paths.indexOfFirst { cell in it }

    private fun cellAt(x: Float, y: Float): Int {
        val c = fit.colAt(x); val r = fit.rowAt(y)
        return if (c < 0 || r < 0) -1 else r * n + c
    }

    override fun down(x: Float, y: Float) {
        if (over) return
        val cell = cellAt(x, y)
        if (cell < 0) return
        val e = endOf[cell]
        if (e >= 0) {
            active = e; paths[e].clear(); paths[e].add(cell)
        } else {
            val o = owner(cell)
            if (o < 0) return
            active = o
            val p = paths[o]
            val idx = p.indexOf(cell)
            while (p.size > idx + 1) p.removeAt(p.size - 1)
        }
        sfx(Sfx.TICK)
        redraw()
    }

    override fun move(x: Float, y: Float) {
        if (active < 0 || over) return
        val cell = cellAt(x, y)
        val p = paths[active]
        if (cell < 0 || p.isEmpty() || cell == p.last()) return
        val last = p.last()
        // only orthogonal neighbours
        if (abs(cell / n - last / n) + abs(cell % n - last % n) != 1) return
        val idx = p.indexOf(cell)
        if (idx >= 0) { while (p.size > idx + 1) p.removeAt(p.size - 1); redraw(); return }
        if (connected(active)) return
        val e = endOf[cell]
        if (e >= 0 && e != active) return
        // cut through other flows
        val o = owner(cell)
        if (o >= 0 && o != active) { val op = paths[o]; val i2 = op.indexOf(cell); while (op.size > i2) op.removeAt(op.size - 1) }
        p.add(cell)
        if (connected(active)) sfx(Sfx.POP)
        status()
        redraw()
    }

    override fun up(x: Float, y: Float) {
        if (active < 0) return
        active = -1
        status()
        if (paths.indices.all { connected(it) }) {
            val filled = paths.sumOf { it.size }
            if (filled == n * n) gameOver(true, "Hoàn hảo!", "Đã nối và phủ kín bảng", clockSeconds)
            else info("Đã nối hết, hãy phủ kín các ô trống!")
        }
        redraw()
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.rrect(fit.x0 - 4 * dp, fit.y0 - 4 * dp, fit.right + 4 * dp, fit.bottom + 4 * dp, 10 * dp, Pal.LINE)
        c.rect(fit.x0, fit.y0, fit.right, fit.bottom, 0xFF12152B.toInt())
        for (k in paths.indices) for (cell in paths[k]) {
            val x = fit.x0 + (cell % n) * cs; val y = fit.y0 + (cell / n) * cs
            c.rect(x, y, x + cs, y + cs, Pal.alpha(colors[k], 0.18f))
        }
        for (k in 1 until n) {
            c.line(fit.x0 + k * cs, fit.y0, fit.x0 + k * cs, fit.bottom, 1f * dp, 0xFF2A2F55.toInt())
            c.line(fit.x0, fit.y0 + k * cs, fit.right, fit.y0 + k * cs, 1f * dp, 0xFF2A2F55.toInt())
        }
        for (k in paths.indices) {
            val p = paths[k]
            for (i in 0 until p.size - 1) c.line(fit.cx(p[i] % n), fit.cy(p[i] / n), fit.cx(p[i + 1] % n), fit.cy(p[i + 1] / n), cs * 0.3f, colors[k])
        }
        gen.ends.forEachIndexed { k, (a, b) ->
            for (e in intArrayOf(a, b)) {
                c.circle(fit.cx(e % n), fit.cy(e / n), cs * 0.36f, colors[k])
                if (connected(k)) c.circle(fit.cx(e % n), fit.cy(e / n), cs * 0.14f, Pal.alpha(Pal.WHITE, 0.7f))
            }
        }
    }
}
