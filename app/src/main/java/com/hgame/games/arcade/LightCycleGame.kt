package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.Swipe
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.text
import kotlin.random.Random

class LightCycleGame : Game() {
    override val realtime = true
    override val options = listOf(GameOption("bots", "Đối thủ", listOf("1 máy", "2 máy", "3 máy"), 1))
    override val help = "Mô tô ánh sáng để lại vệt tường phía sau. Vuốt để rẽ. Ép đối thủ đâm vào tường còn bạn sống sót cuối cùng thì thắng."

    private val cols = 40; private val rows = 56
    private val grid = IntArray(cols * rows)   // 0 empty, k = owner+1
    private class Rider(var r: Int, var c: Int, var dir: Int, val color: Int, var alive: Boolean = true)
    private val riders = ArrayList<Rider>()
    private val fit = BoardFit(); private val swipe = Swipe(14f); private val fx = Particles()
    private var acc = 0f; private var wins = 0; private var started = false
    private var pending = -1

    override fun newGame() {
        grid.fill(0); riders.clear(); fx.clear(); acc = 0f; started = false; pending = -1
        riders.add(Rider(rows - 8, cols / 2, 0, Pal.CYAN))
        val spots = listOf(Triple(7, cols / 2, 2), Triple(rows / 2, 5, 1), Triple(rows / 2, cols - 6, 3))
        for (i in 0 until opt("bots") + 1) { val s = spots[i]; riders.add(Rider(s.first, s.second, s.third, intArrayOf(Pal.ORANGE, Pal.PINK, Pal.GREEN)[i])) }
        riders.forEachIndexed { i, r -> grid[r.r * cols + r.c] = i + 1 }
        wins = host.loadInt("wins", 0); info("Thắng: $wins · Vuốt để bắt đầu")
    }

    override fun layout() { fit.fit(cols, rows, 6 * dp, 6 * dp, w - 6 * dp, h - 6 * dp) }

    override fun down(x: Float, y: Float) = swipe.down(x, y)
    override fun move(x: Float, y: Float) { val d = swipe.move(x, y); if (d >= 0) steer(d) }
    override fun up(x: Float, y: Float) { val d = swipe.up(x, y); if (d >= 0) steer(d) }

    private fun steer(d: Int) {
        if (!started) { started = true; info("Thắng: $wins") }
        val me = riders[0]
        if (d != (me.dir + 2) % 4) pending = d
    }

    private fun free(r: Int, c: Int) = r in 0 until rows && c in 0 until cols && grid[r * cols + c] == 0
    private fun ahead(r: Int, c: Int, d: Int) = when (d) { 0 -> r - 1 to c; 1 -> r to c + 1; 2 -> r + 1 to c; else -> r to c - 1 }

    private fun space(r0: Int, c0: Int, limit: Int): Int {
        if (!free(r0, c0)) return 0
        val seen = HashSet<Int>(); val q = ArrayDeque<Int>(); q.add(r0 * cols + c0); seen.add(r0 * cols + c0)
        while (q.isNotEmpty() && seen.size < limit) {
            val p = q.removeFirst(); val r = p / cols; val c = p % cols
            for (d in 0..3) { val (nr, nc) = ahead(r, c, d); val k = nr * cols + nc; if (free(nr, nc) && seen.add(k)) q.add(k) }
        }
        return seen.size
    }

    private fun botTurn(b: Rider) {
        val opts = listOf(b.dir, (b.dir + 1) % 4, (b.dir + 3) % 4)
        val scored = opts.map { d -> val (r, c) = ahead(b.r, b.c, d); d to space(r, c, 300) + (if (d == b.dir) 4 else 0) + Random.nextInt(3) }
        val best = scored.maxByOrNull { it.second }!!
        if (best.second > 0) b.dir = best.first
    }

    override fun update(dt: Float) {
        fx.update(dt)
        if (!started) return
        acc += dt
        while (acc > 0.05f && !over) { acc -= 0.05f; step() }
    }

    private fun step() {
        if (pending >= 0) { riders[0].dir = pending; pending = -1 }
        riders.drop(1).filter { it.alive }.forEach { if (Random.nextFloat() < 0.95f) botTurn(it) }
        val next = riders.map { if (it.alive) ahead(it.r, it.c, it.dir) else null }
        riders.forEachIndexed { i, rd ->
            val n = next[i] ?: return@forEachIndexed
            val clash = next.withIndex().any { (j, o) -> j != i && o == n && riders[j].alive }
            if (!free(n.first, n.second) || clash) {
                rd.alive = false; sfx(if (i == 0) Sfx.BOOM else Sfx.HIT)
                fx.burst(fit.cx(rd.c), fit.cy(rd.r), rd.color, 24, 200 * dp, 3 * dp)
            }
        }
        riders.forEachIndexed { i, rd -> if (rd.alive) { val n = next[i]!!; rd.r = n.first; rd.c = n.second; grid[rd.r * cols + rd.c] = i + 1 } }
        if (!riders[0].alive) { buzz(80); gameOver(false, "Bạn đã đâm!", "Số trận thắng: $wins", wins) }
        else if (riders.drop(1).none { it.alive }) {
            wins++; host.saveInt("wins", wins); sfx(Sfx.WIN)
            gameOver(true, "Bạn thắng!", "Số trận thắng: $wins", wins)
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF050816.toInt(), 0xFF0B1030.toInt())
        val cs = fit.cell
        c.rect(fit.x0, fit.y0, fit.right, fit.bottom, 0xFF080C22.toInt())
        for (i in 0..cols step 4) c.rect(fit.x0 + i * cs - 0.5f, fit.y0, fit.x0 + i * cs + 0.5f, fit.bottom, 0xFF1C2350.toInt())
        for (i in 0..rows step 4) c.rect(fit.x0, fit.y0 + i * cs - 0.5f, fit.right, fit.y0 + i * cs + 0.5f, 0xFF1C2350.toInt())
        for (i in grid.indices) if (grid[i] > 0) {
            val rd = riders[grid[i] - 1]
            val x = fit.x0 + (i % cols) * cs; val y = fit.y0 + (i / cols) * cs
            c.rect(x, y, x + cs, y + cs, if (rd.alive) Pal.alpha(rd.color, 0.85f) else Pal.alpha(rd.color, 0.3f))
        }
        for (rd in riders) if (rd.alive) { c.circle(fit.cx(rd.c), fit.cy(rd.r), cs * 1.4f, Pal.alpha(rd.color, 0.3f)); c.circle(fit.cx(rd.c), fit.cy(rd.r), cs * 0.7f, Pal.WHITE) }
        fx.draw(c)
        if (!started && !over) c.text("Vuốt để xuất phát", w / 2, h * 0.55f, 20 * dp, Pal.CYAN)
    }
}
