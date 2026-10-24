package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.Gfx
import com.hgame.engine.poly
import com.hgame.engine.ring
import com.hgame.engine.text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class AsteroidStormGame : Game() {
    override val realtime = true
    override val help = "Phi thuyền ở giữa màn hình. Chạm vào đâu, phi thuyền quay về hướng đó và bắn. " +
        "Thiên thạch lớn vỡ thành mảnh nhỏ. Đừng để thiên thạch đâm vào phi thuyền!"

    private class Rock(var x: Float, var y: Float, val vx: Float, val vy: Float, val r: Float, val tier: Int, val shape: FloatArray, var rot: Float)
    private class Bullet(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float)

    private val rocks = ArrayList<Rock>(); private val bullets = ArrayList<Bullet>()
    private val fx = Particles()
    private var angle = -Math.PI.toFloat() / 2; private var aim = angle
    private var holding = false; private var fireT = 0f
    private var lives = 3; private var points = 0; private var wave = 0; private var inv = 0f; private var t = 0f
    private val stars = FloatArray(80) { Random.nextFloat() }

    override fun newGame() { rocks.clear(); bullets.clear(); fx.clear(); lives = 3; points = 0; wave = 0; inv = 2f; score(0) }

    private fun spawnRock(tier: Int, x: Float, y: Float) {
        val a = Random.nextFloat() * 6.283f
        val sp = (40 + Random.nextFloat() * 50 + wave * 6 + (2 - tier) * 30) * dp
        val r = intArrayOf(14, 24, 38)[tier] * dp
        rocks.add(Rock(x, y, cos(a) * sp, sin(a) * sp, r, tier, FloatArray(10) { 0.75f + Random.nextFloat() * 0.35f }, 0f))
    }

    private fun nextWave() {
        wave++; info("Đợt $wave · ❤ $lives")
        repeat(2 + wave) {
            val edge = Random.nextInt(4)
            val x = if (edge % 2 == 0) Random.nextFloat() * w else if (edge == 1) w + 30 * dp else -30 * dp
            val y = if (edge % 2 == 1) Random.nextFloat() * h else if (edge == 2) h + 30 * dp else -30 * dp
            spawnRock(2, x, y)
        }
    }

    override fun down(x: Float, y: Float) { holding = true; aim = atan2(y - h / 2, x - w / 2) }
    override fun move(x: Float, y: Float) { aim = atan2(y - h / 2, x - w / 2) }
    override fun up(x: Float, y: Float) { holding = false }

    override fun update(dt: Float) {
        t += dt; fx.update(dt); if (inv > 0) inv -= dt
        var d = aim - angle
        while (d > Math.PI) d -= (2 * Math.PI).toFloat(); while (d < -Math.PI) d += (2 * Math.PI).toFloat()
        angle += d * (dt * 14f).coerceAtMost(1f)
        fireT -= dt
        if (holding && fireT <= 0f) {
            fireT = 0.18f
            val sp = h * 0.9f
            bullets.add(Bullet(w / 2 + cos(angle) * 20 * dp, h / 2 + sin(angle) * 20 * dp, cos(angle) * sp, sin(angle) * sp, 1.2f))
            sfx(Sfx.SHOOT)
        }
        for (b in bullets) { b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt }
        bullets.removeAll { it.life <= 0 }
        val m = 50 * dp
        for (r in rocks) {
            r.x += r.vx * dt; r.y += r.vy * dt; r.rot += dt * 0.6f
            if (r.x < -m) r.x += w + 2 * m; if (r.x > w + m) r.x -= w + 2 * m
            if (r.y < -m) r.y += h + 2 * m; if (r.y > h + m) r.y -= h + 2 * m
        }
        val split = ArrayList<Rock>()
        val bit = bullets.iterator()
        while (bit.hasNext()) {
            val b = bit.next()
            val r = rocks.firstOrNull { it !in split && hypot(it.x - b.x, it.y - b.y) < it.r } ?: continue
            bit.remove(); split.add(r)
        }
        for (r in split) {
            rocks.remove(r)
            points += intArrayOf(100, 50, 20)[r.tier]; score(points)
            fx.burst(r.x, r.y, 0xFFBCAAA4.toInt(), 10 + r.tier * 6, 160 * dp, 3 * dp)
            sfx(if (r.tier == 2) Sfx.BOOM else Sfx.HIT)
            if (r.tier > 0) repeat(2) { spawnRock(r.tier - 1, r.x, r.y) }
        }
        if (inv <= 0f) rocks.firstOrNull { hypot(it.x - w / 2, it.y - h / 2) < it.r + 12 * dp }?.let {
            lives--; inv = 2.5f; sfx(Sfx.BOOM); buzz(100)
            fx.burst(w / 2, h / 2, Pal.ORANGE, 30, 240 * dp, 4 * dp)
            info("Đợt $wave · ❤ $lives")
            if (lives <= 0) gameOver(false, "Phi thuyền vỡ tan!", "Đợt $wave · $points điểm", points)
        }
        if (rocks.isEmpty()) nextWave()
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF0B0E22.toInt(), 0xFF1A0F2E.toInt())
        for (i in 0 until 40) c.circle(stars[i] * w, stars[i + 40] * h, (0.6f + (i % 3) * 0.5f) * dp, Pal.alpha(Pal.WHITE, 0.3f + (i % 4) * 0.15f))
        val p = Gfx.path
        for (r in rocks) {
            p.reset()
            for (k in 0 until 10) {
                val a = r.rot + k * 0.6283f
                val px = r.x + cos(a) * r.r * r.shape[k]; val py = r.y + sin(a) * r.r * r.shape[k]
                if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close()
            c.drawPath(p, Gfx.fillPaint(0xFF8D6E63.toInt()))
            c.circle(r.x - r.r * 0.3f, r.y - r.r * 0.2f, r.r * 0.22f, 0xFF6D4C41.toInt())
            c.circle(r.x + r.r * 0.3f, r.y + r.r * 0.3f, r.r * 0.15f, 0xFF6D4C41.toInt())
        }
        for (b in bullets) { c.circle(b.x, b.y, 4 * dp, Pal.alpha(Pal.CYAN, 0.4f)); c.circle(b.x, b.y, 2.2f * dp, Pal.WHITE) }
        if (inv <= 0f || (inv * 10).toInt() % 2 == 0) {
            val cx = w / 2; val cy = h / 2; val s = 18 * dp
            c.save(); c.rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f, cx, cy)
            if (holding) c.poly(Pal.ORANGE, cx - s * 0.3f, cy + s * 0.6f, cx + s * 0.3f, cy + s * 0.6f, cx, cy + s * (1.1f + 0.2f * sin(t * 40)))
            c.poly(Pal.CYAN, cx, cy - s, cx + s * 0.75f, cy + s * 0.7f, cx, cy + s * 0.35f, cx - s * 0.75f, cy + s * 0.7f)
            c.circle(cx, cy - s * 0.1f, s * 0.2f, Pal.WHITE)
            c.restore()
        }
        if (inv > 0f) c.ring(w / 2, h / 2, 30 * dp, 2 * dp, Pal.alpha(Pal.CYAN, 0.5f))
        fx.draw(c)
        for (i in 0 until lives) c.text("❤", 16 * dp + i * 18 * dp, 20 * dp, 14 * dp, Pal.PINK)
    }
}
