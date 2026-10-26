package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Pipes
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
import kotlin.math.min
import kotlin.random.Random

class PipesGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("size", "Kích thước", listOf("5 × 5", "7 × 7", "9 × 9"), 1))
    override val help = "Chạm vào ô để xoay ống 90°. Nối tất cả các ống thành một mạng duy nhất từ nguồn ở giữa, " +
        "không để đầu ống nào hở. Ống đã nối với nguồn sẽ sáng lên."

    private lateinit var p: Pipes
    private val fit = BoardFit()
    private var rot = FloatArray(0)    // visual rotation lag in quarter turns
    private var moves = 0

    override fun newGame() {
        val n = intArrayOf(5, 7, 9)[opt("size")]
        do { p = Pipes(n); p.scramble(Random.Default) } while (p.solved())
        rot = FloatArray(n * n); moves = 0
        startClock(); score(0); info("Bước: 0")
    }

    override fun layout() {
        val size = min(w, h) - 24 * dp
        fit.fit(p.n, p.n, (w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2)
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        val i = r * p.n + c
        p.rotate(i); rot[i] -= 1f; moves++
        info("Bước: $moves")
        sfx(Sfx.TICK)
        animate(0.3f)
        if (p.solved()) gameOver(true, "Mạng đã thông!", "$moves lần xoay", clockSeconds)
    }

    override fun update(dt: Float) { for (i in rot.indices) if (rot[i] != 0f) { rot[i] = min(0f, rot[i] + dt * 9f) } }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        val on = p.connected()
        c.rrect(fit.x0 - 6 * dp, fit.y0 - 6 * dp, fit.right + 6 * dp, fit.bottom + 6 * dp, 12 * dp, Pal.SURFACE)
        for (i in 0 until p.n * p.n) {
            val cx = fit.cx(i % p.n); val cy = fit.cy(i / p.n)
            c.rrect(cx - cs * 0.46f, cy - cs * 0.46f, cx + cs * 0.46f, cy + cs * 0.46f, cs * 0.12f, 0xFF242A50.toInt())
            val col = if (on[i]) Pal.CYAN else 0xFF6B7299.toInt()
            c.save()
            c.rotate(rot[i] * 90f, cx, cy)
            val t = p.tiles[i]
            val lw = cs * 0.2f
            for (d in 0..3) if (t and (1 shl d) != 0) {
                val (dx, dy) = when (d) { 0 -> 0f to -0.5f; 1 -> 0.5f to 0f; 2 -> 0f to 0.5f; else -> -0.5f to 0f }
                c.line(cx, cy, cx + dx * cs, cy + dy * cs, lw, col)
            }
            if (Integer.bitCount(t) == 1) c.circle(cx, cy, cs * 0.2f, if (on[i]) Pal.YELLOW else 0xFF8A90B3.toInt())
            else c.circle(cx, cy, lw / 2, col)
            c.restore()
            if (i == p.source) { c.circle(cx, cy, cs * 0.26f, Pal.ORANGE); c.circle(cx, cy, cs * 0.14f, Pal.YELLOW) }
        }
    }
}
