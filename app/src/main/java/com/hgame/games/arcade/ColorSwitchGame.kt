package com.hgame.games.arcade

import android.graphics.Canvas
import android.graphics.RectF
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.random.Random

class ColorSwitchGame : Game() {
    override val realtime = true
    override val help = "Chạm để quả bóng nảy lên. Bóng chỉ được đi qua phần chướng ngại cùng màu với nó. " +
        "Chạm vào viên đổi màu 🌈 để bóng đổi màu, ăn ⭐ để ghi điểm."

    private val cols = intArrayOf(0xFF35E2F2.toInt(), 0xFFF6DF0E.toInt(), 0xFF8C13FB.toInt(), 0xFFFF0080.toInt())
    private class Obs(val y: Float, val type: Int, val speed: Float, var star: Boolean = true, var switcher: Boolean = true)
    private val obs = ArrayList<Obs>()
    private var by = 0f; private var vy = 0f; private var color = 0; private var cam = 0f; private var t = 0f
    private var started = false; private var points = 0
    private val fx = Particles()
    private val arc = RectF()

    override fun newGame() {
        obs.clear(); fx.clear(); by = 0f; vy = 0f; cam = 0f; started = false; points = 0; color = Random.nextInt(4)
        var y = -6f
        repeat(4) { addObs(y); y -= 7f }
        score(0)
    }

    private fun addObs(y: Float) = obs.add(Obs(y, Random.nextInt(3), (if (Random.nextBoolean()) 1 else -1) * (70f + points * 3f).coerceAtMost(160f)))

    private val u get() = w / 10f
    private fun sy(y: Float) = h * 0.65f + (y - cam) * u

    override fun down(x: Float, y: Float) { if (!over) { started = true; vy = -9.5f; sfx(Sfx.JUMP) } }

    override fun update(dt: Float) {
        t += dt; fx.update(dt)
        if (!started) return
        vy += 26f * dt; by += vy * dt
        if (by - cam < -3f) cam = by + 3f
        if (sy(by) > h + 20 * dp) { die(); return }
        for (o in obs) {
            if (o.star && abs(by - o.y) < 0.5f) { o.star = false; points++; score(points); sfx(Sfx.COIN) }
            if (o.switcher && abs(by - (o.y - 3.5f)) < 0.5f) {
                o.switcher = false; var n: Int; do { n = Random.nextInt(4) } while (n == color); color = n; sfx(Sfx.POP)
            }
            val hitColor = collide(o) ?: continue
            if (hitColor != color) { die(); return }
        }
        if (obs.first().y - cam > 8f) { obs.removeAt(0); addObs(obs.last().y - 7f) }
    }

    /** Returns the colour of the obstacle part touching the ball, or null when it is clear. */
    private fun collide(o: Obs): Int? {
        val ang = t * o.speed
        val r = 0.3f
        when (o.type) {
            0, 1 -> { // ring: outer radius 2.4 thickness .35, ball at x=0
                val rad = if (o.type == 0) 2.4f else 1.7f
                val dy = by - o.y
                for (side in intArrayOf(-1, 1)) {
                    val edge = side * rad
                    if (abs(dy - edge) < 0.18f + r) {
                        // angle of that point: top (-90) or bottom (90)
                        val a = if (side < 0) 270f else 90f
                        val rel = (((a - ang) % 360f) + 360f) % 360f
                        return cols[(rel / 90f).toInt() % 4]
                    }
                }
            }
            2 -> { // horizontal bar of 4 colours sliding
                if (abs(by - o.y) < 0.2f + r) {
                    val shift = ((t * o.speed / 40f) % 4f + 4f) % 4f
                    val segW = 10f / 4f
                    val xRel = ((5f + shift * segW) % 10f + 10f) % 10f
                    return cols[(xRel / segW).toInt() % 4]
                }
            }
        }
        return null
    }

    private fun die() {
        fx.burst(w / 2, sy(by), cols[color], 30, 260 * dp, 4 * dp)
        sfx(Sfx.LOSE); buzz(70)
        gameOver(false, "Sai màu!", "Bạn đạt $points ⭐", points)
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF1D1D2C.toInt(), 0xFF101018.toInt())
        val cx = w / 2
        val stroke = Gfx.stroke.apply { shader = null; strokeWidth = 0.36f * u; strokeCap = android.graphics.Paint.Cap.BUTT }
        for (o in obs) {
            val y = sy(o.y); val ang = t * o.speed
            if (y < -4 * u || y > h + 4 * u) continue
            when (o.type) {
                0, 1 -> {
                    val rad = (if (o.type == 0) 2.4f else 1.7f) * u
                    arc.set(cx - rad, y - rad, cx + rad, y + rad)
                    for (k in 0 until 4) { stroke.color = cols[k]; c.drawArc(arc, ang + k * 90f, 90f, false, stroke) }
                }
                2 -> {
                    val segW = w / 4f
                    val shift = ((t * o.speed / 40f) % 4f + 4f) % 4f * segW
                    for (k in -1..4) {
                        val x = k * segW - shift
                        val idx = Math.floorMod(k, 4)
                        c.rect(x, y - 0.2f * u, x + segW, y + 0.2f * u, cols[idx])
                    }
                }
            }
            if (o.star) c.text("⭐", cx, y, u * 0.8f, Pal.WHITE)
            if (o.switcher) {
                val sy2 = sy(o.y - 3.5f)
                for (k in 0 until 4) {
                    arc.set(cx - u * 0.35f, sy2 - u * 0.35f, cx + u * 0.35f, sy2 + u * 0.35f)
                    c.drawArc(arc, k * 90f, 90f, true, Gfx.fillPaint(cols[k]))
                }
            }
        }
        if (!over) c.ball(cx, sy(by), 0.3f * u, cols[color])
        fx.draw(c)
        c.text("$points", 24 * dp, 30 * dp, 24 * dp, Pal.WHITE, android.graphics.Paint.Align.LEFT)
        if (!started) c.text("Chạm để nảy", cx, sy(0f) + 2 * u, 18 * dp, Pal.WHITE)
    }
}
