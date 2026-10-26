package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.rrect
import com.hgame.engine.text
import com.hgame.engine.tile
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class BreakoutGame : Game() {
    override val realtime = true
    override val help = "Kéo ngón tay để di chuyển thanh đỡ, giữ bóng không rơi. Phá hết gạch để qua màn. " +
        "Gạch có viền sáng cần 2 lần đập. Vật phẩm rơi ra: 🟢 thanh dài, 🔵 nhân bóng, ❤ thêm mạng."

    private class Ball(var x: Float, var y: Float, var vx: Float, var vy: Float)
    private class Brick(val r: Int, val c: Int, var hp: Int)
    private class Drop(var x: Float, var y: Float, val kind: Int)

    private val balls = ArrayList<Ball>()
    private val bricks = ArrayList<Brick>()
    private val drops = ArrayList<Drop>()
    private val fx = Particles()
    private var padX = 0f; private var padW = 0f; private var padY = 0f; private var wideT = 0f
    private var lives = 3; private var level = 1; private var points = 0
    private var stuck = true
    private var bw = 0f; private var bh = 0f; private var top = 0f
    private val rowsN = 8; private val colsN = 8
    private var br = 0f; private var speed = 0f

    override fun newGame() { lives = 3; level = 1; points = 0; score(0); buildLevel() }

    private fun buildLevel() {
        bricks.clear(); drops.clear(); balls.clear(); fx.clear()
        val pattern = (level - 1) % 4
        for (r in 0 until rowsN) for (c in 0 until colsN) {
            val keep = when (pattern) {
                0 -> true
                1 -> (r + c) % 2 == 0 || r < 2
                2 -> abs(c - 3.5f) <= r / 2f + 0.5f
                else -> r % 3 != 2
            }
            if (keep) bricks.add(Brick(r, c, if (r < 2 && level > 1) 2 else 1))
        }
        stuck = true; wideT = 0f
        info("Màn $level · ❤ $lives")
    }

    override fun layout() {
        bw = (w - 16 * dp) / colsN; bh = bw * 0.42f; top = 50 * dp
        padY = h - 60 * dp; padW = w * 0.22f; if (padX == 0f) padX = w / 2
        br = 7 * dp; speed = h * 0.62f
    }

    private fun spawnBall() {
        val a = -Math.PI / 2 + (Random.nextFloat() - 0.5f) * 0.8
        val s = speed * (1f + (level - 1) * 0.08f)
        balls.add(Ball(padX, padY - br - 2, (cos(a) * s).toFloat(), (sin(a) * s).toFloat()))
    }

    override fun down(x: Float, y: Float) { padX = x.coerceIn(curPadW() / 2, w - curPadW() / 2) }
    override fun move(x: Float, y: Float) { padX = x.coerceIn(curPadW() / 2, w - curPadW() / 2) }
    override fun up(x: Float, y: Float) { if (stuck && !over) { stuck = false; spawnBall(); sfx(Sfx.TAP) } }

    private fun curPadW() = if (wideT > 0) padW * 1.6f else padW

    override fun update(dt: Float) {
        fx.update(dt)
        if (wideT > 0) wideT -= dt
        if (stuck) return
        val steps = 3; val sdt = dt / steps
        repeat(steps) { balls.toList().forEach { step(it, sdt) } }
        val it = drops.iterator()
        while (it.hasNext()) {
            val d = it.next(); d.y += 180 * dp * dt
            if (d.y > padY - 8 * dp && d.y < padY + 12 * dp && abs(d.x - padX) < curPadW() / 2 + 10 * dp) {
                when (d.kind) {
                    0 -> wideT = 10f
                    1 -> balls.toList().take(2).forEach { b -> balls.add(Ball(b.x, b.y, -b.vx, b.vy)) }
                    else -> lives++
                }
                sfx(Sfx.COIN); it.remove(); info("Màn $level · ❤ $lives")
            } else if (d.y > h) it.remove()
        }
        if (balls.isEmpty()) {
            lives--; sfx(Sfx.LOSE); buzz(50)
            if (lives <= 0) { gameOver(false, "Hết mạng!", "Màn $level · $points điểm", points); return }
            stuck = true; info("Màn $level · ❤ $lives")
        }
        if (bricks.isEmpty()) {
            level++; points += 100; score(points); sfx(Sfx.WIN); buildLevel()
        }
    }

    private fun step(b: Ball, dt: Float) {
        b.x += b.vx * dt; b.y += b.vy * dt
        if (b.x < br) { b.x = br; b.vx = abs(b.vx) }
        if (b.x > w - br) { b.x = w - br; b.vx = -abs(b.vx) }
        if (b.y < br) { b.y = br; b.vy = abs(b.vy) }
        if (b.y > h + br) { balls.remove(b); return }
        val pw = curPadW()
        if (b.vy > 0 && b.y + br >= padY && b.y - br <= padY + 12 * dp && abs(b.x - padX) <= pw / 2 + br) {
            val rel = ((b.x - padX) / (pw / 2)).coerceIn(-1f, 1f)
            val a = -Math.PI / 2 + rel * 1.05
            val s = hypot(b.vx, b.vy)
            b.vx = (cos(a) * s).toFloat(); b.vy = (sin(a) * s).toFloat(); b.y = padY - br
            sfx(Sfx.TICK)
        }
        val bit = bricks.iterator()
        while (bit.hasNext()) {
            val k = bit.next()
            val l = 8 * dp + k.c * bw; val t = top + k.r * bh
            val nx = b.x.coerceIn(l, l + bw); val ny = b.y.coerceIn(t, t + bh)
            if (hypot(b.x - nx, b.y - ny) <= br) {
                val overlapX = minOf(abs(b.x - l), abs(b.x - l - bw)); val overlapY = minOf(abs(b.y - t), abs(b.y - t - bh))
                if (overlapX < overlapY) b.vx = -b.vx else b.vy = -b.vy
                k.hp--
                if (k.hp <= 0) {
                    bit.remove(); points += 10; score(points); sfx(Sfx.POP)
                    fx.burst(l + bw / 2, t + bh / 2, rowColor(k.r), 10, 150 * dp, 3 * dp)
                    if (Random.nextFloat() < 0.12f) drops.add(Drop(l + bw / 2, t + bh / 2, if (Random.nextFloat() < 0.2f) 2 else Random.nextInt(2)))
                } else sfx(Sfx.HIT)
                val s = hypot(b.vx, b.vy) * 1.003f; val a = Math.atan2(b.vy.toDouble(), b.vx.toDouble())
                b.vx = (cos(a) * s).toFloat(); b.vy = (sin(a) * s).toFloat()
                break
            }
        }
        // avoid near-horizontal trajectories
        if (abs(b.vy) < abs(b.vx) * 0.25f) b.vy = if (b.vy < 0) -abs(b.vx) * 0.3f else abs(b.vx) * 0.3f
    }

    private fun rowColor(r: Int) = intArrayOf(Pal.RED, Pal.ORANGE, Pal.YELLOW, Pal.GREEN, Pal.TEAL, Pal.CYAN, Pal.BLUE, Pal.PURPLE)[r % 8]

    override fun draw(c: Canvas) {
        c.background(w, h)
        for (k in bricks) {
            val l = 8 * dp + k.c * bw; val t = top + k.r * bh
            c.tile(l + 2, t + 2, l + bw - 2, t + bh - 2, bh * 0.25f, rowColor(k.r), bh * 0.12f)
            if (k.hp > 1) c.rrect(l + bw * 0.3f, t + bh * 0.35f, l + bw * 0.7f, t + bh * 0.55f, bh * 0.1f, Pal.alpha(Pal.WHITE, 0.7f))
        }
        for (d in drops) {
            val col = intArrayOf(Pal.GREEN, Pal.CYAN, Pal.PINK)[d.kind]
            c.rrect(d.x - 14 * dp, d.y - 8 * dp, d.x + 14 * dp, d.y + 8 * dp, 8 * dp, col)
            c.text(arrayOf("↔", "×2", "❤")[d.kind], d.x, d.y, 11 * dp, Pal.WHITE)
        }
        val pw = curPadW()
        c.rrect(padX - pw / 2, padY + 3 * dp, padX + pw / 2, padY + 15 * dp, 6 * dp, Pal.shade(Pal.ACCENT, 0.5f))
        c.rrect(padX - pw / 2, padY, padX + pw / 2, padY + 12 * dp, 6 * dp, if (wideT > 0) Pal.GREEN else Pal.ACCENT)
        if (stuck) c.ball(padX, padY - br - 2, br, Pal.WHITE)
        for (b in balls) c.ball(b.x, b.y, br, Pal.WHITE)
        fx.draw(c)
        for (i in 0 until lives) c.text("❤", 16 * dp + i * 18 * dp, 22 * dp, 14 * dp, Pal.PINK)
        if (stuck && !over) c.text("Chạm để phóng bóng", w / 2, h * 0.62f, 18 * dp, Pal.alpha(Pal.WHITE, 0.85f))
    }
}
