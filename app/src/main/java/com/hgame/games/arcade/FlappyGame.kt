package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.circle
import com.hgame.engine.gradRect
import com.hgame.engine.poly
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class FlappyGame : Game() {
    override val realtime = true
    override val help = "Chạm để chú chim vỗ cánh bay lên. Luồn qua khe giữa các ống, mỗi ống được 1 điểm. Chạm ống hoặc mặt đất là thua."

    private class Pipe(var x: Float, val gapY: Float, var passed: Boolean = false)

    private val pipes = ArrayList<Pipe>()
    private var by = 0f; private var vy = 0f; private var bx = 0f
    private var started = false; private var points = 0
    private var groundY = 0f; private var gap = 0f; private var pw = 0f; private var scroll = 0f
    private var t = 0f
    private val fx = Particles()
    private val clouds = FloatArray(10) { Random.nextFloat() }

    override fun newGame() {
        pipes.clear(); started = false; points = 0; vy = 0f; fx.clear()
        by = h * 0.42f; score(0)
    }

    override fun layout() {
        groundY = h - 70 * dp; bx = w * 0.3f; gap = h * 0.24f; pw = 62 * dp
        if (!started) by = h * 0.42f
    }

    override fun down(x: Float, y: Float) {
        if (over) return
        if (!started) { started = true; spawn(w + 40 * dp) }
        vy = -h * 0.75f; sfx(Sfx.JUMP)
    }

    private fun spawn(x: Float) {
        val margin = 60 * dp
        pipes.add(Pipe(x, margin + gap / 2 + Random.nextFloat() * (groundY - 2 * margin - gap)))
    }

    override fun update(dt: Float) {
        t += dt; fx.update(dt)
        val speed = w * 0.42f * (1f + points * 0.01f).coerceAtMost(1.5f)
        scroll += speed * dt
        if (!started) { by = h * 0.42f + sin(t * 4) * 8 * dp; return }
        vy += h * 2.1f * dt; by += vy * dt
        for (p in pipes) {
            p.x -= speed * dt
            if (!p.passed && p.x + pw < bx) { p.passed = true; points++; score(points); sfx(Sfx.COIN) }
        }
        pipes.removeAll { it.x < -pw }
        if (pipes.isEmpty() || pipes.last().x < w - w * 0.55f) spawn(w + pw)
        val r = 15 * dp
        if (by + r > groundY || by < -50 * dp) return crash()
        for (p in pipes) if (bx + r > p.x && bx - r < p.x + pw && (by - r < p.gapY - gap / 2 || by + r > p.gapY + gap / 2)) return crash()
    }

    private fun crash() {
        fx.burst(bx, by, Pal.YELLOW, 20, 220 * dp, 4 * dp)
        sfx(Sfx.HIT); buzz(60)
        gameOver(false, "Ối, đâm rồi!", "Bạn vượt qua $points ống", points)
    }

    override fun draw(c: Canvas) {
        c.gradRect(0f, 0f, w, h, 0f, 0xFF4FC3F7.toInt(), 0xFFB3E5FC.toInt())
        for (i in 0 until 5) {
            val cx = ((clouds[i] * w * 1.5f - scroll * 0.2f) % (w * 1.5f) + w * 1.5f) % (w * 1.5f) - w * 0.25f
            val cy = 60 * dp + clouds[i + 5] * h * 0.35f
            c.circle(cx, cy, 22 * dp, Pal.alpha(Pal.WHITE, 0.85f)); c.circle(cx + 22 * dp, cy + 4 * dp, 18 * dp, Pal.alpha(Pal.WHITE, 0.85f)); c.circle(cx - 20 * dp, cy + 6 * dp, 15 * dp, Pal.alpha(Pal.WHITE, 0.85f))
        }
        // distant hills
        val hs = scroll * 0.4f
        for (i in -1..4) {
            val hx = i * w * 0.4f - hs % (w * 0.4f)
            c.circle(hx, groundY + 20 * dp, w * 0.25f, 0xFF81C784.toInt())
        }
        for (p in pipes) {
            val topB = p.gapY - gap / 2; val botT = p.gapY + gap / 2
            c.gradRect(p.x, -10f, p.x + pw, topB, 0f, 0xFF43A047.toInt(), 0xFF66BB6A.toInt())
            c.rrect(p.x - 5 * dp, topB - 26 * dp, p.x + pw + 5 * dp, topB, 5 * dp, 0xFF2E7D32.toInt())
            c.gradRect(p.x, botT, p.x + pw, groundY, 0f, 0xFF66BB6A.toInt(), 0xFF43A047.toInt())
            c.rrect(p.x - 5 * dp, botT, p.x + pw + 5 * dp, botT + 26 * dp, 5 * dp, 0xFF2E7D32.toInt())
            c.rect(p.x + pw * 0.15f, -10f, p.x + pw * 0.28f, topB - 26 * dp, Pal.alpha(Pal.WHITE, 0.25f))
            c.rect(p.x + pw * 0.15f, botT + 26 * dp, p.x + pw * 0.28f, groundY, Pal.alpha(Pal.WHITE, 0.25f))
        }
        c.rect(0f, groundY, w, h, 0xFFDEB887.toInt())
        c.rect(0f, groundY, w, groundY + 14 * dp, 0xFF7CB342.toInt())
        val stripe = 24 * dp
        var sx = -(scroll % stripe)
        while (sx < w) { c.poly(0xFF689F38.toInt(), sx, groundY + 14 * dp, sx + stripe / 2, groundY + 14 * dp, sx + stripe, groundY, sx + stripe / 2, groundY); sx += stripe }
        // bird
        val r = 15 * dp
        c.save()
        val ang = if (!started) 0f else (vy / (h * 0.9f) * 60f).coerceIn(-25f, 80f)
        c.rotate(ang, bx, by)
        c.circle(bx, by, r, Pal.YELLOW)
        c.circle(bx - r * 0.1f, by + r * 0.35f, r * 0.55f, 0xFFFFE082.toInt())
        val flap = if (abs(sin(t * 18)) > 0.5f && (vy < 0 || !started)) -r * 0.5f else r * 0.1f
        c.poly(0xFFF9A825.toInt(), bx - r * 0.9f, by, bx - r * 0.1f, by, bx - r * 0.5f, by + flap)
        c.circle(bx + r * 0.4f, by - r * 0.3f, r * 0.32f, Pal.WHITE); c.circle(bx + r * 0.5f, by - r * 0.3f, r * 0.15f, Pal.BLACK)
        c.poly(Pal.ORANGE, bx + r * 0.7f, by - r * 0.05f, bx + r * 1.35f, by + r * 0.15f, bx + r * 0.7f, by + r * 0.35f)
        c.restore()
        fx.draw(c)
        c.text("$points", w / 2, 60 * dp, 44 * dp, Pal.WHITE)
        if (!started && !over) c.text("Chạm để bay", w / 2, h * 0.62f, 22 * dp, 0xFF1A237E.toInt())
    }
}
