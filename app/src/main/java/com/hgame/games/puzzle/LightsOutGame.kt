package com.hgame.games.puzzle

import android.graphics.Canvas
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
import kotlin.random.Random

class LightsOutGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(GameOption("size", "Kích thước", listOf("5 × 5", "4 × 4", "6 × 6")))
    override val help = "Chạm một ô để đảo trạng thái của nó và 4 ô kề bên (trên, dưới, trái, phải). Tắt hết đèn để thắng."

    private var n = 5
    private var on = BooleanArray(25)
    private var glow = FloatArray(25)
    private val fit = BoardFit()
    private var moves = 0
    private var level = 1

    override fun newGame() {
        n = intArrayOf(5, 4, 6)[opt("size")]
        level = host.loadInt("level$n", 1)
        on = BooleanArray(n * n); glow = FloatArray(n * n)
        // Scramble with random presses so the puzzle is always solvable.
        val presses = 3 + level * 2
        repeat(presses) { press(Random.nextInt(n * n)) }
        if (on.none { it }) press(n * n / 2)
        for (i in on.indices) glow[i] = if (on[i]) 1f else 0f
        moves = 0
        info("Màn $level · Bước 0")
    }

    override fun layout() {
        val pad = 20 * dp
        fit.fit(n, n, pad, pad + 40 * dp, w - pad, h - pad)
    }

    private fun press(i: Int) {
        val r = i / n; val c = i % n
        on[i] = !on[i]
        if (r > 0) on[i - n] = !on[i - n]
        if (r < n - 1) on[i + n] = !on[i + n]
        if (c > 0) on[i - 1] = !on[i - 1]
        if (c < n - 1) on[i + 1] = !on[i + 1]
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        press(r * n + c)
        moves++
        sfx(Sfx.CLICK)
        animate(0.3f)
        info("Màn $level · Bước $moves")
        if (on.none { it }) {
            host.saveInt("level$n", level + 1)
            gameOver(true, "Đã tắt hết đèn!", "Màn $level xong sau $moves bước")
        }
    }

    override fun update(dt: Float) {
        for (i in on.indices) {
            val t = if (on[i]) 1f else 0f
            glow[i] += (t - glow[i]) * minOf(1f, dt * 14f)
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF0E1026.toInt(), 0xFF05060F.toInt())
        c.text("Còn ${on.count { it }} đèn sáng", w / 2, 30 * dp, 18 * dp, Pal.MUTED)
        val cs = fit.cell
        val g = cs * 0.08f
        for (i in on.indices) {
            val x = fit.x0 + (i % n) * cs; val y = fit.y0 + (i / n) * cs
            val k = glow[i]
            if (k > 0.05f) c.circle(x + cs / 2, y + cs / 2, cs * 0.62f, Pal.alpha(Pal.YELLOW, 0.18f * k))
            c.rrect(x + g, y + g, x + cs - g, y + cs - g, cs * 0.2f, Pal.mix(0xFF22264A.toInt(), 0xFFFFD45C.toInt(), k))
            c.rrect(x + g * 2, y + g * 2, x + cs - g * 2, y + cs * 0.45f, cs * 0.15f, Pal.alpha(Pal.WHITE, 0.06f + 0.2f * k))
        }
    }
}
