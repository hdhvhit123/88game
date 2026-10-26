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
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.random.Random

class SnakeGame : Game() {
    override val realtime = true
    override val options = listOf(
        GameOption("speed", "Tốc độ", listOf("Chậm", "Vừa", "Nhanh"), 1),
        GameOption("walls", "Tường", listOf("Xuyên tường", "Đâm tường là thua"), 0),
    )
    override val help = "Vuốt để đổi hướng rắn. Ăn táo để dài ra và ghi điểm, táo vàng ⭐ cho 5 điểm nhưng biến mất nhanh. " +
        "Đừng cắn vào thân mình!"

    private val cols = 18; private val rows = 24
    private val fit = BoardFit()
    private val swipe = Swipe(16f)
    private val body = ArrayDeque<Int>()
    private var dir = 1; private val queue = ArrayDeque<Int>()
    private var food = 0; private var gold = -1; private var goldLeft = 0f
    private var stepTime = 0.12f; private var acc = 0f
    private var points = 0; private var started = false
    private var grow = 0
    private val fx = Particles()

    override fun newGame() {
        body.clear(); queue.clear()
        for (i in 0 until 4) body.addFirst((rows / 2) * cols + 4 + i)
        dir = 1; points = 0; started = false; grow = 0; gold = -1; acc = 0f
        stepTime = floatArrayOf(0.16f, 0.115f, 0.08f)[opt("speed")]
        placeFood(); fx.clear()
        score(0); info("Vuốt để bắt đầu")
    }

    override fun layout() { fit.fit(cols, rows, 8 * dp, 8 * dp, w - 8 * dp, h - 8 * dp) }

    private fun freeCell(): Int { while (true) { val c = Random.nextInt(cols * rows); if (c !in body && c != food && c != gold) return c } }
    private fun placeFood() { food = freeCell() }

    override fun down(x: Float, y: Float) = swipe.down(x, y)
    override fun move(x: Float, y: Float) { val d = swipe.move(x, y); if (d >= 0) turn(d) }
    override fun up(x: Float, y: Float) { val d = swipe.up(x, y); if (d >= 0) turn(d) }

    private fun turn(d: Int) {
        if (over) return
        val last = queue.lastOrNull() ?: dir
        if (d == last || (d + 2) % 4 == last) { if (!started && d != (dir + 2) % 4) started = true; return }
        if (queue.size < 3) queue.addLast(d)
        if (!started) { started = true; info("") }
    }

    override fun update(dt: Float) {
        fx.update(dt)
        if (!started) return
        if (gold >= 0) { goldLeft -= dt; if (goldLeft <= 0) gold = -1 }
        acc += dt
        while (acc >= stepTime && !over) { acc -= stepTime; step() }
    }

    private fun step() {
        if (queue.isNotEmpty()) dir = queue.removeFirst()
        val head = body.first()
        var r = head / cols; var c = head % cols
        when (dir) { 0 -> r--; 1 -> c++; 2 -> r++; 3 -> c-- }
        if (r < 0 || r >= rows || c < 0 || c >= cols) {
            if (opt("walls") == 1) { die(); return }
            r = (r + rows) % rows; c = (c + cols) % cols
        }
        val nh = r * cols + c
        if (grow == 0) body.removeLast() else grow--
        if (nh in body) { die(); return }
        body.addFirst(nh)
        if (nh == food) {
            points++; grow += 2; sfx(Sfx.POP); buzz(10)
            fx.burst(fit.cx(c), fit.cy(r), Pal.RED, 12, 160 * dp, 3 * dp)
            placeFood()
            if (gold < 0 && Random.nextFloat() < 0.2f) { gold = freeCell(); goldLeft = 5f }
            stepTime = (stepTime * 0.985f).coerceAtLeast(0.055f)
        } else if (nh == gold) {
            points += 5; grow += 3; gold = -1; sfx(Sfx.COIN)
            fx.burst(fit.cx(c), fit.cy(r), Pal.YELLOW, 18, 200 * dp, 3 * dp)
        }
        score(points)
    }

    private fun die() {
        sfx(Sfx.LOSE); buzz(60)
        gameOver(false, "Rắn bị đâm!", "Độ dài ${body.size} · $points điểm", points)
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.rrect(fit.x0 - 4 * dp, fit.y0 - 4 * dp, fit.right + 4 * dp, fit.bottom + 4 * dp, 10 * dp,
            if (opt("walls") == 1) Pal.RED else Pal.LINE)
        c.rect(fit.x0, fit.y0, fit.right, fit.bottom, 0xFF12172E.toInt())
        for (r in 0 until rows) for (col in 0 until cols) if ((r + col) % 2 == 0)
            c.rect(fit.x0 + col * cs, fit.y0 + r * cs, fit.x0 + (col + 1) * cs, fit.y0 + (r + 1) * cs, 0xFF161C38.toInt())
        // food
        c.circle(fit.cx(food % cols), fit.cy(food / cols), cs * 0.4f, Pal.RED)
        c.circle(fit.cx(food % cols) - cs * 0.12f, fit.cy(food / cols) - cs * 0.12f, cs * 0.1f, Pal.alpha(Pal.WHITE, 0.6f))
        c.rect(fit.cx(food % cols) - cs * 0.04f, fit.cy(food / cols) - cs * 0.55f, fit.cx(food % cols) + cs * 0.04f, fit.cy(food / cols) - cs * 0.3f, Pal.GREEN)
        if (gold >= 0 && (goldLeft > 1.5f || (goldLeft * 6).toInt() % 2 == 0))
            c.text("⭐", fit.cx(gold % cols), fit.cy(gold / cols), cs * 0.95f, Pal.YELLOW)
        // body: gradient from head to tail
        val n = body.size
        body.forEachIndexed { i, cell ->
            val x = fit.x0 + (cell % cols) * cs; val y = fit.y0 + (cell / cols) * cs
            val col = Pal.mix(Pal.GREEN, Pal.TEAL, i / n.toFloat())
            val pad = if (i == 0) cs * 0.02f else cs * 0.08f
            c.rrect(x + pad, y + pad, x + cs - pad, y + cs - pad, cs * 0.3f, col)
        }
        val head = body.first()
        val hx = fit.cx(head % cols); val hy = fit.cy(head / cols)
        val ex = if (dir % 2 == 0) cs * 0.2f else 0f; val ey = if (dir % 2 == 1) cs * 0.2f else 0f
        val fwdX = when (dir) { 1 -> cs * 0.15f; 3 -> -cs * 0.15f; else -> 0f }
        val fwdY = when (dir) { 2 -> cs * 0.15f; 0 -> -cs * 0.15f; else -> 0f }
        for (s in intArrayOf(-1, 1)) {
            c.circle(hx + fwdX + ex * s, hy + fwdY + ey * s, cs * 0.14f, Pal.WHITE)
            c.circle(hx + fwdX * 1.4f + ex * s, hy + fwdY * 1.4f + ey * s, cs * 0.07f, Pal.BLACK)
        }
        fx.draw(c)
        if (!started && !over) c.text("Vuốt để bắt đầu", w / 2, fit.y0 + fit.cell * rows * 0.3f, 20 * dp, Pal.alpha(Pal.WHITE, 0.85f))
    }
}
